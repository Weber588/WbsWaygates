package wbs.waygates.world;

import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wbs.utils.util.WbsMath;
import wbs.waygates.WbsWaygates;

import java.util.Collection;
import java.util.List;

public class WorldManager {
    private static final double PARTICLE_SPEED = 1;
    private static final int MAX_PARTICLES_PER_TICK = 10;
    private static final int DARKNESS_DAMAGE_THRESHOLD = 5;

    private static final int LIGHT_CHECKS_PER_TICK = 100;
    private static final double LIGHT_CHECKS_RADIUS = 15;

    private static final int SMOKE_MIN_DISTANCE = 2;
    private static final int SMOKE_MAX_DISTANCE = 6;
    private static final int PORTAL_MIN_DISTANCE = 10;
    private static final int PORTAL_MAX_DISTANCE = 25;
    private static final @NotNull DamageSource DAMAGE_SOURCE = DamageSource.builder(DamageType.MAGIC).build();

    private static final Vector INITIAL_WIND_DIRECTION = new Vector(1, -0.25, 0.5);
    // TODO: Make this configurable
    public static final double WIND_VARIATION_PER_TICK = 0.01; // in radians

    private static long lastRotation = 0;
    public static Vector getWindDirection() {
        Vector windDirection = INITIAL_WIND_DIRECTION;
        if (lastRotation < Bukkit.getCurrentTick()) {
            windDirection.rotateAroundY(WIND_VARIATION_PER_TICK * (Math.random() > 0.5 ? -1 : 1));
            lastRotation = Bukkit.getCurrentTick();
        }
        return windDirection;
    }

    public static final String DIMENSION_NAME = "void_nexus";
    public static final @NotNull NamespacedKey DAMAGED_BY_DARKNESS = WbsWaygates.getKey("damaged_by_darkness");

    public static void startWorldTimers() {
        World check = getWorld();

        WbsWaygates plugin = WbsWaygates.getInstance();
        if (check == null) {
            plugin.getLogger().severe("Dimension not loaded! Plugin will be disabled!");

            plugin.runLater(() -> Bukkit.getPluginManager().disablePlugin(plugin), 1);
            return;
        }

        plugin.runTimer(runnable -> {
            World world = getWorld();
            if (world == null) return;

            @NotNull Collection<Item> items = world.getEntitiesByClass(Item.class);

            Vector pushVelocity = WbsMath.scaleVector(getWindDirection(), 0.01);
            items.forEach(item -> {
                if (item.getVelocity().length() < 0.1) {
                    item.setVelocity(item.getVelocity().add(pushVelocity));
                }
            });

            List<Player> players = world.getPlayers();

//            Vector playerPushVelocity = pushVelocity.clone().setY(0);

            for (Player player : players) {
                doVisualEffects(player);
                if (Bukkit.getCurrentTick() % 5 == 0) {
                    damageInDarkness(player);
                }

                doLightChecks(player);

//                if (player.getVelocity().length() < 0.1) {
//                    ItemStack chestplate = player.getInventory().getChestplate();
//                    if (chestplate != null && chestplate.getType().equals(Material.ELYTRA)) {
//                        player.setVelocity(player.getVelocity().add(playerPushVelocity));
//                    }
//                }
            }
        }, 1, 1);
    }

    private static void doLightChecks(Player player) {
        for (int i = 0; i < LIGHT_CHECKS_PER_TICK; i++) {
            double x = (Math.random() * 2 - 1) * LIGHT_CHECKS_RADIUS;
            double y = (Math.random() * 2 - 1) * LIGHT_CHECKS_RADIUS;
            double z = (Math.random() * 2 - 1) * LIGHT_CHECKS_RADIUS;
            Location checkLocation = player.getLocation().add(x, y, z);

            tryBreakLight(checkLocation.getBlock());
        }
    }

    public static @Nullable World getWorld() {
        World world = Bukkit.getWorld(new NamespacedKey("wbs", DIMENSION_NAME));
        if (world == null) {
            WbsWaygates.getInstance().getLogger().severe("Dimension not loaded!");

            return null;
        }
        return world;
    }

    public static boolean isInWorld(@NotNull World world) {
        return world.key().value().equals(DIMENSION_NAME);
    }

