//? if >=1.21.11 {
package lomka.starl.mixins.com.mojang.blaze3d.systems;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SamplerCache.class)
public abstract class MixinSamplerCache {

    @Shadow @Final private GpuSampler[] samplers;

    @Overwrite
    public void initialize() {
        GpuDevice gpudevice = RenderSystem.getDevice();

        AddressMode[] addressModes = AddressMode.values();
        FilterMode[] filterModes = FilterMode.values();

        if (addressModes.length != 2 || filterModes.length != 2) {
            throw new IllegalStateException("AddressMode and FilterMode enum sizes must be 2 - if you expanded them, please update SamplerCache");
        }

        if (FilterMode.NEAREST.ordinal() != 0 || FilterMode.LINEAR.ordinal() != 1) {
            throw new IllegalStateException("FilterMode ordinal order changed - update getClampToEdge/getRepeat shortcuts in SamplerCache");
        }

        OptionalDouble zeroDouble = OptionalDouble.of(0.0D);
        OptionalDouble emptyDouble = OptionalDouble.empty();

        for (AddressMode addressmode : addressModes) {
            for (AddressMode addressmode1 : addressModes) {
                for (FilterMode filtermode : filterModes) {
                    for (FilterMode filtermode1 : filterModes) {
                        for (int flagInt = 0; flagInt <= 1; flagInt++) {
                            boolean flag = flagInt == 1;

                            int index = (addressmode.ordinal() & 1)
                                      | ((addressmode1.ordinal() & 1) << 1)
                                      | ((filtermode.ordinal() & 1) << 2)
                                      | ((filtermode1.ordinal() & 1) << 3)
                                      | (flagInt << 4);

                            this.samplers[index] = gpudevice.createSampler(
                                    addressmode, addressmode1, filtermode, filtermode1, 1,
                                    flag ? emptyDouble : zeroDouble
                            );
                        }
                    }
                }
            }
        }
    }

    @Overwrite
    public GpuSampler getSampler(AddressMode addressmode, AddressMode addressmode1, FilterMode filtermode, FilterMode filtermode1, boolean flag) {
        int index = (addressmode.ordinal() & 1)
                  | ((addressmode1.ordinal() & 1) << 1)
                  | ((filtermode.ordinal() & 1) << 2)
                  | ((filtermode1.ordinal() & 1) << 3)
                  | (flag ? 16 : 0);
        return this.samplers[index];
    }

    @Overwrite
    public GpuSampler getClampToEdge(FilterMode filtermode) {
        int index = 3 | ((filtermode.ordinal() & 1) * 12);
        return this.samplers[index];
    }

    @Overwrite
    public GpuSampler getClampToEdge(FilterMode filtermode, boolean flag) {
        int index = 3 | ((filtermode.ordinal() & 1) * 12) | (flag ? 16 : 0);
        return this.samplers[index];
    }

    @Overwrite
    public GpuSampler getRepeat(FilterMode filtermode) {
        int index = (filtermode.ordinal() & 1) * 12;
        return this.samplers[index];
    }

    @Overwrite
    public GpuSampler getRepeat(FilterMode filtermode, boolean flag) {
        int index = ((filtermode.ordinal() & 1) * 12) | (flag ? 16 : 0);
        return this.samplers[index];
    }
}
//?}
