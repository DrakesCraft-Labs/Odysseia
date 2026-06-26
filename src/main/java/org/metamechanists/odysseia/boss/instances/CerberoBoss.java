package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

/**
 * Cerbero — Guardián del Inframundo. RAVAGER 900 HP.
 * Las "tres cabezas" se modelan como fases de furia: al 66% y 33% gana Speed+Strength.
 */
public class CerberoBoss extends OdysseyBoss {

    private final Random random = new Random();
    private int headsFallen = 0;

    public CerberoBoss(LivingEntity entity) {
        super(entity, "cerbero", "§5§l🐕 Cerbero §7§l- §5Guardián del Inframundo", 900.0, BarColor.PURPLE, BarStyle.SEGMENTED_10);

        var scaleAttr = entity.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(1.7);
        var dmgAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(22.0);
        var kbAttr = entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(1.0);
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        checkFuryPhases();

        Player target = findNearestPlayer(28);
        if (target == null) return;

        switch (random.nextInt(5)) {
            case 0 -> hellhoundBite(target);
            case 1 -> infernalHowl();
            case 2 -> soulScent();
            case 3 -> threeHeadFury(target);
            default -> netherChain(target);
        }
    }

    /** Cada cabeza que "cae" (66%/33% HP) enfurece al guardián: Speed + Strength. */
    private void checkFuryPhases() {
        double pct = entity.getHealth() / maxHealth;
        if ((headsFallen == 0 && pct <= 0.66) || (headsFallen == 1 && pct <= 0.33)) {
            headsFallen++;
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, headsFallen, false, false, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2 + headsFallen, false, false, false));
            Location loc = entity.getLocation();
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 60, 1, 1, 1, 0.1);
            loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.6f);
            Bukkit.broadcastMessage("§5§l[CERBERO] §d¡Una cabeza ha caído! Las restantes entran en furia.");
        }
    }

    /** Mordisco infernal: daño 25 + Wither V + Poison IV + Weakness III. */
    private void hellhoundBite(Player target) {
        if (target.getLocation().distanceSquared(entity.getLocation()) > 36) return;
        target.damage(25.0, entity);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 4, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 3, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 2, false, true));
        Location loc = target.getLocation();
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_WOLF_GROWL, 1.2f, 0.5f);
    }

    /** Aullido: Darkness + Blindness radio 20 + invoca 3 lobos espectro. */
    private void infernalHowl() {
        Location center = entity.getLocation();
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
        for (Player p : findPlayersInRange(20)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, true));
        }
        for (int i = 0; i < 3; i++) {
            Location loc = center.clone().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            Wolf wolf = (Wolf) center.getWorld().spawnEntity(loc, EntityType.WOLF);
            wolf.setCustomName("§8☠ Espectro de Cerbero");
            wolf.setCustomNameVisible(true);
            wolf.setAngry(true);
            wolf.setRemoveWhenFarAway(true);
            var hp = wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hp != null) { hp.setBaseValue(40.0); wolf.setHealth(40.0); }
            Player nearest = findNearestPlayer(20);
            if (nearest != null) wolf.setTarget(nearest);
        }
    }

    /** Rastrea al que huye: teleport instantáneo al jugador más alejado. */
    private void soulScent() {
        Player far = findFarthestPlayer(40);
        if (far == null) return;
        Location dest = far.getLocation().clone().add(far.getLocation().getDirection().multiply(-2));
        entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        entity.teleport(dest);
        dest.getWorld().playSound(dest, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
        far.getWorld().spawnParticle(Particle.DUST, far.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(140, 0, 60), 1.5f));
    }

    /** Las 3 cabezas atacan: 3 golpes rápidos al objetivo (programados). */
    private void threeHeadFury(Player target) {
        Location loc = entity.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ATTACK, 1.2f, 1.2f);
        for (int i = 0; i < 3; i++) {
            Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
                if (entity.isDead() || target.isDead()) return;
                if (target.getLocation().distanceSquared(entity.getLocation()) < 36) {
                    target.damage(20.0, entity);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                }
            }, i * 10L);
        }
    }

    /** Encadena al jugador: Slowness X + Mining Fatigue V durante 8s. */
    private void netherChain(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 9, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 160, 4, false, true));
        Location loc = target.getLocation();
        for (double y = 0; y <= 2; y += 0.25) {
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, y, 0), 4, 0.2, 0.1, 0.2, 0.01);
        }
        loc.getWorld().playSound(loc, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.5f);
    }
}
