package wbs.waygates.listeners;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import wbs.utils.util.persistent.WbsPersistentDataType;
import wbs.waygates.WaygateRegistries;
import wbs.waygates.WbsWaygates;
import wbs.waygates.data.Waygate;
import wbs.waygates.data.WaygateType;
import wbs.waygates.util.WaygateUtils;
import wbs.waygates.world.WorldManager;

import java.util.LinkedList;
import java.util.List;

public class WaygateChangeListener implements Listener {
    @EventHandler
    public void onWaygatePlace(BlockPlaceEvent event) {
        ItemStack heldItem = event.getItemInHand();

        PersistentDataContainerView itemContainer = heldItem.getPersistentDataContainer();
        if (!itemContainer.has(WaygateType.WAYGATE_TYPE_KEY)) {
            return;
        }

        Player player = event.getPlayer();

        NamespacedKey typeKey = itemContainer.get(WaygateType.WAYGATE_TYPE_KEY, WbsPersistentDataType.NAMESPACED_KEY);
        WaygateType matchingType = WaygateRegistries.WAYGATE_TYPES.get(typeKey);

        if (matchingType == null) {
            WbsWaygates.getInstance().sendMessage("Item was waygate but found no matching type -- please report this.\n"
                    + heldItem.getType().key() + ":" + heldItem.getItemMeta().getAsComponentString(), player);
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlockPlaced();
        if (!matchingType.canSpawn(block)) {
            WbsWaygates.getInstance().sendMessage("Cannot place there!", player);
            event.setCancelled(true);
            return;
        }

        World customDimension = WorldManager.getWorld();
        if (customDimension == null) {
            throw new IllegalStateException("Dimension not loaded!");
        }

        if (player.getWorld().equals(customDimension)) {
            player.playSound(block.getLocation().toCenterLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1, 1);
            Material baseBlockType = matchingType.getBaseBlockType();
            player.spawnParticle(
                    Particle.BLOCK,
                    block.getLocation().toCenterLocation(),
                    100,
                    0.5,
                    0.5,
                    0.5,
                    0,
                    baseBlockType.createBlockData()
            );
            event.setCancelled(true);
            return;
        }

        Waygate waygate = matchingType.spawn(block);

        /*
            container.set(MEMBERS_KEY, PersistentDataType.LIST.listTypeFrom(PersistentWaygateMemberType.INSTANCE), members);
            container.set(VISIBILITY, new EnumPersistentDataType<>(WaygateVisibility.class), visibility);
         */

        player.showTitle(Title.title(Component.text("Waygate created!"), Component.empty()));
    }

    @EventHandler
    public void onWaygateBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        Player player = event.getPlayer();

        Waygate waygate = WaygateUtils.getWaygate(broken);

        if (waygate != null) {
            event.setCancelled(true);

            WorldManager.isInWorld(broken.getWorld());
            waygate.breakNaturally(player);
        }
    }

    @EventHandler
    public void onWaygateBreak(BlockBreakBlockEvent event) {
        Block broken = event.getBlock();
        Waygate waygate = WaygateUtils.getWaygate(broken);

        if (waygate != null) {
            waygate.breakNaturally();
        }
    }

    @EventHandler
    public void onWaygateBreak(BlockExplodeEvent event) {
        List<Block> broken = event.blockList();

        List<Block> toRemove = new LinkedList<>();
        for (Block block : broken) {
            Waygate waygate = WaygateUtils.getWaygate(block);

            if (waygate != null) {
                toRemove.add(block);
            }
        }

        broken.removeAll(toRemove);
    }

    @EventHandler
    public void onWaygateBreak(EntityExplodeEvent event) {
        List<Block> broken = event.blockList();

        List<Block> toRemove = new LinkedList<>();
        for (Block block : broken) {
            Waygate waygate = WaygateUtils.getWaygate(block);

            if (waygate != null) {
                toRemove.add(block);
            }
        }

        broken.removeAll(toRemove);
    }

    @EventHandler
    public void onWaygatePiston(BlockPistonExtendEvent event) {
        List<Block> broken = event.getBlocks();

        for (Block block : broken) {
            Waygate waygate = WaygateUtils.getWaygate(block);

            if (waygate != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onWaygatePiston(BlockPistonRetractEvent event) {
        List<Block> broken = event.getBlocks();

        for (Block block : broken) {
            Waygate waygate = WaygateUtils.getWaygate(block);

            if (waygate != null) {
                event.setCancelled(true);
            }
        }
    }
}
