package org.metamechanists.odysseia.boss.instances;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.PoisonCloudSkill;
import org.metamechanists.odysseia.boss.skills.PolymorphSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CirceBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();

    public CirceBoss(LivingEntity entity) {
        super(entity, "circe", "§d§lCirce §7§l- §dLa Hechicera", 500.0, BarColor.PURPLE, BarStyle.SEGMENTED_10);
        skills.add(new PolymorphSkill());
        skills.add(new PoisonCloudSkill());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        // Find nearest player in range
        Player target = null;
        double nearestDist = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity e : entity.getNearbyEntities(25, 25, 25)) {
            if (e instanceof Player p && p.isOnline() && !p.isDead()) {
                double dist = p.getLocation().distance(entity.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    target = p;
                }
            }
        }

        if (target != null) {
            BossSkill skill = skills.get(random.nextInt(skills.size()));
            skill.execute(this, target);
        }
    }
}
