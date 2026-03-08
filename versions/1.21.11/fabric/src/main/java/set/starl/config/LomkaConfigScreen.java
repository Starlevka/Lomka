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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
		
		// Build config entries
		buildConfigEntries();
		
		this.addRenderableWidget(this.list);
		
		// Done button
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
			saveAndClose();
		}).bounds(width / 2 - 100, height - 28, 200, 20).build());
	}
	
	private void buildConfigEntries() {
		// Global settings
		this.list.addHeader(Component.translatable("lomka.config.section.general"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.java22_optimizations"),
			config.java22Optimizations, 
			val -> config.java22Optimizations = val,
			Component.translatable("lomka.config.java22_optimizations.tooltip")
		);
		
		// Shaders section
		this.list.addHeader(Component.translatable("lomka.config.section.shaders"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.shaders.program_binary_cache.enable"),
			config.shaders.programBinaryCache.enable, 
			val -> config.shaders.programBinaryCache.enable = val,
			Component.translatable("lomka.config.shaders.program_binary_cache.enable.tooltip")
		);
		
		// Culling section (includes entity culling options)
		this.list.addHeader(Component.translatable("lomka.config.section.culling"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.entities.enable"),
			config.entities.enable, 
			val -> config.entities.enable = val,
			Component.translatable("lomka.config.entities.enable.tooltip")
		);
		this.list.addBooleanOption(
			Component.translatable("lomka.config.entities.cull_invisible"),
			config.entities.cullInvisible, 
			val -> config.entities.cullInvisible = val,
			Component.translatable("lomka.config.entities.cull_invisible.tooltip")
		);
		this.list.addBooleanOption(
			Component.translatable("lomka.config.entities.frustum_cull_all"),
			config.entities.frustumCullAll, 
			val -> config.entities.frustumCullAll = val,
			Component.translatable("lomka.config.entities.frustum_cull_all.tooltip")
		);
		this.list.addBooleanOption(
			Component.translatable("lomka.config.entities.section_occlusion_cull"),
			config.entities.sectionOcclusionCull, 
			val -> config.entities.sectionOcclusionCull = val,
			Component.translatable("lomka.config.entities.section_occlusion_cull.tooltip")
		);
		this.list.addBooleanOption(
			Component.translatable("lomka.config.entities.client_tick_cull.enable"),
			config.entities.clientTickCull.enable, 
			val -> config.entities.clientTickCull.enable = val,
			Component.translatable("lomka.config.entities.client_tick_cull.enable.tooltip")
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
		
		// NBT section
		this.list.addHeader(Component.translatable("lomka.config.section.nbt"));
		this.list.addBooleanOption(
			Component.translatable("lomka.config.nbt.cache.read_compressed_path"),
			config.nbt.cache.readCompressedPath, 
			val -> config.nbt.cache.readCompressedPath = val,
			Component.translatable("lomka.config.nbt.cache.read_compressed_path.tooltip")
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
	}
	
	private void saveAndClose() {
		LomkaConfig.save();
		this.minecraft.setScreen(this.parent);
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
	
	// === Config List Widget ===
	
	private final class ConfigListWidget extends ContainerObjectSelectionList {
		public ConfigListWidget(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, ITEM_HEIGHT);
		}
		
		public void addHeader(Component text) {
			this.addEntry(new HeaderEntry(text));
		}
		
		public void addHeader(Component text, int color) {
			this.addEntry(new HeaderEntry(text, color));
		}
		
		public void addBooleanOption(Component name, boolean value, Consumer<Boolean> setter, Component tooltip) {
			this.addEntry(new BooleanOptionEntry(name, value, setter, tooltip));
		}
		
		public void addIntOption(Component name, int value, Consumer<Integer> setter, Component tooltip) {
			this.addEntry(new IntOptionEntry(name, value, setter, tooltip));
		}
		
		public void addDoubleOption(Component name, double value, Consumer<Double> setter, Component tooltip) {
			this.addEntry(new DoubleOptionEntry(name, value, setter, tooltip));
		}
		
		@Override
		public int getRowWidth() {
			return Math.min(400, this.width - 50);
		}
	}
	
	// === Header Entry ===
	
	private final class HeaderEntry extends ContainerObjectSelectionList.Entry {
		private final Component text;
		private final int color;
		
		HeaderEntry(Component text) {
			this(text, 0xFFAAAAAA); // Default gray
		}
		
		HeaderEntry(Component text, int color) {
			this.text = text;
			this.color = color;
		}
		
		@Override
		public void renderContent(@NotNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
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
	
	// === Boolean Option Entry ===
	
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
		public void renderContent(@NotNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			// Draw option name
			graphics.drawString(Minecraft.getInstance().font, this.name, this.getContentX(), this.getContentY() + 6, 0xFFFFFFFF);
			
			// Update button position and render
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
	
	// === Int Option Entry (Using Button Cycle for simplicity) ===
	
	private final class IntOptionEntry extends ContainerObjectSelectionList.Entry {
		private final Component name;
		private final Component tooltip;
		private int value;
		private final Consumer<Integer> setter;
		private final Button cycleButton;
		
		IntOptionEntry(Component name, int value, Consumer<Integer> setter, Component tooltip) {
			this.name = name;
			this.value = value;
			this.setter = setter;
			this.tooltip = tooltip;
			this.cycleButton = Button.builder(Component.literal(String.valueOf(this.value)), button -> {
				// Simple cycling for common values
				if (this.value < 5) this.value = 5;
				else if (this.value < 10) this.value = 10;
				else if (this.value < 20) this.value = 20;
				else if (this.value < 40) this.value = 40;
				else if (this.value < 64) this.value = 64;
				else if (this.value < 128) this.value = 128;
				else if (this.value < 256) this.value = 256;
				else if (this.value < 512) this.value = 512;
				else this.value = 1;
				
				this.setter.accept(this.value);
				button.setMessage(Component.literal(String.valueOf(this.value)));
			}).bounds(0, 0, 75, 20).build();
			
			if (this.tooltip != null && !this.tooltip.getString().isEmpty()) {
				this.cycleButton.setTooltip(Tooltip.create(this.tooltip));
			}
		}
		
		@Override
		public void renderContent(@NotNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			graphics.drawString(Minecraft.getInstance().font, this.name, this.getContentX(), this.getContentY() + 6, 0xFFFFFFFF);
			this.cycleButton.setPosition(this.getContentX() + this.getContentWidth() - 80, this.getContentY());
			this.cycleButton.render(graphics, mouseX, mouseY, partialTick);
		}
		
		@Override
		public List<? extends AbstractWidget> children() {
			return List.of(this.cycleButton);
		}
		
		@Override
		public List<? extends AbstractWidget> narratables() {
			return List.of(this.cycleButton);
		}
	}
	
	// === Double Option Entry ===
	
	private final class DoubleOptionEntry extends ContainerObjectSelectionList.Entry {
		private final Component name;
		private final Component tooltip;
		private double value;
		private final Consumer<Double> setter;
		private final Button cycleButton;
		
		DoubleOptionEntry(Component name, double value, Consumer<Double> setter, Component tooltip) {
			this.name = name;
			this.value = value;
			this.setter = setter;
			this.tooltip = tooltip;
			this.cycleButton = Button.builder(Component.literal(String.valueOf((int)this.value)), button -> {
				if (this.value < 32) this.value = 32;
				else if (this.value < 64) this.value = 64;
				else if (this.value < 128) this.value = 128;
				else if (this.value < 160) this.value = 160;
				else if (this.value < 256) this.value = 256;
				else if (this.value < 512) this.value = 512;
				else this.value = 16;
				
				this.setter.accept(this.value);
				button.setMessage(Component.literal(String.valueOf((int)this.value)));
			}).bounds(0, 0, 75, 20).build();
			
			if (this.tooltip != null && !this.tooltip.getString().isEmpty()) {
				this.cycleButton.setTooltip(Tooltip.create(this.tooltip));
			}
		}
		
		@Override
		public void renderContent(@NotNull GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			graphics.drawString(Minecraft.getInstance().font, this.name, this.getContentX(), this.getContentY() + 6, 0xFFFFFFFF);
			this.cycleButton.setPosition(this.getContentX() + this.getContentWidth() - 80, this.getContentY());
			this.cycleButton.render(graphics, mouseX, mouseY, partialTick);
		}
		
		@Override
		public List<? extends AbstractWidget> children() {
			return List.of(this.cycleButton);
		}
		
		@Override
		public List<? extends AbstractWidget> narratables() {
			return List.of(this.cycleButton);
		}
	}
}
