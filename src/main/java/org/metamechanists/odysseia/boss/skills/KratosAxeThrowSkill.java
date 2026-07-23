package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public final class KratosAxeThrowSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null || target == null) return;

        Location eyeLoc = boss.getEntity().getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(eyeLoc.toVector());

        if (direction.lengthSquared() > 0.01) {
            direction.normalize();
        } else {
            direction = new Vector(0, -1, 0);
        }

        eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_SNOWBALL_THROW, 1.2f, 0.5f);

        // Spawn snowball representing the Leviathan Axe
        Snowball snowball = boss.getEntity().launchProjectile(Snowball.class);
        snowball.setVelocity(direction.multiply(1.8));

        // Tag the snowball
        snowball.setMetadata("kratos_axe", new FixedMetadataValue(Odysseia.getInstance(), true));

        // Spawn ice particle trail
        Bukkit.getScheduler().runTaskTimer(Odysseia.getInstance(), task -> {
            if (snowball.isDead() || !snowball.isValid()) {
                task.cancel();
                return;
            }
            snowball.getWorld().spawnParticle(Particle.SNOWFLAKE, snowball.getLocation(), 4, 0.1, 0.1, 0.1, 0.02);
            snowball.getWorld().spawnParticle(Particle.SNOWFLAKE, snowball.getLocation(), 2, 0.1, 0.1, 0.1, 0.0);
        }, 1L, 2L);
    }
}
