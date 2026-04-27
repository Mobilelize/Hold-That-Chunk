package net.mobilelize.hold_that_chunk.client.mixin;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.ChunkPos;
import net.mobilelize.hold_that_chunk.client.Hold_that_chunkClient;
import net.mobilelize.hold_that_chunk.client.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "handleForgetLevelChunk", at = @At("HEAD"), cancellable = true)
    private void holdThatChunk(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        if (ConfigManager.configData.holdThatChunkEnabled && !ConfigManager.configData.respectServerDistance && !Hold_that_chunkClient.chunkUnloader.isBeingProcessedRemove(packet.pos())) {
            Hold_that_chunkClient.chunkUnloader.onUnloadPacket(packet);
            ci.cancel();
        }
        Hold_that_chunkClient.chunkUnloader.unmarkCleared(packet.pos());
    }

    @Redirect(method = "handleSetChunkCacheRadius", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundSetChunkCacheRadiusPacket;getRadius()I"))
    private int setServerDistance(ClientboundSetChunkCacheRadiusPacket instance) {
        Hold_that_chunkClient.chunkUnloader.setOriginalServerRenderDistance(instance.getRadius());
        if (ConfigManager.configData.holdThatChunkEnabled && ConfigManager.configData.ignoreServerDistance) return 256;
        return instance.getRadius();
    }

    @Inject(method = "startWaitingForNewLevel", at = @At("HEAD"))
    public void loadingWorld(LocalPlayer player, ClientLevel world, LevelLoadingScreen.Reason reason, CallbackInfo ci) {
        Hold_that_chunkClient.chunkUnloader.clear();
    }

    @Inject(method = "handleLogin", at = @At("HEAD"))
    public void joiningGame(ClientboundLoginPacket packet, CallbackInfo ci) {
        Hold_that_chunkClient.chunkUnloader.clear();
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("HEAD"), cancellable = true)
    public void onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());
        Hold_that_chunkClient.chunkUnloader.removePending(pos);

        if (isPacketEffectivelyEmpty(packet)
                && Hold_that_chunkClient.chunkUnloader.shouldCancelEmptyChunk(pos)
                && !Hold_that_chunkClient.chunkUnloader.isEmptyBeingProcessedRemove(new ChunkPos(packet.getX(), packet.getZ()))) {
            Hold_that_chunkClient.chunkUnloader.markCleared(pos, packet);
            ci.cancel();
            return;
        }

        Hold_that_chunkClient.chunkUnloader.unmarkCleared(pos);
    }

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    public void onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        ChunkPos chunkPos = ChunkPos.containing(packet.getPos());
        Hold_that_chunkClient.chunkUnloader.loadEmpty(chunkPos);
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("HEAD"))
    public void onChunkDeltaUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        packet.runUpdates((pos, state) -> Hold_that_chunkClient.chunkUnloader.loadEmpty(ChunkPos.containing(pos)));
    }

    @Unique
    private static boolean isPacketEffectivelyEmpty(ClientboundLevelChunkWithLightPacket packet) {
        return packet.getChunkData().getReadBuffer().readableBytes() <= 192;
    }

}
