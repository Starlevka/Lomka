package set.starl.util.compat;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import set.starl.config.LomkaConfig;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin for conditional mixin application based on config.
 * Allows users to disable specific mixin groups without recompilation.
 */
public class LomkaMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            LomkaConfig config = LomkaConfig.get();

            // Culling mixins — gated by culling.enable
            if (mixinClassName.contains(".cull.")) {
                if (mixinClassName.endsWith("SmartLeavesCullingMixin")) {
                    return config.culling.enable && config.culling.smartLeavesCulling;
                }
                if (mixinClassName.endsWith("BlockEntityCullingMixin")) {
                    return config.culling.enable && config.culling.blockEntityCulling;
                }
                if (mixinClassName.endsWith("ClientLevelEntityTickCullMixin")) {
                    return config.entities.enable && config.entities.clientTickCull;
                }
                return config.culling.enable;
            }

            // GL state caching — gated by render settings
            if (mixinClassName.contains(".gl.")) {
                if (mixinClassName.endsWith("RenderSystemGlDebugOffMixin")) {
                    return config.render.openglDebugDisable;
                }
                return true;
            }

            // Chunk mixins
            if (mixinClassName.contains(".chunk.")) {
                if (mixinClassName.endsWith("LevelRendererTranslucencyResortThrottleMixin")) {
                    return config.render.translucencyResortThrottle;
                }
                return config.chunks.enable;
            }

            // All other mixins (crash, network, accessors) — always apply
            return true;
        } catch (Exception e) {
            // Config not yet available during early mixin loading — apply all
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}