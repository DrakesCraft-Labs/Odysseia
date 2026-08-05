package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

/** Egyptian endgame bosses with bounded, distinct signatures. */
public final class EgyptianBoss extends OdysseyBoss {

    @Override
    protected org.metamechanists.odysseia.boss.BossSpectacle.Arquetipo arquetipo() {
        return org.metamechanists.odysseia.boss.BossSpectacle.Arquetipo.DISTANCIA;
    }

    public enum Kind { RA, ISIS, ANUBIS, SET }

    private final Kind kind;
    private final Random random = new Random();

    public EgyptianBoss(LivingEntity entity, Kind kind) {
        super(entity, id(kind), name(kind), health(kind), barColor(kind), BarStyle.SEGMENTED_10);
        this.kind = kind;
        configureEntity(entity, kind);
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;
        switch (kind) {
            case RA -> solarJudgement();
            case ISIS -> veilOfLife();
            case ANUBIS -> weighTheHeart();
            case SET -> desertTempest();
        }
    }

    private void solarJudgement() {
        Player target = findNearestPlayer(32);
        if (target == null) return;
        target.damage(14.0D, entity);
        target.setFireTicks(Math.max(target.getFireTicks(), 60));
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 36, 0.6, 1, 0.6, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.1F, 1.1F);
    }

    private void veilOfLife() {
        heal(18.0D);
        for (Player player : findPlayersInRange(14)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, true, true, true));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 8, 0.35, 0.4, 0.35, 0.02);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.2F, 0.8F);
    }

    private void weighTheHeart() {
        Player target = findNearestPlayer(28);
        if (target == null) return;
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, true, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, true, true, true));
        target.damage(10.0D, entity);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 28, 0.5, 0.8, 0.5, 0.03);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.1F, 0.7F);
    }

    private void desertTempest() {
        Location center = entity.getLocation();
        for (Player player : findPlayersInRange(16)) {
            player.damage(12.0D, entity);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, true, true));
            Vector push = player.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 0.01D) push = new Vector(0, 0, 1);
            player.setVelocity(push.normalize().multiply(1.25D).setY(0.45D));
        }
        center.getWorld().spawnParticle(Particle.DUST, center.add(0, 1, 0), 90, 4, 1.5, 4, 0,
                new Particle.DustOptions(Color.fromRGB(205, 125, 45), 1.8F));
        center.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.3F, 0.65F + random.nextFloat() * 0.15F);
    }

    private static void configureEntity(LivingEntity entity, Kind kind) {
        var damage = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(24.0D);
        var scale = entity.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(1.45D);
        if (entity.getEquipment() == null) return;
        Material helmet = switch (kind) {
            case RA -> Material.GOLDEN_HELMET;
            case ISIS -> Material.QUARTZ;
            case ANUBIS -> Material.SKELETON_SKULL;
            case SET -> Material.RED_SANDSTONE;
        };
        entity.getEquipment().setHelmet(new ItemStack(helmet));
        entity.getEquipment().setHelmetDropChance(0.0F);
    }

    private static String id(Kind kind) { return kind.name().toLowerCase(); }

    private static String name(Kind kind) {
        return switch (kind) {
            case RA -> "§6§l☀ Ra §7§l- §eEl Ojo del Sol";
            case ISIS -> "§b§l✦ Isis §7§l- §fLa Madre del Velo";
            case ANUBIS -> "§5§l☥ Anubis §7§l- §dJuez del Duat";
            case SET -> "§c§l𓂀 Set §7§l- §6La Tempestad Roja";
        };
    }

    private static double health(Kind kind) {
        return switch (kind) {
            case RA -> 1800.0D;
            case ISIS -> 1650.0D;
            case ANUBIS -> 1900.0D;
            case SET -> 2100.0D;
        };
    }

    private static BarColor barColor(Kind kind) {
        return switch (kind) {
            case RA -> BarColor.YELLOW;
            case ISIS -> BarColor.BLUE;
            case ANUBIS -> BarColor.PURPLE;
            case SET -> BarColor.RED;
        };
    }
}
