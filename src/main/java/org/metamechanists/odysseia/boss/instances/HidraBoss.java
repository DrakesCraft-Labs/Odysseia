package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Hidra de Lerna — RAVAGER 1200 HP.
 * Mecánica: al 66% y 33% HP se cura 300 y convoca 2 mini-hidras que la protegen.
 */
public class HidraBoss extends OdysseyBoss {

    private final Random random = new Random();
    private boolean phase66Triggered = false;
    private boolean phase33Triggered = false;
    private final List<UUID> miniHydras = new ArrayList<>();

    public HidraBoss(LivingEntity entity) {
        super(entity, "hidra", "§a§l🐍 Hidra §7§l- §aSerpiente de Lerna", 1200.0, BarColor.GREEN, BarStyle.SEGMENTED_12);

        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(1.6);
        var dmgAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(25.0);
        var kbAttr = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(1.0);
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        checkRegenPhases();
        applyMiniProtection();

        Player target = findNearestPlayer(25);
        if (target == null) return;

        switch (random.nextInt(5)) {
            case 0 -> venomBreath(target);
            case 1 -> tailSwipe();
            case 2 -> headRush(target);
            case 3 -> necroticBite(target);
            default -> hydraRegen();
        }
    }

    /** Cura 300 HP + convoca 2 mini-hidras al cruzar 66% y 33% de vida. */
    private void checkRegenPhases() {
        double pct = entity.getHealth() / maxHealth;
        if (!phase66Triggered && pct <= 0.66) {
            phase66Triggered = true;
            regenerationBurst();
        } else if (!phase33Triggered && pct <= 0.33) {
            phase33Triggered = true;
            regenerationBurst();
        }
    }

    private void regenerationBurst() {
        heal(300.0);
        Location loc = entity.getLocation();
        loc.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc.add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.6f);
        for (int i = 0; i < 2; i++) {
            spawnMiniHydra();
        }
    }

    private void spawnMiniHydra() {
        Location loc = entity.getLocation().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
        Ravager mini = (Ravager) loc.getWorld().spawnEntity(loc, EntityType.RAVAGER);
        mini.setCustomName("§2§l🐍 Cabeza de Hidra");
        mini.setCustomNameVisible(true);
        mini.setRemoveWhenFarAway(false);
        var hp = mini.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(200.0);
        mini.setHealth(200.0);
        var scale = mini.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(0.8);
        miniHydras.add(mini.getUniqueId());
    }

    /** Mientras viva alguna mini-hidra, la principal recibe Resistencia alta. */
    private void applyMiniProtection() {
        miniHydras.removeIf(uuid -> {
            var e = org.bukkit.Bukkit.getEntity(uuid);
            return e == null || e.isDead();
        });
        if (!miniHydras.isEmpty()) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 3, true, false, false));
            entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().add(0, 1.5, 0), 6, 0.4, 0.4, 0.4, 0.05);
        }
    }

    /** Escupe veneno en cono hacia el jugador más cercano: Poison V + Wither III. */
    private void venomBreath(Player target) {
        Location eye = entity.getLocation().add(0, 1.5, 0);
        Vector dir = target.getLocation().toVector().subtract(eye.toVector()).normalize();
        for (double d = 1; d <= 12; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.ITEM_SLIME, point, 6, 0.4, 0.4, 0.4, 0.02);
        }
        eye.getWorld().playSound(eye, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.3f);
        for (Player p : findPlayersInRange(13)) {
            Vector toP = p.getLocation().toVector().subtract(eye.toVector());
            if (toP.lengthSquared() > 0.01 && dir.angle(toP.normalize()) < Math.toRadians(60)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 600, 4, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 2, false, true));
            }
        }
    }

    /** Coletazo en radio 10: lanza a todos por el aire + daño. */
    private void tailSwipe() {
        Location center = entity.getLocation();
        center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 1, 0), 20, 4, 0.5, 4, 0);
        center.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ATTACK, 1.2f, 0.8f);
        for (Player p : findPlayersInRange(10)) {
            Vector push = p.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 0.01) push = new Vector(0, 1, 0);
            push.normalize().multiply(1.5).setY(0.9);
            p.setVelocity(push);
            p.damage(15.0, entity);
        }
    }

    /** Carga contra el objetivo: empuje del boss + daño 30 + Lentitud IV. */
    private void headRush(Player target) {
        Vector charge = target.getLocation().toVector().subtract(entity.getLocation().toVector());
        if (charge.lengthSquared() > 0.01) {
            charge.normalize().multiply(2.5).setY(0.2);
            entity.setVelocity(charge);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_STUNNED, 1.2f, 1.0f);
        if (target.getLocation().distanceSquared(entity.getLocation()) < 16) {
            target.damage(30.0, entity);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 3, false, true));
        }
    }

    /** Mordisco necrótico al más cercano: daño 40 + Wither IV + Weakness V + Slowness V. */
    private void necroticBite(Player target) {
        if (target.getLocation().distanceSquared(entity.getLocation()) > 49) return;
        target.damage(40.0, entity);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 300, 3, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 300, 4, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 4, false, true));
        Location loc = target.getLocation();
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.add(0, 1, 0), 30, 0.4, 0.5, 0.4, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_SPIDER_HURT, 1.0f, 0.5f);
    }

    /** Regeneración pasiva: +50 HP + Poison V en área. */
    private void hydraRegen() {
        heal(50.0);
        entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, 1, 0), 20, 1, 1, 1, 0.05);
        for (Player p : findPlayersInRange(15)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 4, false, true));
        }
    }
}
