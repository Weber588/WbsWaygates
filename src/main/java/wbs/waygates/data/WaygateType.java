package wbs.waygates.data;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import wbs.utils.exceptions.InvalidConfigurationException;
import wbs.utils.util.WbsEnums;
import wbs.utils.util.configuration.WbsConfigReader;
import wbs.utils.util.particles.NormalParticleEffect;
import wbs.utils.util.particles.WbsParticleEffect;
import wbs.utils.util.persistent.BlockChunkStorageUtil;
import wbs.utils.util.persistent.WbsPersistentDataType;
import wbs.utils.util.providers.VectorProvider;
import wbs.waygates.WaygateSettings;
import wbs.waygates.WbsWaygates;
import wbs.waygates.util.PersistentWaygateType;
import wbs.waygates.util.TransformationBuilder;
import wbs.waygates.util.WaygateUtils;
import wbs.waygates.world.WorldManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
@NullMarked
public class WaygateType implements Keyed {
    public static final NamespacedKey WAYGATE_KEY = WbsWaygates.getKey("waygate");
    public static final NamespacedKey WAYGATE_TYPE_KEY = new NamespacedKey(WbsWaygates.getInstance(), "waygate_type");
    public static final NamespacedKey PARENT_WAYGATE = new NamespacedKey(WbsWaygates.getInstance(), "parent_waygate_block");
    public static final double PLATFORM_RADIUS = 2;

    private final NamespacedKey key;

    private ItemStack item = ItemType.CRYING_OBSIDIAN.createItemStack();
    private BlockType platformBlockType = BlockType.STONE_BRICKS;

    private final List<WaygateBlock> blocks = new LinkedList<>();
    private final List<WaygateDisplay> displays = new LinkedList<>();

    @Nullable
    private WaygateParticles particleEffect;

    @Override
    public @NotNull NamespacedKey getKey() {
        return key;
    }

