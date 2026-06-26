package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

/**
 * Tifón — El Padre de los Monstruos. GIANT 2000 HP.
 * No modifica bloques del mundo: usa fuego sobre entidades, partículas y FallingBlocks
 * etiquetados con "boss_rock" (BossManager cancela su colocación al aterrizar).
 */
public class TifonBoss extends OdysseyBoss {

    private final Random random = new Random();
    private boolean furyActive = false;

    public TifonBoss(LivingEntity entity) {
        super(entity, "tifon", "§4§l🌋 Tifón §7§l- §4Padre de los Monstruos", 2000.0, BarColor.RED, BarStyle.SEGMENTED_20);

        var dmgAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(35.0);
        var kbAttr = entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(1.0);
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        checkHundredHeadsFury();

        Player target = findNearestPlayer(30);
        if (target == null) return;

        // <50% HP: a veces desata la erupción apocalíptica
        if (furyActive && random.nextInt(4) == 0) {
            volcanicEruption();
            return;
        }

        switch (random.nextInt(4)) {
            case 0 -> dragonBreath(target);
            case 1 -> earthShatter();
            case 2 -> monsterCall();
            default -> mountainThrow(target);
        }
    }

    /** Pasiva <50% HP: ataca más rápido + sus golpes envenenan con Wither. */
    private void checkHundredHeadsFury() {
        if (!furyActive && entity.getHealth() < maxHealth * 0.5) {
            furyActive = true;
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false, false));
            Bukkit.broadcastMessage("§4§l[TIFÓN] §c¡El Padre de los Monstruos entra en furia total!");
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.4f);
        }
        if (furyActive) {
            for (Player p : findPlayersInRange(6)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 2, false, true));
            }
        }
    }

    /** Aliento de dragón: fuego en línea recta 20 bloques. Fuego + daño, sin tocar bloques. */
    private void dragonBreath(Player target) {
        Location eye = entity.getLocation().add(0, 2, 0);
        Vector dir = target.getLocation().toVector().subtract(eye.toVector()).normalize();
        for (double d = 1; d <= 20; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.FLAME, point, 6, 0.3, 0.3, 0.3, 0.02);
            point.getWorld().spawnParticle(Particle.LAVA, point, 1, 0.2, 0.2, 0.2, 0);
            for (Player p : point.getWorld().getNearbyEntities(point, 1.5, 1.5, 1.5).stream()
                    .filter(e -> e instanceof Player).map(e -> (Player) e).toList()) {
                p.setFireTicks(600);
                p.damage(20.0, entity);
            }
        }
        eye.getWorld().playSound(eye, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.6f);
    }

    /** Golpea el suelo: lanza a todos por el aire + daño 30 en radio 12 (sin romper bloques). */
    private void earthShatter() {
        Location center = entity.getLocation();
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 8, 4, 0.5, 4, 0);
        center.getWorld().spawnParticle(Particle.BLOCK, center.clone().add(0, 0.5, 0), 100, 4, 0.2, 4, 0,
                Material.STONE.createBlockData());
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        for (Player p : findPlayersInRange(12)) {
            p.setVelocity(new Vector((random.nextDouble() - 0.5), 1.2, (random.nextDouble() - 0.5)));
            p.damage(30.0, entity);
        }
    }

    /** Invoca una ola de monstruos. */
    private void monsterCall() {
        Location center = entity.getLocation();
        Player nearest = findNearestPlayer(30);
        EntityType[] wave = {
                EntityType.RAVAGER, EntityType.RAVAGER, EntityType.RAVAGER,
                EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE,
                EntityType.SKELETON, EntityType.SKELETON, EntityType.SKELETON, EntityType.SKELETON, EntityType.SKELETON
        };
        for (EntityType type : wave) {
            Location loc = center.clone().add(random.nextInt(8) - 4, 0, random.nextInt(8) - 4);
            org.bukkit.entity.Entity mob = center.getWorld().spawnEntity(loc, type);
            mob.setCustomName("§4Engendro de Tifón");
            if (mob instanceof org.bukkit.entity.Mob m && nearest != null) m.setTarget(nearest);
            if (mob instanceof LivingEntity le) le.setRemoveWhenFarAway(true);
        }
        center.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.7f);
    }

    /** Lanza una roca de obsidiana que daña al impactar (FallingBlock no destructivo). */
    private void mountainThrow(Player target) {
        Location origin = entity.getLocation().add(0, 4, 0);
        FallingBlock rock = origin.getWorld().spawnFallingBlock(origin, Material.OBSIDIAN.createBlockData());
        rock.setDropItem(false);
        rock.setHurtEntities(false);
        tagRock(rock);
        Vector toTarget = target.getLocation().add(0, 1, 0).toVector().subtract(origin.toVector());
        if (toTarget.lengthSquared() > 0.01) {
            rock.setVelocity(toTarget.normalize().multiply(1.4).setY(0.4));
        }
        origin.getWorld().playSound(origin, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.7f);
        // Daño en área alrededor del destino tras 1s
        Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
            Location impact = rock.getLocation();
            impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 5, 1, 1, 1, 0);
            impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
            for (Player p : impact.getWorld().getNearbyEntities(impact, 4, 4, 4).stream()
                    .filter(e -> e instanceof Player).map(e -> (Player) e).toList()) {
                p.damage(50.0, entity);
            }
            if (!rock.isDead()) rock.remove();
        }, 25L);
    }

    /** Apocalíptica: lluvia de roca volcánica en radio 15 durante 3s + fuego y daño. */
    private void volcanicEruption() {
        Bukkit.broadcastMessage("§4§l[TIFÓN] §6¡ERUPCIÓN VOLCÁNICA! ¡Huid del área!");
        Location center = entity.getLocation();
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        for (int tick = 0; tick < 6; tick++) {
            Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
                for (int i = 0; i < 5; i++) {
                    double ox = (random.nextDouble() - 0.5) * 30;
                    double oz = (random.nextDouble() - 0.5) * 30;
                    Location drop = center.clone().add(ox, 25, oz);
                    FallingBlock magma = drop.getWorld().spawnFallingBlock(drop, Material.MAGMA_BLOCK.createBlockData());
                    magma.setDropItem(false);
                    magma.setHurtEntities(false);
                    tagRock(magma);
                    drop.getWorld().spawnParticle(Particle.LAVA, drop, 3, 0.2, 0.2, 0.2, 0);
                }
                for (Player p : findPlayersInRange(15)) {
                    p.setFireTicks(100);
                    p.damage(8.0, entity);
                }
            }, tick * 10L);
        }
    }

    private void tagRock(FallingBlock block) {
        NamespacedKey key = new NamespacedKey(Odysseia.getInstance(), "boss_rock");
        block.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }
}
