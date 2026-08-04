package org.metamechanists.odysseia.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.metamechanists.odysseia.Odysseia;

/** Prevents capture addons from turning a live Odysseia boss into a spawn egg. */
public final class BossCaptureGuardListener implements Listener {

    private final NamespacedKey bossTypeKey;

    public BossCaptureGuardListener(Odysseia plugin) {
        this.bossTypeKey = new NamespacedKey(plugin, "boss_type");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity target = event.getRightClicked();
        if (!target.getPersistentDataContainer().has(bossTypeKey, PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendActionBar(net.kyori.adventure.text.Component.text(
                "Este jefe no puede ser capturado.", net.kyori.adventure.text.format.NamedTextColor.RED));
    }
}
