package org.metamechanists.odysseia.commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.metamechanists.odysseia.Odysseia;

import java.util.*;
import java.util.function.IntConsumer;

/** Eliminación administrativa absoluta dentro de un radio controlado con efectos divinos. */
public final class OwnerAuraCommand implements CommandExecutor, TabCompleter {

    private static final List<Integer> ALLOWED_RADII = List.of(2, 5, 10, 25, 50, 100);
    /** Entidades procesadas por tick durante la purga (ticket SAORI #9: evita una ráfaga
     *  de cientos de EntityDeathEvent sincrónicos en un único tick). */
    private static final int PURGE_BATCH_SIZE = 25;
    private final Odysseia plugin;

    public OwnerAuraCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando necesita la posición de un jugador.");
            return true;
        }
        Integer radius = args.length == 1 ? parseRadius(args[0]) : null;
        if (radius == null) {
            player.sendMessage(ChatColor.RED + "Uso: /auradueño <2|5|10|25|50|100>");
            return true;
        }

        Location center = player.getLocation().clone();

        // Efectos iniciales masivos de impacto divino
        playGodlikeAuraEffects(player, center, radius);

        // La recolección de objetivos es barata (no dispara eventos); la eliminación
        // real se reparte en lotes por tick para no agotar hilos nativos del servidor
        // con cientos de EntityDeathEvent sincrónicos de golpe (ticket SAORI #9).
        List<Entity> targets = collectPurgeTargets(center, radius, player.getUniqueId());
        schedulePurge(center.getWorld(), targets, removed -> {
            player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "✦ AURA DEL CREADOR: "
                    + ChatColor.RED + removed + ChatColor.DARK_RED + " entidad(es) desintegradas en radio " + radius + "m.");
            plugin.getLogger().warning("[AuraDueño] " + player.getName() + " eliminó " + removed
                    + " entidad(es) en radio " + radius + " desde " + formatLocation(center) + '.');
        });
        return true;
    }

    /** Despliega partículas cinematográficas y sonido de colapso espacial. */
    private void playGodlikeAuraEffects(Player player, Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        // Sonidos orquestales de juicio final
        world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 2.0F, 0.6F);
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0F, 0.7F);
        world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.5F);
        world.playSound(center, Sound.ENTITY_WITHER_DEATH, 1.2F, 0.5F);

        // Flash cegador en el centro
        org.metamechanists.odysseia.util.ParticleCompat.spawnFlash(world, center.clone().add(0, 1.5, 0), 4);
        world.spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1.2, 0), 2, 0, 0, 0, 0);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1.1, 0),
                120, 0.7, 1.0, 0.7, 0.35);

        // Pilar de luz cósmica vertical
        for (double y = center.getY(); y < center.getY() + 40.0; y += 1.5) {
            Location beamLoc = new Location(world, center.getX(), y, center.getZ());
            world.spawnParticle(Particle.END_ROD, beamLoc, 4, 0.2, 0.5, 0.2, 0.02);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, beamLoc, 3, 0.3, 0.5, 0.3, 0.01);
        }

        // Onda expansiva animada en 3D (Cúpula de choque divina)
        int effectiveRadius = Math.min(radius, 50);
        new BukkitRunnable() {
            int step = 1;

            @Override
            public void run() {
                if (step > effectiveRadius) {
                    cancel();
                    return;
                }

                double currentR = step;
                int points = Math.min(120, (int) (currentR * 8));

                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = center.getX() + currentR * Math.cos(angle);
                    double z = center.getZ() + currentR * Math.sin(angle);

                    // Anillo en el suelo
                    Location ringLoc = new Location(world, x, center.getY() + 0.3, z);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, ringLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    world.spawnParticle(Particle.REVERSE_PORTAL, ringLoc, 1, 0.05, 0.05, 0.05, 0.05);
                    world.spawnParticle(Particle.END_ROD, ringLoc.clone().add(0, 0.18, 0),
                            1, 0.02, 0.02, 0.02, 0.005);

                    // Puntos en cúpula elevada
                    if (step % 2 == 0) {
                        double domeY = center.getY() + Math.sqrt(Math.max(0, (effectiveRadius * effectiveRadius) - (currentR * currentR))) * 0.5;
                        Location domeLoc = new Location(world, x, domeY, z);
                        org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(world, domeLoc, 1, 0.1, 0.1, 0.1, 0.01, 1.0F);
                    }
                }

                step += Math.max(1, effectiveRadius / 10);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Encuentra las entidades candidatas a purgar. No las toca todavía. */
    private List<Entity> collectPurgeTargets(Location center, int radius, UUID executorId) {
        World world = center.getWorld();
        if (world == null) return List.of();

        Set<UUID> processed = new HashSet<>();
        List<Entity> targets = new ArrayList<>();
        for (Entity nearby : world.getNearbyEntities(
                center, radius, radius, radius,
                entity -> isInsideSphere(center, entity.getLocation(), radius))) {
            Entity target = nearby instanceof ComplexEntityPart part ? part.getParent() : nearby;
            // Una limpieza administrativa jamás debe matar ni penalizar a un jugador.
            if (target instanceof Player || target.getUniqueId().equals(executorId)
                    || !processed.add(target.getUniqueId())) continue;
            targets.add(target);
        }
        return targets;
    }

    /**
     * Elimina las entidades recolectadas en lotes de {@link #PURGE_BATCH_SIZE} por tick.
     *
     * Matar cientos de entidades en un único tick dispara la misma cantidad de
     * EntityDeathEvent de forma sincrónica en ese tick; si algún otro plugin reacciona a ese
     * evento con trabajo pesado (hilos propios, I/O), una ráfaga tan grande puede ser lo que
     * agote los hilos nativos del proceso (ticket SAORI #9). Repartir la purga en varios ticks
     * no cambia el resultado — se sigue eliminando todo — solo evita la ráfaga de golpe.
     */
    private void schedulePurge(World world, List<Entity> targets, IntConsumer onFinished) {
        if (world == null || targets.isEmpty()) {
            onFinished.accept(0);
            return;
        }
        new BukkitRunnable() {
            int index = 0;
            int removed = 0;

            @Override
            public void run() {
                int end = Math.min(index + PURGE_BATCH_SIZE, targets.size());
                for (; index < end; index++) {
                    Entity target = targets.get(index);
                    if (!target.isValid()) continue;
                    try {
                        Location tLoc = target.getLocation();

                        // Partículas de desintegración en la posición de cada entidad purgada
                        world.spawnParticle(Particle.LARGE_SMOKE, tLoc.clone().add(0, 0.8, 0), 12, 0.3, 0.5, 0.3, 0.05);
                        world.spawnParticle(Particle.SOUL, tLoc.clone().add(0, 1.0, 0), 8, 0.4, 0.4, 0.4, 0.08);
                        world.spawnParticle(Particle.EXPLOSION, tLoc.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);

                        target.setInvulnerable(false);
                        if (target instanceof LivingEntity living) {
                            living.setHealth(0.0);
                        } else {
                            target.remove();
                        }
                        removed++;
                    } catch (RuntimeException exception) {
                        plugin.getLogger().severe("[AuraDueño] No se pudo eliminar " + target.getType()
                                + " (" + target.getUniqueId() + "): " + exception.getMessage());
                    }
                }
                if (index >= targets.size()) {
                    cancel();
                    onFinished.accept(removed);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    static Integer parseRadius(String raw) {
        try {
            int radius = Integer.parseInt(raw);
            return ALLOWED_RADII.contains(radius) ? radius : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static boolean isInsideSphere(Location center, Location target, int radius) {
        return center.getWorld() == target.getWorld() && center.distanceSquared(target) <= radius * radius;
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + ' ' + location.getBlockX() + ','
                + location.getBlockY() + ',' + location.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return ALLOWED_RADII.stream().map(String::valueOf)
                .filter(radius -> radius.startsWith(args[0])).toList();
    }
}
