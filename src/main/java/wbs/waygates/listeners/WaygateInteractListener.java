package wbs.waygates.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import wbs.waygates.data.Waygate;
import wbs.waygates.util.WaygateUtils;

public class WaygateInteractListener implements Listener {
    @EventHandler
    public void onWaygateInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();

        // Crafting, not crafter or workbench -- represents the internal player inventory. Returned when nothing open.
        InventoryType inventoryType = player.getOpenInventory().getType();
        if (inventoryType != InventoryType.CRAFTING && inventoryType != InventoryType.CREATIVE) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            Waygate waygate = WaygateUtils.getWaygate(clickedBlock);

            if (waygate != null) {
                event.setCancelled(true);
                // TODO: Warp player to the in between
                waygate.triggerPlayerTeleport(player);
            }
        }
    }
}
