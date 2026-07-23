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
}
