package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class PoisonCloudSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null) return;

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WITCH_DRINK, 1.0f, 1.0f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 0.8f);

        AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setRadius(4.0f);
        cloud.setRadiusOnUse(-0.1f);
        cloud.setDuration(160); // 8 seconds
        cloud.setWaitTime(10);
        cloud.setColor(Color.GREEN);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 1), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1), true);
        
        // Spawn magic green particles
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 1, 0), 40, 2.0, 0.5, 2.0, 0.05);
    }
}
