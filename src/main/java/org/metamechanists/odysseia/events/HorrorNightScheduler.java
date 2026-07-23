package org.metamechanists.odysseia.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.instances.WitherStormBoss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evento Nocturno de Terror del Wither Storm.
 * Se ejecuta ESPECÍFICAMENTE UNA VEZ POR DÍA DE MINECRAFT durante la noche.
 * Distribuye aproximadamente 3 eventos terroríficos y screamers aleatorios entre los jugadores conectados.
 */
public class HorrorNightScheduler implements Listener {

    private final Odysseia plugin;
    private final Random random = new Random();
    private final ConcurrentHashMap<String, Long> lastNightProcessedPerWorld = new ConcurrentHashMap<>();
    private BukkitTask task;

    public HorrorNightScheduler(Odysseia plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) task.cancel();
        if (!plugin.getConfig().getBoolean("horror-night.enabled", true)) {
            plugin.getLogger().info("[HorrorNight] Desactivado por configuración.");
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Revisa cada 5 segundos (100 ticks) el estado del ciclo día/noche
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkNightCycle, 100L, 100L);
        plugin.getLogger().info("[HorrorNight] Sistema de Noche de Terror activado (sin invocación automática de jefes).");
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void checkNightCycle() {
        if (!plugin.getConfig().getBoolean("horror-night.enabled", true) || Bukkit.getOnlinePlayers().isEmpty()) return;
        Set<String> enabledWorlds = new HashSet<>(plugin.getConfig().getStringList("horror-night.worlds"));

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(world.getName())) continue;
            if (world.getPlayers().stream().noneMatch(player -> player.isOnline() && !player.isDead()
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR)) continue;

            long time = world.getTime();
            long dayIndex = world.getFullTime() / 24000L;

