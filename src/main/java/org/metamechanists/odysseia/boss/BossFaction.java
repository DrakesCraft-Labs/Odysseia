package org.metamechanists.odysseia.boss;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/**
 * Bando comun para jefes y sus invocaciones.
 *
 * Dos cosas rompian las peleas multiples:
 *
 *   - Con varios jefes invocados a la vez se atacaban entre ellos, porque para el juego son mobs
 *     hostiles de especies distintas.
 *   - Los lobos de Artemisa la atacaban a ella. No era un bug del jefe: Artemisa es un esqueleto,
 *     y la IA vanilla del lobo ataca esqueletos. Estaba funcionando exactamente como debe.
 *
 * El equipo del scoreboard corta el dano entre miembros, pero **no** impide que se elijan como
 * objetivo: por eso ademas hay que limpiar el target, que es lo que hace {@link #corregirObjetivo}.
 */
public final class BossFaction {

    private static final String NOMBRE = "drakes_bosses";

    private BossFaction() {
    }

    /** Equipo compartido, creado la primera vez que hace falta. */
    private static Team equipo() {
        var scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NOMBRE);
        if (team == null) {
            team = scoreboard.registerNewTeam(NOMBRE);
            team.setAllowFriendlyFire(false);
            team.setCanSeeFriendlyInvisibles(true);
            // Sin esto el nombre del equipo aparece sobre cada mob y ensucia la arena.
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        return team;
    }

    /** Suma la entidad al bando. Vale tanto para el jefe como para lo que invoque. */
    public static void alistar(Entity entidad) {
        if (entidad == null) return;
        try {
            equipo().addEntry(entidad.getUniqueId().toString());
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            // Scoreboard no disponible: el combate sigue, solo sin proteccion de bando.
        }
    }

    /** Saca a la entidad del bando cuando muere o se limpia, para que el equipo no crezca sin fin. */
    public static void licenciar(Entity entidad) {
        if (entidad == null) return;
        try {
            equipo().removeEntry(entidad.getUniqueId().toString());
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            // nada que hacer
        }
    }

    /** True si la entidad pertenece al bando de los jefes. */
    public static boolean esAliado(Entity entidad) {
        if (entidad == null) return false;
        try {
            return equipo().hasEntry(entidad.getUniqueId().toString());
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Si el mob esta apuntando a un aliado, le quita el objetivo y busca un jugador cercano.
     *
     * El equipo del scoreboard evita el dano pero no el targeting: sin esto, los lobos de Artemisa
     * seguirian persiguiendola sin poder herirla, que se ve igual de roto.
     */
    public static void corregirObjetivo(LivingEntity entidad, double radioBusqueda) {
        if (!(entidad instanceof Mob mob)) return;
        var objetivo = mob.getTarget();
        if (objetivo == null || !esAliado(objetivo)) return;

        mob.setTarget(null);
        Player masCercano = null;
        double mejor = radioBusqueda * radioBusqueda;
        for (Player jugador : entidad.getWorld().getPlayers()) {
            if (jugador.isDead() || jugador.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            double distancia = jugador.getLocation().distanceSquared(entidad.getLocation());
            if (distancia < mejor) {
                mejor = distancia;
                masCercano = jugador;
            }
        }
        if (masCercano != null) mob.setTarget(masCercano);
    }
}
