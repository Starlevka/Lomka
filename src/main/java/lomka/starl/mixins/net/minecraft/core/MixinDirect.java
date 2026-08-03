package lomka.starl.mixins.net.minecraft.core;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Set;

@Mixin(targets = "net.minecraft.core.HolderSet$Direct")
public abstract class MixinDirect<T> {

	@Shadow private List<Holder<T>> contents;

	@Shadow private Set<Holder<T>> contentsSet;

	/**
	 * @author Starlev
	 * @reason Avoids materialising a Set.copyOf for tiny direct holder sets.
	 * Linear scans of 1..4 elements beat hashing (allocation + hash lookup)
	 * on the hot tag-membership path; larger sets keep the lazy Set cache.
	 */
	@Overwrite
	public boolean contains(Holder<T> holder) {
		if (this.contents.size() <= 4) {
			return this.contents.contains(holder);
		}
		if (this.contentsSet == null) {
			this.contentsSet = Set.copyOf(this.contents);
		}
		return this.contentsSet.contains(holder);
	}
}