package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

public final class LokiClonesSkill implements BossSkill {

    private final Random random = new Random();

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null || target == null) return;

        Location bossLoc = boss.getEntity().getLocation();
        bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        bossLoc.getWorld().spawnParticle(Particle.PORTAL, bossLoc, 50, 1.0, 1.0, 1.0, 0.1);

        NamespacedKey cloneKey = new NamespacedKey(Odysseia.getInstance(), "loki_clone");

        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 3 + random.nextDouble() * 3;
            Location spawnLoc = bossLoc.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

            Illusioner clone = (Illusioner) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ILLUSIONER);
            clone.setCustomName("§aIlusión de Loki");
            clone.setCustomNameVisible(true);
            clone.setTarget(target);
            
            // Set max health to 1.0
            var healthAttr = clone.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(1.0);
            }
            clone.setHealth(1.0);

            // Tag clone
            clone.getPersistentDataContainer().set(cloneKey, PersistentDataType.BYTE, (byte) 1);

            // Remove clone after 15 seconds (300 ticks)
            Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
                if (clone.isValid() && !clone.isDead()) {
                    clone.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, clone.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
                    clone.remove();
                }
            }, 300L);
        }
    }
}
