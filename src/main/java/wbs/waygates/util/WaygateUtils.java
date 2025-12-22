package wbs.waygates.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import wbs.utils.util.persistent.BlockChunkStorageUtil;
import wbs.utils.util.persistent.WbsPersistentDataType;
import wbs.waygates.WbsWaygates;
import wbs.waygates.data.Waygate;
import wbs.waygates.data.WaygateType;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public class WaygateUtils {

    public static @Nullable Waygate getWaygate(Block block) {
        PersistentDataContainer container = BlockChunkStorageUtil.getContainer(block);
        Location waygateLocation = container.get(WaygateType.PARENT_WAYGATE, WbsPersistentDataType.LOCATION);

        if (waygateLocation == null) {
            return null;
        }

        if (!waygateLocation.equals(block.getLocation())) {
            Waygate waygate = getWaygate(waygateLocation.getBlock());
            if (waygate == null) {
                cleanupWaygate(block, container, waygateLocation);
            }
            return waygate;
        }

        Waygate waygate = container.get(WaygateType.WAYGATE_KEY, PersistentWaygateType.INSTANCE);

        if (waygate == null) {
            cleanupWaygate(block, container, waygateLocation);
            return null;
        }

        return waygate;
    }

    private static void cleanupWaygate(Block block, PersistentDataContainer container, Location waygateLocation) {
        container.remove(WaygateType.PARENT_WAYGATE);
        WbsWaygates.getInstance().getLogger().warning("A waygate was registered to the chunk but was not in memory! ");
        WbsWaygates.getInstance().getLogger().warning(block.getLocation() + " -> " + waygateLocation);
    }

    public static Map<Block, Waygate> getChunkWaygates(@NotNull Chunk chunk) {
        Map<Block, PersistentDataContainer> containerMap = BlockChunkStorageUtil.getBlockContainerMap(chunk);
        Map<Block, Waygate> waygateMap = new HashMap<>();

        for (Block block : containerMap.keySet()) {
            Waygate waygate = getWaygate(block);
            if (waygate != null && waygate.getBaseBlock().equals(block)) {
                waygateMap.put(block, waygate);
            }
        }

        return waygateMap;
    }
}
