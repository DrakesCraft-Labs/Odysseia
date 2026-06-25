package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.SpearChargeSkill;
import org.metamechanists.odysseia.boss.skills.DamageAuraSkill;
import org.metamechanists.odysseia.boss.skills.SpartanSummonSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AresBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();
    private boolean rageActivated = false;

    public AresBoss(LivingEntity entity) {
        super(entity, "ares", "§c§lAres §7§l- §cDios de la Guerra", 1200.0, BarColor.RED, BarStyle.SEGMENTED_12);

        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.3);
        }

        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            entity.getEquipment().setHelmetDropChance(0.0f);
            entity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            entity.getEquipment().setLeggingsDropChance(0.0f);
            entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            entity.getEquipment().setBootsDropChance(0.0f);

            // Spear weapon represented as trident
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        skills.add(new SpearChargeSkill());
        skills.add(new DamageAuraSkill());
        skills.add(new SpartanSummonSkill());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = getNearestPlayer(25);
        if (target != null) {
            // Berserker rage at <30% HP
            if (entity.getHealth() < maxHealth * 0.3 && !rageActivated) {
                rageActivated = true;
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1)); // 15 seconds Strength II
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1)); // 15 seconds Speed II
                target.sendMessage("§c§l¡Ares desata su Furia Berserker! Su fuerza y velocidad aumentan radicalmente.");
            }

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
