package org.metamechanists.odysseia.reencarnacion;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Escucha eventos de conexion para entregar la Capsula de Recuerdos a jugadores renacidos
 * y delega los clics de la GUI de la Capsula.
 */
public final class ReencarnacionListener implements Listener {

    private final ReencarnacionManager manager;

    public ReencarnacionListener(ReencarnacionManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        List<ItemStack> pendingCapsule = ReencarnacionManager.getPendingDeliveries().remove(uuid);
        if (pendingCapsule != null) {
            ReencarnacionManager.savePendingDeliveries();
            ReencarnacionExecutor.handlePlayerJoin(player, pendingCapsule);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CapsulaRecuerdosGUI gui) {
            gui.onInventoryClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CapsulaRecuerdosGUI gui) {
            gui.onInventoryDrag(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CapsulaRecuerdosGUI gui) {
            gui.onInventoryClose(event);
        }
    }
}
