package wbs.waygates.listeners;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import io.papermc.paper.event.block.BeaconActivatedEvent;
import io.papermc.paper.event.block.BeaconDeactivatedEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.Ticks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.*;
import org.bukkit.block.Beacon;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import wbs.waygates.WbsWaygates;
import wbs.waygates.util.PersistentWaygateType;
import wbs.waygates.world.WorldManager;

import java.util.*;
import java.util.stream.Collectors;

public class WorldListener implements Listener {

    public static final NamespacedKey BEACON_IMMUNE_TO_FOG = WbsWaygates.getKey("beacon_immune_to_fog");
    public static final @Nullable Biome VOID_NEXUS = getBiome("void_nexus");
    // TODO: Programmatically generate these
    public static final @Nullable Biome VOID_NEXUS_LIGHT_1 = getBiome("void_nexus_light_1");
    public static final @Nullable Biome VOID_NEXUS_LIGHT_2 = getBiome("void_nexus_light_2");
    public static final @Nullable Biome VOID_NEXUS_LIGHT_3 = getBiome("void_nexus_light_3");
    public static final @Nullable Biome VOID_NEXUS_LIGHT_4 = getBiome("void_nexus_light_4");

    private static @org.jetbrains.annotations.Nullable Biome getBiome(String biomeName) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(WbsWaygates.getKey(biomeName));
    }

    public static final int MAX_BEACON_RANGE = 50;// Double max beacon range to account for any mods and stuff
    public static final int CHUNK_CHECK_RANGE = MAX_BEACON_RANGE * 2 / 16;

    static {
        if (VOID_NEXUS == null) {
            WbsWaygates.getInstance().getLogger().severe("void_nexus biome not found! Please report this error.");
        }
        if (VOID_NEXUS_LIGHT_1 == null) {
            WbsWaygates.getInstance().getLogger().severe("void_nexus_light_1 biome not found! Please report this error.");
        }
        if (VOID_NEXUS_LIGHT_2 == null) {
            WbsWaygates.getInstance().getLogger().severe("void_nexus_light_2 biome not found! Please report this error.");
        }
        if (VOID_NEXUS_LIGHT_3 == null) {
            WbsWaygates.getInstance().getLogger().severe("void_nexus_light_3 biome not found! Please report this error.");
        }
        if (VOID_NEXUS_LIGHT_4 == null) {
            WbsWaygates.getInstance().getLogger().severe("void_nexus_light_4 biome not found! Please report this error.");
        }
    }

    @EventHandler
    public void onNetherLight(PortalCreateEvent event) {
        if (WorldManager.isVoidNexus(event.getWorld())) {
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

        if (WorldManager.isVoidNexus(block.getWorld())) {
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
        if (WorldManager.isVoidNexus(item.getWorld())) {
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
                        WorldManager.processDimensionEffects(block);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBeaconEffect(BeaconEffectEvent event) {
        if (!WbsWaygates.getInstance().getSettings().isFakeFogEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (WorldManager.isVoidNexus(player.getWorld())) {
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
    public void onBeaconActivate(BeaconActivatedEvent event) {
        if (VOID_NEXUS_LIGHT_1 == null) {
            return;
        }

        Beacon beacon = event.getBeacon();

        onBeaconToggle(beacon);
    }

    @EventHandler
    public void onBeaconDeactivate(BeaconDeactivatedEvent event) {
        if (VOID_NEXUS == null) {
            return;
        }

        WbsWaygates.getInstance().runSync(() ->
                onBeaconToggle(event.getBlock().getLocation())
        );
    }

    @EventHandler
    public void onBeaconDestroy(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEACON) {
            return;
        }

        WbsWaygates.getInstance().runSync(() ->
                onBeaconToggle(event.getBlock().getLocation())
        );
    }

    private static void onBeaconToggle(Location beaconLocation) {
        onBeaconToggle(beaconLocation, MAX_BEACON_RANGE);
    }
    private static void onBeaconToggle(Beacon beacon) {
        int range = (int) Math.ceil(beacon.getEffectRange());
        if (beacon.getTier() <= 0) {
            range = MAX_BEACON_RANGE;
        }

        onBeaconToggle(beacon.getLocation(), range);
    }
    private static void onBeaconToggle(Location center, int range) {
        World world = center.getWorld();
        if (!WorldManager.isVoidNexus(world)) {
            return;
        }

        if (center.getChunk().getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING) {
            return;
        }

        updateBiomeCyl(center, range, world);
    }

    private static void updateBiomeCyl(Location center, int range, World world) {
        WbsWaygates.getInstance().getAsync(
                () -> getLocationsToUpdate(center, range, world),
                toUpdate -> updateChunks(world, toUpdate)
        );
    }

    private static void updateChunks(World world, Map<Location, Biome> toUpdate) {
        Set<Chunk> updatedChunks = new HashSet<>();
        toUpdate.forEach((location, biome) -> {
            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                location.setY(y);
                if (!world.getBiome(location).equals(biome)) {
                    world.setBiome(location, biome);
                    updatedChunks.add(location.getChunk());
                }
            }
        });

        // NMS start
        List<ChunkAccess> accesses = updatedChunks.stream()
                .map(updatedChunk -> ((CraftChunk) updatedChunk).getHandle(ChunkStatus.BIOMES))
                .collect(Collectors.toCollection(LinkedList::new));

        ((CraftWorld) world).getHandle().getChunkSource().chunkMap.resendBiomesForChunks(accesses);
        // NMS end
    }

    private static @NonNull Map<Location, Biome> getLocationsToUpdate(Location center, int range, World world) {
        Set<Location> locations = getLocationCyl(center, range, world);
        Map<Location, Beacon> closestBeacons = getClosestBeacons(world, locations);
        Map<Location, Biome> toUpdate = new HashMap<>();

        for (Location location : locations) {
            Beacon beacon = closestBeacons.get(location);

            boolean inRangeOfBeacon =  false;
            double distance = -1;
            if (beacon != null) {
                distance = beacon.getLocation().distance(location);
                inRangeOfBeacon = distance <= range;
            }

            Biome biome;
            if (inRangeOfBeacon) {
                double actualRange = beacon.getEffectRange();
                double tierSize = actualRange / (beacon.getTier() + 1);
                int tierAtRange = Math.clamp(beacon.getTier() - (int) (distance / tierSize) + 1, 1, 4);

                biome = switch (tierAtRange) {
                    case 1 -> VOID_NEXUS_LIGHT_1;
                    case 2 -> VOID_NEXUS_LIGHT_2;
                    case 3 -> VOID_NEXUS_LIGHT_3;
                    case 4 -> VOID_NEXUS_LIGHT_4;
                    default -> throw new IllegalStateException("Unexpected value: " + tierAtRange);
                };
            } else {
                biome = VOID_NEXUS;
            }

            if (biome != null) {
                toUpdate.put(location, biome);
            }
        }

        return toUpdate;
    }

    private static @NotNull Set<Location> getLocationCyl(Location center, int range, World world) {
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        Set<Location> locations = new HashSet<>();
        for (int xOffset = -range; xOffset <= range; xOffset++) {
            for (int zOffset = -range; zOffset <= range; zOffset++) {
                if ((xOffset * xOffset + zOffset * zOffset) <= (range * range)) {
                    Location location = new Location(world, centerX + xOffset, 0, centerZ + zOffset);

                    locations.add(location);
                }
            }
        }
        return locations;
    }

    private static @NotNull Map<Location, Beacon> getClosestBeacons(World world, Set<Location> locations) {
        Set<Chunk> sourceChunks = new HashSet<>();
        for (Location check : locations) {
            if (check.isChunkLoaded()) {
                Chunk location1Chunk = check.getChunk();
                sourceChunks.add(location1Chunk);
            }
        }

        List<Chunk> nearbyChunks = Arrays.stream(world.getLoadedChunks())
                .filter(check -> {
                    int minDistanceToSource = sourceChunks.stream()
                            .mapToInt(chunk -> {
                                int x = chunk.getX();
                                int z = chunk.getZ();

                                int checkX = check.getX() - x;
                                int checkZ = check.getZ() - z;

                                return (checkX * checkX + checkZ * checkZ);
                            }).min().orElse(Integer.MAX_VALUE);

                    return minDistanceToSource <= CHUNK_CHECK_RANGE;
                }).toList();

        List<Beacon> beacons = new LinkedList<>();
        for (Chunk nearbyChunk : nearbyChunks) {
            nearbyChunk.getTileEntities(block -> block.getState() instanceof Beacon, true).stream()
                    .map(state -> (Beacon) state)
                    .filter(beacon -> beacon.getTier() > 0)
                    .forEach(beacons::add);
        }

        Map<Location, Beacon> closest = new HashMap<>();
        for (Location location : locations) {
            Location flatY = location.clone();
            flatY.setY(0);

            beacons.stream()
                    .min(Comparator.comparing(beacon -> {
                        Location stateLoc = beacon.getLocation();
                        stateLoc.setY(0);
                        return stateLoc.distance(flatY) - beacon.getEffectRange();
                    }))
                    .ifPresent(nearestBeacon -> closest.put(location, nearestBeacon));
        }

        return closest;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (WorldManager.isVoidNexus(player.getWorld())) {
            WorldManager.onEnterWorld(player);
        } else {
            WorldManager.onLeaveWorld(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (WorldManager.isVoidNexus(event.getPlayer().getWorld())) {
            WorldManager.onEnterWorld(event.getPlayer());
        } else {
            WorldManager.removeFakeFog(event.getPlayer());
        }
    }
}
