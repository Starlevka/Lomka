package set.starl.injection.mixins.crash;

import net.minecraft.CrashReportCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrashReportCategory.class)
public class CrashReportCategoryFileNameNullGuardMixin {
	@Redirect(
		method = "validateStackTrace",
		at = @At(value = "INVOKE", target = "Ljava/lang/StackTraceElement;getFileName()Ljava/lang/String;")
	)
	private String lomka$nullSafeFileName(final StackTraceElement element) {
		String fileName = element.getFileName();
		return fileName == null ? "\u0000" : fileName;
	}
}
