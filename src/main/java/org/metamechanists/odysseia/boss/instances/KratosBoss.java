package org.metamechanists.odysseia.boss.instances;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.KratosAxeThrowSkill;
import org.metamechanists.odysseia.boss.skills.KratosChainsSkill;
import org.metamechanists.odysseia.boss.skills.KratosRageSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class KratosBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();
    private static final String KRATOS_HEAD_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODY3YWNlZDhjNDRlMTU3ZTkzNTg0OTBiNTRkYWFiZDA3ZjQ2MjljNjU5YjdhNTE0NWRjNTYzYTQ5YTQ5ZTFjMyJ9fX0=";
    private boolean rageActivated = false;

    public KratosBoss(LivingEntity entity) {
        super(entity, "kratos", "§c§lKratos §7§l- §cEl Fantasma de Esparta", 2500.0, BarColor.RED, BarStyle.SEGMENTED_20);

        // Scale entity size to 2.5 times (attribute GENERIC_SCALE)
        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(2.5);
        }

        // Equip Kratos custom head
        ItemStack kratosHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) kratosHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cMarca de Kratos");
            PlayerProfile profile = (PlayerProfile) Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", KRATOS_HEAD_BASE64));
            meta.setPlayerProfile(profile);
            kratosHead.setItemMeta(meta);
        }

        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(kratosHead);
            entity.getEquipment().setHelmetDropChance(0.0f);

            // Netherite Chestplate & Leggings & Boots
            entity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            entity.getEquipment().setLeggingsDropChance(0.0f);
            entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            entity.getEquipment().setBootsDropChance(0.0f);

            // Blades of Chaos in main hand (Netherite Sword), Leviathan Axe in off hand (Diamond Axe)
            ItemStack blades = new ItemStack(Material.NETHERITE_SWORD);
            blades.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2);
            entity.getEquipment().setItemInMainHand(blades);
            entity.getEquipment().setItemInMainHandDropChance(0.0f);

            ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
            entity.getEquipment().setItemInOffHand(axe);
            entity.getEquipment().setItemInOffHandDropChance(0.0f);
        }

        // Add skills
        skills.add(new KratosChainsSkill());
        skills.add(new KratosAxeThrowSkill());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = getNearestPlayer(25);
        if (target != null) {
            if (entity.getHealth() < maxHealth * 0.3 && !rageActivated) {
                rageActivated = true;
                new KratosRageSkill().execute(this, target);
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
