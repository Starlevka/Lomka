package set.starl.mixin.render;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.weather.optimize", "true"));

	@Unique
	private static final boolean LOMKA$DISABLE = "true".equalsIgnoreCase(System.getProperty("lomka.weather.disable", "false"));

	@Unique
	private static final RandomSource LOMKA$RANDOM = RandomSource.create();

	@Shadow
	private Biome.Precipitation getPrecipitationAt(final Level level, final BlockPos pos) {
		throw new AssertionError();
	}

	@Shadow
	private WeatherEffectRenderer.ColumnInstance createRainColumnInstance(final RandomSource random, final int ticks, final int x, final int bottomY, final int topY, final int z, final int lightCoords, final float partialTicks) {
		throw new AssertionError();
	}

	@Shadow
	private WeatherEffectRenderer.ColumnInstance createSnowColumnInstance(final RandomSource random, final int ticks, final int x, final int bottomY, final int topY, final int z, final int lightCoords, final float partialTicks) {
		throw new AssertionError();
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void lomka$extractRenderStateOptimized(final Level level, final int ticks, final float partialTicks, final Vec3 cameraPos, final WeatherRenderState renderState, final CallbackInfo ci) {
		if (!LOMKA$ENABLE) {
			return;
		}

		if (LOMKA$DISABLE) {
			renderState.rainColumns.clear();
			renderState.snowColumns.clear();
			renderState.intensity = 0.0F;
			renderState.radius = 0;
			ci.cancel();
			return;
		}

		renderState.intensity = level.getRainLevel(partialTicks);
		if (renderState.intensity <= 0.0F) {
			ci.cancel();
			return;
		}

		renderState.radius = (Integer)Minecraft.getInstance().options.weatherRadius().get();
		int radius = renderState.radius;
		int diameter = radius * 2 + 1;
		int capacity = diameter * diameter;
		((ArrayList<?>)renderState.rainColumns).ensureCapacity(capacity);
		((ArrayList<?>)renderState.snowColumns).ensureCapacity(capacity);

		int cameraBlockX = Mth.floor(cameraPos.x);
		int cameraBlockY = Mth.floor(cameraPos.y);
		int cameraBlockZ = Mth.floor(cameraPos.z);
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		RandomSource random = LOMKA$RANDOM;

		for (int z = cameraBlockZ - radius; z <= cameraBlockZ + radius; ++z) {
			for (int x = cameraBlockX - radius; x <= cameraBlockX + radius; ++x) {
				int terrainHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				int y0 = Math.max(cameraBlockY - radius, terrainHeight);
				int y1 = Math.max(cameraBlockY + radius, terrainHeight);
				if (y1 - y0 == 0) {
					continue;
				}

				Biome.Precipitation precipitation = this.getPrecipitationAt(level, mutablePos.set(x, cameraBlockY, z));
				if (precipitation == Biome.Precipitation.NONE) {
					continue;
				}

				int seed = x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761;
				random.setSeed((long)seed);
				int lightSampleY = Math.max(cameraBlockY, terrainHeight);
				int lightCoords = LevelRenderer.getLightColor(level, mutablePos.set(x, lightSampleY, z));
				if (precipitation == Biome.Precipitation.RAIN) {
					renderState.rainColumns.add(this.createRainColumnInstance(random, ticks, x, y0, y1, z, lightCoords, partialTicks));
				} else if (precipitation == Biome.Precipitation.SNOW) {
					renderState.snowColumns.add(this.createSnowColumnInstance(random, ticks, x, y0, y1, z, lightCoords, partialTicks));
				}
			}
		}

		ci.cancel();
	}
}
