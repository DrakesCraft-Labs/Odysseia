package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public final class KratosRageSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null) return;

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.7f);

        // Apply Strength and Speed to Kratos
        boss.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 2)); // Strength III (200 ticks = 10s)
        boss.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));    // Speed II

        // Spawn a massive fire ring around Kratos
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
            double x = Math.cos(angle) * 3.0;
            double z = Math.sin(angle) * 3.0;
            Location particleLoc = loc.clone().add(x, 0.5, z);
            particleLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 5, 0.1, 0.1, 0.1, 0.05);
            particleLoc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 1, 0.1, 0.1, 0.1, 0.0);
        }

        if (target != null) {
            target.sendMessage("§c§l¡KRATOS ENTRA EN IRA DE ESPARTA!");
        }
    }
}
