package lomka.starl.mixins.net.minecraft.world.level.chunk.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkStatus.class)
public abstract class MixinChunkStatus {

	@Unique private static List<ChunkStatus> lomka$cachedStatusList;

	/**
	 * @author Starlev
	 * @reason Caches the FULL->EMPTY status chain. Since 1.21 ChunkGenerationTask.scheduleNextLayer
	 *         rebuilds a fresh ArrayList on every layer of every chunk generation task (8-10 lists
	 *         per chunk during loading bursts), the allocation churn shows up on the worldgen threads.
	 *         The chain is static after registration, so a single cached list is returned instead.
	 */
	@Overwrite
	public static List<ChunkStatus> getStatusList() {
		List<ChunkStatus> list = lomka$cachedStatusList;
		if (list == null) {
			list = new ArrayList<>();
			ChunkStatus chunkstatus = ChunkStatus.FULL;
			for (; chunkstatus.getParent() != chunkstatus; chunkstatus = chunkstatus.getParent()) {
				list.add(chunkstatus);
			}
			list.add(chunkstatus);
			Collections.reverse(list);
			lomka$cachedStatusList = list;
		}
		return list;
	}
}