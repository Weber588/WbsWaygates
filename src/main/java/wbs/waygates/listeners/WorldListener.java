package wbs.waygates.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.util.Vector;
import wbs.waygates.WbsWaygates;
import wbs.waygates.util.PersistentWaygateType;
import wbs.waygates.world.WorldManager;

public class WorldListener implements Listener {
    @EventHandler
    public void onNetherLight(PortalCreateEvent event) {
        if (WorldManager.isInWorld(event.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFire(BlockPlaceEvent event) {
        Block block = event.getBlock();

        // Don't run on soul fire -- it'll go out naturally
        if (block.getType() != Material.FIRE) {
            return;
        }

        if (WorldManager.isInWorld(block.getWorld())) {
            WbsWaygates.getInstance().runLater(() -> {
                if (block.getType() == Material.FIRE) {
                    block.setType(Material.VOID_AIR);
                }
            }, (int) (Math.random() * 2 * Ticks.TICKS_PER_SECOND));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Integer damagedByDarknessTick = player.getPersistentDataContainer().get(
                WorldManager.DAMAGED_BY_DARKNESS,
                PersistentWaygateType.INTEGER
        );
        WbsWaygates.getInstance().getLogger().info("damagedByDarknessTick: " + damagedByDarknessTick);
        if (damagedByDarknessTick != null) {
            int currentTick = Bukkit.getCurrentTick();
            WbsWaygates.getInstance().getLogger().info("currentTick: " + currentTick);
            if (damagedByDarknessTick == currentTick || damagedByDarknessTick == currentTick - 1) {
                event.deathMessage(Component.text(player.getName() + " was swallowed by darkness"));
            }
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (WorldManager.isInWorld(item.getWorld())) {
            Vector velocity = item.getVelocity();

            item.setVelocity(WorldManager.getWindDirection().clone().setY(velocity.getY()));
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        World world = WorldManager.getWorld();

        if (world == null || !chunk.getWorld().equals(world)) {
            return;
        }

        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (!block.isEmpty()) {
                        WorldManager.tryBreakLight(block);
                    }
                }
            }
        }
    }
}
