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
        if (!plugin.getConfig().getBoolean("horror-night.enabled", false)) {
            plugin.getLogger().info("[HorrorNight] Desactivado por configuración.");
            return;
        }

        // Revisa cada 5 segundos (100 ticks) el estado del ciclo día/noche
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkNightCycle, 100L, 100L);
        plugin.getLogger().info("[HorrorNight] Sistema de eventos de terror nocturno y Wither Storm activado.");
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void checkNightCycle() {
        if (!plugin.getConfig().getBoolean("horror-night.enabled", false) || Bukkit.getOnlinePlayers().isEmpty()) return;
        Set<String> enabledWorlds = new HashSet<>(plugin.getConfig().getStringList("horror-night.worlds"));

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(world.getName())) continue;
            if (world.getPlayers().stream().noneMatch(player -> player.isOnline() && !player.isDead()
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR)) continue;

            long time = world.getTime();
            long dayIndex = world.getFullTime() / 24000L;

            // Noche en Minecraft: entre tick 13000 y 23000
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

    /** Dispara la secuencia nocturna (3 eventos terroríficos distribuidos en la noche). */
    private void triggerNightHorrorSequence(World world) {
        plugin.getLogger().info("[HorrorNight] Se ha iniciado la secuencia de terror nocturno para el día " + (world.getFullTime() / 24000L));

        // Programar 3 eventos en momentos aleatorios de la noche (entre 5 y 45 segundos de diferencia)
        for (int i = 1; i <= 3; i++) {
            long delayTicks = (long) (i * 200 + random.nextInt(400));
            final int eventIndex = i;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeRandomHorrorStrike(world, eventIndex);
            }, delayTicks);
        }
    }

    /** Ejecuta un evento de terror aleatorio a un jugador conectado. */
    private void executeRandomHorrorStrike(World world, int strikeIndex) {
        if (!plugin.getConfig().getBoolean("horror-night.enabled", false)
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
            case 3 -> triggerWitherStormAtmosphere(victim, strikeIndex == 3);
        }
    }

    /** Evento 1: Susurros del Más Allá y distorsión ambiental. */
    private void triggerCreepyWhispers(Player player) {
        String[] whispers = {
            "§8[§4???§8] §7\"Puedo oler tu miedo en la oscuridad...\"",
            "§8[§4???§8] §7\"El Wither Storm todo lo consume...\"",
            "§8[§4???§8] §7\"No estás solo en estas sombras...\"",
            "§8[§4???§8] §7\"Tus gritos no llegarán a la superficie...\""
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

    /** Evento 3: Atmósfera de la Tormenta con relámpago inofensivo e invasión visual. */
    private void triggerWitherStormAtmosphere(Player player, boolean isFinalStrike) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&5&l⚡ &dEl cielo se oscurece... La Presencia de la Tormenta se avecina."));

        player.getWorld().strikeLightningEffect(player.getLocation().add(random.nextInt(10) - 5, 0, random.nextInt(10) - 5));
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 1, false, false));

        org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(
                player.getWorld(), player.getLocation().add(0, 2, 0),
                80, 2.0, 2.0, 2.0, 0.1, 0.1f);

        if (isFinalStrike) {
            // En el 3er impacto de la noche, si se desea, invoca al Wither Storm cerca
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isDead()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&4&l👁️ EL WITHER STORM HA DESPERTADO CERCA DE TI."));
                    plugin.getBossManager().spawnBoss("wither_storm", player.getLocation().add(15, 5, 15));
                }
            }, 60L);
        }
    }
}
