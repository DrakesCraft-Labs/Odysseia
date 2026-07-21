package org.metamechanists.odysseia.dragon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Sistema Avanzado de Montura Dracónica.
 * Incluye velocidad súper rápida configurable (/dragon speed <1-20>)
 * y tipos de aliento personalizables (/dragon aliento <fuego|rayos|arboles|estrellas|hielo|vacío>).
 */
public class DragonMountService implements CommandExecutor, TabCompleter, Listener {

    private final Odysseia plugin;
    private final Random random = new Random();
    private final Map<UUID, EnderDragon> activeDragons = new HashMap<>();
    private final Map<UUID, Boolean> isKikaDragon = new HashMap<>();
    private final Map<UUID, Double> playerSpeeds = new HashMap<>();
    private final Map<UUID, String> playerBreaths = new HashMap<>();
    private BukkitTask flightTask;

    private static final List<String> BREATH_TYPES = List.of("fuego", "rayos", "arboles", "estrellas", "hielo", "vacío");
    private static final TreeType[] TREE_TYPES = {
        TreeType.TREE, TreeType.BIRCH, TreeType.BIG_TREE, TreeType.JUNGLE, TreeType.ACACIA, TreeType.DARK_OAK, TreeType.CHERRY, TreeType.AZALEA
    };

    public DragonMountService(Odysseia plugin) {
        this.plugin = plugin;
        startFlightLoop();
    }

    public void startFlightLoop() {
        flightTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeDragons.isEmpty()) return;

