package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

/**
 * Coloso del End — Titán del Vacío.
 * Enderman gigante (Scale 2.8) con 1500 HP, ataques de teletransporte, vacío y control de espacio.
 */
public class ColosoEndBoss extends OdysseyBoss {

    private final Random random = new Random();

    public ColosoEndBoss(LivingEntity entity) {
        super(entity, "coloso_end", "§5§l🌌 Coloso del End §7§l- §dSeñor del Vacío", 1500.0, BarColor.PURPLE, BarStyle.SEGMENTED_20);

        var dmgAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(40.0);

        var kbAttr = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(1.0);

        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(2.8);

        if (entity instanceof Enderman enderman) {
            enderman.setCarriedBlock(Material.CRYING_OBSIDIAN.createBlockData());
        }
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = findNearestPlayer(32);
        if (target == null) return;

        int choice = random.nextInt(5);
        switch (choice) {
            case 0 -> voidRift(target);
            case 1 -> pearlStorm();
            case 2 -> voidBlindness();
            case 3 -> summonEndermites(target);
            default -> teleportAssault(target);
        }
    }

    /** Grieta del Vacío: Atraé a los jugadores cercanos y los aturde con Slowness y Darkness. */
    private void voidRift(Player target) {
        speak("El vacío reclama vuestra existencia.");
        Location center = entity.getLocation();
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 120, 3, 3, 3, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);

        for (Player p : findPlayersInRange(20)) {
            Vector pull = center.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.2);
            p.setVelocity(pull);
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 1, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, false, true));
            p.damage(25.0, entity);
        }
    }

    /** Lluvia de Perlas: Intercambia de posición a los jugadores aleatoriamente entre sí. */
    private void pearlStorm() {
        var players = findPlayersInRange(24);
        if (players.size() < 2) return;

        speak("El espacio no tiene significado ante mí.");
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);

        Player p1 = players.get(0);
        Player p2 = players.get(1);
        Location loc1 = p1.getLocation();
        Location loc2 = p2.getLocation();

        p1.teleport(loc2);
        p2.teleport(loc1);

        p1.getWorld().spawnParticle(Particle.PORTAL, loc1, 40, 0.5, 1, 0.5, 0.1);
        p2.getWorld().spawnParticle(Particle.PORTAL, loc2, 40, 0.5, 1, 0.5, 0.1);
        p1.damage(15.0, entity);
        p2.damage(15.0, entity);
    }

    /** Ceguera Absoluta del End. */
    private void voidBlindness() {
        speak("¡Contemplad la oscuridad del abismo!");
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.4f);
        for (Player p : findPlayersInRange(30)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 1, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true));
        }
    }

    /** Invocación de Parásitos del Vacío. */
    private void summonEndermites(Player target) {
        Location center = entity.getLocation();
        speak("¡Surgid, parásitos del plano estelar!");
        for (int i = 0; i < 4; i++) {
            Location loc = center.clone().add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);
            Endermite mite = (Endermite) center.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.ENDERMITE);
            mite.setCustomName("§dParásito del Vacío");
            mite.setTarget(target);
            var scale = mite.getAttribute(Attribute.SCALE);
            if (scale != null) scale.setBaseValue(1.5);
        }
        center.getWorld().playSound(center, Sound.ENTITY_ENDERMITE_HURT, 1.5f, 0.6f);
    }

    /** Asalto de Teletransporte: Se teletransporta detrás de la víctima y la golpea críticamente. */
    private void teleportAssault(Player target) {
        Location behind = target.getLocation().subtract(target.getLocation().getDirection().multiply(1.5));
        behind.setDirection(target.getLocation().toVector().subtract(behind.toVector()));
        entity.teleport(behind);

        entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, behind, 50, 0.5, 1.5, 0.5, 0.1);
        entity.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_SCREAM, 1.8f, 0.6f);
        target.damage(35.0, entity);
        target.setVelocity(target.getLocation().getDirection().multiply(-1.5).setY(0.6));
    }
}