    public WaygateType(NamespacedKey key, ConfigurationSection section) {
        String directory = key.value() + ".yml";
        this.key = key;

        ConfigurationSection itemSection = section.getConfigurationSection("item");
        WaygateSettings settings = WbsWaygates.getInstance().getSettings();
        if (itemSection != null) {
            String itemDirectory = directory + "/item";

            String baseItemString = itemSection.getString("base-item", item.getItemMeta().getAsComponentString());

            try {
                item = Bukkit.getItemFactory().createItemStack(baseItemString);
            } catch (IllegalArgumentException ex) {
                throw new InvalidConfigurationException("Invalid base-item string \"" + baseItemString + "\".", itemDirectory + "/base-item");
            }

            Component itemName = itemSection.getComponent("name", MiniMessage.miniMessage());

            if (itemName != null) {
                item.setData(DataComponentTypes.ITEM_NAME, itemName);
            }

            NamespacedKey modelKey = WbsConfigReader.getNamespacedKey(itemSection, "item-model", settings, itemDirectory, null);

            if (modelKey != null) {
                item.setData(DataComponentTypes.ITEM_MODEL, modelKey);
            }

            String playerHeadTexture = itemSection.getString("player-head-texture");
            if (playerHeadTexture != null) {
                ResolvableProfile profile = ResolvableProfile.resolvableProfile()
                        .addProperty(new ProfileProperty("textures", playerHeadTexture))
                        .build();

                item.setData(DataComponentTypes.PROFILE, profile);
            }
        }

        item.editPersistentDataContainer(container -> container.set(WaygateType.WAYGATE_TYPE_KEY, WbsPersistentDataType.NAMESPACED_KEY, this.getKey()));

        platformBlockType = WbsConfigReader.getRegistryEntry(section, "platform-base", RegistryKey.BLOCK, platformBlockType);

        ConfigurationSection blocksSection = section.getConfigurationSection("blocks");
        if (blocksSection == null) {
            throw new InvalidConfigurationException("blocks is a required section.", directory);
        }

        for (String sectionKey : blocksSection.getKeys(false)) {
            ConfigurationSection blockSection =  Objects.requireNonNull(blocksSection.getConfigurationSection(sectionKey));

            String blockDataString = blockSection.getString("block");
            if (blockDataString == null) {
                settings.logError("Block data is a required for each block.", directory + "/blocks/" + sectionKey + "/block");
                continue;
            }

            BlockData blockData;
            try {
                blockData = Bukkit.getServer().createBlockData(blockDataString);
            } catch (IllegalArgumentException ex) {
                settings.logError("Invalid block data \"" + blockDataString + "\"", directory + "/blocks/" + sectionKey + "/block");
                continue;
            }

            Vector offset = WbsConfigReader.getVector(blockSection, "offset", new Vector(0, 0, 0));

            blocks.add(new WaygateBlock(blockData, offset));
        }

        if (blocks.stream().noneMatch(block -> block.offset.lengthSquared() == 0)) {
            throw new InvalidConfigurationException("At least one block must have no offset.", directory + "/blocks");
        }

        if (blocks.isEmpty()) {
            throw new InvalidConfigurationException("At least one block is required.", directory + "/blocks");
        }

        ConfigurationSection displaysSection = section.getConfigurationSection("displays");
        if (displaysSection != null) {
            for (String sectionKey : displaysSection.getKeys(false)) {
                ConfigurationSection displaySection =  Objects.requireNonNull(displaysSection.getConfigurationSection(sectionKey));

                String blockDataString = displaySection.getString("block");
                if (blockDataString == null) {
                    settings.logError("Block data is a required for each block.", directory + "/blocks/" + sectionKey + "/block");
                    continue;
                }

                BlockData blockData;
                try {
                    blockData = Bukkit.getServer().createBlockData(blockDataString);
                } catch (IllegalArgumentException ex) {
                    settings.logError("Invalid block data \"" + blockDataString + "\"", directory + "/blocks/" + sectionKey + "/block");
                    continue;
                }

                Vector offset = WbsConfigReader.getVector(displaySection, "offset", new Vector(0, 0, 0));
                Vector scale = WbsConfigReader.getVector(displaySection, "scale", new Vector(1, 1, 1));

                displays.add(new WaygateDisplay(blockData, offset, scale));
            }
        }

        String particlesDirectory = directory + "/particles";
        if (section.isString("particles")) {
            String particleAsString = Objects.requireNonNull(section.getString("particles"));
            Particle particle = WbsEnums.getEnumFromString(Particle.class, particleAsString);

            if (particle == null) {
                settings.logError("Invalid particle \"" + particleAsString +  "\"", particlesDirectory);
            } else {
                WbsParticleEffect effect = new NormalParticleEffect().setXYZ(1).setY(2).setAmount(10);
                particleEffect = new WaygateParticles(effect, particle, new VectorProvider(0, 1, 0));
            }
        } else {
            ConfigurationSection particlesSection = section.getConfigurationSection("particles");
            if (particlesSection != null) {
                // Full WbsParticleEffect support
                if (particlesSection.contains("type")) {
                    try {
                        WbsParticleEffect effect = WbsParticleEffect.buildParticleEffect(particlesSection, settings, particlesDirectory);

                        VectorProvider offset;
                        ConfigurationSection offsetSection = particlesSection.getConfigurationSection("offset");
                        if (offsetSection != null) {
                            offset = new VectorProvider(offsetSection, settings, particlesDirectory);
                        } else {
                            offset = new VectorProvider(0, 0, 0);
                        }

                        Particle particle = WbsConfigReader.getRequiredEnum(particlesSection, "particle", settings, particlesDirectory, Particle.class);

                        particleEffect = new WaygateParticles(effect, particle, offset);
                    } catch (InvalidConfigurationException ex) {
                        if (ex.getMessage() != null && ex.getDirectory() != null) {
                            settings.logError(ex.getMessage(), ex.getDirectory());
                        }
                    }
                } else {
                    // TODO: Add simplified version:
                    /*
                        #particles:
                        #  particle: 'enchantment_table'
                        #  speed: 0.1
                        #  radius: 0.5
                        #  spread:
                        #    x: 0.5
                        #    y: 1
                        #    z: 0.5
                     */
                }
            }
        }
    }

    public Material getBaseBlockType() {
        return blocks.stream()
                .filter(block -> block.offset.length() == 0)
                .findAny()
                .orElseThrow()
                .data()
                .getMaterial();
    }

    public boolean canSpawn(Block baseBlock) {
        for (WaygateBlock block : blocks) {
            Block worldBlock = baseBlock.getLocation().add(block.offset()).getBlock();
            if (!worldBlock.isReplaceable()) {
                return false;
            }
            if (WaygateUtils.getWaygate(worldBlock) != null) {
                return false;
            }
        }

        return true;
    }

