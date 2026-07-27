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
import org.bukkit.entity.WitherSkeleton;
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
 * Uso: /troll <screamer|herobrine|haunting|shadow|whispers|redsky|fakeop|fakecrash|voidfall|anvil|creeper|spiders|lightning> <jugador>
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
            case "herobrine" -> {
                triggerHerobrine(target);
                sender.sendMessage(ChatColor.GREEN + "Herobrine apareció ante " + target.getName());
            }
            case "haunting" -> {
                triggerHaunting(target);
                sender.sendMessage(ChatColor.GREEN + "Acecho de HorrorNight enviado a " + target.getName());
            }
            case "shadow" -> {
                triggerShadow(target);
                sender.sendMessage(ChatColor.GREEN + "La sombra comenzó a seguir a " + target.getName());
            }
            case "whispers" -> {
                triggerWhispers(target);
                sender.sendMessage(ChatColor.GREEN + "Susurros enviados a " + target.getName());
            }
            case "redsky" -> {
                triggerRedSky(target);
                sender.sendMessage(ChatColor.GREEN + "Falsa luna roja mostrada a " + target.getName());
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
            return List.of("screamer", "herobrine", "haunting", "shadow", "whispers", "redsky", "fakeop", "fakecrash", "voidfall", "anvil", "creeper", "spiders", "lightning")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }

    /** Creates a short Herobrine sighting without damage, movement or world edits. */
    private void triggerHerobrine(Player target) {
        Location origin = target.getLocation().clone();
        Location apparition = origin.clone().add(origin.getDirection().normalize().multiply(6.0));
        apparition.setY(Math.max(apparition.getWorld().getMinHeight() + 1,
            apparition.getWorld().getHighestBlockYAt(apparition) + 1));
        WitherSkeleton shadow = (WitherSkeleton) apparition.getWorld().spawnEntity(apparition, EntityType.WITHER_SKELETON);
        shadow.setCustomName("§4§lHerobrine");
        shadow.setCustomNameVisible(true);
        shadow.setAI(false);
        shadow.setInvulnerable(true);
        shadow.setCollidable(false);
        shadow.setGlowing(true);

        target.sendTitle("§4§lHEROBRINE", "§8Te está observando...", 5, 35, 10);
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 45, 0, false, false));
        target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 2.0f, 0.45f);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1.4f, 0.5f);
        apparition.getWorld().spawnParticle(Particle.PORTAL, apparition.clone().add(0, 1, 0), 80, 0.5, 1.0, 0.5, 0.08);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!shadow.isValid()) return;
            shadow.getWorld().spawnParticle(Particle.LARGE_SMOKE, shadow.getLocation().add(0, 1, 0), 35, 0.5, 1.0, 0.5, 0.04);
            shadow.getWorld().playSound(shadow.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.6f, 0.45f);
            shadow.remove();
        }, 55L);
    }

    /** Adds an ambient horror trail and private whispers for the HorrorNight theme. */
    private void triggerHaunting(Player target) {
        target.sendMessage("§8[§4???§8] §7No mires atrás.");
        target.sendMessage("§8[§4???§8] §7La noche recuerda tu nombre.");
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 70, 0, false, false));
        for (int tick = 0; tick < 60; tick += 10) {
            int delay = tick;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline() || target.isDead()) return;
                Location point = target.getLocation().clone().subtract(target.getLocation().getDirection().multiply(2.5));
                point.add(0, 1.0, 0);
                point.getWorld().spawnParticle(Particle.SOUL, point, 18, 0.35, 0.7, 0.35, 0.02);
                target.playSound(target.getLocation(), Sound.AMBIENT_CAVE, 0.8f, 0.35f + delay / 300.0f);
            }, delay);
        }
    }

    /** Moves an invulnerable visual shadow twice, then removes it automatically. */
    private void triggerShadow(Player target) {
        Location first = target.getLocation().subtract(target.getLocation().getDirection().multiply(7.0));
        first.setY(Math.max(first.getWorld().getMinHeight() + 1, first.getWorld().getHighestBlockYAt(first) + 1));
        WitherSkeleton shadow = (WitherSkeleton) first.getWorld().spawnEntity(first, EntityType.WITHER_SKELETON);
        shadow.setCustomName("§8§lLa Sombra");
        shadow.setCustomNameVisible(false);
        shadow.setAI(false);
        shadow.setInvulnerable(true);
        shadow.setCollidable(false);
        shadow.setGlowing(true);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1.2f, 0.4f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> moveShadow(target, shadow), 35L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> moveShadow(target, shadow), 75L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (shadow.isValid()) shadow.remove();
        }, 115L);
    }

    private void moveShadow(Player target, WitherSkeleton shadow) {
        if (!target.isOnline() || target.isDead() || !shadow.isValid()) return;
        Location point = target.getLocation().subtract(target.getLocation().getDirection().multiply(6.0));
        point.setY(Math.max(point.getWorld().getMinHeight() + 1, point.getWorld().getHighestBlockYAt(point) + 1));
        shadow.teleport(point);
        point.getWorld().spawnParticle(Particle.PORTAL, point.add(0, 1, 0), 45, 0.3, 0.8, 0.3, 0.04);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.35f);
    }

    /** Delivers a timed private whisper chain, with no public chat spam. */
    private void triggerWhispers(Player target) {
        String[] messages = {
            "§8[§4???§8] §7Tu luz se ve desde muy lejos.",
            "§8[§4???§8] §7Hay algo detrás de la niebla.",
            "§8[§4???§8] §7Herobrine no necesita entrar por la puerta.",
            "§8[§4???§8] §7Ahora puedes volver a mirar."
        };
        for (int index = 0; index < messages.length; index++) {
            int delay = index * 35;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline() || target.isDead()) return;
                target.sendMessage(messages[delay / 35]);
                target.playSound(target.getLocation(), Sound.AMBIENT_CAVE, 1.4f, 0.35f);
            }, delay);
        }
    }

    /** Creates a blood-red visual sky moment without changing world time or weather. */
    private void triggerRedSky(Player target) {
        target.sendTitle("§4§lLUNA DE SANGRE", "§8Algo te observa desde arriba", 5, 45, 15);
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 65, 0, false, false));
        for (int tick = 0; tick < 80; tick += 10) {
            int delay = tick;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline() || target.isDead()) return;
                Location point = target.getLocation().add(0, 2.5, 0);
                point.getWorld().spawnParticle(Particle.DUST, point, 32, 2.5, 0.25, 2.5,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 0, 0), 1.4f));
                if (delay % 20 == 0) target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.45f);
            }, delay);
        }
    }
}
