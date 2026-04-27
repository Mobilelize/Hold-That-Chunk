package net.mobilelize.hold_that_chunk.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.mobilelize.hold_that_chunk.client.Hold_that_chunkClient;

public class HoldThatChunkClothConfig {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Hold That Chunk V2"));

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Enable Hold That Chunk"), ConfigManager.configData.holdThatChunkEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(val -> ConfigManager.configData.holdThatChunkEnabled = val)
                .setTooltip(
                        Component.literal("Toggles the Hold That Chunk system on or off."),
                        Component.literal("Note: changes take effect when switching worlds or reconnecting.")
                )
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Ignore Server's Render Distance"), ConfigManager.configData.ignoreServerDistance)
                .setDefaultValue(true)
                .setSaveConsumer(val -> ConfigManager.configData.ignoreServerDistance = val)
                .setTooltip(
                        Component.literal("True: Render Distance can exceed the server's render distance (fog too)."),
                        Component.literal("False: max render distance is capped to the server's value."),
                        Component.literal("Note: changes take effect when switching worlds or reconnecting.")
                )
                .build());


        general.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Respect Server's Render Distance"), ConfigManager.configData.respectServerDistance)
                .setDefaultValue(false)
                .setSaveConsumer(val -> ConfigManager.configData.respectServerDistance = val)
                .setTooltip(Component.literal("Sets hold distance to the server's render distance."))
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Link to Render Distance"), ConfigManager.configData.linkRenderDistance)
                .setDefaultValue(false)
                .setSaveConsumer(val -> ConfigManager.configData.linkRenderDistance = val)
                .setTooltip(Component.literal("Sets hold distance to your render distance."))
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(Component.literal("Hold Distance"), ConfigManager.configData.holdDistance, 2, 256)
                .setDefaultValue(64)
                .setMin(2).setMax(256)
                .setSaveConsumer(val -> ConfigManager.configData.holdDistance = val)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Ignore Empty Chunks"), ConfigManager.configData.cancelEmptyChunks)
                .setDefaultValue(false)
                .setSaveConsumer(val -> ConfigManager.configData.cancelEmptyChunks = val)
                .setTooltip(
                        Component.literal("Sets if the client should ignore empty chunks")
                )
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(Component.literal("Ignore Empty Chunks Distance"), ConfigManager.configData.ignoreEmptyChunksDistance, 2, 256)
                .setDefaultValue(5)
                .setMin(2).setMax(256)
                .setSaveConsumer(val -> ConfigManager.configData.ignoreEmptyChunksDistance = val)
                .setTooltip(Component.literal("Distance needed before ignoring empty chunks."))
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(Component.literal("Restore Empty Chunks Distance"), ConfigManager.configData.restoreEmptyChunksDistance, 2, 256)
                .setDefaultValue(2)
                .setMin(2).setMax(256)
                .setSaveConsumer(val -> ConfigManager.configData.restoreEmptyChunksDistance = val)
                .setTooltip(Component.literal("Distance needed before restoring empty chunks."))
                .build());

        general.addEntry(entryBuilder.startTextDescription(Component.literal("Server's Render Distance: " + Hold_that_chunkClient.chunkUnloader.getOriginalServerRenderDistance()))
                .build());

        builder.setSavingRunnable(ConfigManager::saveConfig);

        return builder.build();
    }
}
