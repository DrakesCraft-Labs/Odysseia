package org.metamechanists.odysseia.deaths;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lleva la cuenta de cuantas veces ha muerto alguien en poco rato.
 *
 * Una muerte suelta da para un chiste; tres seguidas dan para uno mejor, porque la gracia ya no
 * esta en como murio sino en que lleva toda la tarde muriendo. Sirve para añadir una coletilla
 * cuando la racha lo merece.
 *
 * Solo guarda las marcas de tiempo dentro de la ventana: las viejas se tiran al consultar, asi que
 * no crece sin control aunque el servidor lleve meses arriba.
 */
public final class DeathStreakTracker {

    private final long ventanaMillis;
    private final Map<UUID, Deque<Long>> muertes = new HashMap<>();

    public DeathStreakTracker(long ventanaMillis) {
        this.ventanaMillis = Math.max(1000L, ventanaMillis);
    }

    /**
     * Apunta una muerte.
     *
     * @return cuantas lleva dentro de la ventana, contando esta. Uno significa que no hay racha.
     */
    public int registrar(UUID jugador, long ahora) {
        Deque<Long> propias = muertes.computeIfAbsent(jugador, k -> new ArrayDeque<>());
        propias.addLast(ahora);
        while (!propias.isEmpty() && ahora - propias.peekFirst() > ventanaMillis) {
            propias.removeFirst();
        }
        return propias.size();
    }

    /** Borra el historial de alguien; se llama al desconectarse para no acumular jugadores. */
    public void olvidar(UUID jugador) {
        muertes.remove(jugador);
    }

    /** Cuantos jugadores hay en memoria. Visible para pruebas de la limpieza. */
    public int seguidos() {
        return muertes.size();
    }
}
