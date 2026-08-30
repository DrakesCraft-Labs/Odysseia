package org.metamechanists.odysseia.papa;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Identifica una Papa de mar de verdad.
 *
 * Hasta ahora la papa se reconocia por su nombre y su lore, y nada mas. Con eso basta para
 * mostrarla bonita, pero no para canjearla: el servidor tiene ItemEdit e ItemEditor instalados, y
 * con un yunque y algo de paciencia cualquiera fabrica una parecida. Montar premios sobre un item
 * falsificable es el mismo agujero del arbitraje de bambu, pero con la recompensa mucho mas alta.
 *
 * Asi que ahora lleva una **marca interna** que no se puede escribir desde el juego: una clave en
 * el PersistentDataContainer. El canje solo acepta papas con esa marca.
 *
 * Las papas que ya estaban circulando no la tienen, y no se le van a invalidar a nadie: se
 * reconocen por su forma antigua (nombre + lore) y se les estampa la marca la primera vez que se
 * las ve. Ver {@link PapaDeMarService}.
 */
public final class PapaDeMarItem {

    private static final String CLAVE = "papa_de_mar";

    private PapaDeMarItem() {
    }

    public static NamespacedKey clave(Plugin plugin) {
        return new NamespacedKey(plugin, CLAVE);
    }

    /** Pone la marca. Devuelve el mismo item, ya modificado. */
    public static ItemStack marcar(Plugin plugin, ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(clave(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** True si el item lleva la marca autentica. */
    public static boolean marcada(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(clave(plugin), PersistentDataType.BYTE);
    }

    /**
     * True si el item tiene la forma de una papa antigua: el material y el nombre que se venian
     * repartiendo. Solo se usa para migrar las que ya circulaban, nunca para aceptar un canje.
     *
     * Se compara el nombre ya sin codigos de color, porque en el archivo van con '&' y en el item
     * con seccion.
     */
    public static boolean pareceAntigua(ItemStack item, String materialEsperado,
                                        String nombreEsperado, List<String> loreEsperado) {
        if (item == null || !item.hasItemMeta()) return false;
        if (!item.getType().name().equalsIgnoreCase(materialEsperado)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        if (!sinColores(meta.getDisplayName()).equals(sinColores(nombreEsperado))) return false;

        // El nombre por si solo NO basta: un yunque renombra una patata cocida a "✦ Papa de mar ✦"
        // en diez segundos, y con eso se fabricarian papas autenticas a voluntad. El lore es lo que
        // no se puede poner desde el juego, asi que tiene que coincidir entero.
        if (loreEsperado == null || loreEsperado.isEmpty()) return false;
        List<String> lore = meta.getLore();
        if (lore == null || lore.size() != loreEsperado.size()) return false;
        for (int i = 0; i < lore.size(); i++) {
            if (!sinColores(lore.get(i)).equals(sinColores(loreEsperado.get(i)))) return false;
        }
        return true;
    }

    /**
     * True si el item tiene material y nombre de papa, sin mirar el lore.
     *
     * NO sirve para aceptar un canje --un yunque falsifica el nombre en diez segundos-- pero
     * distingue "no llevas papas" de "llevas papas que no reconozco", que es justo la
     * diferencia que hacia falta para diagnosticar el reporte de Alaska001.
     */
    public static boolean pareceCandidata(ItemStack item, String materialEsperado,
                                          String nombreEsperado) {
        if (item == null || !item.hasItemMeta()) return false;
        if (!item.getType().name().equalsIgnoreCase(materialEsperado)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return sinColores(meta.getDisplayName()).equals(sinColores(nombreEsperado));
    }

    /** Quita codigos de color, tanto los de '&' como los de seccion, y los espacios de los bordes. */
    static String sinColores(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").trim();
    }

    /** Cuantas papas autenticas hay en un conjunto de items. Los huecos vacios se ignoran. */
    public static int contar(Plugin plugin, List<ItemStack> items) {
        int total = 0;
        for (ItemStack item : items) {
            if (marcada(plugin, item)) total += item.getAmount();
        }
        return total;
    }
}
