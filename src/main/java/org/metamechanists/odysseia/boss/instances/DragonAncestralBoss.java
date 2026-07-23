package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

/**
 * Dragón Ancestral — Señor Mítico de los Cielos.
 * EnderDragon colosal con rugido supersónico, ráfaga de bolas de fuego, aliento del Vacío y vendaval de alas.
 */
public class DragonAncestralBoss extends OdysseyBoss {

    private final Random random = new Random();

    public DragonAncestralBoss(LivingEntity entity) {
        super(entity, "dragon_ancestral", "§d§l🐉 Dragón Ancestral §7§l- §6Señor de los Cielos", 3500.0, BarColor.PINK, BarStyle.SEGMENTED_20);

        var dmgAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(45.0);

        var kbAttr = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(1.0);

        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(1.5);
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = findNearestPlayer(45);
        if (target == null) return;

        int choice = random.nextInt(4);
        switch (choice) {
            case 0 -> dragonRoar();
            case 1 -> fireballBarrage(target);
            case 2 -> wingBuffet();
            default -> voidBreath(target);
        }
    }

    /** Rugido del Dragón: Onda expansiva que ralentiza y empuja a todos en radio 30. */
    private void dragonRoar() {
        speak("¡MI RUGIDO ESTREMECERÁ LOS CIELOS Y LA TIERRA!");
        Location loc = entity.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.4f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 2, 2, 2, 0);

        for (Player p : findPlayersInRange(30)) {
            p.setVelocity(p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.8).setY(0.7));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, true));
            p.damage(25.0, entity);
        }
    }

    /** Ráfaga de Bolas de Fuego del Dragón. */
    private void fireballBarrage(Player target) {
        speak("¡SENTID LAS LLAMAS ANCESTRALES DEL ABISMO!");
        Location eye = entity.getLocation().add(0, 3, 0);
        eye.getWorld().playSound(eye, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.8f, 0.6f);

        for (int i = 0; i < 3; i++) {
            Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
                if (entity.isDead() || target == null || !target.isOnline()) return;
                Vector dir = target.getLocation().add(0, 1, 0).toVector().subtract(eye.toVector()).normalize();
                DragonFireball fb = (DragonFireball) eye.getWorld().spawnEntity(eye, org.bukkit.entity.EntityType.DRAGON_FIREBALL);
                fb.setDirection(dir.multiply(1.2));
            }, i * 10L);
        }
    }

    /** Vendaval de Alas: Lanza por los aires a los combatientes cercanos. */
    private void wingBuffet() {
        speak("¡MIS ALAS DESATARÁN LA TEMPESTAD!");
        Location loc = entity.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.5f);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 120, 5, 2, 5, 0.2);

        for (Player p : findPlayersInRange(25)) {
            p.setVelocity(new Vector(0, 1.6, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1, false, true));
            p.damage(20.0, entity);
        }
    }

    /** Aliento del Vacío: Haz de partículas mágicas y pudrición Wither. */
    private void voidBreath(Player target) {
        speak("EL ALIENTO DEL VACÍO CONSUMIRÁ VUESTRA ALMA.");
        Location eye = entity.getLocation().add(0, 2, 0);
        Vector dir = target.getLocation().toVector().subtract(eye.toVector()).normalize();

        for (double d = 1; d <= 25; d += 0.8) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(
                    point.getWorld(), point, 10, 0.4, 0.4, 0.4, 0.05, 1.0f);
            point.getWorld().spawnParticle(Particle.WITCH, point, 5, 0.3, 0.3, 0.3, 0.02);

            for (Player p : point.getWorld().getNearbyEntities(point, 2.0, 2.0, 2.0).stream()
                    .filter(e -> e instanceof Player).map(e -> (Player) e).toList()) {
                p.damage(30.0, entity);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 2, false, true));
            }
        }
    }
}
