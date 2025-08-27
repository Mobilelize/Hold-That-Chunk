package net.mobilelize.hold_that_chunk.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HoldThatChunkClothConfig {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Hold That Chunk V2"));

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Respect Server's Render Distance"), ConfigManager.configData.respectServerDistance)
                .setDefaultValue(false)
                .setSaveConsumer(val -> ConfigManager.configData.respectServerDistance = val)
                .setTooltip(Text.literal("Sets hold distance to the server's render distance."))
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Link to Render Distance"), ConfigManager.configData.linkRenderDistance)
                .setDefaultValue(true)
                .setSaveConsumer(val -> ConfigManager.configData.linkRenderDistance = val)
                .setTooltip(Text.literal("Sets if your hold distance should be linked to your render distance."))
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Keep Chunks Loaded"), ConfigManager.configData.keepChunksLoaded)
                .setDefaultValue(false)
                .setSaveConsumer(val -> ConfigManager.configData.keepChunksLoaded = val)
                .setTooltip(Text.literal("If \"Link to Render Distance\" is activated, sets if the chunks should be kept loaded outside that render distance."))
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(Text.literal("Hold Distance"), ConfigManager.configData.holdDistance, 2, 256)
                .setDefaultValue(64)
                .setMin(2).setMax(256)
                .setSaveConsumer(val -> ConfigManager.configData.holdDistance = val)
                .build());

        builder.setSavingRunnable(ConfigManager::saveConfig);

        return builder.build();
    }
}
