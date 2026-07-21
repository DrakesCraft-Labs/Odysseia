package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.instances.WitherStormBoss;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sistema de Trolleos Inofensivos para Staff.
 * Uso: /troll <screamer|fakeop|fakecrash|voidfall|anvil|creeper|spiders|lightning> <jugador>
 */
public class TrollCommand implements CommandExecutor, TabCompleter {

    private final Odysseia plugin;

    public TrollCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("odysseia.troll") && !sender.hasPermission("drakes.staff")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar comandos de troll.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.GOLD + "Uso: " + ChatColor.YELLOW + "/troll <subcomando> <jugador>");
            sender.sendMessage(ChatColor.GRAY + "Subcomandos disponibles: screamer, fakeop, fakecrash, voidfall, anvil, creeper, spiders, lightning");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado o desconectado: " + args[1]);
            return true;
        }

        switch (sub) {
            case "screamer" -> {
                WitherStormBoss.triggerScreamer(target);
                sender.sendMessage(ChatColor.GREEN + "Screamer enviado a " + target.getName());
            }
            case "fakeop" -> {
                target.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "[Server: Made " + target.getName() + " a server operator]");
                sender.sendMessage(ChatColor.GREEN + "Mensaje de Fake OP enviado a " + target.getName());
            }
            case "fakecrash" -> {
                target.kickPlayer("Internal Exception: java.io.IOException: An existing connection was forcibly closed by the remote host");
                sender.sendMessage(ChatColor.GREEN + "Fake Crash (Kick silencioso) ejecutado a " + target.getName());
            }
            case "voidfall" -> {
                target.setVelocity(new Vector(0, 2.5, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 1, false, false));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0, false, false));
                target.playSound(target.getLocation(), Sound.AMBIENT_CAVE, 2.0f, 0.4f);
                sender.sendMessage(ChatColor.GREEN + "Voidfall ejecutado en " + target.getName());
            }
            case "anvil" -> {
                Location loc = target.getLocation().add(0, 3, 0);
                FallingBlock anvil = loc.getWorld().spawnFallingBlock(loc, Material.ANVIL.createBlockData());
                anvil.setDropItem(false);
                anvil.setHurtEntities(false);
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "boss_rock");
                anvil.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                sender.sendMessage(ChatColor.GREEN + "Yunque inofensivo lanzado sobre " + target.getName());
            }
            case "creeper" -> {
                Location behind = target.getLocation().subtract(target.getLocation().getDirection().multiply(1.5));
                Creeper creeper = (Creeper) behind.getWorld().spawnEntity(behind, EntityType.CREEPER);
                creeper.setIgnited(true);
                creeper.setCustomName("§c§lBOOM");
                creeper.setCustomNameVisible(true);
                target.playSound(target.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 2.0f, 1.0f);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (creeper.isValid()) {
                        creeper.getWorld().spawnParticle(Particle.EXPLOSION, creeper.getLocation(), 2, 0, 0, 0, 0);
                        creeper.remove();
                    }
                }, 25L);
                sender.sendMessage(ChatColor.GREEN + "Falso Creeper generado tras " + target.getName());
            }
            case "spiders" -> {
                List<Spider> spiders = new ArrayList<>();
                Location center = target.getLocation();
                for (int i = 0; i < 4; i++) {
                    Location loc = center.clone().add((Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3);
                    Spider spider = (Spider) loc.getWorld().spawnEntity(loc, EntityType.SPIDER);
                    spider.setGlowing(true);
                    spider.setCustomName("§4§lAraña del Vacío");
                    spiders.add(spider);
                }
                target.playSound(target.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 2.0f, 0.6f);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Spider s : spiders) {
                        if (s.isValid()) {
                            s.getWorld().spawnParticle(Particle.LARGE_SMOKE, s.getLocation().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.05);
                            s.remove();
                        }
                    }
                }, 60L);
                sender.sendMessage(ChatColor.GREEN + "Arañas de troll lanzadas a " + target.getName());
            }
            case "lightning" -> {
                target.getWorld().strikeLightningEffect(target.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Rayo inofensivo lanzado a " + target.getName());
            }
            default -> sender.sendMessage(ChatColor.RED + "Subcomando de troll desconocido.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("screamer", "fakeop", "fakecrash", "voidfall", "anvil", "creeper", "spiders", "lightning")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
