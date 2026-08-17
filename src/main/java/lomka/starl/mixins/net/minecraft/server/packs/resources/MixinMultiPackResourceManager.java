package lomka.starl.mixins.net.minecraft.server.packs.resources;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MultiPackResourceManager.class)
public abstract class MixinMultiPackResourceManager {

    @Shadow @Final private Map<String, FallbackResourceManager> namespacedManagers;
    @Shadow private static void checkTrailingDirectoryPath(String path) {}

    /**
     * @author Starlev
     * @reason Bypass redundant secondary TreeMap allocation when only one namespace manager is present.
     */
    @Overwrite
    public Map<Identifier, Resource> listResources(String path, Predicate<Identifier> filter) {
        checkTrailingDirectoryPath(path);
        Collection<FallbackResourceManager> managers = this.namespacedManagers.values();
        int size = managers.size();
        if (size == 0) {
            return new TreeMap<>();
        }
        if (size == 1) {
            return managers.iterator().next().listResources(path, filter);
        }
        TreeMap<Identifier, Resource> result = new TreeMap<>();
        for (FallbackResourceManager manager : managers) {
            result.putAll(manager.listResources(path, filter));
        }
        return result;
    }

    /**
     * @author Starlev
     * @reason Bypass redundant secondary TreeMap allocation when only one namespace manager is present.
     */
    @Overwrite
    public Map<Identifier, List<Resource>> listResourceStacks(String path, Predicate<Identifier> filter) {
        checkTrailingDirectoryPath(path);
        Collection<FallbackResourceManager> managers = this.namespacedManagers.values();
        int size = managers.size();
        if (size == 0) {
            return new TreeMap<>();
        }
        if (size == 1) {
            return managers.iterator().next().listResourceStacks(path, filter);
        }
        TreeMap<Identifier, List<Resource>> result = new TreeMap<>();
        for (FallbackResourceManager manager : managers) {
            result.putAll(manager.listResourceStacks(path, filter));
        }
        return result;
    }
}
