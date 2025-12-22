package wbs.waygates;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import wbs.utils.exceptions.InvalidConfigurationException;
import wbs.utils.util.plugin.WbsPlugin;
import wbs.utils.util.plugin.WbsSettings;
import wbs.waygates.data.WaygateType;

import java.io.File;

public class WaygateSettings extends WbsSettings {

    protected WaygateSettings(WbsPlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
        loadDefaultConfig("config.yml");

        loadWaygateTypes();
    }

    private void loadWaygateTypes() {
        String waygateTypesFolderName = "waygate_types";
        final File typesDirectory =  new File(plugin.getDataFolder() + File.separator + waygateTypesFolderName);

        if (!typesDirectory.exists()) {
            plugin.saveResourceFolder(waygateTypesFolderName, false);
        }

        if (!typesDirectory.exists()) {
            logError("Failed to create types directory. Please contact your system administrator.", "Internal");
            return;
        }

        File[] typeFiles = typesDirectory.listFiles();
        if (typeFiles == null) {
            logError("An unexpected error occurred due while loading the waygate_types directory. Please contact your system administrator.", "Internal");
            return;
        }

        int successful = 0;
        int failed = 0;

        for (File file : typeFiles) {
            ConfigurationSection section = loadConfigSafely(file);

            String typeName = file.getName().substring(0, file.getName().lastIndexOf("."));

            try {
                WaygateType type = new WaygateType(new NamespacedKey(plugin, typeName), section);

                WaygateRegistries.WAYGATE_TYPES.register(type);

                successful++;
            } catch (InvalidConfigurationException ex) {
                failed++;
            }
        }

        if (successful > 0) {
            getComponentLogger().info(Component.text("Successfully loaded " + successful + " waygate types.").color(NamedTextColor.GREEN));
        }
        if (failed > 0) {
            getComponentLogger().info(Component.text("Failed to load " + successful + " waygate types.").color(NamedTextColor.RED));
        }
    }
}