            activeDragons.entrySet().removeIf(entry -> {
                UUID playerUuid = entry.getKey();
                EnderDragon dragon = entry.getValue();
                Player player = Bukkit.getPlayer(playerUuid);

                if (player == null || !player.isOnline() || dragon == null || dragon.isDead() || !dragon.getPassengers().contains(player)) {
                    dismountAndCleanup(player, dragon);
                    return true;
                }

                boolean kika = isKikaDragon.getOrDefault(playerUuid, false);
                double customSpeed = playerSpeeds.getOrDefault(playerUuid, kika ? 2.0 : 3.0);

                // Dirección de la mirada
                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection();

                // Teleportación y velocidad hiper-rápida
                Location dragonLoc = dragon.getLocation();
                dragonLoc.setYaw(eye.getYaw() + 180F);
                dragonLoc.setPitch(eye.getPitch());

                Vector velocity = dir.clone().multiply(customSpeed);
                dragon.teleport(dragonLoc.add(velocity));

                // Estela de partículas
                if (kika) {
                    dragonLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, dragonLoc, 8, 0.6, 0.4, 0.6, 0.05);
                    dragonLoc.getWorld().spawnParticle(Particle.COMPOSTER, dragonLoc, 5, 0.5, 0.3, 0.5, 0.02);
                } else {
                    dragonLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, dragonLoc, 8, 0.8, 0.5, 0.8, 0.05);
                    dragonLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, dragonLoc, 6, 0.8, 0.5, 0.8, 0.02);
                    dragonLoc.getWorld().spawnParticle(Particle.END_ROD, dragonLoc, 4, 0.5, 0.5, 0.5, 0.05);
                }

                return false;
            });
        }, 1L, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede usar este comando.");
            return true;
        }

        String nameLower = player.getName().toLowerCase(Locale.ROOT);
        boolean isOwner = nameLower.contains("jackstar") || player.hasPermission("odysseia.dragon.owner");
        boolean isKika = nameLower.contains("kika") || player.hasPermission("odysseia.dragon.kika");

        if (!isOwner && !isKika && !player.hasPermission("odysseia.dragon.use")) {
            player.sendMessage(ChatColor.RED + "🔒 Reservado exclusivamente para JackStar6677 y Kika.");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            // Subcomando: /dragon speed <1-20>
            if (sub.equals("speed") || sub.equals("velocidad")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GOLD + "Velocidad actual: " + playerSpeeds.getOrDefault(player.getUniqueId(), isKika ? 2.0 : 3.0));
                    player.sendMessage(ChatColor.YELLOW + "Uso: /dragon speed <1-20>");
                    return true;
                }
                try {
                    double val = Double.parseDouble(args[1]);
                    val = Math.max(0.5, Math.min(20.0, val));
                    playerSpeeds.put(player.getUniqueId(), val);
                    player.sendMessage(ChatColor.GREEN + "⚡ Velocidad del Dragón establecida en: " + ChatColor.AQUA + val + "x");
                } catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Por favor ingresa un número válido entre 1 y 20.");
                }
                return true;
            }

            // Subcomando: /dragon aliento <tipo>
            if (sub.equals("aliento") || sub.equals("breath") || sub.equals("element")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GOLD + "Aliento actual: " + playerBreaths.getOrDefault(player.getUniqueId(), isKika ? "arboles" : "fuego"));
                    player.sendMessage(ChatColor.YELLOW + "Opciones: fuego, rayos, arboles, estrellas, hielo, vacío");
                    return true;
                }
                String breath = args[1].toLowerCase(Locale.ROOT);
                if (!BREATH_TYPES.contains(breath)) {
                    player.sendMessage(ChatColor.RED + "Aliento no válido. Opciones: fuego, rayos, arboles, estrellas, hielo, vacío");
                    return true;
                }
                playerBreaths.put(player.getUniqueId(), breath);
                player.sendMessage(ChatColor.GREEN + "🔥 Aliento del Dragón cambiado a: " + ChatColor.LIGHT_PURPLE + breath.toUpperCase(Locale.ROOT));
                return true;
            }
        }

        // Toggle invocación de montura
        if (activeDragons.containsKey(player.getUniqueId())) {
            EnderDragon existing = activeDragons.remove(player.getUniqueId());
            isKikaDragon.remove(player.getUniqueId());
            dismountAndCleanup(player, existing);
            player.sendMessage(ChatColor.YELLOW + "🐉 Has desmontado de tu Dragón.");
            return true;
        }

        Location spawnLoc = player.getLocation();
        EnderDragon dragon = (EnderDragon) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
        dragon.setAI(false);
        dragon.setInvulnerable(true);

        var scaleAttr = dragon.getAttribute(Attribute.SCALE);

        if (isKika) {
            dragon.setCustomName("§a§l🐉 Dragón Esmeralda Cute de " + player.getName());
            dragon.setCustomNameVisible(true);
            if (scaleAttr != null) scaleAttr.setBaseValue(0.65);
            isKikaDragon.put(player.getUniqueId(), true);
            if (!playerBreaths.containsKey(player.getUniqueId())) playerBreaths.put(player.getUniqueId(), "arboles");

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 1.8f);
            player.sendMessage(ChatColor.GREEN + "✨ ¡Dragón Esmeralda Invocado! Usa /dragon speed <1-20> y /dragon aliento <tipo>");
        } else {
            dragon.setCustomName("§d§l🐉 Dragón Supremo de " + player.getName());
            dragon.setCustomNameVisible(true);
            if (scaleAttr != null) scaleAttr.setBaseValue(1.4);
            isKikaDragon.put(player.getUniqueId(), false);
            if (!playerBreaths.containsKey(player.getUniqueId())) playerBreaths.put(player.getUniqueId(), "fuego");

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.6f);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "🐉 ¡Dragón Supremo Invocado! Usa /dragon speed <1-20> y /dragon aliento <tipo>");
        }

        dragon.addPassenger(player);
        activeDragons.put(player.getUniqueId(), dragon);
        return true;
    }

    /** Ejecuta el aliento seleccionado al hacer clic izquierdo. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!activeDragons.containsKey(player.getUniqueId())) return;

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            EnderDragon dragon = activeDragons.get(player.getUniqueId());
            if (dragon == null || dragon.isDead()) return;

            boolean kika = isKikaDragon.getOrDefault(player.getUniqueId(), false);
            String breath = playerBreaths.getOrDefault(player.getUniqueId(), kika ? "arboles" : "fuego");
            Location eye = player.getEyeLocation();
            Vector dir = eye.getDirection();

            switch (breath) {
                case "arboles" -> {
                    // Genera árboles donde mira la cámara
                    org.bukkit.block.Block target = player.getTargetBlockExact(50);
                    if (target != null && target.getType() != Material.AIR) {
                        Location treeLoc = target.getLocation().add(0, 1, 0);
                        TreeType randomTree = TREE_TYPES[random.nextInt(TREE_TYPES.length)];
                        treeLoc.getWorld().generateTree(treeLoc, randomTree);
                        treeLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, treeLoc, 40, 1, 1, 1, 0.1);
                        player.playSound(player.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.5f, 1.2f);
                    }
                }
                case "rayos" -> {
                    org.bukkit.block.Block target = player.getTargetBlockExact(60);
                    Location targetLoc = target != null ? target.getLocation() : eye.add(dir.multiply(30));
                    targetLoc.getWorld().strikeLightningEffect(targetLoc);
                    targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 1, 0, 0, 0, 0);
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.8f, 0.8f);
                }
                case "estrellas" -> {
                    eye.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, eye.clone().add(dir.multiply(2)), 40, 0.8, 0.8, 0.8, 0.1);
                    eye.getWorld().spawnParticle(Particle.FIREWORK, eye.clone().add(dir.multiply(2)), 20, 0.5, 0.5, 0.5, 0.1);
                    player.playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.4f);
                }
                case "hielo" -> {
                    for (double d = 1; d <= 20; d += 1) {
                        Location p = eye.clone().add(dir.clone().multiply(d));
                        p.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 15, 0.3, 0.3, 0.3, 0.05);
                    }
                    player.playSound(eye, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.5f);
                }
                case "vacío" -> {
                    for (double d = 1; d <= 20; d += 1) {
                        Location p = eye.clone().add(dir.clone().multiply(d));
                        p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p, 15, 0.4, 0.4, 0.4, 0.02);
                    }
                    player.playSound(eye, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
                }
                default -> { // fuego
                    DragonFireball fireball = (DragonFireball) eye.getWorld().spawnEntity(eye.add(dir.multiply(1.8)), EntityType.DRAGON_FIREBALL);
                    fireball.setDirection(dir.multiply(1.8));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.8f, 0.8f);
                }
            }
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && activeDragons.containsKey(player.getUniqueId())) {
            EnderDragon dragon = activeDragons.remove(player.getUniqueId());
            isKikaDragon.remove(player.getUniqueId());
            dismountAndCleanup(player, dragon);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeDragons.containsKey(player.getUniqueId())) {
            EnderDragon dragon = activeDragons.remove(player.getUniqueId());
            isKikaDragon.remove(player.getUniqueId());
            dismountAndCleanup(player, dragon);
        }
    }

    private void dismountAndCleanup(Player player, EnderDragon dragon) {
        if (dragon != null && !dragon.isDead()) {
            dragon.remove();
        }
        if (player != null && player.isOnline()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0, false, false));
        }
    }

    public void shutdown() {
        if (flightTask != null) flightTask.cancel();
        for (EnderDragon dragon : activeDragons.values()) {
            if (dragon != null && !dragon.isDead()) {
                dragon.remove();
            }
        }
        activeDragons.clear();
        isKikaDragon.clear();
        playerSpeeds.clear();
        playerBreaths.clear();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("speed", "aliento").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("speed")) {
            return List.of("1", "2", "3", "5", "10", "15", "20");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("aliento") || args[0].equalsIgnoreCase("breath"))) {
            return BREATH_TYPES.stream().filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
