package wbs.waygates;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import wbs.utils.util.commands.brigadier.WbsCommand;
import wbs.utils.util.plugin.WbsPlugin;
import wbs.waygates.command.CommandWaygateGet;
import wbs.waygates.listeners.*;
import wbs.waygates.world.WorldManager;

public class WbsWaygates extends WbsPlugin {
    private static WbsWaygates INSTANCE;
    public static @NotNull WbsWaygates getInstance() {
        return INSTANCE;
    }

    private WaygateSettings settings;

    public static NamespacedKey getKey(String keyString) {
        return new NamespacedKey(getInstance(), keyString);
    }

    @Override
    public WaygateSettings getSettings() {
        return settings;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        this.settings = new WaygateSettings(this);
        settings.reload();

        registerListener(new WaygateChangeListener());
        registerListener(new WaygateInteractListener());
        registerListener(new WaygateLoadListener());
        registerListener(new WorldListener());
        registerListener(new PlayerListener());

        WbsCommand.getStatic(this, "wbswaygates")
                .addSubcommands(
                        new CommandWaygateGet(this),
                        getErrorsCommand(),
                        getReloadCommand()
                )
                .addAliases("wbswg", "wg", "wwg", "wbswaygate", "waygate", "waygates")
                .register();

        WorldManager.startWorldTimers();
    }
}
