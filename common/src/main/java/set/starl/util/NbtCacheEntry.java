package set.starl.util;

import net.minecraft.nbt.CompoundTag;

public record NbtCacheEntry(CompoundTag tag, long mtime, long size) {}
