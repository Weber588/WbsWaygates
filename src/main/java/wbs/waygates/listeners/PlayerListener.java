package wbs.waygates.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import wbs.waygates.WbsWaygates;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (recipe instanceof Keyed keyed) {
                if (keyed.key().namespace().equalsIgnoreCase(WbsWaygates.getInstance().getName())) {
                    player.discoverRecipe(keyed.getKey());
                }
            }
        });
    }
}
