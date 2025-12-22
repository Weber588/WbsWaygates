package wbs.waygates;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class WbsWaygatesBootstrap implements PluginBootstrap {

    private WaygatesBootstrapSettings settings;

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new WbsWaygates();
    }

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        settings = new WaygatesBootstrapSettings(context);
        settings.reload();
    }
}