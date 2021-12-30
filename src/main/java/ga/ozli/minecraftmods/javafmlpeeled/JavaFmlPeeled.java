package ga.ozli.minecraftmods.javafmlpeeled;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import ga.ozli.minecraftmods.javafmlpeeled.javafmlmodpeeled.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("javafmlpeeled")
public class JavaFmlPeeled
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public JavaFmlPeeled() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("javafmlpeeled dummymod started");
    }

}
