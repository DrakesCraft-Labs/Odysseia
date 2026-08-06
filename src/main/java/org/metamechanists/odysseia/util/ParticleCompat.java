package org.metamechanists.odysseia.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Centralizes particles whose required data changed in Paper 1.21.11. */
public final class ParticleCompat {
    private ParticleCompat() {
    }

    public static void spawnDragonBreath(World world, Location location, int count,
                                         double offsetX, double offsetY, double offsetZ,
                                         double extra, float velocity) {
        world.spawnParticle(Particle.DRAGON_BREATH, location, count,
                offsetX, offsetY, offsetZ, extra, velocity);
    }

    public static void spawnDragonBreath(Player player, Location location, int count,
                                         double offsetX, double offsetY, double offsetZ,
                                         double extra, float velocity) {
        player.spawnParticle(Particle.DRAGON_BREATH, location, count,
                offsetX, offsetY, offsetZ, extra, velocity);
    }

    /**
     * Destello sin datos obligatorios.
     *
     * Particle.FLASH exige un Color en Paper 1.21.11 y las llamadas que no lo pasan mueren con
     * IllegalArgumentException, matando la tarea entera: fue lo que reventó los altares de
     * FNAmplifications. END_ROD da el mismo golpe de luz y no lleva dato, asi que no puede
     * fallar aunque cambie la API.
     */
    public static void spawnFlash(World world, Location location, int count) {
        world.spawnParticle(Particle.END_ROD, location, count, 0.05, 0.05, 0.05, 0.02);
    }
}
