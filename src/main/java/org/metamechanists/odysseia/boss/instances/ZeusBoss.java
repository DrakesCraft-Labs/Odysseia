package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.LightningStormSkill;
import org.metamechanists.odysseia.boss.skills.DivineShieldSkill;
import org.metamechanists.odysseia.boss.skills.ZeusTeleportSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZeusBoss extends OdysseyBoss {

    @Override
    protected org.metamechanists.odysseia.boss.BossSpectacle.Arquetipo arquetipo() {
        return org.metamechanists.odysseia.boss.BossSpectacle.Arquetipo.DISTANCIA;
    }

    @Override
    protected String disfraz() {
        return "ILLUSIONER";
    }

    @Override
    protected double escalaDisfraz() {
        return 1.4D;
    }

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();
    private long nextAttackAt;
    private long shieldAvailableAt;

    public ZeusBoss(LivingEntity entity) {
        super(entity, "zeus", "§e§lZeus §7§l- §eRey del Olimpo", 2000.0, BarColor.YELLOW, BarStyle.SEGMENTED_20);

        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(2.5);
        }

        if (entity.getEquipment() != null) {
            // Golden armor for Zeus
            entity.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
            entity.getEquipment().setHelmetDropChance(0.0f);
            entity.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
            entity.getEquipment().setLeggingsDropChance(0.0f);
            entity.getEquipment().setBoots(new ItemStack(Material.GOLDEN_BOOTS));
            entity.getEquipment().setBootsDropChance(0.0f);

            // Lightning Bolt weapon represented as trident
            ItemStack lightning = new ItemStack(Material.TRIDENT);
            lightning.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.CHANNELING, 1);
            entity.getEquipment().setItemInMainHand(lightning);
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        skills.add(new LightningStormSkill());
        skills.add(new DivineShieldSkill());
        skills.add(new ZeusTeleportSkill());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        long now = System.currentTimeMillis();
        if (now < nextAttackAt) return;

        Player target = getNearestPlayer(25);
        if (target == null) return;

        List<BossSkill> available = new ArrayList<>(skills);
        if (now < shieldAvailableAt || entity.hasPotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE)) {
            available.removeIf(skill -> skill instanceof DivineShieldSkill);
        }
        if (available.isEmpty()) return;

        BossSkill skill = available.get(random.nextInt(available.size()));
        skill.execute(this, target);
        nextAttackAt = now + 5_000L;
        if (skill instanceof DivineShieldSkill) {
            shieldAvailableAt = now + 45_000L;
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
