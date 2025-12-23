package wbs.waygates.world;

import net.kyori.adventure.util.Ticks;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.CopperBulb;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import wbs.utils.util.WbsCollectionUtil;
import wbs.utils.util.WbsMath;
import wbs.waygates.WbsWaygates;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public static Vector getItemPushVelocity() {
        return WbsMath.scaleVector(getWindDirection(), 0.01);
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

            Vector pushVelocity = getItemPushVelocity();
            items.forEach(item -> {
                if (item.getVelocity().length() < 0.1) {
                    item.setVelocity(item.getVelocity().add(pushVelocity));
                }
            });

            List<Player> players = world.getPlayers();

            Vector pushOffset = WbsMath.scaleVector(pushVelocity.clone().multiply(-1), 0.1);

            Vec3 elytraPushVelocity = CraftVector.toVec3(pushVelocity);
            Vec3 elytraPushVelocityGliding = CraftVector.toVec3(pushVelocity.clone().multiply(10));

            for (Player player : players) {
                doVisualEffects(player);
                if (Bukkit.getCurrentTick() % 5 == 0) {
                    damageInDarkness(player);
                }

                doLightChecks(player);

                if (player.getVelocity().length() < 0.1) {
                    ItemStack chestplate = player.getInventory().getChestplate();
                    if (chestplate != null && chestplate.getType().equals(Material.ELYTRA)) {
                        // Use explosion packet to get delta movement sent to player, not just setting velocity
                        ((CraftPlayer) player).getHandle().connection.send(
                                new ClientboundExplodePacket(
                                        CraftLocation.toVec3(player.getLocation().add(pushOffset)),
                                        Optional.of(player.isGliding() ? elytraPushVelocityGliding : elytraPushVelocity),
                                        ParticleTypes.MYCELIUM,
                                        Holder.direct(new SoundEvent(ResourceLocation.parse("minecraft:empty"), Optional.empty()))
                                )
                        );
                    }
                }
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

        double chanceToBreak = blockData.getLightEmission();
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
            if (!(lightable instanceof Furnace)) {
                lightable.setLit(false);
                // Do once with physics to trigger changes around it, but then again without to bypass individual block physics
                block.setBlockData(lightable, true);
                block.setBlockData(lightable, false);

                Sound sound = getSound(blockData, material);

                world.playSound(block.getLocation().toCenterLocation(), sound, SoundCategory.BLOCKS, 1, 1);
                return;
            }
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

    private static Sound getSound(BlockData blockData, Material material) {
        Sound sound = Sound.BLOCK_FIRE_EXTINGUISH;

        if (blockData instanceof Candle) {
            sound = Sound.BLOCK_CANDLE_EXTINGUISH;
        } else if (blockData instanceof CopperBulb) {
            sound = Sound.BLOCK_COPPER_BULB_TURN_OFF;
        } else if (blockData instanceof Powerable || material == Material.REDSTONE_TORCH || material == Material.REDSTONE_WALL_TORCH) {
            sound = Sound.BLOCK_REDSTONE_TORCH_BURNOUT;
        }
        return sound;
    }

    public static void addFakeFog(@NotNull Player player) {
        removeFakeFog(player);
        WbsWaygates.getInstance().runLater(() -> {
            if (isInWorld(player.getWorld())) {
                createFakeFog(player);
            }
        }, 1);
    }

    private static void createFakeFog(@NotNull Player player) {
        World world = player.getWorld();

        for (int i = 0; i < 8; i++) {
            float scale = 9 + i;

            int finalI = i;
            NamespacedKey playerKey = getPlayerKey(player);
            Location spawnLoc = player.getLocation();
            spawnLoc.setPitch(WbsCollectionUtil.getRandom(Set.of(-90, 0, 90)));
            spawnLoc.setYaw(WbsCollectionUtil.getRandom(Set.of(0, 90, 180, 270)));
            world.spawn(spawnLoc, BlockDisplay.class, display -> {
                display.getPersistentDataContainer().set(playerKey, PersistentDataType.BOOLEAN, true);

                Transformation transformation = new Transformation(
                        new Vector3f(scale / 2, scale / 2, scale / 2),
                        new AxisAngle4f(0, 0, 0, 0),
                        new Vector3f(-scale, -scale, -scale),
                        new AxisAngle4f(0, 0, 0, 0)
                );

                display.setTransformation(transformation);
                display.setBlock(Material.BLACK_STAINED_GLASS.createBlockData());
                if (finalI == 7) {
                    display.setBlock(Material.BLACK_CONCRETE.createBlockData());
                }
                display.setBrightness(new Display.Brightness(0, 0));

                display.setPersistent(false);
                display.setVisibleByDefault(false);
                player.showEntity(WbsWaygates.getInstance(), display);
                player.addPassenger(display);
            });
        }
    }

    private static @NotNull NamespacedKey getPlayerKey(@NotNull Player player) {
        return WbsWaygates.getKey("darkness_" + player.getUniqueId());
    }

    public static void removeFakeFog(@NotNull Player player) {
        NamespacedKey playerKey = getPlayerKey(player);

        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(BlockDisplay.class).forEach(display -> {
            if (display.getPersistentDataContainer().has(playerKey)) {
                display.remove();
            }
        }));
    }
}
