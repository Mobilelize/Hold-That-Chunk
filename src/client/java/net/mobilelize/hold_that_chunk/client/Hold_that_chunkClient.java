package net.mobilelize.hold_that_chunk.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hold_that_chunkClient implements ClientModInitializer {

    public static Logger logger = LoggerFactory.getLogger("HoldThatChunk");

    public static ChunkUnloader chunkUnloader;

    @Override
    public void onInitializeClient() {
        chunkUnloader = new ChunkUnloader();

    }
}
