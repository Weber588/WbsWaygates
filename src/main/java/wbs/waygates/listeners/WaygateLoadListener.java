package wbs.waygates.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.*;
import wbs.waygates.data.Waygate;
import wbs.waygates.util.WaygateUtils;

public class WaygateLoadListener implements Listener {
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Waygate waygate : WaygateUtils.getChunkWaygates(event.getChunk()).values()) {
            waygate.startParticles();
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Waygate waygate : WaygateUtils.getChunkWaygates(event.getChunk()).values()) {
            waygate.stopParticles();
        }
    }
}
