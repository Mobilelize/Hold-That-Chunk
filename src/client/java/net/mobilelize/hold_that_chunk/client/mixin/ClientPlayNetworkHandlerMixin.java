package net.mobilelize.hold_that_chunk.client.mixin;

import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.ChunkPos;
import net.mobilelize.hold_that_chunk.client.Hold_that_chunkClient;
import net.mobilelize.hold_that_chunk.client.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onUnloadChunk", at = @At("HEAD"), cancellable = true)
    private void holdThatChunk(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        if (ConfigManager.configData.holdThatChunkEnabled && !ConfigManager.configData.respectServerDistance && !Hold_that_chunkClient.chunkUnloader.isBeingProcessedRemove(packet.pos())) {
            Hold_that_chunkClient.chunkUnloader.onUnloadPacket(packet);
            ci.cancel();
        }
        Hold_that_chunkClient.chunkUnloader.unmarkCleared(packet.pos());
    }

    @Redirect(method = "onChunkLoadDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ChunkLoadDistanceS2CPacket;getDistance()I"))
    private int setServerDistance(ChunkLoadDistanceS2CPacket instance) {
        Hold_that_chunkClient.chunkUnloader.setOriginalServerRenderDistance(instance.getDistance());
        if (ConfigManager.configData.holdThatChunkEnabled && ConfigManager.configData.ignoreServerDistance) return 256;
        return instance.getDistance();
    }

    @Inject(method = "startWorldLoading", at = @At("HEAD"))
    public void loadingWorld(ClientPlayerEntity player, ClientWorld world, DownloadingTerrainScreen.WorldEntryReason worldEntryReason, CallbackInfo ci) {
        Hold_that_chunkClient.chunkUnloader.clear();
    }

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    public void joiningGame(GameJoinS2CPacket packet, CallbackInfo ci) {
        Hold_that_chunkClient.chunkUnloader.clear();
    }

    @Inject(method = "onChunkData", at = @At("HEAD"), cancellable = true)
    public void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        ChunkPos pos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
        Hold_that_chunkClient.chunkUnloader.removePending(pos);

        if (pos.x == 6 && pos.z == 15) {
            Hold_that_chunkClient.logger.info("this chunk size {}", packet.getChunkData().getSectionsDataBuf().array().length);
        }

        if (isPacketEffectivelyEmpty(packet)
                && Hold_that_chunkClient.chunkUnloader.shouldCancelEmptyChunk(pos)
                && !Hold_that_chunkClient.chunkUnloader.isEmptyBeingProcessedRemove(new ChunkPos(packet.getChunkX(), packet.getChunkZ()))) {
            Hold_that_chunkClient.chunkUnloader.markCleared(pos, packet);
            ci.cancel();
            return;
        }

        Hold_that_chunkClient.chunkUnloader.unmarkCleared(pos);
    }

    @Inject(method = "onBlockUpdate", at = @At("HEAD"))
    public void onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        ChunkPos chunkPos = new ChunkPos(packet.getPos());
        Hold_that_chunkClient.chunkUnloader.loadEmpty(chunkPos);
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("HEAD"))
    public void onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        packet.visitUpdates((pos, state) -> Hold_that_chunkClient.chunkUnloader.loadEmpty(new ChunkPos(pos)));
    }

    @Unique
    private static boolean isPacketEffectivelyEmpty(ChunkDataS2CPacket packet) {
        return packet.getChunkData().getSectionsDataBuf().array().length <= 144;
    }

}
