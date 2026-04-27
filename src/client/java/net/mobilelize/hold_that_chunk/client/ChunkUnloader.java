package net.mobilelize.hold_that_chunk.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.mobilelize.hold_that_chunk.client.config.ConfigManager;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkUnloader {

    private final Set<ChunkPos> pendingUnloads = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos> processedUnloads = ConcurrentHashMap.newKeySet();

    private final Map<ChunkPos, ClientboundLevelChunkWithLightPacket> clearedChunks = new ConcurrentHashMap<>();
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

    public void markCleared(ChunkPos pos, ClientboundLevelChunkWithLightPacket packet) { Minecraft.getInstance().execute(() -> clearedChunks.put(pos, packet)); }
    public void unmarkCleared(ChunkPos pos) { clearedChunks.remove(pos); }

    public boolean shouldCancelEmptyChunk(ChunkPos pos) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return false;
        if (!ConfigManager.configData.cancelEmptyChunks) return false;
        if (!ConfigManager.configData.holdThatChunkEnabled) return false;
        if (ConfigManager.configData.respectServerDistance) return false;

        if (!isChunkLoaded(pos)) return false;

        if (closeToEmptyChunk(pos)) return false;

        return farEnoughFromEmptyChunk(pos);
    }

    public boolean closeToEmptyChunk(ChunkPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return true;
        int dist = pos.getChessboardDistance(client.player.chunkPosition());
        int prox = Math.max(2, ConfigManager.configData.restoreEmptyChunksDistance);
        return dist <= prox;
    }

    public boolean farEnoughFromEmptyChunk(ChunkPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return true;
        int dist = pos.getChessboardDistance(client.player.chunkPosition());
        int prox = Math.min(256, ConfigManager.configData.ignoreEmptyChunksDistance);
        return dist >= prox;
    }

    private boolean isChunkLoaded(ChunkPos pos) {
        var client = Minecraft.getInstance();
        assert client.level != null;
        var cm = client.level.getChunkSource();
        return cm.hasChunk(pos.x(), pos.z());
    }

    public void onUnloadPacket(ClientboundForgetLevelChunkPacket packet) {
        Minecraft.getInstance().execute(() -> pendingUnloads.add(packet.pos()));
    }

    public void processUnloads() {
        if (pendingUnloads.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || client.player == null) return;

        ChunkPos playerPos = client.player.chunkPosition();

        Iterator<ChunkPos> it = pendingUnloads.iterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();

            boolean isOutsideDistance = pos.getChessboardDistance(playerPos) > getHoldDistance();
            boolean respectServerDistance = ConfigManager.configData.respectServerDistance;
            boolean modEnabled = ConfigManager.configData.holdThatChunkEnabled;

            if (!modEnabled || isOutsideDistance || respectServerDistance) {
                processedUnloads.add(pos);
                client.getConnection().handleForgetLevelChunk(new ClientboundForgetLevelChunkPacket(pos));
                it.remove();
            }
        }
    }

    public void processEmptyLoads() {
        if (clearedChunks.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || client.level == null || client.player == null) return;
        Iterator<Map.Entry<ChunkPos, ClientboundLevelChunkWithLightPacket>> it = clearedChunks.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<ChunkPos, ClientboundLevelChunkWithLightPacket> empty = it.next();

            boolean emptyChunks = ConfigManager.configData.cancelEmptyChunks;
            boolean modEnabled = ConfigManager.configData.holdThatChunkEnabled;
            if (!modEnabled || !emptyChunks || closeToEmptyChunk(empty.getKey())) {
                processedClears.add(empty.getKey());
                client.getConnection().handleLevelChunkWithLight(empty.getValue());
                it.remove();
            }
        }
    }

    public void loadEmpty(ChunkPos pos) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.getConnection() == null || client.level == null || client.player == null) return;
            if (clearedChunks.containsKey(pos)) {
                ClientboundLevelChunkWithLightPacket remove = clearedChunks.remove(pos);
                processedClears.add(pos);
                client.getConnection().handleLevelChunkWithLight(remove);
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
        if (ConfigManager.configData.linkRenderDistance) return Minecraft.getInstance().options.renderDistance().get();

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
