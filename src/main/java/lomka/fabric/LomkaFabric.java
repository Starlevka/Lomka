package lomka.fabric;

import lomka.Lomka;
import net.fabricmc.api.ModInitializer;

public class LomkaFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Lomka.init();
    }
}