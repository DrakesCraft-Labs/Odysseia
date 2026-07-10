package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishCommand implements CommandExecutor, Listener {

    private static final String VANISH_PERMISSION = "odysseia.vanish";
    private static final String SEE_PERMISSION = "odysseia.vanish.see";
    private static final String VANISH_METADATA = "vanished";

    private final Odysseia plugin;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();

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
        if (!player.hasPermission(VANISH_PERMISSION)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para desvanecerte."));
            return true;
        }

        setVanished(player, !isVanished(player), true);
        return true;
    }

    public void setVanished(Player player, boolean vanished, boolean playEffects) {
        if (vanished) {
            vanishedPlayers.add(player.getUniqueId());
            player.setMetadata(VANISH_METADATA, new FixedMetadataValue(plugin, true));

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) {
                    continue;
                }
                if (!canSeeVanished(viewer)) {
                    viewer.hidePlayer(plugin, player);
                }
            }

            // Ajustes ligeros de estado para que el staff desaparezca de forma más consistente.
            player.setSleepingIgnored(true);
            player.setCanPickupItems(false);
            player.setCollidable(false);
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(player.isFlying() || player.getAllowFlight());
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aVanish &2activado&a. Quedaste oculto para jugadores sin permiso de staff."));
        } else {
            vanishedPlayers.remove(player.getUniqueId());
            player.removeMetadata(VANISH_METADATA, plugin);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) {
                    viewer.showPlayer(plugin, player);
                }
            }

            player.setSleepingIgnored(false);
            player.setCanPickupItems(true);
            player.setCollidable(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aVanish &7desactivado&a. Volviste a ser visible."));
        }

        if (playEffects) {
            playVanishEffects(player);
        }
    }

    public void revealAll() {
        for (UUID uuid : vanishedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            setVanished(player, false, false);
        }
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

        if (isVanished(joining)) {
            e.joinMessage(null);
            joining.setMetadata(VANISH_METADATA, new FixedMetadataValue(plugin, true));
            joining.setSleepingIgnored(true);
            joining.setCanPickupItems(false);
            joining.setCollidable(false);
        }

        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && vanished.isOnline() && !canSeeVanished(joining)) {
                joining.hidePlayer(plugin, vanished);
            }
        }

        if (isVanished(joining)) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(joining)) {
                    continue;
                }
                if (!canSeeVanished(viewer)) {
                    viewer.hidePlayer(plugin, joining);
                }
            }
            joining.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEntraste al servidor manteniendo tu modo &2Vanish&a."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player quitting = e.getPlayer();
        if (isVanished(quitting)) {
            e.quitMessage(null);
        }
    }

    private boolean canSeeVanished(Player player) {
        return player.hasPermission(SEE_PERMISSION) || player.hasPermission(VANISH_PERMISSION);
    }
}
