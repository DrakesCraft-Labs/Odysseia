package org.metamechanists.odysseia.restart;

/**
 * En que segundos de la cuenta atras se avisa a los jugadores.
 *
 * Avisar cada segundo desde el minuto uno es ruido y la gente lo ignora; avisar solo al final no da
 * tiempo a guardar lo que estabas haciendo. El reparto es denso al final y espaciado al principio.
 *
 * No toca Bukkit, asi que se puede comprobar sin servidor.
 */
public final class RestartCountdown {

    private RestartCountdown() {
    }

    /** True si a {@code restantes} segundos del reinicio toca avisar. */
    public static boolean debeAnunciar(int restantes) {
        if (restantes <= 0) return false;
        if (restantes <= 5) return true;              // ultimos cinco, uno a uno
        if (restantes <= 30) return restantes % 10 == 0;   // 10, 20, 30
        if (restantes <= 300) return restantes % 60 == 0;  // cada minuto hasta cinco
        return restantes % 300 == 0;                  // cada cinco minutos por encima
    }

    /** Texto humano del tiempo restante, en español y sin decimales. */
    public static String tiempo(int restantes) {
        if (restantes >= 60) {
            int minutos = restantes / 60;
            int segundos = restantes % 60;
            String base = minutos + (minutos == 1 ? " minuto" : " minutos");
            return segundos == 0 ? base : base + " y " + segundos + "s";
        }
        return restantes + (restantes == 1 ? " segundo" : " segundos");
    }
}
