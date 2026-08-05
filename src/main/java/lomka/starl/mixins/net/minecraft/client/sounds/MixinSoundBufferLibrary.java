package lomka.starl.mixins.net.minecraft.client.sounds;

import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.Identifier;
*///?}
import net.minecraft.server.packs.resources.ResourceProvider;
//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundBufferLibrary.class)
public abstract class MixinSoundBufferLibrary {

    //? if >=1.21.11 {
    @Shadow
    @Final
    @Mutable
    private Map<Identifier, CompletableFuture<SoundBuffer>> cache;
    //?} else {
    /*@Shadow
    @Final
    @Mutable
    private Map<Identifier, CompletableFuture<SoundBuffer>> cache;
    *///?}

    @Shadow
    @Final
    private ResourceProvider resourceManager;

    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void lomka$initConcurrentCache(ResourceProvider resourceProvider, CallbackInfo ci) {
        this.cache = new ConcurrentHashMap<>();
    }

    //? if >=1.21.11 {
    /**
     * @author Starlev
     * @reason Safe async sound buffer loading with automatic failed future eviction
     */
    @Overwrite
    public CompletableFuture<SoundBuffer> getCompleteBuffer(Identifier identifier) {
        return this.cache.computeIfAbsent(identifier, id -> {
            CompletableFuture<SoundBuffer> future = CompletableFuture.supplyAsync(() -> {
                try (InputStream inputStream = this.resourceManager.open(id);
                     JOrbisAudioStream audioStream = new JOrbisAudioStream(inputStream)) {
                    ByteBuffer byteBuffer = audioStream.readAll();
                    return new SoundBuffer(byteBuffer, audioStream.getFormat());
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, Util.nonCriticalIoPool());

            future.whenComplete((buffer, throwable) -> {
                if (throwable != null) {
                    this.cache.remove(id, future);
                }
            });

            return future;
        });
    }
    //?} else {
    /*@Overwrite
    public CompletableFuture<SoundBuffer> getCompleteBuffer(Identifier identifier) {
        return this.cache.computeIfAbsent(identifier, id -> {
            CompletableFuture<SoundBuffer> future = CompletableFuture.supplyAsync(() -> {
                try (InputStream inputStream = this.resourceManager.open(id);
                     JOrbisAudioStream audioStream = new JOrbisAudioStream(inputStream)) {
                    ByteBuffer byteBuffer = audioStream.readAll();
                    return new SoundBuffer(byteBuffer, audioStream.getFormat());
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, Util.nonCriticalIoPool());

            future.whenComplete((buffer, throwable) -> {
                if (throwable != null) {
                    this.cache.remove(id, future);
                }
            });

            return future;
        });
    }
    *///?}

    /**
     * @author Starlev
     * @reason Zero-allocation sound preloading bypassing Stream API
     */
    @Overwrite
    public CompletableFuture<?> preload(Collection<Sound> collection) {
        if (collection == null || collection.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        int size = collection.size();
        CompletableFuture<?>[] futures = new CompletableFuture[size];
        int i = 0;
        for (Sound sound : collection) {
            futures[i++] = this.getCompleteBuffer(sound.getPath());
        }

        return CompletableFuture.allOf(futures);
    }
}
