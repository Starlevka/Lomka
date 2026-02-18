package set.starl.mixin.render;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrackingItemStackRenderState.class)
public class TrackingItemStackRenderStateModelIdentityCacheMixin {
	@Shadow
	@Final
	private List modelIdentityElements;

	@Unique
	private Object lomka$modelIdentityKey;

	@Unique
	private int lomka$modelIdentitySize = -1;

	@Inject(method = "appendModelIdentityElement(Ljava/lang/Object;)V", at = @At("HEAD"))
	private void lomka$invalidateModelIdentityCache(final Object element, final CallbackInfo ci) {
		this.lomka$modelIdentityKey = null;
		this.lomka$modelIdentitySize = -1;
	}

	@Inject(method = "getModelIdentity()Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
	private void lomka$getModelIdentityCached(final CallbackInfoReturnable<Object> cir) {
		int size = this.modelIdentityElements.size();
		Object cached = this.lomka$modelIdentityKey;
		if (cached != null && this.lomka$modelIdentitySize == size) {
			cir.setReturnValue(cached);
			return;
		}

		Object[] elements = this.modelIdentityElements.toArray();
		ModelIdentityKey key = new ModelIdentityKey(elements);
		this.lomka$modelIdentityKey = key;
		this.lomka$modelIdentitySize = size;
		cir.setReturnValue(key);
	}

	@Unique
	private static final class ModelIdentityKey {
		private final Object[] elements;
		private final int hash;

		private ModelIdentityKey(final Object[] elements) {
			this.elements = elements;
			this.hash = Arrays.hashCode(elements);
		}

		@Override
		public int hashCode() {
			return this.hash;
		}

		@Override
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof ModelIdentityKey other)) {
				return false;
			}
			return Arrays.equals(this.elements, other.elements);
		}
	}
}
