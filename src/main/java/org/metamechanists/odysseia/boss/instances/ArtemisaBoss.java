package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

/**
 * Artemisa — Diosa de la Caza. SKELETON 600 HP, especialista a distancia.
 */
public class ArtemisaBoss extends OdysseyBoss {

    private final Random random = new Random();

    public ArtemisaBoss(LivingEntity entity) {
        super(entity, "artemisa", "§9§l🏹 Artemisa §7§l- §9Diosa de la Caza", 600.0, BarColor.BLUE, BarStyle.SEGMENTED_10);

        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(1.4);
        var spdAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (spdAttr != null) spdAttr.setBaseValue(0.32);

        if (entity.getEquipment() != null) {
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
            entity.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
            entity.getEquipment().setHelmetDropChance(0.0f);
        }
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;
        Player target = findNearestPlayer(35);
        if (target == null) return;

        switch (random.nextInt(5)) {
            case 0 -> moonArrowVolley();
            case 1 -> huntressMark();
            case 2 -> lunarStrike(target);
            case 3 -> dianaSprint();
            default -> wolfPack();
        }
    }

    /** 12 flechas en arco 360° que aplican Slowness V + Poison III al impactar. */
    private void moonArrowVolley() {
        Location loc = entity.getLocation().add(0, 1.2, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1.2f, 1.0f);
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            Vector dir = new Vector(Math.cos(angle), 0.05, Math.sin(angle));
            Arrow arrow = entity.getWorld().spawnArrow(loc, dir, 2.0f, 0.0f);
            arrow.setShooter(entity);
            arrow.setDamage(7.0);
            arrow.setCustomName("moon_arrow");
            arrow.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4), true);
            arrow.setColor(org.bukkit.Color.AQUA);
        }
    }

    /** Marca al jugador con más vida: lo resalta con partículas de luna (objetivo prioritario). */
    private void huntressMark() {
        Player marked = null;
        double maxHp = -1;
        for (Player p : findPlayersInRange(35)) {
            if (p.getHealth() > maxHp) { maxHp = p.getHealth(); marked = p; }
        }
        if (marked == null) return;
        marked.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, false));
        Location loc = marked.getLocation().add(0, 2.3, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 0.3, 0.3, 0.3, 0.02);
        loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.5f);
        // Lluvia inmediata de flechas sobre el marcado
        rainArrowsOn(marked.getLocation(), 8);
    }

    /** Invoca la luna: Darkness + Blindness a todos + 20 flechas caen del cielo. */
    private void lunarStrike(Player target) {
        for (Player p : findPlayersInRange(30)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.4f);
        rainArrowsOn(target.getLocation(), 20);
    }

    /** Sprint divino: Speed V + Resistencia IV durante 5s. */
    private void dianaSprint() {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 4, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 3, false, false));
        Location loc = entity.getLocation();
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_HORSE_GALLOP, 1.0f, 1.2f);
    }

    /** Invoca 5 lobos sagrados de Diana en modo agresivo. */
    private void wolfPack() {
        Location center = entity.getLocation();
        Player nearest = findNearestPlayer(35);
        for (int i = 0; i < 5; i++) {
            Location loc = center.clone().add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);
            Wolf wolf = (Wolf) center.getWorld().spawnEntity(loc, EntityType.WOLF);
            wolf.setCustomName("§b🐺 Lobo de Diana");
            wolf.setCustomNameVisible(true);
            wolf.setAngry(true);
            wolf.setRemoveWhenFarAway(true);
            var hp = wolf.getAttribute(Attribute.MAX_HEALTH);
            if (hp != null) { hp.setBaseValue(30.0); wolf.setHealth(30.0); }
            if (nearest != null) wolf.setTarget(nearest);
        }
        center.getWorld().playSound(center, Sound.ENTITY_WOLF_GROWL, 1.2f, 1.0f);
    }

    /** Hace caer N flechas desde el cielo sobre posiciones aleatorias alrededor del punto. */
    private void rainArrowsOn(Location base, int count) {
        for (int i = 0; i < count; i++) {
            double ox = (random.nextDouble() - 0.5) * 12;
            double oz = (random.nextDouble() - 0.5) * 12;
            Location spawn = base.clone().add(ox, 30, oz);
            Arrow arrow = entity.getWorld().spawnArrow(spawn, new Vector(0, -1, 0), 2.5f, 1.0f);
            arrow.setShooter(entity);
            arrow.setDamage(6.0);
            arrow.setCustomName("moon_arrow");
            arrow.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 80, 2), true);
        }
    }
}
