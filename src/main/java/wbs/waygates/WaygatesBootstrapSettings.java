package wbs.waygates;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jspecify.annotations.NullMarked;
import wbs.utils.util.plugin.bootstrap.WbsBootstrapSettings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@SuppressWarnings("UnstableApiUsage")
@NullMarked
public class WaygatesBootstrapSettings extends WbsBootstrapSettings<WbsWaygates> {
    @SuppressWarnings("NotNullFieldNotInitialized")
    private static WaygatesBootstrapSettings INSTANCE;

    public static WaygatesBootstrapSettings getInstance() {
        return INSTANCE;
    }

    public WaygatesBootstrapSettings(BootstrapContext context) {
        super(context, WbsWaygates.class);

        INSTANCE = this;
    }

    @Override
    public void reload() {
        loadDimension();
    }

    private void loadDimension() {
        String waygateTypesFolderName = "WbsWaygatesDimension";

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY.newHandler(event -> {
            try {
                // Retrieve the URI of the datapack folder.
                URI uri = this.getClass().getResource(File.separator + waygateTypesFolderName).toURI();
                // Discover the pack. The ID is set to "provided", which indicates to
                // a server owner that your plugin includes this data pack.
                event.registrar().discoverPack(uri, "provided");
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}

