package org.metamechanists.odysseia.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Avisa de claves del config que el codigo ya no lee.
 *
 * Cuando una opcion se mueve de sitio, la vieja se queda en el archivo desplegado y **no da ningun
 * error**: Bukkit devuelve el valor por defecto y el servidor arranca igual. Quien vaya a
 * configurarla se encuentra con que "no hace nada" y no hay forma de saber por que.
 *
 * Paso de verdad: el bloque de anuncios de compra vivio en {@code store.*} y se movio a
 * {@code purchase-engine.announcements.*}. Las claves viejas siguen en el archivo desplegado, justo
 * al lado de {@code store.api-key}, que si se lee. Configurar el webhook en la de arriba es el
 * error natural, y es silencioso.
 */
public final class ConfigLegacyKeys {

    /** Clave muerta -> clave viva que la sustituye, o cadena vacia si ya no existe equivalente. */
    private static final Map<String, String> REEMPLAZOS = new LinkedHashMap<>();

    static {
        REEMPLAZOS.put("store.enabled", "purchase-engine.announcements.enabled");
        REEMPLAZOS.put("store.announcement-webhook-url", "purchase-engine.announcements.webhook-url");
        REEMPLAZOS.put("store.chat-announcement", "purchase-engine.announcements.chat-announcement");
        REEMPLAZOS.put("store.discord-announcement", "purchase-engine.announcements.discord-announcement");
        REEMPLAZOS.put("store.global-sound", "purchase-engine.announcements.global-sound");
        REEMPLAZOS.put("protectionstones.give-command", "");
        REEMPLAZOS.put("discord-translator.ingest-secret", "");
    }

    private ConfigLegacyKeys() {
    }

    /** Las claves que este aviso vigila. Visible para pruebas. */
    static Set<String> vigiladas() {
        return REEMPLAZOS.keySet();
    }

    /**
     * @param presentes rutas que existen en el config desplegado
     * @return un aviso por cada clave muerta encontrada; lista vacia si esta todo limpio
     */
    public static List<String> avisos(Set<String> presentes) {
        List<String> resultado = new ArrayList<>();
        for (Map.Entry<String, String> entrada : REEMPLAZOS.entrySet()) {
            if (!presentes.contains(entrada.getKey())) continue;
            String viva = entrada.getValue();
            resultado.add(viva.isEmpty()
                    ? "'" + entrada.getKey() + "' ya no se usa; puedes borrarla del config."
                    : "'" + entrada.getKey() + "' ya no se lee. La que manda es '" + viva + "'.");
        }
        return resultado;
    }
}
