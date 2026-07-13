package org.metamechanists.odysseia.boss.instances;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.LokiBlindnessSkill;
import org.metamechanists.odysseia.boss.skills.LokiClonesSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class LokiBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();
    private static final String LOKI_HEAD_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmM2NzNjODE5ODVlMTQ5MTU4ZjZiMDYzZTZiZTA5MGFlZjU0MTUxZjBmM2FmZDdhODMyMjMwM2FiOTkyODBlOSJ9fX0=";

    public LokiBoss(LivingEntity entity) {
        super(entity, "loki", "§a§lLoki §7§l- §aDios del Engaño", 1200.0, BarColor.GREEN, BarStyle.SEGMENTED_20);

        // Scale entity size slightly larger (1.2)
        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.2);
        }

        // Equip Loki custom head
        ItemStack lokiHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) lokiHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aCorona de Loki");
            PlayerProfile profile = (PlayerProfile) Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", LOKI_HEAD_BASE64));
            meta.setPlayerProfile(profile);
            lokiHead.setItemMeta(meta);
        }

        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(lokiHead);
            entity.getEquipment().setHelmetDropChance(0.0f);

            // Green dyed leather gear
            ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
            LeatherArmorMeta chestMeta = (LeatherArmorMeta) chest.getItemMeta();
            if (chestMeta != null) {
                chestMeta.setColor(Color.fromRGB(34, 139, 34)); // Dark green
                chest.setItemMeta(chestMeta);
            }
            entity.getEquipment().setChestplate(chest);
            entity.getEquipment().setChestplateDropChance(0.0f);

            ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
            LeatherArmorMeta legsMeta = (LeatherArmorMeta) legs.getItemMeta();
            if (legsMeta != null) {
                legsMeta.setColor(Color.fromRGB(0, 100, 0)); // Very dark green
                legs.setItemMeta(legsMeta);
            }
            entity.getEquipment().setLeggings(legs);
            entity.getEquipment().setLeggingsDropChance(0.0f);

            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
            LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
            if (bootsMeta != null) {
                bootsMeta.setColor(Color.fromRGB(0, 0, 0)); // Black
                boots.setItemMeta(bootsMeta);
            }
            entity.getEquipment().setBoots(boots);
            entity.getEquipment().setBootsDropChance(0.0f);

            // Golden Hoe as Scepter
            ItemStack scepter = new ItemStack(Material.GOLDEN_HOE);
            entity.getEquipment().setItemInMainHand(scepter);
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        // Add skills
        skills.add(new LokiClonesSkill());
        skills.add(new LokiBlindnessSkill());
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
