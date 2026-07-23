package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public final class KratosChainsSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null || target == null) return;

        Location start = boss.getEntity().getLocation().add(0, 1.0, 0);
        Location end = target.getLocation().add(0, 1.0, 0);
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();

        // Draw chains of fire particles
        if (distance > 0.1) {
            direction.normalize();
            for (double d = 0; d < distance; d += 0.4) {
                Location point = start.clone().add(direction.clone().multiply(d));
                point.getWorld().spawnParticle(Particle.FLAME, point, 5, 0.05, 0.05, 0.05, 0.02);
                point.getWorld().spawnParticle(Particle.LAVA, point, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }

        start.getWorld().playSound(start, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.8f);
        end.getWorld().playSound(end, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.5f);

        // Pull player towards Kratos
        Vector pull = start.toVector().subtract(end.toVector());
        if (pull.lengthSquared() > 0.01) {
            pull.normalize().multiply(1.4).setY(0.4);
            target.setVelocity(pull);
        }

        // Set target on fire
        target.setFireTicks(100); // 5 seconds
        target.damage(12.0, boss.getEntity()); // fire strike damage
    }
}
