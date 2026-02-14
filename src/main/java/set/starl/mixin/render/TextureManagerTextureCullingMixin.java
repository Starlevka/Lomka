package set.starl.mixin.render;

import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TickableTexture;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureManager.class)
public class TextureManagerTextureCullingMixin {
	@Unique
	private static final boolean LOMKA$ENABLE = !"false".equalsIgnoreCase(System.getProperty("lomka.textures.cull.enable", "true"));

	@Shadow
	@Final
	private Map byPath;

	@Unique
	private Object2IntOpenHashMap<Identifier> lomka$lastUsedTickById;

	@Unique
	private final Object lomka$textureIdLock = new Object();

	@Unique
	private final IdentityHashMap<AbstractTexture, Identifier> lomka$idByTexture = new IdentityHashMap<>();

	@Unique
	private int lomka$tick;

	@Unique
	private int lomka$nextSweepTick;

	@Unique
	private static final int lomka$sweepIntervalTicks = Integer.parseInt(System.getProperty("lomka.textures.cull.sweepIntervalTicks", "200"));

	@Unique
	private static final int lomka$unusedThresholdTicks = Integer.parseInt(System.getProperty("lomka.textures.cull.unusedThresholdTicks", String.valueOf(20 * 60 * 5)));

	@Unique
	private static final int lomka$maxEvictionsPerSweep = Integer.parseInt(System.getProperty("lomka.textures.cull.maxEvictionsPerSweep", "4"));

	@Unique
	private static final int lomka$maxScanPerSweep = Integer.parseInt(System.getProperty("lomka.textures.cull.maxScanPerSweep", "512"));

	@Unique
	private Object2IntOpenHashMap<Identifier> lomka$lastUsedTickById() {
		Object2IntOpenHashMap<Identifier> map = this.lomka$lastUsedTickById;
		if (map == null) {
			map = new Object2IntOpenHashMap<>();
			map.defaultReturnValue(-1);
			this.lomka$lastUsedTickById = map;
		}
		return map;
	}

	@Inject(method = "getTexture", at = @At("HEAD"))
	private void lomka$markUsed(final Identifier location, final CallbackInfoReturnable<AbstractTexture> cir) {
		if (!LOMKA$ENABLE) {
			return;
		}
		int tick = this.lomka$tick;
		Object2IntOpenHashMap<Identifier> lastUsed = this.lomka$lastUsedTickById();
		if (lastUsed.getInt(location) != tick) {
			lastUsed.put(location, tick);
		}
	}

	@Inject(method = "register", at = @At("TAIL"))
	private void lomka$markRegisteredUsed(final Identifier location, final AbstractTexture texture, final CallbackInfo ci) {
		if (!LOMKA$ENABLE) {
			return;
		}
		synchronized (this.lomka$textureIdLock) {
			this.lomka$idByTexture.put(texture, location);
		}
		int tick = this.lomka$tick;
		Object2IntOpenHashMap<Identifier> lastUsed = this.lomka$lastUsedTickById();
		if (lastUsed.getInt(location) != tick) {
			lastUsed.put(location, tick);
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void lomka$evictUnused(final CallbackInfo ci) {
		if (!LOMKA$ENABLE) {
			return;
		}
		this.lomka$tick++;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}

		if (this.lomka$tick < this.lomka$nextSweepTick) {
			return;
		}

		int sweepInterval = lomka$sweepIntervalTicks;
		if (sweepInterval <= 0) {
			sweepInterval = 200;
		}
		this.lomka$nextSweepTick = this.lomka$tick + sweepInterval;

		int evicted = 0;
		Object2IntOpenHashMap<Identifier> lastUsed = this.lomka$lastUsedTickById();
		Iterator<Map.Entry> it = this.byPath.entrySet().iterator();
		int scanned = 0;
		int maxScan = lomka$maxScanPerSweep;
		if (maxScan <= 0) {
			maxScan = 512;
		}

		while (it.hasNext() && evicted < lomka$maxEvictionsPerSweep) {
			if (scanned >= maxScan) {
				break;
			}
			Map.Entry entry = it.next();
			scanned++;
			Identifier id = (Identifier)entry.getKey();
			AbstractTexture texture = (AbstractTexture)entry.getValue();

			int lastUsedTick = lastUsed.getInt(id);
			if (lastUsedTick < 0 || this.lomka$tick - lastUsedTick < lomka$unusedThresholdTicks) {
				continue;
			}

			if (!lomka$isEvictable(texture)) {
				continue;
			}

			it.remove();
			lastUsed.remove(id);
			synchronized (this.lomka$textureIdLock) {
				this.lomka$idByTexture.remove(texture);
			}
			try {
				texture.close();
			} catch (Exception ignored) {
			}
			evicted++;
		}
	}

	@Unique
	public void lomka$markTextureUsed(final AbstractTexture texture) {
		if (!LOMKA$ENABLE) {
			return;
		}
		Identifier id;
		synchronized (this.lomka$textureIdLock) {
			id = this.lomka$idByTexture.get(texture);
		}
		if (id == null) {
			return;
		}
		int tick = this.lomka$tick;
		Object2IntOpenHashMap<Identifier> lastUsed = this.lomka$lastUsedTickById();
		if (lastUsed.getInt(id) != tick) {
			lastUsed.put(id, tick);
		}
	}

	@Unique
	private static boolean lomka$isEvictable(final AbstractTexture texture) {
		if (texture instanceof TextureAtlas) {
			return false;
		}
		if (texture instanceof DynamicTexture) {
			return false;
		}
		if (texture instanceof TickableTexture) {
			return false;
		}
		return texture instanceof SimpleTexture;
	}
}
