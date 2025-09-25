package net.mobilelize.hold_that_chunk.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.mobilelize.hold_that_chunk.client.config.ConfigManager;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkUnloader {

    private final Set<ChunkPos> pendingUnloads = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos> processedUnloads = ConcurrentHashMap.newKeySet();

    private final Map<ChunkPos, ChunkDataS2CPacket> clearedChunks = new ConcurrentHashMap<>();
    private final Set<ChunkPos> processedClears = ConcurrentHashMap.newKeySet();

    private int originalServerRenderDistance = 128;

    public ChunkUnloader() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> processUnloads());
        ClientTickEvents.END_CLIENT_TICK.register(client -> processEmptyLoads());
    }

    public void setOriginalServerRenderDistance(int originalServerRenderDistance) {
        this.originalServerRenderDistance = originalServerRenderDistance;
    }

    public int getOriginalServerRenderDistance() {
        return originalServerRenderDistance;
    }

    public void markCleared(ChunkPos pos, ChunkDataS2CPacket packet) { MinecraftClient.getInstance().execute(() -> clearedChunks.put(pos, packet)); }
    public void unmarkCleared(ChunkPos pos) { clearedChunks.remove(pos); }

    public boolean shouldCancelEmptyChunk(ChunkPos pos) {
        var client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return false;
        if (!ConfigManager.configData.cancelEmptyChunks) return false;
        if (!ConfigManager.configData.holdThatChunkEnabled) return false;
        if (ConfigManager.configData.respectServerDistance) return false;

        if (!isChunkLoaded(pos)) return false;

        if (closeToEmptyChunk(pos)) return false;

        return farEnoughFromEmptyChunk(pos);
    }

    public boolean closeToEmptyChunk(ChunkPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return true;
        int dist = pos.getChebyshevDistance(client.player.getChunkPos());
        int prox = Math.max(2, ConfigManager.configData.restoreEmptyChunksDistance);
        return dist <= prox;
    }

    public boolean farEnoughFromEmptyChunk(ChunkPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return true;
        int dist = pos.getChebyshevDistance(client.player.getChunkPos());
        int prox = Math.min(256, ConfigManager.configData.ignoreEmptyChunksDistance);
        return dist >= prox;
    }

    private boolean isChunkLoaded(ChunkPos pos) {
        var client = MinecraftClient.getInstance();
        assert client.world != null;
        var cm = client.world.getChunkManager();
        return cm != null && cm.isChunkLoaded(pos.x, pos.z);
    }

    /**
     * Called when the server sends an unload packet.
     * Instead of unloading immediately, we save the chunk+packet.
     */
    public void onUnloadPacket(UnloadChunkS2CPacket packet) {
        MinecraftClient.getInstance().execute(() -> pendingUnloads.add(packet.pos()));
    }

    /**
     * Called periodically (e.g. each tick).
     * Goes through saved chunks and unloads any that are too far away.
     */
    public void processUnloads() {
        if (pendingUnloads.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.player == null) return;

        ChunkPos playerPos = client.player.getChunkPos();

        Iterator<ChunkPos> it = pendingUnloads.iterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();

            boolean isOutsideDistance = pos.getChebyshevDistance(playerPos) > getHoldDistance();
            boolean respectServerDistance = ConfigManager.configData.respectServerDistance;
            boolean modEnabled = ConfigManager.configData.holdThatChunkEnabled;

            // Use chessboard distance (same as vanilla chunk distance logic)
            if (!modEnabled || isOutsideDistance || respectServerDistance) {
                processedUnloads.add(pos);
                client.getNetworkHandler().onUnloadChunk(new UnloadChunkS2CPacket(pos));
                it.remove();
            }
        }
    }

    public void processEmptyLoads() {
        if (clearedChunks.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.world == null || client.player == null) return;
        Iterator<Map.Entry<ChunkPos, ChunkDataS2CPacket>> it = clearedChunks.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<ChunkPos, ChunkDataS2CPacket> empty = it.next();

            boolean emptyChunks = ConfigManager.configData.cancelEmptyChunks;
            boolean modEnabled = ConfigManager.configData.holdThatChunkEnabled;
            if (!modEnabled || !emptyChunks || closeToEmptyChunk(empty.getKey())) {
                processedClears.add(empty.getKey());
                client.getNetworkHandler().onChunkData(empty.getValue());
                it.remove();
            }
        }
    }

    public void loadEmpty(ChunkPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.getNetworkHandler() == null || client.world == null || client.player == null) return;
            if (clearedChunks.containsKey(pos)) {
                ChunkDataS2CPacket remove = clearedChunks.remove(pos);
                processedClears.add(pos);
                client.getNetworkHandler().onChunkData(remove);
            }
        });
    }

    public boolean isBeingProcessedRemove(ChunkPos pos) {
        if (processedUnloads.contains(pos)) {
            processedUnloads.remove(pos);
            return true;
        }
        return false;
    }

    public boolean isEmptyBeingProcessedRemove(ChunkPos pos) {
        if (processedClears.contains(pos)) {
            processedClears.remove(pos);
            return true;
        }
        return false;
    }

    private int getHoldDistance() {
        if (ConfigManager.configData.linkRenderDistance) return MinecraftClient.getInstance().options.getViewDistance().getValue();

        return ConfigManager.configData.holdDistance;
    }

    public void clear() {
        pendingUnloads.clear();
        processedUnloads.clear();
        clearedChunks.clear();
        processedClears.clear();
    }

    public void removePending(ChunkPos pos) {
        pendingUnloads.remove(pos);
        processedUnloads.remove(pos);
    }

}