    public Waygate spawn(Block baseBlock) {
        return spawn(baseBlock, null);
    }
    public Waygate spawn(Block baseBlock, @Nullable Block remoteGateBase) {
        World world = baseBlock.getWorld();

        blocks.forEach(block -> {
            Block worldBlock = baseBlock.getLocation().add(block.offset()).getBlock();
            worldBlock.setBlockData(block.data());
            PersistentDataContainer container = BlockChunkStorageUtil.getContainer(worldBlock);
            container.set(PARENT_WAYGATE, WbsPersistentDataType.LOCATION, baseBlock.getLocation());
            BlockChunkStorageUtil.writeContainer(worldBlock, container);
        });

        displays.forEach(display -> {
            Location location = baseBlock.getLocation();
            world.spawn(location, BlockDisplay.class, entity -> {
                entity.setBlock(display.data());
                entity.setTransformation(new TransformationBuilder()
                        .translation(display.offset())
                        .scale(display.scale())
                        .build()
                );
                entity.getPersistentDataContainer().set(PARENT_WAYGATE, WbsPersistentDataType.LOCATION, baseBlock.getLocation());
            });
        });

        World customDimension = WorldManager.getWorld();

        if (customDimension == null) {
            throw new IllegalStateException("Dimension not loaded while placing waygate!!!");
        }

        double coordinateScale = customDimension.getCoordinateScale();

        if (!world.equals(customDimension)) {
            Location remoteLocation = baseBlock.getLocation();

            //noinspection IntegerDivisionInFloatingPointContext
            remoteLocation.set(
                    Math.round(remoteLocation.x() / coordinateScale) + 0.5,
                    customDimension.getMaxHeight() / 2,
                    Math.round(remoteLocation.z() / coordinateScale) + 0.5
            );

            remoteLocation.setWorld(customDimension);

            remoteGateBase = remoteLocation.getBlock().getRelative(0, 0, 2);
            spawn(remoteGateBase, baseBlock);
        } else if (remoteGateBase == null) {
            throw new IllegalStateException("remoteGateBase not provided when constructing in-dimension waygate!");
        } else {
            Block platformCenter = baseBlock.getLocation().add(0.5, -1, 0.5).getBlock();
            for (double x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
                for (double z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) > PLATFORM_RADIUS || (Math.abs(x) == PLATFORM_RADIUS && Math.abs(z) == PLATFORM_RADIUS)) {
                        continue;
                    }

                    Block block = platformCenter.getLocation().add(x, 0, z).getBlock();

                    if (block.getType().isAir()) {
                        //noinspection DataFlowIssue,deprecation
                        block.setType(platformBlockType.asMaterial());
                    }
                }
            }
        }

        Waygate waygate = new Waygate(baseBlock, this, remoteGateBase);

        BlockChunkStorageUtil.modifyContainer(baseBlock, baseContainer ->
                baseContainer.set(WAYGATE_KEY, PersistentWaygateType.INSTANCE, waygate)
        );

        waygate.startParticles();

        return waygate;
    }

    public static @NotNull Location getSpawnLocation(Block baseBlock) {
        return baseBlock.getLocation().toCenterLocation().add(0, -0.49, -2);
    }

    public void remove(Waygate waygate) {
        blocks.forEach(block -> {
            Block worldBlock = waygate.getBaseBlock().getLocation().add(block.offset()).getBlock();
            PersistentDataContainer container = BlockChunkStorageUtil.getContainer(worldBlock);
            if (container.has(PARENT_WAYGATE)) {
                worldBlock.setBlockData(Material.AIR.createBlockData());
            }
            container.remove(PARENT_WAYGATE);
            BlockChunkStorageUtil.writeContainer(worldBlock, container);
        });

        waygate.getBaseBlock().getLocation().toCenterLocation().getNearbyEntitiesByType(BlockDisplay.class, 2, check -> {
            Location parentLocation = check.getPersistentDataContainer().get(PARENT_WAYGATE, WbsPersistentDataType.LOCATION);
            return parentLocation != null && waygate.getBaseBlock().equals(parentLocation.getBlock());
        }).forEach(Entity::remove);

        BlockChunkStorageUtil.modifyContainer(waygate.getBaseBlock(), container -> {
            container.remove(WAYGATE_KEY);
        });
    }

    public ItemStack getItem() {
        return item.clone();
    }

    @Nullable
    public WaygateParticles getParticleDefinition() {
        return particleEffect;
    }

    public record WaygateParticles(WbsParticleEffect particleEffect, Particle particle, VectorProvider particleOffset) {}

    private record WaygateBlock(BlockData data, Vector offset) {
        @Override
        public Vector offset() {
            return offset.clone();
        }
    }

    private record WaygateDisplay(BlockData data, Vector offset, Vector scale) {
        @Override
        public Vector offset() {
            return offset.clone();
        }
        @Override
        public Vector scale() {
            return scale.clone();
        }
    }
}
