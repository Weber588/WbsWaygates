package wbs.waygates.util;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import wbs.utils.util.persistent.WbsPersistentDataType;
import wbs.waygates.WaygateRegistries;
import wbs.waygates.WbsWaygates;
import wbs.waygates.data.Waygate;
import wbs.waygates.data.WaygateType;

public class PersistentWaygateType implements PersistentDataType<PersistentDataContainer, Waygate> {
    public static final PersistentWaygateType INSTANCE = new PersistentWaygateType();

    private static final NamespacedKey BASE_BLOCK = new NamespacedKey(WbsWaygates.getInstance(), "block");
    private static final NamespacedKey REMOTE_BLOCK = new NamespacedKey(WbsWaygates.getInstance(), "remote_block");
    private static final NamespacedKey TYPE = new NamespacedKey(WbsWaygates.getInstance(), "type");

    @Override
    public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    public @NotNull Class<Waygate> getComplexType() {
        return Waygate.class;
    }

    @Override
    public @NotNull PersistentDataContainer toPrimitive(@NotNull Waygate waygate, @NotNull PersistentDataAdapterContext context) {
        PersistentDataContainer container = context.newPersistentDataContainer();

        container.set(BASE_BLOCK, WbsPersistentDataType.LOCATION, waygate.getBaseBlock().getLocation());
        container.set(REMOTE_BLOCK, WbsPersistentDataType.LOCATION, waygate.getRemoteGateBase().getLocation());
        container.set(TYPE, WbsPersistentDataType.NAMESPACED_KEY, waygate.getType().getKey());

        return container;
    }

    @Override
    public @NotNull Waygate fromPrimitive(@NotNull PersistentDataContainer container, @NotNull PersistentDataAdapterContext context) {
        Location location = container.get(BASE_BLOCK, WbsPersistentDataType.LOCATION);
        Location remoteLocation = container.get(REMOTE_BLOCK, WbsPersistentDataType.LOCATION);
        NamespacedKey typeKey = container.get(TYPE, WbsPersistentDataType.NAMESPACED_KEY);
        WaygateType type = WaygateRegistries.WAYGATE_TYPES.get(typeKey);

        if (location == null) {
            throw new IllegalStateException("Invalid Waygate data -- " + BASE_BLOCK + " is a required field.");
        }
        if (remoteLocation == null) {
            throw new IllegalStateException("Invalid Waygate data -- " + REMOTE_BLOCK + " is a required field.");
        }
        if (typeKey == null) {
            throw new IllegalStateException("Invalid Waygate data -- " + TYPE + " is a required field.");
        }
        if (type == null) {
            throw new IllegalStateException("Invalid Waygate Type \"" + typeKey.asString());
        }

        return new Waygate(location.getBlock(), type, remoteLocation.getBlock());
    }
}
