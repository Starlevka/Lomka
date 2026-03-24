package set.starl.config;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class LomkaConfigScreen extends Screen {
	private static final Component TITLE = Component.translatable("lomka.config.title");
	private static final int ITEM_HEIGHT = 24;
	
	private final Screen parent;
	private final LomkaConfig config;
	private ConfigListWidget list;
	
	public static Screen create(Screen parent) {
		return new LomkaConfigScreen(parent);
	}
	
	private LomkaConfigScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
		this.config = LomkaConfig.get();
	}
	
	@Override
	protected void init() {
		this.list = new ConfigListWidget(minecraft, width, height - 64, 32);
		
		buildConfigEntries();
		
		this.addRenderableWidget(this.list);
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
			saveAndClose();
		}).bounds(width / 2 - 100, height - 28, 200, 20).build());
	}
	
	private void buildConfigEntries() {
		// Shaders section
		this.list.addHeader(Component.translatable("lomka.config.section.shaders"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.shaders.program_binary_cache.enable"),
			config.shaders.programBinaryCache.enable, 
			val -> config.shaders.programBinaryCache.enable = val,
			Component.translatable("lomka.config.shaders.program_binary_cache.enable.tooltip")
		);
		
        // Culling section
        this.list.addHeader(Component.translatable("lomka.config.section.culling"));
        this.list.addBooleanOption(
            Component.translatable("lomka.config.culling.enable"),
            config.culling.enable, 
            val -> config.culling.enable = val,
            Component.translatable("lomka.config.culling.enable.tooltip")
        );
        this.list.addBooleanOption(
            Component.translatable("lomka.config.culling.frustum_cull_all"),
            config.culling.frustumCullAll, 
            val -> config.culling.frustumCullAll = val,
            Component.translatable("lomka.config.culling.frustum_cull_all.tooltip")
        );
        this.list.addBooleanOption(
            Component.translatable("lomka.config.culling.section_occlusion_cull"),
            config.culling.sectionOcclusionCull, 
            val -> config.culling.sectionOcclusionCull = val,
            Component.translatable("lomka.config.culling.section_occlusion_cull.tooltip")
        );
        this.list.addBooleanOption(
            Component.translatable("lomka.config.culling.block_entity_culling"),
            config.culling.blockEntityCulling, 
            val -> config.culling.blockEntityCulling = val,
            Component.translatable("lomka.config.culling.block_entity_culling.tooltip")
        );
        this.list.addBooleanOption(
            Component.translatable("lomka.config.culling.smart_leaves_culling"),
            config.culling.smartLeavesCulling,
            val -> config.culling.smartLeavesCulling = val,
            Component.translatable("lomka.config.culling.smart_leaves_culling.tooltip")
        );
        // Textures section
		this.list.addHeader(Component.translatable("lomka.config.section.textures"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.textures.decode.off_thread"),
			config.textures.decode.offThread, 
			val -> config.textures.decode.offThread = val,
			Component.translatable("lomka.config.textures.decode.off_thread.tooltip")
		);
		
		// Chunks section
		this.list.addHeader(Component.translatable("lomka.config.section.chunks"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.chunks.silence_ignore_out_of_range_log"),
			config.chunks.silenceIgnoreOutOfRangeLog, 
			val -> config.chunks.silenceIgnoreOutOfRangeLog = val,
			Component.translatable("lomka.config.chunks.silence_ignore_out_of_range_log.tooltip")
		);
		
		// NBT section
		this.list.addHeader(Component.translatable("lomka.config.section.nbt"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.nbt.cache.read_compressed_path"),
			config.nbt.cache.readCompressedPath, 
			val -> config.nbt.cache.readCompressedPath = val,
			Component.translatable("lomka.config.nbt.cache.read_compressed_path.tooltip")
		);
		
		// Network section
		this.list.addHeader(Component.translatable("lomka.config.section.network"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.network.compression_decoder.direct_input_reuse"),
			config.network.compressionDecoder.directInputReuse, 
			val -> config.network.compressionDecoder.directInputReuse = val,
			Component.translatable("lomka.config.network.compression_decoder.direct_input_reuse.tooltip")
		);
		
		// Render section
		this.list.addHeader(Component.translatable("lomka.config.section.render"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.render.opengl_debug_disable"),
			config.render.openglDebugDisable, 
			val -> config.render.openglDebugDisable = val,
			Component.translatable("lomka.config.render.opengl_debug_disable.tooltip")
		);
		this.list.addBooleanOption(
			Component.translatable("lomka.config.render.translucency_resort_throttle"),
			config.render.translucencyResortThrottle, 
			val -> config.render.translucencyResortThrottle = val,
			Component.translatable("lomka.config.render.translucency_resort_throttle.tooltip")
        );
        this.list.addBooleanOption(
            Component.translatable("lomka.config.render.framebuffer_binding_fix"),
            config.render.framebufferBindingFix,
            val -> config.render.framebufferBindingFix = val,
            Component.translatable("lomka.config.render.framebuffer_binding_fix.tooltip")
        );
        this.list.addBooleanOption(
            Component.translatable("lomka.config.render.sampler_cache_optimization"),
            config.render.samplerCacheOptimization,
            val -> config.render.samplerCacheOptimization = val,
            Component.translatable("lomka.config.render.sampler_cache_optimization.tooltip")
        );
        this.list.addBooleanOption(
            Component.translatable("lomka.config.render.polygon_mode_cache"),
            config.render.polygonModeCache,
            val -> config.render.polygonModeCache = val,
            Component.translatable("lomka.config.render.polygon_mode_cache.tooltip")
        );

        // Java section
		this.list.addHeader(Component.translatable("lomka.config.section.java"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.java.java25_optimizations"),
			config.java.java25Optimizations, 
			val -> config.java.java25Optimizations = val,
			Component.translatable("lomka.config.java.java25_optimizations.tooltip")
		);
	}
	
	private void saveAndClose() {
		LomkaConfig.save();
		this.minecraft.setScreen(this.parent);
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
	
	private final class ConfigListWidget extends ContainerObjectSelectionList {
		public ConfigListWidget(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, ITEM_HEIGHT);
		}
		
		public void addHeader(Component text) {
			this.addEntry(new HeaderEntry(text));
		}
		
		public void addBooleanOption(Component name, boolean value, Consumer<Boolean> setter, Component tooltip) {
			this.addEntry(new BooleanOptionEntry(name, value, setter, tooltip));
		}
		
		@Override
		public int getRowWidth() {
			return Math.min(400, this.width - 50);
		}
	}
	
	private final class HeaderEntry extends ContainerObjectSelectionList.Entry {
		private final Component text;
		private final int color;
		
		HeaderEntry(Component text) {
			this(text, 0xFFAAAAAA);
		}
		
		HeaderEntry(Component text, int color) {
			this.text = text;
			this.color = color;
		}
		
		@Override
		public void renderContent(@NonNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			graphics.drawCenteredString(Minecraft.getInstance().font, this.text, LomkaConfigScreen.this.width / 2, this.getContentY() + 4, this.color);
		}
		
		@Override
		public List<? extends AbstractWidget> children() {
			return List.of();
		}
		
		@Override
		public List<? extends AbstractWidget> narratables() {
			return List.of();
		}
	}
	
	private final class BooleanOptionEntry extends ContainerObjectSelectionList.Entry {
		private final Component name;
		private final Component tooltip;
		private boolean value;
		private final Consumer<Boolean> setter;
		private final Button toggleButton;
		
		BooleanOptionEntry(Component name, boolean value, Consumer<Boolean> setter, Component tooltip) {
			this.name = name;
			this.value = value;
			this.setter = setter;
			this.tooltip = tooltip;
			this.toggleButton = Button.builder(getValueText(), button -> {
				BooleanOptionEntry.this.value = !BooleanOptionEntry.this.value;
				BooleanOptionEntry.this.setter.accept(BooleanOptionEntry.this.value);
				button.setMessage(getValueText());
			}).bounds(0, 0, 75, 20).build();
			
			if (this.tooltip != null && !this.tooltip.getString().isEmpty()) {
				this.toggleButton.setTooltip(Tooltip.create(this.tooltip));
			}
		}
		
		@Override
		public void renderContent(@NonNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			graphics.drawString(Minecraft.getInstance().font, this.name, this.getContentX(), this.getContentY() + 6, 0xFFFFFFFF);
			this.toggleButton.setPosition(this.getContentX() + this.getContentWidth() - 80, this.getContentY());
			this.toggleButton.render(graphics, mouseX, mouseY, partialTick);
		}
		
		@Override
		public List<? extends AbstractWidget> children() {
			return List.of(this.toggleButton);
		}
		
		@Override
		public List<? extends AbstractWidget> narratables() {
			return List.of(this.toggleButton);
		}
		
		private Component getValueText() {
			return this.value 
				? Component.translatable("lomka.config.on")
				: Component.translatable("lomka.config.off");
		}
	}
}