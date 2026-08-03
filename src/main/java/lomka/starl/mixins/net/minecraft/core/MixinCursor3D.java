package lomka.starl.mixins.net.minecraft.core;

import net.minecraft.core.Cursor3D;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Cursor3D.class)
public abstract class MixinCursor3D {

	@Shadow private int index;
	@Shadow private int end;
	@Shadow private int x;
	@Shadow private int y;
	@Shadow private int z;
	@Shadow private int width;
	@Shadow private int height;

	/**
	 * @author Starlev
	 * @reason Replaces two integer divisions (and two modulos) per voxel in
	 * vanilla's index decode with incremental counter stepping. Iteration order
	 * and nextX/Y/Z/getNextType outputs are byte-identical; the (0,0,0) first
	 * cell is preserved via the index>0 guard (fresh cursors start at index 0).
	 */
	@Overwrite
	public boolean advance() {
		if (this.index == this.end) {
			return false;
		}
		if (this.index > 0) {
			this.x++;
			if (this.x == this.width) {
				this.x = 0;
				this.y++;
				if (this.y == this.height) {
					this.y = 0;
					this.z++;
				}
			}
		}
		this.index++;
		return true;
	}
}