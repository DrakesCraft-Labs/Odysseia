package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.TsunamiSkill;
import org.metamechanists.odysseia.boss.skills.GuardianSummonSkill;
import org.metamechanists.odysseia.boss.skills.LevitationFallSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PoseidonBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();

    public PoseidonBoss(LivingEntity entity) {
        super(entity, "poseidon", "§9§lPoseidón §7§l- §9Dios del Océano", 1200.0, BarColor.BLUE, BarStyle.SEGMENTED_12);

        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(2.0);
        }

        if (entity.getEquipment() != null) {
            // Golden/Diamond armor representing sea king
            entity.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
            entity.getEquipment().setHelmetDropChance(0.0f);
            entity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            entity.getEquipment().setLeggingsDropChance(0.0f);
            entity.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            entity.getEquipment().setBootsDropChance(0.0f);

            // Trident weapon
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        skills.add(new TsunamiSkill());
        skills.add(new GuardianSummonSkill());
        skills.add(new LevitationFallSkill());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = getNearestPlayer(25);
        if (target != null) {
            BossSkill skill = skills.get(random.nextInt(skills.size()));
            skill.execute(this, target);
        }
    }

    private Player getNearestPlayer(double radius) {
        Player target = null;
        double nearestDist = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p && p.isOnline() && !p.isDead()) {
                double dist = p.getLocation().distance(entity.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    target = p;
                }
            }
        }
        return target;
    }
}
