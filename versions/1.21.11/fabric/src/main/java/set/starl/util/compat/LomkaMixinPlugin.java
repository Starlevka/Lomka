package set.starl.util.compat;

import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class LomkaMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(final String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
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
