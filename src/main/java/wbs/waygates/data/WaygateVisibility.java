package wbs.waygates.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.TriState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum WaygateVisibility {
    /**
     * Anyone can warp to this waygate, even if they've never seen it before.
     */
    GLOBAL(
            Component.text("Global").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
            Component.text("Visible to all players, even"),
            Component.text("if they've never seen it.")
    ),
    /**
     * The owner and people added can view warp to this waygate, but anyone
     * can add warp to it after right-clicking on it once
     */
    DISCOVERABLE(
            Component.text("Discoverable").color(NamedTextColor.GRAY).decorate(TextDecoration.BOLD),
            Component.text("Visible to players who have"),
            Component.text("right clicked the waygate"),
            Component.text("at least once.")
    ),
    /**
     * Only the owner and people they explicitly add can warp to this waygate.
     */
    PRIVATE(
            Component.text("Private").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD),
            Component.text("Only visible to users"),
            Component.text("specifically added by the"),
            Component.text("owner.")
    ),
    ;

    final Component display;
    final List<Component> description;

    WaygateVisibility(Component display, Component ... description) {
        this(display, List.of(description));
    }

    WaygateVisibility(Component display, List<Component> description) {
        this.display = display;
        this.description = description.stream()
                .map(component ->
                    component
                            .color(NamedTextColor.GRAY)
                            .style(Style.style(TextDecoration.ITALIC.withState(TriState.FALSE)))
                )
                .toList();
    }

    public Component display() {
        return display;
    }

    public List<Component> description() {
        return description;
    }

    public boolean canUse(Player player) {
        if (this == PRIVATE) {
            return true;
        }
        return player.hasPermission("wbswaygates.visibility." + name().toLowerCase());
    }

    public static @NotNull WaygateVisibility getNextVisibility(WaygateVisibility visibility, Player player) {
        return switch (visibility) {
            case GLOBAL -> {
                if (WaygateVisibility.DISCOVERABLE.canUse(player)) {
                    yield WaygateVisibility.DISCOVERABLE;
                } else {
                    yield WaygateVisibility.PRIVATE;
                }
            }
            case DISCOVERABLE -> WaygateVisibility.PRIVATE;
            case PRIVATE -> {
                if (WaygateVisibility.GLOBAL.canUse(player)) {
                    yield WaygateVisibility.GLOBAL;
                } else if (WaygateVisibility.DISCOVERABLE.canUse(player)) {
                    yield WaygateVisibility.DISCOVERABLE;
                }
                yield WaygateVisibility.PRIVATE;
            }
        };
    }
}
