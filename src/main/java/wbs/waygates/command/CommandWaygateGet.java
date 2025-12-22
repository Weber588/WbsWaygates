package wbs.waygates.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wbs.utils.util.commands.brigadier.WbsSubcommand;
import wbs.utils.util.commands.brigadier.argument.WbsSimpleArgument;
import wbs.utils.util.plugin.WbsPlugin;
import wbs.waygates.WaygateRegistries;
import wbs.waygates.data.WaygateType;

import java.util.stream.Collectors;

public class CommandWaygateGet extends WbsSubcommand {
    private static final WbsSimpleArgument.KeyedSimpleArgument TYPE = new WbsSimpleArgument.KeyedSimpleArgument(
            "type",
            ArgumentTypes.namespacedKey(),
            null
    ).setKeyedSuggestions(WaygateRegistries.WAYGATE_TYPES.values());

    public CommandWaygateGet(@NotNull WbsPlugin plugin) {
        super(plugin, "get");

        setPermission(plugin.getName().toLowerCase() + ".command." + getLabel());

        addSimpleArgument(TYPE);
    }

    @Override
    protected int executeNoArgs(CommandContext<CommandSourceStack> context) {
        return sendSimpleArgumentUsage(context);
    }

    @Override
    protected int onSimpleArgumentCallback(CommandContext<CommandSourceStack> context, WbsSimpleArgument.ConfiguredArgumentMap configuredArgumentMap) {
        NamespacedKey typeKey = configuredArgumentMap.get(TYPE);

        CommandSender sender = context.getSource().getSender();
        if (typeKey == null) {
            plugin.sendMessage("Choose a waygate type: "
                            + WaygateRegistries.WAYGATE_TYPES.stream()
                            .map(Keyed::key)
                            .map(Key::asString)
                            .collect(Collectors.joining(", ")),
                    sender);
            return Command.SINGLE_SUCCESS;
        }

        WaygateType type = WaygateRegistries.WAYGATE_TYPES.get(typeKey);
        if (type == null) {
            plugin.sendMessage("Invalid type \"" + typeKey.asString() + "\". Choose a waygate type: "
                            + WaygateRegistries.WAYGATE_TYPES.stream()
                            .map(Keyed::key)
                            .map(Key::asString)
                            .collect(Collectors.joining(", ")),
                    sender);
            return Command.SINGLE_SUCCESS;
        }

        if (!(sender instanceof Player player)) {
            plugin.sendMessage("This command is only usable by players.", sender);
            return Command.SINGLE_SUCCESS;
        }

        player.give(type.getItem());
        plugin.sendMessage("Received waygate.", player);

        return Command.SINGLE_SUCCESS;
    }
}
