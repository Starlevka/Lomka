package lomka.forge;

import lomka.Lomka;
import net.minecraftforge.fml.common.Mod;

@Mod(Lomka.MOD_ID)
public class LomkaForge {
    public LomkaForge() {
        Lomka.init();
    }
}
