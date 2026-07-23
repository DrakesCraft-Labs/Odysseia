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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishCommand implements CommandExecutor, org.bukkit.command.TabCompleter, Listener {

    private static final String VANISH_PERMISSION = "odysseia.vanish";
    private static final String SEE_PERMISSION = "odysseia.vanish.see";
    private static final String VANISH_METADATA = "vanished";

    private final Odysseia plugin;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask reminderTask;

    public VanishCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    /** Keeps an unobtrusive personal reminder visible while staff are hidden. */
    public void startReminder() {
        reminderTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : vanishedPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendActionBar(vanishReminder());
                }
            }
        }, 20L, 60L);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(VANISH_PERMISSION) && !sender.hasPermission("drakes.staff")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para desvanecerte."));
            return true;
        }

        Player target = null;
        Boolean forceState = null;

        if (args.length == 0) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(ChatColor.RED + "Especifica un jugador: /vani <on|off|toggle> <jugador>");
                return true;
            }
        } else if (args.length == 1) {
            String arg0 = args[0].toLowerCase(java.util.Locale.ROOT);
            if (arg0.equals("on") || arg0.equals("enable") || arg0.equals("activar")) {
                forceState = true;
                if (sender instanceof Player p) target = p;
            } else if (arg0.equals("off") || arg0.equals("disable") || arg0.equals("desactivar")) {
                forceState = false;
                if (sender instanceof Player p) target = p;
            } else if (arg0.equals("toggle")) {
                if (sender instanceof Player p) target = p;
            } else {
                target = Bukkit.getPlayer(args[0]);
            }
        } else {
            String arg0 = args[0].toLowerCase(java.util.Locale.ROOT);
            if (arg0.equals("on") || arg0.equals("enable") || arg0.equals("activar")) {
                forceState = true;
            } else if (arg0.equals("off") || arg0.equals("disable") || arg0.equals("desactivar")) {
                forceState = false;
            }
            target = Bukkit.getPlayer(args[1]);
        }

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador objetivo no encontrado.");
            return true;
        }

        boolean newState = forceState != null ? forceState : !isVanished(target);
        setVanished(target, newState, true);

        if (!sender.equals(target)) {
            sender.sendMessage(ChatColor.GREEN + "Estado de Vanish de " + target.getName() + " cambiado a: " + (newState ? "ACTIVADO" : "DESACTIVADO"));
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            java.util.List<String> options = new java.util.ArrayList<>(java.util.List.of("on", "off", "toggle"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                options.add(p.getName());
            }
            return options.stream().filter(s -> s.toLowerCase(java.util.Locale.ROOT).startsWith(args[0].toLowerCase(java.util.Locale.ROOT))).toList();
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase(java.util.Locale.ROOT).startsWith(args[1].toLowerCase(java.util.Locale.ROOT))).toList();
        }
        return java.util.List.of();
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
            player.sendActionBar(vanishReminder());
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
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aVanish &7desactivado&a. Volviste a ser visible."));
        }

        if (playEffects) {
            playVanishEffects(player);
        }
    }

    public void revealAll() {
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
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

    private net.kyori.adventure.text.Component vanishReminder() {
        return net.kyori.adventure.text.Component.text("ESTAS EN VANISH ACTIVADO", net.kyori.adventure.text.format.NamedTextColor.GREEN)
                .append(net.kyori.adventure.text.Component.text(" | Sigues oculto", net.kyori.adventure.text.format.NamedTextColor.GRAY));
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
            org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(
                    player.getWorld(), particleLoc2, 1, 0, 0, 0, 0, 1.0f);
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
