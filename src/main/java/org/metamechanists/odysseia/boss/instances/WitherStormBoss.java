package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.List;
import java.util.Random;

/**
 * Wither Storm — Devorador de Mundos (Inspirado en Minecraft Story Mode).
 * Wither colosal (Scale 3.0, 3000 HP) con Rayo Tractor, Screamers, Láser del Bloque de Comandos y Escombros.
 */
public class WitherStormBoss extends OdysseyBoss {

    private final Random random = new Random();

    public WitherStormBoss(LivingEntity entity) {
        super(entity, "wither_storm", "§5§l👁️ Wither Storm §7§l- §4Devorador de Mundos", 3000.0, BarColor.PURPLE, BarStyle.SEGMENTED_20);

        var dmgAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(50.0);

        var kbAttr = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.setBaseValue(1.0);

        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(3.0);

        if (entity instanceof Wither wither) {
            wither.setCustomNameVisible(true);
        }
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = findNearestPlayer(40);
        if (target == null) return;

        int choice = random.nextInt(5);
        switch (choice) {
            case 0 -> tractorBeam(target);
            case 1 -> triggerScreamer(target);
            case 2 -> commandBlockLaser(target);
            case 3 -> witherStormDebris(target);
            default -> summonWitherMinions(target);
        }
    }

    /** Rayo Tractor: Succiona a los jugadores hacia arriba con partículas del Vacío y levitación. */
    private void tractorBeam(Player target) {
        speak("NADA ESCAPA A MI RAYO TRACTOR.");
        Location origin = entity.getLocation().add(0, 3, 0);
        origin.getWorld().playSound(origin, Sound.ENTITY_WARDEN_EMERGE, 2.0f, 0.5f);

        List<Player> inRange = findPlayersInRange(25);
        for (Player p : inRange) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 80, 2, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 1, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 2, false, true));

            Vector pull = origin.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.8).setY(0.6);
            p.setVelocity(pull);
            p.damage(20.0, entity);
        }

        // Partículas helicoidales del Rayo Tractor
        for (int i = 0; i < 40; i++) {
            double angle = i * 0.3;
            double radius = 2.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = origin.clone().add(x, -i * 0.4, z);
            org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(
                    particleLoc.getWorld(), particleLoc, 3, 0.1, 0.1, 0.1, 0.02, 1.0f);
            particleLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    /** Screamer / Jumpscare Terrorífico: Efecto visual súbito + distorsión sonora de alta intensidad. */
    public static void triggerScreamer(Player player) {
        if (player == null || !player.isOnline()) return;

        player.sendTitle("§4§lÉL TE OBSERVA", "§cEl Wither Storm consume tu mente...", 5, 30, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.6f);
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 2.0f, 0.4f);

        player.spawnParticle(Particle.ELDER_GUARDIAN, player.getLocation().add(0, 1, 0), 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 1, false, false));
    }

    /** Láser del Bloque de Comandos: Haz concentrado de energía destructiva. */
    private void commandBlockLaser(Player target) {
        speak("¡EL NÚCLEO DE COMANDO EXTERMINARÁ ESTE MUNDO!");
        Location eye = entity.getLocation().add(0, 3, 0);
        Vector dir = target.getLocation().add(0, 1, 0).toVector().subtract(eye.toVector()).normalize();

        eye.getWorld().playSound(eye, Sound.ENTITY_ENDER_DRAGON_SHOOT, 2.0f, 0.4f);
        for (double d = 1; d <= 30; d += 0.8) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.EXPLOSION, point, 2, 0.2, 0.2, 0.2, 0);
            point.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 8, 0.3, 0.3, 0.3, 0.05);
            point.getWorld().spawnParticle(Particle.END_ROD, point, 4, 0.1, 0.1, 0.1, 0.02);

            for (Player p : point.getWorld().getNearbyEntities(point, 2.0, 2.0, 2.0).stream()
                    .filter(e -> e instanceof Player).map(e -> (Player) e).toList()) {
                p.damage(35.0, entity);
                p.setFireTicks(120);
                p.getWorld().strikeLightningEffect(p.getLocation());
            }
        }
    }

    /** Escombros del Wither Storm: Lanza rocas de Crying Obsidian gigantes. */
    private void witherStormDebris(Player target) {
        speak("LA MATERIA DE ESTE MUNDO PERTENECE A LA TORMENTA.");
        Location origin = entity.getLocation().add(0, 5, 0);

        for (int i = 0; i < 3; i++) {
            FallingBlock rock = origin.getWorld().spawnFallingBlock(origin.clone().add(random.nextInt(4) - 2, 0, random.nextInt(4) - 2),
                    Material.CRYING_OBSIDIAN.createBlockData());
            rock.setDropItem(false);
            rock.setHurtEntities(false);
            tagRock(rock);

            Vector toTarget = target.getLocation().add(0, 1, 0).toVector().subtract(origin.toVector());
            if (toTarget.lengthSquared() > 0.01) {
                rock.setVelocity(toTarget.normalize().multiply(1.5).add(new Vector((random.nextDouble() - 0.5) * 0.4, 0.3, (random.nextDouble() - 0.5) * 0.4)));
            }

            Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
                Location impact = rock.getLocation();
                impact.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impact, 2, 1, 1, 1, 0);
                impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                for (Player p : impact.getWorld().getNearbyEntities(impact, 4.5, 4.5, 4.5).stream()
                        .filter(e -> e instanceof Player).map(e -> (Player) e).toList()) {
                    p.damage(45.0, entity);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2, false, true));
                }
                if (!rock.isDead()) rock.remove();
            }, 30L);
        }
    }

    /** Invocación de Esbirros del Vacío. */
    private void summonWitherMinions(Player target) {
        speak("¡SERES DE LA TORMENTA, CONSUMID SU CARNE!");
        Location center = entity.getLocation();
        for (int i = 0; i < 3; i++) {
            Location loc = center.clone().add(random.nextInt(8) - 4, 0, random.nextInt(8) - 4);
            WitherSkeleton skeleton = (WitherSkeleton) center.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.WITHER_SKELETON);
            skeleton.setCustomName("§5§lEngendro de la Tormenta");
            skeleton.setTarget(target);
            skeleton.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            skeleton.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
        }
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.7f);
    }

    private void tagRock(FallingBlock block) {
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(Odysseia.getInstance(), "boss_rock");
        block.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
    }
}
