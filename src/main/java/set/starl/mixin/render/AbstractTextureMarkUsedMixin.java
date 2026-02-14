package set.starl.mixin.render;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractTexture.class)
public class AbstractTextureMarkUsedMixin {
	@Unique
	private static volatile Method LOMKA$MARK_USED;

	@Unique
	private static Method lomka$markUsedMethod(final TextureManager textureManager) {
		Method m = LOMKA$MARK_USED;
		if (m != null) {
			return m;
		}
		try {
			m = textureManager.getClass().getMethod("lomka$markTextureUsed", AbstractTexture.class);
		} catch (Exception e) {
			m = null;
		}
		LOMKA$MARK_USED = m;
		return m;
	}

	@Unique
	private void lomka$markUsed() {
		Minecraft minecraft;
		try {
			minecraft = Minecraft.getInstance();
		} catch (Throwable ignored) {
			return;
		}
		if (minecraft == null) {
			return;
		}
		TextureManager textureManager = minecraft.getTextureManager();
		if (textureManager == null) {
			return;
		}
		Method m = lomka$markUsedMethod(textureManager);
		if (m == null) {
			return;
		}
		try {
			m.invoke(textureManager, (AbstractTexture)(Object)this);
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "getTextureView", at = @At("HEAD"))
	private void lomka$markUsedOnGetTextureView(final CallbackInfoReturnable<?> cir) {
		this.lomka$markUsed();
	}

	@Inject(method = "getTexture", at = @At("HEAD"))
	private void lomka$markUsedOnGetTexture(final CallbackInfoReturnable<?> cir) {
		this.lomka$markUsed();
	}
}
