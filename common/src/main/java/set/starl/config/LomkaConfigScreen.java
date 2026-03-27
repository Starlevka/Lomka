package set.starl.config;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class LomkaConfigScreen extends OptionsSubScreen {

    private static final Component TITLE = Component.translatable("lomka.config.title");

    public LomkaConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, TITLE);
    }

    @Override
    protected void addOptions() {
        LomkaConfig cfg = LomkaConfig.get();

        // Culling
        this.list.addHeader(Component.translatable("lomka.config.section.culling"));
        this.list.addSmall(
                boolOpt("lomka.config.culling.enable", cfg.culling.enable, v -> cfg.culling.enable = v),
                boolOpt("lomka.config.culling.frustum_cull_all", cfg.culling.frustumCullAll, v -> cfg.culling.frustumCullAll = v)
        );
        this.list.addSmall(
                boolOpt("lomka.config.culling.section_occlusion_cull", cfg.culling.sectionOcclusionCull, v -> cfg.culling.sectionOcclusionCull = v),
                boolOpt("lomka.config.culling.block_entity_culling", cfg.culling.blockEntityCulling, v -> cfg.culling.blockEntityCulling = v)
        );
        this.list.addSmall(
                boolOpt("lomka.config.culling.smart_leaves_culling", cfg.culling.smartLeavesCulling, v -> cfg.culling.smartLeavesCulling = v)
        );

        // Entities
        this.list.addHeader(Component.translatable("lomka.config.section.entities"));
        this.list.addSmall(
                boolOpt("lomka.config.entities.enable", cfg.entities.enable, v -> cfg.entities.enable = v),
                boolOpt("lomka.config.entities.client_tick_cull", cfg.entities.clientTickCull, v -> cfg.entities.clientTickCull = v)
        );
        this.list.addSmall(
                boolOpt("lomka.config.entities.client_tick_cull_include_display", cfg.entities.clientTickCullIncludeDisplay, v -> cfg.entities.clientTickCullIncludeDisplay = v)
        );

        // Render
        this.list.addHeader(Component.translatable("lomka.config.section.render"));
        this.list.addSmall(
                boolOpt("lomka.config.render.opengl_debug_disable", cfg.render.openglDebugDisable, v -> cfg.render.openglDebugDisable = v),
                boolOpt("lomka.config.render.translucency_resort_throttle", cfg.render.translucencyResortThrottle, v -> cfg.render.translucencyResortThrottle = v)
        );

        // Chunks
        this.list.addHeader(Component.translatable("lomka.config.section.chunks"));
        this.list.addSmall(
                boolOpt("lomka.config.chunks.enable", cfg.chunks.enable, v -> cfg.chunks.enable = v),
                boolOpt("lomka.config.chunks.silence_ignore_out_of_range_log", cfg.chunks.silenceIgnoreOutOfRangeLog, v -> cfg.chunks.silenceIgnoreOutOfRangeLog = v)
        );

        // NBT
        this.list.addHeader(Component.translatable("lomka.config.section.nbt"));
        this.list.addSmall(
                boolOpt("lomka.config.nbt.cache.read_compressed_path", cfg.nbt.cache.readCompressedPath, v -> cfg.nbt.cache.readCompressedPath = v)
        );

        // Java
        this.list.addHeader(Component.translatable("lomka.config.section.java"));
        this.list.addSmall(
                boolOpt("lomka.config.java.java25_optimizations", cfg.java.java25Optimizations, v -> cfg.java.java25Optimizations = v)
        );
    }

    @Override
    public void onClose() {
        LomkaConfig.save();
        super.onClose();
    }

    private OptionInstance<Boolean> boolOpt(String translationKey, boolean initialValue, Consumer<Boolean> setter) {
        return OptionInstance.createBoolean(translationKey, initialValue, setter);
    }

    public static Screen create(Screen parent) {
        return new LomkaConfigScreen(parent);
    }
}