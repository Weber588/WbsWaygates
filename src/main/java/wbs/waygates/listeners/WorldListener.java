package wbs.waygates.listeners;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import wbs.waygates.WbsWaygates;
import wbs.waygates.util.PersistentWaygateType;
import wbs.waygates.world.WorldManager;

public class WorldListener implements Listener {

    public static final NamespacedKey BEACON_IMMUNE_TO_FOG = WbsWaygates.getKey("beacon_immune_to_fog");

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
        PersistentDataContainer container = player.getPersistentDataContainer();
        Integer damagedByDarknessTick = container.get(
                WorldManager.DAMAGED_BY_DARKNESS,
                PersistentWaygateType.INTEGER
        );
        if (damagedByDarknessTick != null) {
            int currentTick = Bukkit.getCurrentTick();
            if (damagedByDarknessTick == currentTick || damagedByDarknessTick == currentTick - 1) {
                event.deathMessage(Component.text(player.getName() + " was swallowed by darkness"));
                container.remove(WorldManager.DAMAGED_BY_DARKNESS);
            }
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (WorldManager.isInWorld(item.getWorld())) {
            item.setVelocity(item.getVelocity().add(WorldManager.getItemPushVelocity().multiply(5)));
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

    @EventHandler
    public void onBeaconEffect(BeaconEffectEvent event) {
        Player player = event.getPlayer();
        if (WorldManager.isInWorld(player.getWorld())) {
            WorldManager.removeFakeFog(player);
            int lastAppliedTick = Bukkit.getCurrentTick();
            player.getPersistentDataContainer().set(BEACON_IMMUNE_TO_FOG, PersistentDataType.INTEGER, lastAppliedTick);

            WbsWaygates.getInstance().runLater(() -> {
                Player updatedPlayer = Bukkit.getPlayer(player.getUniqueId());
                if (updatedPlayer != null && updatedPlayer.isOnline()) {
                    Integer appliedTick = updatedPlayer.getPersistentDataContainer().get(BEACON_IMMUNE_TO_FOG, PersistentDataType.INTEGER);
                    if (appliedTick != null && appliedTick == lastAppliedTick) {
                        WorldManager.addFakeFog(updatedPlayer);
                        updatedPlayer.getPersistentDataContainer().remove(BEACON_IMMUNE_TO_FOG);
                    }
                }
            }, event.getEffect().getDuration());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (WorldManager.isInWorld(event.getTo().getWorld())) {
            WorldManager.addFakeFog(event.getPlayer());
        } else {
            WorldManager.removeFakeFog(event.getPlayer());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (WorldManager.isInWorld(event.getPlayer().getWorld())) {
            WorldManager.addFakeFog(event.getPlayer());
        } else {
            WorldManager.removeFakeFog(event.getPlayer());
        }
    }
}
