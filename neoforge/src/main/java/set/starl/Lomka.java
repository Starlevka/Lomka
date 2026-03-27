package set.starl;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import set.starl.LomkaCore;
import set.starl.config.LomkaConfigScreen;

@Mod(LomkaCore.MOD_ID)
public class Lomka {
	public Lomka() {
		LomkaCore.init();
		ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (client, parent) -> LomkaConfigScreen.create(parent));
	}
}
