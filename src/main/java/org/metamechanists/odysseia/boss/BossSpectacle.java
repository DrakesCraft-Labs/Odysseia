package org.metamechanists.odysseia.boss;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Capa compartida de espectaculo y comportamiento para todos los jefes.
 *
 * Vive aparte de {@link OdysseyBoss} para que mejorar la puesta en escena no obligue a tocar los
 * 22 jefes: cada uno hereda esto por la base y solo declara su arquetipo.
 *
 * Todo lo de aqui es del lado del cliente o efimero. No modifica el mundo, asi que no hay nada
 * que revertir si un combate termina mal.
 */
public final class BossSpectacle {

    /**
     * Como se comporta un jefe en combate. Cada jefe declara el suyo en una linea y hereda un
     * patron de movimiento coherente, en vez de perseguir al jugador mas cercano como cualquier
     * zombi.
     */
    public enum Arquetipo {
        /** Encima del jugador, sin darle aire. Cerbero, Kratos. */
        ACOSADOR(1.35D, 0.0D, false),
        /** Mantiene distancia y castiga al que se acerca. Zeus, Artemisa. */
        DISTANCIA(0.85D, 9.0D, true),
        /** Ataca y se retira antes de que le respondan. Loki, Circe. */
        EMBOSCADA(1.5D, 5.0D, true),
        /** Lento e imparable. Tifon, Coloso, Wither Storm. */
        TANQUE(0.65D, 0.0D, false);

        private final double velocidad;
        private final double distanciaPreferida;
        private final boolean seRetira;

        Arquetipo(double velocidad, double distanciaPreferida, boolean seRetira) {
            this.velocidad = velocidad;
            this.distanciaPreferida = distanciaPreferida;
            this.seRetira = seRetira;
        }

        public double velocidad() {
            return velocidad;
        }

        public double distanciaPreferida() {
            return distanciaPreferida;
        }

        public boolean seRetira() {
            return seRetira;
        }
    }

    private BossSpectacle() {
    }

    /**
     * Mantiene al jefe a su distancia preferida del objetivo.
     *
     * Se hace empujando la entidad, no tocando el pathfinding: funciona con cualquier tipo de mob
     * y no depende de NMS, asi que sobrevive a las actualizaciones de Paper.
     */
    public static void mantenerPosicion(LivingEntity jefe, Arquetipo arquetipo) {
        if (arquetipo.distanciaPreferida() <= 0 || !(jefe instanceof org.bukkit.entity.Mob mob)) return;
        var objetivo = mob.getTarget();
        if (objetivo == null || objetivo.isDead()) return;

        double distancia = jefe.getLocation().distance(objetivo.getLocation());
        double preferida = arquetipo.distanciaPreferida();
        // Margen para que no vibre quedandose justo en el limite.
        if (distancia >= preferida - 1.5D && distancia <= preferida + 2.5D) return;

        Vector hacia = objetivo.getLocation().toVector().subtract(jefe.getLocation().toVector());
        if (hacia.lengthSquared() < 0.01D) return;
        hacia = hacia.normalize().multiply(distancia < preferida && arquetipo.seRetira() ? -0.35D : 0.25D);
        jefe.setVelocity(jefe.getVelocity().add(new Vector(hacia.getX(), 0, hacia.getZ())));
    }

    /**
     * Marca en el suelo donde va a caer un ataque, con un aviso previo.
     *
     * Es lo que separa un jefe espectacular de uno injusto: el jugador ve venir el golpe y puede
     * reaccionar. Sin esto, un ataque fuerte solo se siente arbitrario.
     */
    public static void telegrafiar(Location centro, double radio, Color color) {
        var mundo = centro.getWorld();
        if (mundo == null) return;
        var polvo = new Particle.DustOptions(color, 2.0F);
        for (int grados = 0; grados < 360; grados += 12) {
            double rad = Math.toRadians(grados);
            Location punto = centro.clone().add(Math.cos(rad) * radio, 0.15D, Math.sin(rad) * radio);
            mundo.spawnParticle(Particle.DUST, punto, 2, 0.05D, 0.05D, 0.05D, 0, polvo);
        }
        mundo.playSound(centro, Sound.BLOCK_NOTE_BLOCK_BASS, 1.2F, 0.6F);
    }

