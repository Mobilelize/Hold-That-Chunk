package net.mobilelize.hold_that_chunk.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.mobilelize.hold_that_chunk.client.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkUnloader {

    private final Map<ChunkPos, UnloadChunkS2CPacket> pendingUnloads = new ConcurrentHashMap<>();
    private final Set<ChunkPos> processedUnloads = new HashSet<>();
    private int originalServerRenderDistance = 128;

    public ChunkUnloader() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> processUnloads());
    }

    public void setOriginalServerRenderDistance(int originalServerRenderDistance) {
        this.originalServerRenderDistance = originalServerRenderDistance;
    }

    /**
     * Called when the server sends an unload packet.
     * Instead of unloading immediately, we save the chunk+packet.
     */
    public void onUnloadPacket(UnloadChunkS2CPacket packet) {
        MinecraftClient.getInstance().execute(() -> pendingUnloads.put(packet.pos(), packet));
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

        Iterator<Map.Entry<ChunkPos, UnloadChunkS2CPacket>> it = pendingUnloads.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkPos, UnloadChunkS2CPacket> entry = it.next();
            ChunkPos pos = entry.getKey();
            UnloadChunkS2CPacket packet = entry.getValue();

            boolean isOutsideDistance = pos.getChebyshevDistance(playerPos) > getHoldDistance();
            boolean respectServerDistance = ConfigManager.configData.respectServerDistance && pos.getChebyshevDistance(playerPos) > originalServerRenderDistance;

            // Use chessboard distance (same as vanilla chunk distance logic)
            if (isOutsideDistance || respectServerDistance) {
                processedUnloads.add(pos);
                client.getNetworkHandler().onUnloadChunk(packet);
                it.remove();
            }
        }
    }

    public boolean isBeingProcessedRemove(ChunkPos pos) {
        if (processedUnloads.contains(pos)) {
            processedUnloads.remove(pos);
            return true;
        }
        return false;
    }

    private int getHoldDistance() {
        if (ConfigManager.configData.linkRenderDistance && !ConfigManager.configData.keepChunksLoaded) return MinecraftClient.getInstance().options.getViewDistance().getValue();

        return Math.max(ConfigManager.configData.holdDistance, MinecraftClient.getInstance().options.getViewDistance().getValue());
    }

    public void clear() {
        pendingUnloads.clear();
        processedUnloads.clear();
    }

    public void removePending(ChunkPos pos) {
        pendingUnloads.remove(pos);
        processedUnloads.remove(pos);
    }

}

