package lomka.starl.mixins.net.minecraft.core;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.IdMapper;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IdMapper.class)
public abstract class MixinIdMapper<T> {

    @Shadow private int nextId;
    //? if >=1.21 {
    @Shadow @Final private Reference2IntMap<T> tToId;
    //?} else {
    /*@Shadow @Final private it.unimi.dsi.fastutil.objects.Object2IntMap<T> tToId;
    *///?}
    @Shadow @Final private List<T> idToT;

    @Unique
    private T[] lomka$byId;

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>(I)V", at = @At("TAIL"))
    private void lomka$initArray(int i, CallbackInfo ci) {
        this.lomka$byId = (T[]) new Object[Math.max(i, 1)];
    }

    /**
     * @author Starlev
     * @reason Mirror idToT into a raw array during bootstrap (one-time cost, only runs
     *         at registry population) so byId() — called on every palette/block-state resolution
     *         during chunk decode and mesh building — can skip List-interface dispatch and
     *         ArrayList's own internal (redundant with ours) bounds check. idToT itself stays
     *         fully maintained so iterator() keeps working unchanged.
     */
    @Overwrite
    public void addMapping(T t0, int i) {
        this.tToId.put(t0, i);

        while (this.idToT.size() <= i) {
            this.idToT.add(null);
        }
        this.idToT.set(i, t0);

        if (i >= this.lomka$byId.length) {
            this.lomka$byId = Arrays.copyOf(this.lomka$byId, Math.max(i + 1, this.lomka$byId.length * 2));
        }
        this.lomka$byId[i] = t0;

        if (this.nextId <= i) {
            this.nextId = i + 1;
        }
    }

    /**
     * @author Starlev
     * @reason Single bounds check + direct array read, replacing vanilla's List-interface
     *         dispatch plus ArrayList's own internal bounds check on every call.
     */
    @Overwrite
    public @Nullable T byId(int i) {
        T[] arr = this.lomka$byId;
        return i >= 0 && i < arr.length ? arr[i] : null;
    }
}