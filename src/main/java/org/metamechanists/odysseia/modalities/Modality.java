package org.metamechanists.odysseia.modalities;

import java.util.List;

/**
 * Una modalidad de juego de DrakesCraft. Los rangos y los kits se comparten entre todas;
 * lo que se separa es el inventario y el almacenamiento.
 *
 * @param id          identificador interno, tambien usado como clave de las bovedas
 * @param displayName nombre visible, admite codigos de color con &amp;
 * @param description linea corta para el menu /modalidades
 * @param icon        material del icono en el menu
 * @param command     comando que lleva al jugador a la modalidad
 * @param worlds      prefijos de mundo que pertenecen a la modalidad (cubre nether y end)
 */
public record Modality(String id, String displayName, String description, String icon,
                       String command, List<String> worlds) {

    /** True si el mundo indicado pertenece a esta modalidad. */
    public boolean matches(String worldName) {
        if (worldName == null) return false;
        String name = worldName.toLowerCase(java.util.Locale.ROOT);
        for (String prefix : worlds) {
            String candidate = prefix.toLowerCase(java.util.Locale.ROOT);
            if (name.equals(candidate) || name.startsWith(candidate + "_")) return true;
        }
        return false;
    }
}
