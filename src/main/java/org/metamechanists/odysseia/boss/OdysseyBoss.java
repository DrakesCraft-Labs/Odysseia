package org.metamechanists.odysseia.boss;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
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

    public OdysseyBoss(LivingEntity entity, String id, String displayName, double maxHealth, BarColor barColor, BarStyle barStyle) {
        this.entity = entity;
        this.id = id;
        this.displayName = displayName;
        this.maxHealth = maxHealth;

        // Configure entity properties
        entity.setCustomName(displayName);
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);

        // Health attributes
        var healthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(maxHealth);
        }
        entity.setHealth(maxHealth);

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
        // Use Math.max and Math.min for clamp, since Math.clamp might be Java 21 only and we want to ensure compatibility or just use it (it is available in Java 21)
        double progress = Math.max(0.0, Math.min(entity.getHealth() / maxHealth, 1.0));
        bossBar.setProgress(progress);

        // Update players in range (e.g., 30 blocks)
        Set<UUID> currentInRange = new HashSet<>();
        entity.getWorld().getNearbyEntities(entity.getLocation(), 30, 30, 30).forEach(e -> {
            if (e instanceof Player player) {
                currentInRange.add(player.getUniqueId());
                if (!playersWatching.contains(player.getUniqueId())) {
                    bossBar.addPlayer(player);
                    playersWatching.add(player.getUniqueId());
                }
            }
        });

        // Remove players out of range
        Set<UUID> toRemove = new HashSet<>(playersWatching);
        toRemove.removeAll(currentInRange);
        for (UUID uuid : toRemove) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                bossBar.removePlayer(player);
            }
            playersWatching.remove(uuid);
        }
    }

    public void cleanup() {
        bossBar.removeAll();
        playersWatching.clear();
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    public abstract void executeSkillsRotation();
}
