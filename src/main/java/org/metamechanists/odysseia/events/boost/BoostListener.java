package org.metamechanists.odysseia.events.boost;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * Escucha eventos del juego para aplicar los multiplicadores activos del ServerBoostManager.
 */
public class BoostListener implements Listener {

    private final ServerBoostManager boostManager;

    public BoostListener(ServerBoostManager boostManager) {
        this.boostManager = boostManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        double xpMult = boostManager.getMultiplier(ServerBoostManager.BoostCategory.XP);
        if (xpMult > 1.0 && event.getAmount() > 0) {
            int newAmount = (int) Math.round(event.getAmount() * xpMult);
            event.setAmount(newAmount);
        }
    }
}
