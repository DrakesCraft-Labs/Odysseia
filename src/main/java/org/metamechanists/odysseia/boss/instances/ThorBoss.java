package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.MjolnirThrowSkill;
import org.metamechanists.odysseia.boss.skills.ThunderStrikeSkill;
import org.metamechanists.odysseia.boss.skills.LightningStormSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ThorBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();
    private boolean stormTriggered = false;

    public ThorBoss(LivingEntity entity) {
        super(entity, "thor", "§6§lThor §7§l- §6Dios del Trueno", 1000.0, BarColor.YELLOW, BarStyle.SEGMENTED_10);

        // Scale entity size to make it large
        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.8);
        }

        // Equip armor and Mace
        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
            entity.getEquipment().setHelmetDropChance(0.0f);
            entity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            entity.getEquipment().setLeggingsDropChance(0.0f);
            entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            entity.getEquipment().setBootsDropChance(0.0f);

            // Mace weapon in main hand
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.MACE));
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        skills.add(new MjolnirThrowSkill());
        skills.add(new ThunderStrikeSkill());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = getNearestPlayer(25);
        if (target != null) {
            // Under 30% HP storm trigger once
            if (entity.getHealth() < maxHealth * 0.3 && !stormTriggered) {
                stormTriggered = true;
                new LightningStormSkill().execute(this, target);
            } else {
                BossSkill skill = skills.get(random.nextInt(skills.size()));
                skill.execute(this, target);
            }
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