    /**
     * Sacude la pantalla de los jugadores cercanos.
     *
     * Se consigue moviendo minimamente su vista, no con paquetes crudos: el efecto es el mismo y
     * no se rompe cuando cambia el protocolo.
     */
    public static void temblor(Location centro, double radio, float intensidad) {
        var mundo = centro.getWorld();
        if (mundo == null) return;
        double radioCuadrado = radio * radio;
        for (Player jugador : mundo.getPlayers()) {
            if (jugador.getLocation().distanceSquared(centro) > radioCuadrado) continue;
            var vista = jugador.getLocation();
            vista.setYaw(vista.getYaw() + (float) ((Math.random() - 0.5D) * intensidad));
            vista.setPitch(Math.clamp(vista.getPitch() + (float) ((Math.random() - 0.5D) * intensidad), -90F, 90F));
            jugador.teleport(vista);
        }
    }

    /** Onda expansiva visible que se abre desde el jefe. Para entradas y cambios de fase. */
    public static void onda(Location centro, double radioMaximo, Particle particula) {
        var mundo = centro.getWorld();
        if (mundo == null) return;
        for (double radio = 1.0D; radio <= radioMaximo; radio += 1.5D) {
            for (int grados = 0; grados < 360; grados += 10) {
                double rad = Math.toRadians(grados);
                mundo.spawnParticle(particula,
                        centro.clone().add(Math.cos(rad) * radio, 0.4D, Math.sin(rad) * radio),
                        1, 0, 0, 0, 0);
            }
        }
    }


    /**
     * Como pelea el jefe en una fase concreta.
     *
     * Un jefe que se mueve igual con 100% de vida que con 10% se vuelve predecible a la tercera
     * pelea. Al acorralarse cambia de actitud: el que guardaba distancia se lanza, el emboscador
     * deja de huir, y el tanque acelera. Es lo que hace que la fase 3 se sienta distinta y no
     * solo "lo mismo pero pega mas".
     */
    public static Arquetipo enFase(Arquetipo base, int fase) {
        if (fase < 3) return base;
        // En fase 3 todos se lanzan: el que guardaba distancia pierde la paciencia, el
        // emboscador ya no puede permitirse huir y el tanque despierta.
        return Arquetipo.ACOSADOR;
    }

    /**
     * Cierre del combate: implosion, onda y silencio antes del grito final.
     *
     * Un jefe que simplemente desaparece se siente anticlimatico despues de varios minutos de
     * pelea. Esto no da recompensas ni toca el mundo; solo cierra la escena.
     */
    public static void muerte(Location centro, int fasesAlcanzadas) {
        var mundo = centro.getWorld();
        if (mundo == null) return;

        // Implosion: las particulas caen hacia el centro en vez de salir.
        for (int grados = 0; grados < 360; grados += 8) {
            double rad = Math.toRadians(grados);
            for (double radio = 6.0D; radio >= 0.5D; radio -= 1.5D) {
                Location punto = centro.clone().add(Math.cos(rad) * radio, 0.6D, Math.sin(rad) * radio);
                Vector hacia = centro.toVector().subtract(punto.toVector()).normalize().multiply(0.25D);
                mundo.spawnParticle(Particle.SOUL_FIRE_FLAME, punto, 0,
                        hacia.getX(), hacia.getY(), hacia.getZ(), 1);
            }
        }
        mundo.playSound(centro, Sound.ENTITY_WITHER_DEATH, 1.4F, 0.6F);
        onda(centro, 8.0D + fasesAlcanzadas * 3, Particle.END_ROD);
        temblor(centro, 32.0D, 6.0F);
        mundo.spawnParticle(Particle.EXPLOSION, centro, 3, 1.5D, 1.0D, 1.5D, 0);
    }

    /**
     * Escala vida y dano segun cuanta gente hay peleando.
     *
     * Un jefe calibrado para uno se vuelve trivial con seis. El factor es suave a proposito: la
     * idea es que sumar gente siga siendo una ventaja, no que castigue al grupo.
     */
    public static double factorPorJugadores(List<Player> presentes) {
        int cuantos = Math.max(1, presentes.size());
        return Math.min(1.0D + (cuantos - 1) * 0.35D, 3.0D);
    }
}
