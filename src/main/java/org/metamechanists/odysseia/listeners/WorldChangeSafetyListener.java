package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.metamechanists.odysseia.Odysseia;

public final class WorldChangeSafetyListener implements Listener {

    private final Odysseia plugin;

    public WorldChangeSafetyListener(Odysseia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;

        String fromWorld = event.getFrom().getName().toLowerCase();
        String toWorld = player.getWorld().getName().toLowerCase();

        boolean isFromOneBlock = fromWorld.contains("oneblock") || fromWorld.contains("skyblock") || fromWorld.contains("bskyblock");
        boolean isToOneBlock = toWorld.contains("oneblock") || toWorld.contains("skyblock") || toWorld.contains("bskyblock");

        if (isFromOneBlock != isToOneBlock || !fromWorld.equals(toWorld)) {
            player.sendMessage(ChatColor.GOLD + "[DrakesCraft] " + ChatColor.YELLOW +
                    "Guarda tus cosas en algún cofre protegido en este mundo antes de cambiar de modalidad para evitar posibles fallos de eliminación de objetos.");

            player.sendTitle(
                    ChatColor.GOLD + "¡Atención al cambiar de mundo!",
                    ChatColor.YELLOW + "Guarda tus cosas en cofres protegidos antes de cambiar de modalidad.",
                    10, 70, 20
            );
        }
    }
}
