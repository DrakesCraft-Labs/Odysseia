package org.metamechanists.odysseia.deaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Los mensajes de muerte disponibles, agrupados por como te moriste.
 *
 * Dos cosas que parecen detalles y no lo son:
 *
 *   - **No repite el ultimo.** Un chiste repetido dos veces seguidas deja de tener gracia, y con
 *     pocas muertes al dia se nota enseguida. Se elige entre los que no salieron la vez anterior.
 *   - **Cae al generico** si para esa causa no hay nada escrito, en vez de dejar al jugador sin
 *     mensaje. Añadir causas nuevas al archivo no obliga a rellenarlas todas de golpe.
 *
 * No toca Bukkit: se le pasa el azar desde fuera para poder comprobarlo sin servidor.
 */
public final class DeathMessageCatalog {

    public static final String GENERICO = "generico";

    /**
     * Causas de Bukkit que comparten grupo de mensajes.
     *
     * Bukkit distingue si una explosion la causo un bloque o una entidad, y si el fuego te quemo
     * de golpe o poco a poco. Para contar la muerte con gracia esa diferencia no aporta nada, y
     * mantenerla obligaba a escribir el mismo chiste dos veces con nombres distintos.
     *
     * Sin esto un grupo llamado "explosion" no se usaba nunca, porque la causa real que llega
     * siempre es block_explosion o entity_explosion: los mensajes estaban escritos y el jugador
     * veia el generico.
     */
    private static final Map<String, String> ALIAS = Map.ofEntries(
            Map.entry("block_explosion", "explosion"),
            Map.entry("entity_explosion", "explosion"),
            Map.entry("fire_tick", "fire"),
            Map.entry("campfire", "fire"),
            Map.entry("hot_floor", "lava"),
            Map.entry("melting", "fire"),
            Map.entry("entity_attack", "generico"),
            Map.entry("entity_sweep_attack", "generico"),
            Map.entry("suicide", "autogol"),
            Map.entry("kill", "generico"),
            Map.entry("dryout", "drowning"));

    private final Map<String, List<String>> grupos;
    /** Ultimo mensaje servido por clave, para no repetirlo seguido. */
    private final Map<String, String> ultimo = new HashMap<>();

    public DeathMessageCatalog(Map<String, List<String>> grupos) {
        this.grupos = new HashMap<>();
        grupos.forEach((clave, mensajes) -> {
            List<String> limpios = new ArrayList<>();
            for (String mensaje : mensajes) {
                if (mensaje != null && !mensaje.isBlank()) limpios.add(mensaje);
            }
            if (!limpios.isEmpty()) this.grupos.put(clave, List.copyOf(limpios));
        });
    }

    /** Cuantas claves tienen al menos un mensaje. */
    public int grupos() {
        return grupos.size();
    }

    public boolean tiene(String clave) {
        return grupos.containsKey(clave);
    }

    /**
     * Un mensaje para esa causa, evitando el que salio la vez anterior.
     *
     * @return el mensaje, o {@code null} si no hay nada ni para la causa ni para el generico, en
     *         cuyo caso hay que dejar el mensaje de vanilla en paz
     */
    public String elegir(String clave, RandomGenerator azar) {
        List<String> grupo = grupos.get(clave);
        if (grupo == null) grupo = grupos.get(ALIAS.getOrDefault(clave, ""));
        if (grupo == null) grupo = grupos.get(GENERICO);
        if (grupo == null || grupo.isEmpty()) return null;

        List<String> candidatos = grupo;
        String previo = ultimo.get(clave);
        if (previo != null && grupo.size() > 1) {
            candidatos = new ArrayList<>(grupo);
            candidatos.remove(previo);
        }

        String elegido = candidatos.get(azar.nextInt(candidatos.size()));
        ultimo.put(clave, elegido);
        return elegido;
    }
}