            // Noche en Minecraft: entre tick 13000 y 22000
            if (time >= 13000 && time <= 22000) {
                String worldKey = world.getName();
                Long lastDay = lastNightProcessedPerWorld.get(worldKey);

                if (lastDay == null || lastDay < dayIndex) {
                    lastNightProcessedPerWorld.put(worldKey, dayIndex);
                    triggerNightHorrorSequence(world);
                }
            }
        }
    }

    /** Dispara la secuencia nocturna (eventos atmosféricos y ambientación de terror). */
    private void triggerNightHorrorSequence(World world) {
        plugin.getLogger().info("[HorrorNight] Secuencia nocturna iniciada para el día " + (world.getFullTime() / 24000L));

        for (Player p : world.getPlayers()) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&4&l✦ [NOCHE DE TERROR] &cLa niebla oscura envuelve el mapa... Los monstruos nocturnos llevan reliquias de Slimefun."));
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 2.0f, 0.4f);
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f);
        }

        // Programar 3 eventos en momentos aleatorios de la noche (entre 10 y 50 segundos)
        for (int i = 1; i <= 3; i++) {
            long delayTicks = (long) (i * 250 + random.nextInt(400));
            final int eventIndex = i;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeRandomHorrorStrike(world, eventIndex);
            }, delayTicks);
        }
    }

    /** Ejecuta un evento de terror aleatorio a un jugador conectado. */
    private void executeRandomHorrorStrike(World world, int strikeIndex) {
        if (!plugin.getConfig().getBoolean("horror-night.enabled", true)
                || world.getTime() < 13000L || world.getTime() > 22000L) {
            return;
        }
        List<Player> eligiblePlayers = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.isOnline() && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                eligiblePlayers.add(p);
            }
        }

        if (eligiblePlayers.isEmpty()) return;

        Player victim = eligiblePlayers.get(random.nextInt(eligiblePlayers.size()));
        int horrorType = random.nextInt(4);

        switch (horrorType) {
            case 0 -> WitherStormBoss.triggerScreamer(victim);
            case 1 -> triggerCreepyWhispers(victim);
            case 2 -> triggerShadowStalker(victim);
            case 3 -> triggerAtmosphericEclipse(victim);
        }
    }

    /** Evento 1: Susurros del Más Allá y distorsión ambiental. */
    private void triggerCreepyWhispers(Player player) {
        String[] whispers = {
            "§8[§4???§8] §7\"Puedo oler tu miedo en la oscuridad...\"",
            "§8[§4???§8] §7\"Las sombras acechan este mundo...\"",
            "§8[§4???§8] §7\"No estás solo en la penumbra...\"",
            "§8[§4???§8] §7\"Los monstruos de la noche portan secretos del Slimefun...\""
        };
        String msg = whispers[random.nextInt(whispers.length)];
        player.sendMessage(msg);
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.8f, 0.4f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1.5f, 0.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 1, false, false));
    }

    /** Evento 2: Aparición fugaz de una sombra que se desvanece al mirar. */
    private void triggerShadowStalker(Player player) {
        Location behind = player.getLocation().subtract(player.getLocation().getDirection().multiply(2.5)).add(0, 0.5, 0);
        if (behind.getWorld() == null) return;

        WitherSkeleton ghost = (WitherSkeleton) behind.getWorld().spawnEntity(behind, EntityType.WITHER_SKELETON);
        ghost.setCustomName("§8§lSombra del Vacío");
        ghost.setCustomNameVisible(true);
        ghost.setGlowing(true);
        ghost.setAI(false);
        ghost.setInvulnerable(true);

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ghost.isValid()) {
                ghost.getWorld().spawnParticle(Particle.LARGE_SMOKE, ghost.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
                ghost.getWorld().playSound(ghost.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.5f);
                ghost.remove();
            }
        }, 40L); // Desaparece tras 2 segundos
    }

    /** Evento 3: Eclipse atmosférico sin invocación de jefes. */
    private void triggerAtmosphericEclipse(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&5&l⚡ &dUn relámpago de sangre rasga el firmamento. La Noche de Terror se intensifica."));

        player.getWorld().strikeLightningEffect(player.getLocation().add(random.nextInt(10) - 5, 0, random.nextInt(10) - 5));
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 1, false, false));

        org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(
                player.getWorld(), player.getLocation().add(0, 2, 0),
                80, 2.0, 2.0, 2.0, 0.1, 0.1f);
    }

    /** Dropeo especial de ítems de Slimefun al matar monstruos en la Noche de Terror. */
    @org.bukkit.event.EventHandler(ignoreCancelled = true)
    public void onNightMobDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Monster mob)) return;
        if (mob.getKiller() == null) return;

        World world = mob.getWorld();
        long time = world.getTime();
        boolean isNight = time >= 13000L && time <= 22000L;
        if (!isNight && !plugin.getBloodMoonManager().isActive(world)) return;

        double chance = isNight ? 0.35D : 0.20D;
        if (random.nextDouble() > chance) return;

        String[] slimefunItemIds = {
            "MAGICAL_LUMP_1", "MAGICAL_LUMP_2", "SULFUR", "COPPER_DUST",
            "GOLD_DUST", "IRON_DUST", "ALUMINUM_DUST", "ZINC_DUST", "TIN_DUST",
            "MAGNESIUM_DUST", "SILVER_DUST", "DAMASCUS_STEEL", "REINFORCED_ALLOY_INGOT",
            "CORINTHIAN_BRONZE_INGOT", "HARDENED_METAL_INGOT", "REDSTONE_ALLOY",
            "SYNTHETIC_EMERALD", "SIFTED_ORE", "STRANGE_NETHER_DUST", "ENDER_LUMP"
        };

        String selectedId = slimefunItemIds[random.nextInt(slimefunItemIds.length)];
        org.bukkit.inventory.ItemStack sfItem = getSlimefunItem(selectedId);
        if (sfItem != null) {
            sfItem.setAmount(random.nextInt(3) + 1);
            event.getDrops().add(sfItem);

            mob.getWorld().spawnParticle(Particle.WITCH, mob.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }
    }

    /** Intenta obtener un ItemStack de Slimefun por su ID con fallback multi-paquete. */
    private org.bukkit.inventory.ItemStack getSlimefunItem(String id) {
        try {
            Class<?> sfClass;
            try {
                sfClass = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
            } catch (ClassNotFoundException e) {
                sfClass = Class.forName("com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem");
            }
            java.lang.reflect.Method getById = sfClass.getMethod("getById", String.class);
            java.lang.reflect.Method getItem = sfClass.getMethod("getItem");
            Object sfObj = getById.invoke(null, id);
            if (sfObj != null) {
                org.bukkit.inventory.ItemStack item = (org.bukkit.inventory.ItemStack) getItem.invoke(sfObj);
                return item == null ? null : item.clone();
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
