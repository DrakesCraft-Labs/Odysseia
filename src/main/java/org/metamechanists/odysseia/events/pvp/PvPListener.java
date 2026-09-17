package org.metamechanists.odysseia.events.pvp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Garantiza la integridad del inventario de los jugadores durante el torneo PvP:
 * evita drop de items de kit, gestiona bajas y restaura inventarios huérfanos al reconectar.
 */
public class PvPListener implements Listener {

    private final EventPvPManager pvpManager;

    public PvPListener(EventPvPManager pvpManager) {
        this.pvpManager = pvpManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (pvpManager.isParticipant(victim.getUniqueId())) {
            // Evitar que los ítems del kit caigan al piso o contaminen la arena
            event.getDrops().clear();
            event.setDroppedExp(0);

            Player killer = victim.getKiller();
            pvpManager.handleKill(killer, victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (pvpManager.isParticipant(player.getUniqueId())) {
            pvpManager.leaveTournament(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        EventPvPStashRepository repo = pvpManager.getStashRepository();
        if (repo != null && repo.hasStash(player.getUniqueId())) {
            // Huérfano detectado tras crash o reinicio mientras estaba en torneo
            boolean ok = repo.restorePlayer(player);
            if (ok) {
                player.sendMessage("§a[Odysseia] Tu inventario original ha sido recuperado con éxito.");
            }
        }
    }
}
