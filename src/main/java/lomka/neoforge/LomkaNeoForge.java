package lomka.neoforge;

import lomka.Lomka;
import net.neoforged.fml.common.Mod;

@Mod(Lomka.MOD_ID)
public class LomkaNeoForge {
    public LomkaNeoForge() {
        Lomka.init();
    }
}
