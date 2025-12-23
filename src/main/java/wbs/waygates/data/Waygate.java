package wbs.waygates.data;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import wbs.utils.util.entities.WbsEntityUtil;
import wbs.utils.util.particles.WbsParticleEffect;
import wbs.waygates.WbsWaygates;
import wbs.waygates.util.WaygateUtils;
import wbs.waygates.world.WorldManager;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public class Waygate {
    private static final Map<Block, Integer> PARTICLE_TASKS = new HashMap<>();

    private final Block baseBlock;
    private final WaygateType type;
    private final Block remoteGateBase;

    public Waygate(Block baseBlock, WaygateType type, Block remoteGateBase) {
        this.baseBlock = baseBlock;
        this.type = type;
        this.remoteGateBase = remoteGateBase;
    }

    public void remove() {
        type.remove(this);
    }
    public void breakNaturally() {
        breakNaturally(null);
    }
    public void breakNaturally(@Nullable Player player) {
        breakNaturally(player, true);
    }
    public void breakNaturally(@Nullable Player player, boolean breakOther) {
        stopParticles();
        remove();
        if (player == null || player.getGameMode() != GameMode.CREATIVE) {
            baseBlock.getWorld().dropItemNaturally(baseBlock.getLocation().toCenterLocation(), buildItem());
        }

        if (breakOther) {
            Waygate remoteWaygate = WaygateUtils.getWaygate(remoteGateBase);
            if (remoteWaygate != null) {
                remoteWaygate.breakNaturally(player, false);
            } else {
                WbsWaygates.getInstance().getLogger().warning("Remote waygate not found when breaking " + baseBlock + "! Remote: " + remoteGateBase);
            }
        }
    }

    public ItemStack buildItem() {
        return type.getItem();
    }

    public void startParticles() {
        Integer taskId = PARTICLE_TASKS.get(baseBlock);
        if (taskId != null) {
            throw new IllegalStateException("Particles are already running!");
        }

        WaygateType.WaygateParticles particles = type.getParticleDefinition();

        if (particles == null) {
            return;
        }

        WbsParticleEffect effectClone = particles.particleEffect().clone();

        PARTICLE_TASKS.put(baseBlock, WbsWaygates.getInstance().runTimer(task -> {
            if (!isLoaded()) {
                task.cancel();
                PARTICLE_TASKS.remove(baseBlock);
                return;
            }

            particles.particleOffset().refresh();

            effectClone.buildAndPlay(particles.particle(), getBaseBlock().getLocation().toCenterLocation().add(particles.particleOffset().val()));
        }, 1, 1));
    }

    public void stopParticles() {
        Integer taskId = PARTICLE_TASKS.get(baseBlock);
        if (taskId == null) {
            return;
        }

        Bukkit.getScheduler().cancelTask(taskId);
        PARTICLE_TASKS.remove(baseBlock);
    }

    public Block getBaseBlock() {
        return baseBlock;
    }

    public boolean triggerPlayerTeleport(Player player) {
        WorldManager.removeFakeFog(player);
        Location previousLocation = WbsEntityUtil.getMiddleLocation(player);

        Location remoteLocation = WaygateType.getSpawnLocation(this.remoteGateBase);
        boolean successfulTeleport = player.teleport(remoteLocation);

        if (successfulTeleport) {
            Sound tpSound = Sound.sound().type(Key.key("entity.enderman.teleport")).build();

            World prevWorld = previousLocation.getWorld();
            prevWorld.spawnParticle(Particle.DRAGON_BREATH, previousLocation, 25, 0.15, 0.15, 0.15, 0);
            prevWorld.spawnParticle(Particle.WITCH, previousLocation, 250, 0.6, 1, 0.6, 0);
            prevWorld.playSound(tpSound, remoteLocation.x(), remoteLocation.y(), remoteLocation.z());

            World newWorld = remoteLocation.getWorld();
            newWorld.spawnParticle(Particle.DRAGON_BREATH, remoteLocation.clone().add(0, 1, 0), 25, 0.15, 0.15, 0.15, 0);
            newWorld.spawnParticle(Particle.WITCH, remoteLocation, 50, 0.6, 1, 0.6, 0);
            newWorld.playSound(tpSound, remoteLocation.x(), remoteLocation.y(), remoteLocation.z());
        }

        return successfulTeleport;
    }

    public @NotNull WaygateType getType() {
        return type;
    }

    public boolean isLoaded() {
        return baseBlock.getLocation().isChunkLoaded();
    }

    public @NotNull Block getRemoteGateBase() {
        return remoteGateBase;
    }
}
