package org.metamechanists.odysseia.boss;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.Odysseia;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public abstract class OdysseyBoss {

    protected final LivingEntity entity;
    protected final String id;
    protected final String displayName;
    protected final double maxHealth;
    protected final BossBar bossBar;
    protected final Set<UUID> playersWatching = new HashSet<>();

    // Sistema de fases y diálogos común a todos los jefes.
    protected int currentPhase = 1;
    private long lastDialogue = 0L;

    public OdysseyBoss(LivingEntity entity, String id, String displayName, double maxHealth, BarColor barColor, BarStyle barStyle) {
        this.entity = entity;
        this.id = id;
        this.displayName = displayName;
        double healthMultiplier = Math.clamp(
            Odysseia.getInstance().getConfig().getDouble("boss-balance.health-multiplier", 1.60D),
            1.0D,
            10.0D
        );
        this.maxHealth = maxHealth * healthMultiplier;

        // Configure entity properties
        entity.setCustomName(displayName);
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);

        // Health attributes
        var healthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(this.maxHealth);
        }
        entity.setHealth(this.maxHealth);

        // Tag the entity with persistent data
        NamespacedKey bossKey = new NamespacedKey(Odysseia.getInstance(), "boss_type");
        entity.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, id);

        NamespacedKey uuidKey = new NamespacedKey(Odysseia.getInstance(), "boss_uuid");
        entity.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, entity.getUniqueId().toString());

        // Create BossBar
        this.bossBar = Bukkit.createBossBar(displayName, barColor, barStyle);
        this.bossBar.setProgress(1.0);
    }

    public void updateBossBar() {
        if (entity == null || entity.isDead()) {
            bossBar.setProgress(0.0);
            return;
        }
        double progress = Math.max(0.0, Math.min(entity.getHealth() / maxHealth, 1.0));
        bossBar.setProgress(progress);

        // Use player list (O(players)) instead of spatial entity scan (O(entities in chunk radius))
        Set<UUID> currentInRange = new HashSet<>();
        Location bossLoc = entity.getLocation();
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(bossLoc) <= 900) { // 30^2
                currentInRange.add(player.getUniqueId());
                if (playersWatching.add(player.getUniqueId())) {
                    bossBar.addPlayer(player);
                }
            }
        }

        playersWatching.removeIf(uuid -> {
            if (!currentInRange.contains(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) bossBar.removePlayer(p);
                return true;
            }
            return false;
        });
    }

    public void cleanup() {
        bossBar.removeAll();
        playersWatching.clear();
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    // ─── Helpers de targeting reutilizables por los jefes ────────────────────

    /** Jugador vivo más cercano dentro del radio, o null. */
    protected Player findNearestPlayer(double radius) {
        Player target = null;
        double best = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p && p.isOnline() && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                double d = p.getLocation().distanceSquared(entity.getLocation());
                if (d < best) {
                    best = d;
                    target = p;
                }
            }
        }
        return target;
    }

    /** Exposes the valid combat target without leaking spectators or dead players. */
    public Player nearestCombatTarget(double radius) {
        return findNearestPlayer(radius);
    }

    /** Jugador vivo más alejado dentro del radio, o null. */
    protected Player findFarthestPlayer(double radius) {
        Player target = null;
        double best = -1;
        for (org.bukkit.entity.Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p && p.isOnline() && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                double d = p.getLocation().distanceSquared(entity.getLocation());
                if (d > best) {
                    best = d;
                    target = p;
                }
            }
        }
        return target;
    }

    /** Todos los jugadores vivos dentro del radio. */
    protected java.util.List<Player> findPlayersInRange(double radius) {
        java.util.List<Player> players = new java.util.ArrayList<>();
        for (org.bukkit.entity.Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p && p.isOnline() && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                players.add(p);
            }
        }
        return players;
    }

    /** Cura al boss una cantidad de HP, sin superar su máximo. */
    protected void heal(double amount) {
        if (entity == null || entity.isDead()) return;
        entity.setHealth(Math.min(maxHealth, entity.getHealth() + amount));
    }

    /** Nombre corto y limpio del jefe para los diálogos. */
    public String shortName() {
        String n = ChatColor.stripColor(displayName);
        int dash = n.indexOf(" - ");
        if (dash > 0) {
            n = n.substring(0, dash);
        }
        return n.replaceAll("[^\\p{L} ]", "").trim();
    }

    /** Hace que el jefe hable únicamente a los jugadores que participan cerca. */
    public void speak(String frase) {
        long now = System.currentTimeMillis();
        if (now - lastDialogue < 18000) {
            return;
        }

        lastDialogue = now;
        String message = ChatColor.translateAlternateColorCodes('&',
            "&8[&6&l✦&8] &e" + shortName() + "&7: &f\"" + frase + "\"");
        double radius = Math.clamp(Odysseia.getInstance().getConfig().getDouble("boss-balance.dialogue-radius", 48.0D), 16.0D, 128.0D);
        double radiusSquared = radius * radius;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= radiusSquared) {
                player.sendMessage(message);
            }
        }
    }

    /** Detecta cruces de 66% y 33% de vida y dispara la fase correspondiente. */
    public void checkPhases() {
        if (entity == null || entity.isDead()) {
            return;
        }

        double pct = entity.getHealth() / maxHealth;
        if (currentPhase == 1 && pct <= 0.66) {
            currentPhase = 2;
            onPhaseChange(2);
        } else if (currentPhase == 2 && pct <= 0.33) {
            currentPhase = 3;
            onPhaseChange(3);
        }
    }

    /**
     * Comportamiento por defecto al entrar en una fase: buff progresivo, aura de
     * partículas, sonido y diálogo. Los jefes pueden sobreescribirlo para añadir
     * mecánicas propias (llamando super.onPhaseChange(phase) para conservar esto).
     */
    protected void onPhaseChange(int phase) {
        if (entity == null || entity.isDead()) {
            return;
        }

        int amplifier = phase + 1; // fase 2 -> IV, fase 3 -> V
        int duration = 20 * 60 * 20; // 20 minutos, suficiente para el combate sin usar Integer.MAX_VALUE.
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, amplifier, false, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, false, false, false));
        if (phase == 3) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 6, false, false, false));
        }

        var loc = entity.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 60, 1, 1.5, 1, 0.1);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 1, 1, 1, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.2f, phase == 3 ? 0.5f : 0.8f);
        speak(phase == 3
                ? "¡No conocéis mi verdadero poder!"
                : "Esto apenas comienza, mortales.");
    }

    /** Aura de partículas constante según la fase (lo llama el tick del manager). */
    public void tickAura() {
        if (entity == null || entity.isDead() || currentPhase < 2) {
            return;
        }

        var loc = entity.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(currentPhase >= 3 ? Particle.SOUL_FIRE_FLAME : Particle.FLAME,
                loc, currentPhase * 2, 0.5, 0.8, 0.5, 0.01);
    }

    public abstract void executeSkillsRotation();
}
