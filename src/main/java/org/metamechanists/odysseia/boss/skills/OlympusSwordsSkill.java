package org.metamechanists.odysseia.boss.skills;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

public class OlympusSwordsSkill implements BossSkill {

    private final Random random = new Random();

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null) return;

        LivingEntity bossEntity = boss.getEntity();
        Location center = bossEntity.getLocation();

        // Warning sound/effects
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
        
        // Spawn particles in a circle
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double x = Math.cos(rad) * 10.0;
            double z = Math.sin(rad) * 10.0;
            Location pLoc = center.clone().add(x, 0.2, z);
            center.getWorld().spawnParticle(Particle.FLAME, pLoc, 3, 0.1, 0.1, 0.1, 0.05);
            center.getWorld().spawnParticle(Particle.SMOKE, pLoc, 2, 0.1, 0.1, 0.1, 0.02);
        }

        // Strike cosmetic lightning around the boss
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = random.nextDouble() * 10.0;
            double x = r * Math.cos(angle);
            double z = r * Math.sin(angle);
            Location strikeLoc = center.clone().add(x, 0, z);
            center.getWorld().strikeLightningEffect(strikeLoc);
        }

        // Damage and ignite players in range
        center.getWorld().getNearbyEntities(center, 10.0, 5.0, 10.0).forEach(e -> {
            if (e instanceof Player p && p.isOnline() && !p.isDead()) {
                p.damage(6.0, bossEntity); // 3 hearts
                p.setFireTicks(60); // Ignite for 3 seconds (60 ticks)
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l¡La lluvia de Espadas del Olimpo cae sobre ti!"));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 1.0f);
            }
        });
    }
}
