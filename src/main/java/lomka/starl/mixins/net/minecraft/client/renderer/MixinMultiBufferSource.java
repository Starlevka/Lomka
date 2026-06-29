package lomka.starl.mixins.net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiBufferSource.BufferSource.class)
public abstract class MixinMultiBufferSource {

    @Shadow public abstract void endLastBatch();
    @Shadow public abstract void endBatch(RenderType rendertype);

    @Shadow @Final @Mutable protected Map<RenderType, BufferBuilder> startedBuilders;

    @Unique private RenderType[] lomka$cachedFixedTypes;

    /**
     * @author Starlev
     * @reason Pre-size startedBuilders to 16 (typical active render type count) and cache fixed buffer type keys as array.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void lomka$onInit(ByteBufferBuilder bytebufferbuilder, SequencedMap<RenderType, ByteBufferBuilder> sequencedmap, CallbackInfo ci) {
        this.lomka$cachedFixedTypes = sequencedmap.keySet().toArray(new RenderType[0]);
        this.startedBuilders = new HashMap<>(16);
    }

    @Overwrite
    public void endBatch() {
        this.endLastBatch();

        RenderType[] types = this.lomka$cachedFixedTypes;
        for (int i = 0; i < types.length; ++i) {
            this.endBatch(types[i]);
        }
    }
}