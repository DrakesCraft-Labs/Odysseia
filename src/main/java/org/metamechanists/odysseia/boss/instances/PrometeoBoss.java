package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

/**
 * Prometeo — Titán del Fuego Robado. BLAZE 700 HP.
 * Mecánica Fénix: evita una muerte letal, renace una vez y vuelve en fase final.
 */
public class PrometeoBoss extends OdysseyBoss {

    private final Random random = new Random();
    private boolean phoenixUsed = false;
    private boolean invulnerable = false;
    private boolean hasCurse = false;

    public PrometeoBoss(LivingEntity entity) {
        super(entity, "prometeo", "§e§l🔥 Prometeo §7§l- §eTitán del Fuego Robado", 700.0, BarColor.YELLOW, BarStyle.SEGMENTED_10);

        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(1.8);
        var dmgAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(20.0);
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = findNearestPlayer(28);
        if (target == null) return;

        switch (random.nextInt(4)) {
            case 0 -> stolenFire();
            case 1 -> titanFlame();
            case 2 -> eternalPunishment();
            default -> celestialFireball(target);
        }
    }

    /** Consume su única vida extra cuando un golpe sería letal. */
    public boolean beginPhoenixRebirth() {
        if (phoenixUsed || entity == null || entity.isDead()) {
            return false;
        }

        phoenixUsed = true;
        invulnerable = true;
        entity.setHealth(maxHealth * 0.65D);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60 * 10, 3, false, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 10, 2, false, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60 * 10, 1, false, false, false));

        Location loc = entity.getLocation();
        launchFirework(loc, Color.ORANGE);
        launchFirework(loc, Color.RED);
        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 180, 1.8, 2.5, 1.8, 0.25);
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 80, 1.2, 1.6, 1.2, 0.08);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
        speak("¡Las cenizas no son mi final! Ahora conoceréis el fuego eterno.");
        Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> invulnerable = false, 100L);
        return true;
    }

    @Override
    protected void onPhaseChange(int phase) {
        super.onPhaseChange(phase);
        if (phase == 2) {
            speak("El fuego robado arderá en vuestra sangre.");
        } else if (phase == 3) {
            speak("Mi castigo sólo acaba de comenzar.");
        }
    }

    /** Lanza 8 fireballs en todas direcciones + Fuego a todos en radio 10. */
    private void stolenFire() {
        Location loc = entity.getLocation().add(0, 1, 0);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            Vector dir = new Vector(Math.cos(angle), 0.1, Math.sin(angle));
            Fireball fb = entity.getWorld().spawn(loc, org.bukkit.entity.SmallFireball.class);
            fb.setShooter(entity);
            fb.setDirection(dir);
            fb.setYield(0f);
            fb.setIsIncendiary(false);
        }
        loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.8f);
        for (Player p : findPlayersInRange(10)) {
            p.setFireTicks(400);
        }
    }

    /** Se rodea de fuego: Fuego a quien esté a 3 bloques (sin modificar bloques). */
    private void titanFlame() {
        Location loc = entity.getLocation();
        loc.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 0.5, 0), 60, 1, 0.5, 1, 0.05);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 10, 1, 0.3, 1, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1.5f, 0.7f);
        for (Player p : findPlayersInRange(3)) {
            p.setFireTicks(200);
            p.damage(6.0, entity);
        }
    }

    /** Maldición: el jugador con más HP pierde 2♥ por segundo durante 30s. */
    private void eternalPunishment() {
        Player victim = null;
        double maxHp = -1;
        for (Player p : findPlayersInRange(28)) {
            if (p.getHealth() > maxHp) { maxHp = p.getHealth(); victim = p; }
        }
        if (victim == null || hasCurse) return;
        hasCurse = true;
        final Player target = victim;
        target.sendMessage("§4§l☠ §c¡Prometeo te ha maldecido con el Castigo Eterno!");
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
        Bukkit.getScheduler().runTaskTimer(Odysseia.getInstance(), task -> {
            if (entity.isDead() || target.isDead() || !target.isOnline()
                    || target.getLocation().distanceSquared(entity.getLocation()) > 2500) {
                hasCurse = false;
                task.cancel();
                return;
            }
            target.damage(4.0, entity); // 2 corazones
            target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.02);
        }, 20L, 20L);
    }

    /** Lanza un Fireball de Ghast teledirigido al jugador. */
    private void celestialFireball(Player target) {
        Location loc = entity.getLocation().add(0, 1.5, 0);
        Fireball fb = entity.getWorld().spawn(loc, org.bukkit.entity.LargeFireball.class);
        fb.setShooter(entity);
        fb.setYield(2f);
        fb.setIsIncendiary(true);
        Vector dir = target.getLocation().add(0, 1, 0).toVector().subtract(loc.toVector());
        if (dir.lengthSquared() > 0.01) {
            fb.setDirection(dir.normalize());
        }
        loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.9f);
    }

    private void launchFirework(Location loc, Color color) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(color).with(FireworkEffect.Type.BURST).flicker(true).trail(true).build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
        fw.detonate();
    }
}
