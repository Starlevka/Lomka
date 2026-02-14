package set.starl.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class LomkaMixinPlugin implements IMixinConfigPlugin {
	private static final boolean LOMKA$ALLOW_CHUNK_MIXINS_WITH_SODIUM = "true".equalsIgnoreCase(System.getProperty("lomka.compat.sodium.allowChunkMixins", "false"));

	private static volatile boolean lomka$checkedMods;
	private static volatile boolean lomka$hasSodium;
	private static volatile boolean lomka$hasC2ME;

	@Override
	public void onLoad(final String mixinPackage) {
		if (!lomka$checkedMods) {
			try {
				lomka$hasSodium = FabricLoader.getInstance().isModLoaded("Sodium");
			} catch (Exception ignored) {
				lomka$hasSodium = false;
			}
			try {
				lomka$hasC2ME = FabricLoader.getInstance().isModLoaded("C2ME");
			} catch (Exception ignored) {
				lomka$hasC2ME = false;
			}
			lomka$checkedMods = true;
		}
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
		if (!LOMKA$ALLOW_CHUNK_MIXINS_WITH_SODIUM && (lomka$hasSodium || lomka$hasC2ME)) {
			if (mixinClassName.startsWith("set.starl.mixin.chunk.")) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(final String targetClassName, final org.objectweb.asm.tree.ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(final String targetClassName, final org.objectweb.asm.tree.ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {
	}
}