    private static void doVisualEffects(Player player) {
        int elytraSoundDuration = 10 * Ticks.TICKS_PER_SECOND;
        if (Bukkit.getCurrentTick() % elytraSoundDuration == 0) {
            Location effectLocation = player.getLocation().add(WbsMath.randomVector(10));
            player.playSound(effectLocation, Sound.ITEM_ELYTRA_FLYING, SoundCategory.AMBIENT, 0.5f, 1f);
        }

        Vector windDirection = getWindDirection();

        for (int i = 0; i < Math.random() * MAX_PARTICLES_PER_TICK; i++) {
            Vector vec = WbsMath.randomVector(SMOKE_MAX_DISTANCE - SMOKE_MIN_DISTANCE);
            vec = WbsMath.scaleVector(vec, vec.length() + SMOKE_MIN_DISTANCE);
            Location effectLocation = player.getLocation().add(vec);

            player.spawnParticle(
                    Particle.SMOKE,
                    effectLocation,
                    0,
                    windDirection.getX(),
                    windDirection.getY(),
                    windDirection.getZ(),
                    PARTICLE_SPEED
            );
        }


        for (int i = 0; i < Math.random() * MAX_PARTICLES_PER_TICK; i++) {
            Vector vec = WbsMath.randomVector(PORTAL_MAX_DISTANCE - PORTAL_MIN_DISTANCE);
            vec = WbsMath.scaleVector(vec, vec.length() + PORTAL_MIN_DISTANCE);
            Location effectLocation = player.getLocation().add(vec);

            player.spawnParticle(
                    Particle.REVERSE_PORTAL,
                    effectLocation,
                    0,
                    windDirection.getX(),
                    windDirection.getY(),
                    windDirection.getZ(),
                    PARTICLE_SPEED * 1.5,
                    null,
                    true
            );
        }
    }

    private static void damageInDarkness(Player player) {
        byte lightLevel = player.getLocation().getBlock().getLightLevel();

        if (lightLevel > DARKNESS_DAMAGE_THRESHOLD) {
            return;
        }

        if (Bukkit.getCurrentTick() / 5 % (lightLevel + 1) == 0) {
            player.getPersistentDataContainer().set(DAMAGED_BY_DARKNESS, PersistentDataType.INTEGER, Bukkit.getCurrentTick());
            player.setNoDamageTicks(0);
            player.damage(1, DAMAGE_SOURCE);
        }
    }

    public static void tryBreakLight(Block block) {
        if (block.isEmpty()) {
            return;
        }

        BlockData blockData = block.getBlockData();

        Material material = blockData.getMaterial();

        if (material == Material.BEACON
                || material == Material.LIGHT
                || material.key().value().contains("sculk")
                || material.key().value().contains("portal")
        ) {
            return;
        }

        int lightEmission = blockData.getLightEmission();
        if (blockData instanceof RedstoneWire wire) {
            lightEmission = wire.getLightEmission();
        }
        double chanceToBreak = lightEmission;
        if (chanceToBreak <= 0) {
            return;
        }
        if (!material.isCollidable()) {
            chanceToBreak *= 5;
        }
        if (material.key().value().contains("soul")) {
            chanceToBreak /= 3;
        }
        if (material.key().value().contains("fire")) {
            chanceToBreak *= 4;
        }
        if (material != Material.SEA_LANTERN && material.key().value().contains("lantern")) {
            chanceToBreak /= 3;
        }

        if (WbsMath.chance(chanceToBreak)) {
            breakLightSource(block, blockData, material);
        }
    }

    private static void breakLightSource(Block block, BlockData blockData, Material material) {
        World world = block.getWorld();

        if (blockData instanceof Lightable lightable) {
            if (!(lightable instanceof Powerable) && !(lightable instanceof Furnace)) {
                lightable.setLit(false);
                block.setBlockData(lightable, false);

                Sound sound = Sound.BLOCK_FIRE_EXTINGUISH;

                if (blockData instanceof Candle) {
                    sound = Sound.BLOCK_CANDLE_EXTINGUISH;
                } else if (material == Material.REDSTONE_TORCH || material == Material.REDSTONE_WALL_TORCH) {
                    sound = Sound.BLOCK_REDSTONE_TORCH_BURNOUT;
                }

                world.playSound(block.getLocation().toCenterLocation(), sound, SoundCategory.BLOCKS, 1, 1);
                return;
            }
        }
        if (blockData instanceof RedstoneWire wire) {
            wire.setPower(0);
            block.setBlockData(wire, false);
            return;
        }

        if (material == Material.LAVA) {
            block.setType(Material.OBSIDIAN);
            world.playSound(block.getLocation().toCenterLocation(), Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1, 1);
            return;
        }

        if (material == Material.LAVA_CAULDRON) {
            block.setType(Material.CAULDRON);
            world.playSound(block.getLocation().toCenterLocation(), Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1, 1);
            world.dropItemNaturally(block.getLocation().toCenterLocation(), new ItemStack(Material.OBSIDIAN));
            return;
        }

        if (material == Material.CRYING_OBSIDIAN) {
            block.setType(Material.OBSIDIAN);
            return;
        }

        block.breakNaturally();
    }
}
