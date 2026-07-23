package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.List;
import java.util.Random;

/**
 * Heimdall — Guardián del Bifröst.
 * STRAY 800 HP. Especialista en visión (nadie se esconde) y control de área arcoíris.
 */
public class HeimdallBoss extends OdysseyBoss {

    private final Random random = new Random();

    public HeimdallBoss(LivingEntity entity) {
        super(entity, "heimdall", "§f§l🌈 Heimdall §7§l- §fGuardián del Bifröst", 800.0, BarColor.WHITE, BarStyle.SEGMENTED_10);

        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(1.5);
        var dmgAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(18.0);

        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
            entity.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
            entity.getEquipment().setHelmetDropChance(0.0f);
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;
        Player target = findNearestPlayer(30);
        if (target == null) return;

        // Omnisciencia siempre activa: nadie se esconde mientras Heimdall vive
        omniscience();

        switch (random.nextInt(4)) {
            case 0 -> bifrostBeam(target);
            case 1 -> rainbowStrike(target);
            case 2 -> bifrostTeleport();
            default -> hornBlast();
        }
    }

    /** Haz del Bifröst: empuja al jugador 15 bloques atrás + Ceguera III + Náusea III. */
    private void bifrostBeam(Player target) {
        Vector back = target.getLocation().toVector().subtract(entity.getLocation().toVector());
        if (back.lengthSquared() < 0.01) back = new Vector(0, 0, 1);
        back.normalize().multiply(2.2).setY(0.5);
        target.setVelocity(back);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 2, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 2, false, true));
        Location loc = target.getLocation();
        loc.getWorld().spawnParticle(Particle.DUST, loc.add(0, 1, 0), 40, 0.5, 1, 0.5,
                new Particle.DustOptions(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)), 2f));
        loc.getWorld().playSound(loc, Sound.ENTITY_SHULKER_SHOOT, 1.2f, 1.0f);
    }

    /** Lluvia de 7 colores: 7 rayos + 7 fuegos artificiales alrededor del jugador. */
    private void rainbowStrike(Player target) {
        Location base = target.getLocation();
        for (int i = 0; i < 7; i++) {
            double ox = (random.nextDouble() - 0.5) * 12;
            double oz = (random.nextDouble() - 0.5) * 12;
            Location loc = base.clone().add(ox, 0, oz);
            base.getWorld().strikeLightning(loc);
            launchColoredFirework(loc, Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
        }
        base.getWorld().playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
    }

    /** Todos los jugadores en el mapa brillan: nadie puede esconderse. */
    private void omniscience() {
        for (Player p : findPlayersInRange(60)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, true, false, false));
            p.getWorld().spawnParticle(Particle.WAX_ON, p.getLocation().add(0, 2.2, 0), 5, 0.2, 0.2, 0.2, 0.01);
        }
    }

    /** Se teletransporta al jugador más alejado dejando rastro de arcoíris. */
    private void bifrostTeleport() {
        Player far = findFarthestPlayer(40);
        if (far == null) return;
        Location from = entity.getLocation();
        for (double t = 0; t <= 1; t += 0.1) {
            Location p = from.clone().add(far.getLocation().clone().subtract(from).toVector().multiply(t));
            p.getWorld().spawnParticle(Particle.DUST, p, 8, 0.2, 0.2, 0.2,
                    new Particle.DustOptions(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)), 1.5f));
        }
        Location dest = far.getLocation().clone().add(far.getLocation().getDirection().multiply(-2));
        entity.teleport(dest);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.8f);
    }

    /** Toca el Gjallarhorn: Knockback masivo + los lanza por el aire en radio 15. */
    private void hornBlast() {
        Location center = entity.getLocation();
        center.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.7f);
        center.getWorld().spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
        for (Player p : findPlayersInRange(15)) {
            Vector push = p.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 0.01) push = new Vector(0, 1, 0);
            push.normalize().multiply(2.0).setY(1.2);
            p.setVelocity(push);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 1, false, true));
        }
    }

    private void launchColoredFirework(Location loc, Color color) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(color).with(FireworkEffect.Type.BALL_LARGE).flicker(true).build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        fw.detonate();
    }
}
