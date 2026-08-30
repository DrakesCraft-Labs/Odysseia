package org.metamechanists.odysseia.papa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * El canje de la Papa de mar.
 *
 * Tres reglas que sostienen todo lo demas:
 *
 *   1. **Solo cuentan las papas marcadas.** Ver {@link PapaDeMarItem}: sin eso, cualquiera con un
 *      yunque se lleva el premio gordo.
 *   2. **El intercambio es atomico.** Primero se comprueba que estan, luego se quitan, y solo si se
 *      quitaron todas se entrega el premio. Nadie paga sin recibir ni recibe sin pagar.
 *   3. **Los canjes de una sola vez se recuerdan en disco.** Un reinicio no debe regalar otra
 *      armadura.
 */
public final class PapaDeMarService {

    private static final String ARCHIVO_NIVELES = "papa-trader.yml";
    private static final String ARCHIVO_CANJES = "papa-canjes.yml";

    private final JavaPlugin plugin;
    private PapaLadder escalera = new PapaLadder(List.of());
    private final File archivoCanjes;
    private FileConfiguration canjes;
    private boolean enabled;
    private String titulo;

    public PapaDeMarService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.archivoCanjes = new File(plugin.getDataFolder(), ARCHIVO_CANJES);
        reload();
    }

    public void reload() {
        File archivo = new File(plugin.getDataFolder(), ARCHIVO_NIVELES);
        if (!archivo.exists()) plugin.saveResource(ARCHIVO_NIVELES, false);
        FileConfiguration datos = YamlConfiguration.loadConfiguration(archivo);

        enabled = datos.getBoolean("enabled", true);
        titulo = ChatColor.translateAlternateColorCodes('&',
                datos.getString("titulo-menu", "&6Trueque de la Papa de mar"));

        List<PapaTier> niveles = new ArrayList<>();
        var seccion = datos.getConfigurationSection("niveles");
        if (seccion != null) {
            for (String id : seccion.getKeys(false)) {
                var nivel = seccion.getConfigurationSection(id);
                if (nivel == null) continue;
                int coste = nivel.getInt("coste", 0);
                if (coste <= 0) {
                    plugin.getLogger().warning("[Papa] El nivel '" + id + "' no tiene coste; se ignora.");
                    continue;
                }
                niveles.add(new PapaTier(id,
                        ChatColor.translateAlternateColorCodes('&', nivel.getString("nombre", id)),
                        coste,
                        nivel.getString("icono", "BAKED_POTATO"),
                        nivel.getStringList("descripcion").stream()
                                .map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList(),
                        nivel.getStringList("comandos"),
                        nivel.getBoolean("unica", false)));
            }
        }
        escalera = new PapaLadder(niveles);
        canjes = YamlConfiguration.loadConfiguration(archivoCanjes);
        plugin.getLogger().info("[Papa] " + niveles.size() + " niveles de canje cargados"
                + (enabled ? "" : " (desactivados)") + ".");
    }

    public boolean activo() {
        return enabled;
    }

    public PapaLadder escalera() {
        return escalera;
    }

    public String titulo() {
        return titulo;
    }

    /** Papas autenticas que lleva encima el jugador. */
    public int enInventario(Player jugador) {
        return PapaDeMarItem.contar(plugin, Arrays.asList(jugador.getInventory().getContents()));
    }

    /**
     * Papas guardadas en la alcancia del comerciante.
     *
     * Existe por un limite fisico: en un inventario caben 36 x 64 = 2304 papas y ni una mas. Sin un
     * sitio donde acumularlas, cualquier nivel por encima de eso seria impagable por mucho que se
     * juntara. Depositando, el total sube sin techo.
     */
    public int enAlcancia(UUID jugador) {
        return canjes.getInt("alcancia." + jugador, 0);
    }

    /** Lo que cuenta para los niveles: lo que lleva encima mas lo depositado. */
    public int contar(Player jugador) {
        return enInventario(jugador) + enAlcancia(jugador.getUniqueId());
    }

    /**
     * Mete en la alcancia todas las papas marcadas del inventario.
     *
     * @return cuantas se depositaron
     */
    public int depositar(Player jugador) {
        int llevaba = enInventario(jugador);
        if (llevaba <= 0) return 0;
        int quitadas = quitarDelInventario(jugador, llevaba);
        int guardadas = quitadas - merma(quitadas);
        canjes.set("alcancia." + jugador.getUniqueId(), enAlcancia(jugador.getUniqueId()) + guardadas);
        guardarCanjes();
        plugin.getLogger().info("[Papa] " + jugador.getName() + " deposito " + quitadas
                + " papas y guardo " + guardadas + ".");
        return guardadas;
    }

    /**
     * Cuantas papas se pierden al depositar {@code cuantas}.
     *
     * La papa se reparte sola cada 15 minutos y no se gasta en nada mas, asi que sin una fuga la
     * cantidad en circulacion solo puede subir. Una merma pequena al guardar hace que acumular
     * cueste algo y que la economia de papas no se infle sola.
     *
     * Se redondea hacia abajo y **nunca se lleva la ultima**: depositar 1 papa siempre guarda 1,
     * porque perder lo poco que tienes desanima mas de lo que regula.
     */
    public int merma(int cuantas) {
        double porcentaje = plugin.getConfig().getDouble("papa-de-mar.trueque.merma-porcentaje", 5.0);
        if (porcentaje <= 0 || cuantas <= 1) return 0;
        int perdidas = (int) Math.floor(cuantas * porcentaje / 100.0);
        return Math.min(perdidas, cuantas - 1);
    }

    /** El porcentaje configurado, para poder ensenarlo en el menu. */
    public double porcentajeMerma() {
        return plugin.getConfig().getDouble("papa-de-mar.trueque.merma-porcentaje", 5.0);
    }

    public Set<String> canjeados(UUID jugador) {
        return new HashSet<>(canjes.getStringList(jugador.toString()));
    }

    /**
     * Marca las papas antiguas que el jugador lleve encima.
     *
     * Las que se repartieron antes de que existiera la marca son legitimas y no se le van a
     * invalidar a nadie. Se reconocen por su forma antigua y se les estampa la marca al vuelo.
     *
     * @return cuantas se convirtieron
     */
    public int migrar(Player jugador) {
        String material = plugin.getConfig().getString("papa-de-mar.item.material", "BAKED_POTATO");
        String nombre = plugin.getConfig().getString("papa-de-mar.item.name", "&6&l✦ Papa de mar ✦");
        List<String> lore = plugin.getConfig().getStringList("papa-de-mar.item.lore");
        int convertidas = 0;
        ItemStack[] contenido = jugador.getInventory().getContents();
        int candidatasNoMigradas = 0;
        for (ItemStack item : contenido) {
            if (item == null || PapaDeMarItem.marcada(plugin, item)) continue;
            if (!PapaDeMarItem.pareceAntigua(item, material, nombre, lore)) {
                // Una papa que lleva el nombre correcto pero no migra es el caso que deja al
                // jugador con "no llevas papas" y al staff sin nada que mirar: el rechazo era
                // silencioso. Se registra el motivo para poder diagnosticarlo desde el log.
                if (PapaDeMarItem.pareceCandidata(item, material, nombre)) {
                    candidatasNoMigradas += item.getAmount();
                }
                continue;
            }
            PapaDeMarItem.marcar(plugin, item);
            convertidas += item.getAmount();
        }
        if (candidatasNoMigradas > 0) {
            plugin.getLogger().warning("[Papa] " + jugador.getName() + " lleva "
                    + candidatasNoMigradas + " item(s) con el nombre de la papa que NO migran: "
                    + "su lore no coincide con papa-de-mar.item.lore del config. "
                    + "Comparar el lore real del item con el configurado antes de tocar nada.");
        }
        if (convertidas > 0) jugador.getInventory().setContents(contenido);
        return convertidas;
    }

    /**
     * Canjea un nivel.
     *
     * @return un mensaje con lo ocurrido; nunca null
     */
    public String canjear(Player jugador, String idNivel) {
        var buscado = escalera.porId(idNivel);
        if (buscado.isEmpty()) return "&cEse trueque ya no existe.";
        PapaTier nivel = buscado.get();

        Set<String> yaCanjeados = canjeados(jugador.getUniqueId());
        if (nivel.unica() && yaCanjeados.contains(nivel.id())) {
            return "&7Ese trueque es de una sola vez y ya te lo llevaste.";
        }

        int tiene = contar(jugador);
        if (tiene < nivel.coste()) {
            return "&7Te faltan &e" + (nivel.coste() - tiene) + "&7 papas para eso.";
        }

        int quitadas = quitar(jugador, nivel.coste());
        if (quitadas != nivel.coste()) {
            // No deberia pasar: se conto y se quito en el mismo tick. Si pasa, se devuelve lo
            // quitado antes de tocar las recompensas, para no cobrar sin entregar.
            devolver(jugador, quitadas);
            plugin.getLogger().warning("[Papa] Canje abortado para " + jugador.getName()
                    + ": se esperaban " + nivel.coste() + " papas y se quitaron " + quitadas);
            return "&cAlgo salio mal con el trueque. No se te cobro nada, avisa al staff.";
        }

        if (nivel.unica()) {
            List<String> lista = new ArrayList<>(canjes.getStringList(jugador.getUniqueId().toString()));
            lista.add(nivel.id());
            canjes.set(jugador.getUniqueId().toString(), lista);
            guardarCanjes();
        }

        for (String comando : nivel.comandos()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    comando.replace("{jugador}", jugador.getName()));
        }

        plugin.getLogger().info("[Papa] " + jugador.getName() + " canjeo '" + nivel.id()
                + "' por " + nivel.coste() + " papas.");
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        return "&aTrueque hecho: &6" + ChatColor.stripColor(nivel.nombre());
    }

    /**
     * Cobra el coste, primero del inventario y luego de la alcancia.
     *
     * En ese orden a proposito: lo que se ve desaparecer es lo que uno lleva encima, y asi el
     * jugador entiende que pago.
     */
    private int quitar(Player jugador, int cuantas) {
        int delInventario = quitarDelInventario(jugador, cuantas);
        int pendiente = cuantas - delInventario;
        if (pendiente <= 0) return delInventario;

        int alcancia = enAlcancia(jugador.getUniqueId());
        int deLaAlcancia = Math.min(alcancia, pendiente);
        if (deLaAlcancia > 0) {
            canjes.set("alcancia." + jugador.getUniqueId(), alcancia - deLaAlcancia);
            guardarCanjes();
        }
        return delInventario + deLaAlcancia;
    }

    /** Quita hasta {@code cuantas} papas marcadas del inventario. Devuelve cuantas quito. */
    private int quitarDelInventario(Player jugador, int cuantas) {
        Inventory inventario = jugador.getInventory();
        int pendientes = cuantas;
        for (int slot = 0; slot < inventario.getSize() && pendientes > 0; slot++) {
            ItemStack item = inventario.getItem(slot);
            if (!PapaDeMarItem.marcada(plugin, item)) continue;
            int usar = Math.min(item.getAmount(), pendientes);
            pendientes -= usar;
            if (item.getAmount() == usar) {
                inventario.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - usar);
            }
        }
        return cuantas - pendientes;
    }

    /** Devuelve papas al jugador; lo que no quepa cae al suelo, que es mejor que perderlo. */
    private void devolver(Player jugador, int cuantas) {
        while (cuantas > 0) {
            int lote = Math.min(64, cuantas);
            ItemStack papa = crearPapa();
            papa.setAmount(lote);
            jugador.getInventory().addItem(papa).values()
                    .forEach(sobra -> jugador.getWorld().dropItemNaturally(jugador.getLocation(), sobra));
            cuantas -= lote;
        }
    }

    /** Una Papa de mar nueva, ya marcada. */
    public ItemStack crearPapa() {
        FileConfiguration config = plugin.getConfig();
        Material material = Material.matchMaterial(
                config.getString("papa-de-mar.item.material", "BAKED_POTATO"));
        ItemStack item = new ItemStack(material == null ? Material.BAKED_POTATO : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    config.getString("papa-de-mar.item.name", "&6&l✦ Papa de mar ✦")));
            meta.setLore(config.getStringList("papa-de-mar.item.lore").stream()
                    .map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            for (String bruto : config.getStringList("papa-de-mar.item.enchantments")) {
                String[] partes = bruto.split(":", 2);
                var encantamiento = org.bukkit.enchantments.Enchantment.getByName(
                        partes[0].toUpperCase(Locale.ROOT));
                if (encantamiento == null) continue;
                int nivel = partes.length > 1 ? Integer.parseInt(partes[1].trim()) : 1;
                meta.addEnchant(encantamiento, nivel, true);
            }
            item.setItemMeta(meta);
        }
        return PapaDeMarItem.marcar(plugin, item);
    }

    private void guardarCanjes() {
        try {
            canjes.save(archivoCanjes);
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "[Papa] No se pudieron guardar los canjes", error);
        }
    }
}
