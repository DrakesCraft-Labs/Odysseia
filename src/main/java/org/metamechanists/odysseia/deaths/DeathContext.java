package org.metamechanists.odysseia.deaths;

import java.util.Locale;

/**
 * Los datos de una muerte, ya resueltos y sin nada de Bukkit dentro.
 *
 * @param jugador  nombre del que murio
 * @param asesino  nombre visible de quien lo mato, o cadena vacia si no hubo nadie
 * @param arma     nombre del arma usada, o cadena vacia
 * @param mundo    mundo donde ocurrio
 * @param altura   coordenada Y, redondeada
 * @param causa    causa cruda de Bukkit, por ejemplo {@code FALL}
 * @param esPvp    true si lo mato otro jugador
 * @param esJefe   true si lo mato uno de los jefes de Odysseia
 * @param esPropia true si lo mato algo suyo: una mascota, su propia flecha o su propia explosion
 */
public record DeathContext(String jugador, String asesino, String arma, String mundo,
                           int altura, String causa, boolean esPvp, boolean esJefe,
                           boolean esPropia) {

    /**
     * Que grupo de mensajes toca.
     *
     * El orden importa: quien te mato manda sobre como te mato. Morir contra un jefe cayendote al
     * vacio es una anecdota sobre el jefe, no sobre el vacio.
     */
    public String clave() {
        if (esJefe) return "jefe";
        if (esPvp) return "pvp";
        if (esPropia) return "autogol";
        return causa == null || causa.isBlank()
                ? DeathMessageCatalog.GENERICO
                : causa.toLowerCase(Locale.ROOT);
    }

    /** Sustituye los marcadores del mensaje por los datos de esta muerte. */
    public String aplicar(String plantilla) {
        return plantilla
                .replace("{jugador}", jugador == null ? "" : jugador)
                .replace("{asesino}", asesino == null || asesino.isBlank() ? "algo sin nombre" : asesino)
                .replace("{arma}", arma == null || arma.isBlank() ? "sus propias manos" : arma)
                .replace("{mundo}", mundo == null ? "" : mundo)
                .replace("{y}", String.valueOf(altura));
    }
}
