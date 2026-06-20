package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VanishCommand implements CommandExecutor, Listener {

    private final Odysseia plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cEste comando solo puede ser ejecutado por un jugador."));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("odysseia.vanish")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para desvanecerte."));
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (vanishedPlayers.contains(uuid)) {
            // Unvanish
            vanishedPlayers.remove(uuid);
            player.removeMetadata("vanished", plugin);
            
            // Show to all online players
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showPlayer(plugin, player);
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aVanish &7desactivado. Ahora eres visible."));
            playVanishEffects(player);
        } else {
            // Vanish
            vanishedPlayers.add(uuid);
            player.setMetadata("vanished", new FixedMetadataValue(plugin, true));

            // Hide from all online players (who don't have override permission)
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player && !p.hasPermission("odysseia.vanish.see")) {
                    p.hidePlayer(plugin, player);
                }
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aVanish &2activado. Ahora eres invisible."));
            playVanishEffects(player);
        }

        return true;
    }

    private void playVanishEffects(Player player) {
        try {
            spawnVanishEffects(player);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("No se pudieron reproducir las particulas de vanish para "
                    + player.getName() + ": " + ex.getMessage());
        }
    }

    private void spawnVanishEffects(Player player) {
        Location loc = player.getLocation();
        
        // Spawn a double helix of PORTAL and DRAGON_BREATH particles
        for (double y = 0; y <= 2; y += 0.1) {
            double radius = 0.6;
            double angle = y * Math.PI * 4;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            
            Location particleLoc1 = loc.clone().add(x, y, z);
            Location particleLoc2 = loc.clone().add(-x, y, -z);
            
            player.getWorld().spawnParticle(Particle.PORTAL, particleLoc1, 2, 0, 0, 0, 0);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, particleLoc2, 1, 0, 0, 0, 0, 1.0f);
        }

        // Spawn explosion and cloud puff particles
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
        
        // Play epic sound
        player.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player joining = e.getPlayer();

        // 1. Hide all currently vanished players from the joining player
        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && vanished.isOnline() && !joining.hasPermission("odysseia.vanish.see")) {
                joining.hidePlayer(plugin, vanished);
            }
        }

        // 2. If the joining player is supposed to be vanished (e.g. saved state or re-logged staff), vanish them
        if (joining.hasPermission("odysseia.vanish") && vanishedPlayers.contains(joining.getUniqueId())) {
            joining.setMetadata("vanished", new FixedMetadataValue(plugin, true));
            e.joinMessage(null); // Silent join
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != joining && !p.hasPermission("odysseia.vanish.see")) {
                    p.hidePlayer(plugin, joining);
                }
            }
            joining.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEntraste al servidor en modo &2Vanish&a."));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player quitting = e.getPlayer();
        if (vanishedPlayers.contains(quitting.getUniqueId())) {
            e.quitMessage(null); // Silent quit
            // Keep in memory or cleanup? Cleanup so that they don't leak memory, 
            // but let's keep their UUID in the vanished set in case they relog within the same session.
            // Actually, keep it so if they relog they are still vanished.
        }
    }
}
