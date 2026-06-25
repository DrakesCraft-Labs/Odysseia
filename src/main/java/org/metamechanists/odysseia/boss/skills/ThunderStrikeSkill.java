package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

public class ThunderStrikeSkill implements BossSkill {

    private final Random random = new Random();
    private final int count;

    public ThunderStrikeSkill() {
        this.count = 3;
    }

    public ThunderStrikeSkill(int count) {
        this.count = count;
    }

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        Location base = target.getLocation();
        for (int i = 0; i < count; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 8;
            double offsetZ = (random.nextDouble() - 0.5) * 8;
            Location strikeLoc = base.clone().add(offsetX, 0, offsetZ);
            base.getWorld().strikeLightning(strikeLoc);
        }
        // 5 hearts = 10 damage
        target.damage(10.0, boss.getEntity());
    }
}
