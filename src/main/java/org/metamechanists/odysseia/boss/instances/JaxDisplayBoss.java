package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Boss backed by a hidden living entity and a display-entity sculpture.
 * The living core keeps combat, boss bars and reward tracking compatible with BossManager.
 */
public final class JaxDisplayBoss extends OdysseyBoss {

    @Override
    protected org.metamechanists.odysseia.boss.BossSpectacle.Arquetipo arquetipo() {
        return org.metamechanists.odysseia.boss.BossSpectacle.Arquetipo.TANQUE;
    }

    @Override
    protected String disfraz() {
        return "IRON_GOLEM";
    }

    @Override
    protected double escalaDisfraz() {
        return 1.5D;
    }

    private BlockDisplay visualRoot;

    public JaxDisplayBoss(LivingEntity entity) {
        super(entity, "jax", "§5§lJax §7§l- §dEl Centinela Fragmentado",
                configuredDouble("health", 1350.0D, 100.0D, 4000.0D), BarColor.PURPLE, BarStyle.SEGMENTED_12);

        var attackDamage = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(configuredDouble("core-damage", 16.0D, 1.0D, 40.0D));
        }
        var scale = entity.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(1.15D);
        }

        scheduleVisual(entity.getLocation());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity.isDead()) {
            return;
        }
        Player target = findNearestPlayer(configuredDouble("target-range", 28.0D, 8.0D, 64.0D));
        if (target == null) {
            idlePulse();
            return;
        }
        charge(target);
    }

    @Override
    public void tickAura() {
        super.tickAura();
        if (visualRoot == null || !visualRoot.isValid()) {
            return;
        }
        Location at = entity.getLocation().add(0.0D, 1.15D, 0.0D);
        entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, at, currentPhase + 1, 0.32D, 0.55D, 0.32D, 0.015D);
    }

    @Override
    public void cleanup() {
        removeVisual(visualRoot, new HashSet<>());
        visualRoot = null;
        super.cleanup();
    }

    private void idlePulse() {
        Location at = entity.getLocation().add(0.0D, 1.15D, 0.0D);
        entity.getWorld().spawnParticle(Particle.END_ROD, at, 4, 0.25D, 0.45D, 0.25D, 0.004D);
    }

    private void charge(Player target) {
        announceAttack("Embate fracturado");
        Location from = entity.getLocation();
        Vector direction = target.getLocation().toVector().subtract(from.toVector()).setY(0.0D);
        if (direction.lengthSquared() < 0.04D) {
            return;
        }
        double speed = configuredDouble("charge-speed", 1.05D, 0.25D, 2.0D);
        entity.setVelocity(direction.normalize().multiply(speed).setY(0.16D));
        from.getWorld().spawnParticle(Particle.ENCHANTED_HIT, from.clone().add(0.0D, 1.0D, 0.0D), 24, 0.55D, 0.8D, 0.55D, 0.08D);
        from.getWorld().playSound(from, Sound.ENTITY_RAVAGER_ROAR, 1.0F, 0.72F);

        Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> impact(target), 10L);
    }

    private void impact(Player target) {
        if (entity.isDead() || !target.isOnline() || target.isDead() || target.getWorld() != entity.getWorld()) {
            return;
        }
        double radius = configuredDouble("impact-radius", 4.0D, 1.0D, 8.0D);
        if (target.getLocation().distanceSquared(entity.getLocation()) > radius * radius) {
            return;
        }
        double damage = scaleArenaDamage(configuredDouble("impact-damage", 11.0D, 0.0D, 30.0D));
        target.damage(damage, entity);
        Vector push = target.getLocation().toVector().subtract(entity.getLocation().toVector()).setY(0.0D);
        if (push.lengthSquared() < 0.01D) {
            push = new Vector(0.1D, 0.0D, 0.1D);
        }
        target.setVelocity(push.normalize().multiply(0.78D).setY(0.35D));
        Location at = entity.getLocation().add(0.0D, 1.0D, 0.0D);
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 10, 0.45D, 0.35D, 0.45D, 0.02D);
        at.getWorld().playSound(at, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2F, 0.72F);
    }

    /** Dispatches the display model, then attaches it after Paper registers passengers. */
    private void scheduleVisual(Location location) {
        String command = readModelCommand();
        if (command == null) {
            logModelFallback();
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            logModelFallback();
            return;
        }
        String marker = "odysseia_jax_" + entity.getUniqueId().toString().replace("-", "");
        String taggedCommand = command.replaceFirst("\\{", "{Tags:[\"" + marker + "\"],");
        String positioned = String.format(Locale.ROOT, "execute positioned %.3f %.3f %.3f run %s",
                location.getX(), location.getY(), location.getZ(), taggedCommand);
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), positioned)) {
            logModelFallback();
            return;
        }
        Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> attachVisual(world, marker), 1L);
    }

    private void attachVisual(World world, String marker) {
        BlockDisplay root = world.getEntitiesByClass(BlockDisplay.class).stream()
                .filter(display -> display.getScoreboardTags().contains(marker))
                .findFirst()
                .orElse(null);
        if (root == null || entity.isDead()) {
            if (root != null) root.remove();
            logModelFallback();
            return;
        }
        visualRoot = root;
        root.setTeleportDuration(2);
        root.setInterpolationDuration(2);
        entity.addPassenger(root);
        entity.setInvisible(true);
        entity.setSilent(true);
        entity.setCustomNameVisible(false);
    }

    private void logModelFallback() {
        Odysseia.getInstance().getLogger().warning(
                "[Bosses] Jax no pudo cargar su modelo; el núcleo sigue visible para evitar un boss invisible.");
    }

    private String readModelCommand() {
        try (InputStream stream = Odysseia.getInstance().getResource("models/jax-model.command")) {
            if (stream == null) {
                return null;
            }
            String command = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
            return command.startsWith("summon block_display ") ? command : null;
        } catch (IOException exception) {
            Odysseia.getInstance().getLogger().warning("[Bosses] No se pudo leer el modelo de Jax: " + exception.getMessage());
            return null;
        }
    }

    private void removeVisual(Entity visual, Set<UUID> visited) {
        if (visual == null || !visited.add(visual.getUniqueId())) {
            return;
        }
        for (Entity passenger : visual.getPassengers()) {
            removeVisual(passenger, visited);
        }
        visual.remove();
    }

    private static double configuredDouble(String key, double fallback, double minimum, double maximum) {
        return Math.clamp(Odysseia.getInstance().getConfig().getDouble("bosses.jax." + key, fallback), minimum, maximum);
    }
}
