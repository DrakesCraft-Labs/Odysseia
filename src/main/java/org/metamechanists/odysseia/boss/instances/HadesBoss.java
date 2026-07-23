package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.DarknessZoneSkill;
import org.metamechanists.odysseia.boss.skills.LifeDrainSkill;
import org.metamechanists.odysseia.boss.skills.UnderworldPortalSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HadesBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();

    public HadesBoss(LivingEntity entity) {
        super(entity, "hades", "§5§lHades §7§l- §5Dios del Inframundo", 1500.0, BarColor.PURPLE, BarStyle.SEGMENTED_12);

        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(2.2);
        }

        if (entity.getEquipment() != null) {
            // Full netherite gear
            entity.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            entity.getEquipment().setHelmetDropChance(0.0f);
            entity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            entity.getEquipment().setLeggingsDropChance(0.0f);
            entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            entity.getEquipment().setBootsDropChance(0.0f);

            // Scythe represented as netherite hoe
            ItemStack scythe = new ItemStack(Material.NETHERITE_HOE);
            scythe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 5);
            entity.getEquipment().setItemInMainHand(scythe);
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        skills.add(new DarknessZoneSkill());
        skills.add(new LifeDrainSkill());
        skills.add(new UnderworldPortalSkill());
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
