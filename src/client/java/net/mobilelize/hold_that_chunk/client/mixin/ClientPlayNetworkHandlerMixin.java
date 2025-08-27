package net.mobilelize.hold_that_chunk.client.mixin;

import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.mobilelize.hold_that_chunk.client.Hold_that_chunkClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onUnloadChunk", at = @At("HEAD"), cancellable = true)
    private void holdThatChunk(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        if (!Hold_that_chunkClient.chunkUnloader.isBeingProcessedRemove(packet.pos())) {
            Hold_that_chunkClient.chunkUnloader.onUnloadPacket(packet);
            ci.cancel();
        }
    }

    @Redirect(method = "onChunkLoadDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ChunkLoadDistanceS2CPacket;getDistance()I"))
    private int setServerDistance(ChunkLoadDistanceS2CPacket instance) {
        Hold_that_chunkClient.chunkUnloader.setOriginalServerRenderDistance(instance.getDistance());
        return 256;
    }

    @Inject(method = "startWorldLoading", at = @At("HEAD"))
    public void loadingWorld(ClientPlayerEntity player, ClientWorld world, DownloadingTerrainScreen.WorldEntryReason worldEntryReason, CallbackInfo ci) {
        Hold_that_chunkClient.chunkUnloader.clear();
    }

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    public void joiningGame(GameJoinS2CPacket packet, CallbackInfo ci) {
        Hold_that_chunkClient.chunkUnloader.clear();
    }

    @Inject(method = "onChunkData", at = @At("HEAD"))
    public void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        ChunkPos pos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
        Hold_that_chunkClient.chunkUnloader.removePending(pos);
    }
}
