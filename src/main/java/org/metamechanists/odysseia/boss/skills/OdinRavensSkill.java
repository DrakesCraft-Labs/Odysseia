package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public final class OdinRavensSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null || target == null) return;

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_BITE, 1.0f, 0.7f);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 1.0, 0), 20, 0.5, 0.5, 0.5, 0.05);

        String[] ravens = {"§eHugin §7- §fCuervo de Odín", "§eMunin §7- §fCuervo de Odín"};

        for (String ravenName : ravens) {
            Vex raven = (Vex) loc.getWorld().spawnEntity(loc.clone().add(0, 2, 0), EntityType.VEX);
            raven.setCustomName(ravenName);
            raven.setCustomNameVisible(true);
            raven.setTarget(target);

            // Scale raven properties (high speed, lower health)
            var speedAttr = raven.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(0.4);
            }
            var healthAttr = raven.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(15.0);
            }
            raven.setHealth(15.0);

            // Auto cleanup after 15 seconds
            Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
                if (raven.isValid() && !raven.isDead()) {
                    raven.getWorld().spawnParticle(Particle.POOF, raven.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);
                    raven.remove();
                }
            }, 300L);
        }
    }
}
