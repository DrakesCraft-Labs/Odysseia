package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Comando para activar/desactivar la Niebla Ultra Densa de Terror.
 * Uso: /niebla <on|off|toggle> [jugador|all]
 */
public class HorrorFogCommand implements CommandExecutor, TabCompleter {

    private final org.metamechanists.odysseia.Odysseia plugin;
    private final Set<UUID> fogActivePlayers = new HashSet<>();
    private BukkitTask fogTask;

    public HorrorFogCommand(org.metamechanists.odysseia.Odysseia plugin) {
        this.plugin = plugin;
        startFogLoop();
    }

    private void startFogLoop() {
        fogTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (fogActivePlayers.isEmpty()) return;

            List<UUID> toRemove = new ArrayList<>();
            for (UUID uuid : fogActivePlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    toRemove.add(uuid);
                    continue;
                }

                // Aplica niebla de renderizado ultra densa (~1.5 bloques de visión)
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 1, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));

                // Spawn de partículas de humo denso y partículas de dragón alrededor de la cabeza
                var head = player.getEyeLocation();
                org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(
                        player, head, 6, 0.4, 0.4, 0.4, 0.01, 1.0f);
                player.spawnParticle(Particle.LARGE_SMOKE, head, 8, 0.5, 0.5, 0.5, 0.02);
                player.spawnParticle(Particle.SQUID_INK, head, 4, 0.3, 0.3, 0.3, 0.01);
            }
            fogActivePlayers.removeAll(toRemove);
        }, 10L, 10L); // Ejecuta cada 0.5s
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("odysseia.horrorfog") && !sender.hasPermission("drakes.staff")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para controlar la Niebla de Terror.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Uso: " + ChatColor.YELLOW + "/niebla <on|off|toggle> [jugador|all]");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        List<Player> targets = new ArrayList<>();

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("@a")) {
                targets.addAll(Bukkit.getOnlinePlayers());
            } else {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado: " + args[1]);
                    return true;
                }
                targets.add(p);
            }
        } else {
            if (sender instanceof Player player) {
                targets.add(player);
            } else {
                targets.addAll(Bukkit.getOnlinePlayers());
            }
        }

        switch (action) {
            case "on", "enable", "activar" -> {
                for (Player target : targets) {
                    fogActivePlayers.add(target.getUniqueId());
                    target.sendMessage(ChatColor.DARK_GRAY + "🌫️ " + ChatColor.DARK_RED + "Una niebla ultra densa ha envuelto tus sentidos...");
                    target.playSound(target.getLocation(), Sound.AMBIENT_CAVE, 1.5f, 0.4f);
                }
                sender.sendMessage(ChatColor.GREEN + "Niebla densa activada para " + targets.size() + " jugador(es).");
            }
            case "off", "disable", "desactivar" -> {
                for (Player target : targets) {
                    fogActivePlayers.remove(target.getUniqueId());
                    target.removePotionEffect(PotionEffectType.DARKNESS);
                    target.removePotionEffect(PotionEffectType.BLINDNESS);
                    target.sendMessage(ChatColor.GRAY + "🌫️ " + ChatColor.GREEN + "La densa niebla se ha disipado.");
                }
                sender.sendMessage(ChatColor.GREEN + "Niebla densa desactivada para " + targets.size() + " jugador(es).");
            }
            case "toggle" -> {
                int countOn = 0;
                for (Player target : targets) {
                    if (fogActivePlayers.contains(target.getUniqueId())) {
                        fogActivePlayers.remove(target.getUniqueId());
                        target.removePotionEffect(PotionEffectType.DARKNESS);
                        target.removePotionEffect(PotionEffectType.BLINDNESS);
                        target.sendMessage(ChatColor.GRAY + "🌫️ " + ChatColor.GREEN + "La densa niebla se ha disipado.");
                    } else {
                        fogActivePlayers.add(target.getUniqueId());
                        target.sendMessage(ChatColor.DARK_GRAY + "🌫️ " + ChatColor.DARK_RED + "Una niebla ultra densa ha envuelto tus sentidos...");
                        target.playSound(target.getLocation(), Sound.AMBIENT_CAVE, 1.5f, 0.4f);
                        countOn++;
                    }
                }
                sender.sendMessage(ChatColor.GOLD + "Niebla alternada. Jugadores con niebla activa: " + countOn);
            }
            default -> sender.sendMessage(ChatColor.RED + "Acción desconocida. Usa: /niebla <on|off|toggle> [jugador|all]");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "toggle").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2) {
            List<String> list = new ArrayList<>(List.of("all"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
            return list.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
