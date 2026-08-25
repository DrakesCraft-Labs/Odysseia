package org.metamechanists.odysseia.papa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * El menu del trueque, y el comando que lo abre.
 *
 * El comando existe para que el NPC del spawn lo ejecute al hacerle click derecho: asi el NPC es
 * solo la puerta y toda la logica vive aqui, donde se puede comprobar la marca de la papa y
 * registrar cada canje.
 */
public final class PapaTraderMenu implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final PapaDeMarService servicio;

    public PapaTraderMenu(JavaPlugin plugin, PapaDeMarService servicio) {
        this.plugin = plugin;
        this.servicio = servicio;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("Este comando es para jugadores.");
            return true;
        }
        if (!servicio.activo()) {
            jugador.sendMessage(color("&7El trueque de la Papa de mar esta cerrado ahora mismo."));
            return true;
        }
        if (!mundoPermitido(jugador.getWorld().getName(), mundosPermitidos())) {
            jugador.sendMessage(color("&cEl trueque solo funciona dentro de la modalidad Slimefun. "
                    + "&7Usa &f/survival &7y vuelve a intentarlo."));
            return true;
        }
        // Las papas viejas se marcan al abrir, para que nadie pierda lo que ya tenia guardado.
        int migradas = servicio.migrar(jugador);
        if (migradas > 0) {
            jugador.sendMessage(color("&8Se validaron &7" + migradas + "&8 papas antiguas de tu inventario."));
        }
        abrir(jugador);
        return true;
    }

    public void abrir(Player jugador) {
        int papas = servicio.contar(jugador);
        Set<String> yaCanjeados = servicio.canjeados(jugador.getUniqueId());
        List<PapaTier> niveles = servicio.escalera().todos();

        int filas = Math.max(3, Math.min(6, (niveles.size() / 9) + 2));
        Holder holder = new Holder();
        Inventory menu = Bukkit.createInventory(holder, filas * 9, servicio.titulo());
        holder.inventory = menu;

        for (int i = 0; i < niveles.size() && i < (filas - 1) * 9; i++) {
            PapaTier nivel = niveles.get(i);
            boolean gastado = nivel.unica() && yaCanjeados.contains(nivel.id());
            boolean puede = servicio.escalera().disponible(nivel, papas, yaCanjeados);
            menu.setItem(i, icono(nivel, papas, puede, gastado));
            holder.porSlot.put(i, nivel.id());
        }

        menu.setItem(filas * 9 - 5, resumen(jugador, papas, yaCanjeados));
        menu.setItem(filas * 9 - 1, botonDeposito(jugador));
        holder.slotDeposito = filas * 9 - 1;
        jugador.openInventory(menu);
    }

    /** El boton que vacia el inventario en la alcancia. */
    private ItemStack botonDeposito(Player jugador) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(color("&e&lDepositar papas"));
        int llevaba = servicio.enInventario(jugador);
        meta.setLore(List.of(
                color("&7Guarda tus papas aqui para poder"),
                color("&7juntar mas de las que caben encima."),
                color(""),
                color("&7Llevas encima: &e" + llevaba),
                color("&7En la alcancia: &6" + servicio.enAlcancia(jugador.getUniqueId())),
                color(""),
                llevaba > 0
                        ? color("&a▶ Click para depositar " + llevaba
                                + " &8(guardas " + (llevaba - servicio.merma(llevaba)) + ")")
                        : color("&8No llevas papas encima."),
                color("&8El mar se queda un " + (int) servicio.porcentajeMerma() + "% de lo que guardas.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack icono(PapaTier nivel, int papas, boolean puede, boolean gastado) {
        Material material = Material.matchMaterial(nivel.icono());
        ItemStack item = new ItemStack(material == null ? Material.BAKED_POTATO : material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(nivel.nombre());
        List<String> lore = new ArrayList<>(nivel.descripcion());
        lore.add("");
        lore.add(color("&7Cuesta: &6" + nivel.costeLegible()));
        if (nivel.unica()) lore.add(color("&8Una sola vez por jugador"));
        lore.add("");
        if (gastado) {
            lore.add(color("&8Ya te lo llevaste."));
        } else if (puede) {
            lore.add(color("&a▶ Click para canjear"));
        } else {
            lore.add(color("&cTe faltan &4" + (nivel.coste() - papas) + "&c papas."));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack resumen(Player jugador, int papas, Set<String> yaCanjeados) {
        ItemStack item = new ItemStack(Material.BAKED_POTATO);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(color("&6&l✦ Tus papas de mar ✦"));
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Encima: &e" + servicio.enInventario(jugador)));
        lore.add(color("&7En la alcancia: &6" + servicio.enAlcancia(jugador.getUniqueId())));
        lore.add(color("&7Total: &f" + papas));
        int faltan = servicio.escalera().faltanParaSiguiente(papas, yaCanjeados);
        servicio.escalera().siguiente(papas, yaCanjeados).ifPresent(siguiente ->
                lore.add(color("&7Siguiente: &f" + ChatColor.stripColor(siguiente.nombre())
                        + " &8(&c" + faltan + " mas&8)")));
        lore.add("");
        lore.add(color("&8Solo cuentan las papas repartidas por el servidor."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Los iconos del menu son items de verdad --uno es un pico de netherita--, asi que arrastrar
     * dentro de el sacaria objetos gratis. Clic y arrastre son eventos distintos: cancelar solo el
     * primero deja la puerta abierta.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);   // el menu no es un cofre: nada entra ni sale
        if (!(event.getWhoClicked() instanceof Player jugador)) return;

        // Se valida otra vez al pulsar: el jugador pudo cambiar de mundo con el menu abierto.
        // Esto evita depositar un inventario aislado y recuperar despues su copia anterior.
        if (!mundoPermitido(jugador.getWorld().getName(), mundosPermitidos())) {
            jugador.closeInventory();
            jugador.sendMessage(color("&cEl trueque se cerró porque cambiaste de modalidad."));
            return;
        }

        if (event.getRawSlot() == holder.slotDeposito) {
            int depositadas = servicio.depositar(jugador);
            jugador.sendMessage(color(depositadas > 0
                    ? "&aDepositaste &e" + depositadas + "&a papas en la alcancia."
                    : "&7No llevas papas que depositar."));
            repintar(jugador);
            return;
        }

        String id = holder.porSlot.get(event.getRawSlot());
        if (id == null) return;

        String resultado = servicio.canjear(jugador, id);
        jugador.sendMessage(color(resultado));
        if (resultado.startsWith("&c") || resultado.startsWith("&7")) {
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
        }
        repintar(jugador);
    }

    /**
     * Vuelve a pintar el menu en el tick siguiente.
     *
     * Abrir un inventario desde dentro de InventoryClickEvent deja al cliente descolocado --el
     * servidor cree que hay una ventana y el jugador ve otra--, y ese desajuste es de donde salen
     * la mitad de los dupes de menus. Se aplaza un tick.
     */
    private void repintar(Player jugador) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (jugador.isOnline()) abrir(jugador);
        });
    }

    private static String color(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }

    private List<String> mundosPermitidos() {
        List<String> configured = plugin.getConfig().getStringList("papa-de-mar.trueque.mundos-permitidos");
        return configured.isEmpty() ? List.of("world", "world_nether", "world_the_end") : configured;
    }

    /** Comprueba nombres exactos; un prefijo ambiguo podría abrir el trueque en otra modalidad. */
    static boolean mundoPermitido(String mundo, List<String> permitidos) {
        if (mundo == null) return false;
        String normalizado = mundo.toLowerCase(Locale.ROOT);
        return permitidos.stream()
                .filter(java.util.Objects::nonNull)
                .map(nombre -> nombre.toLowerCase(Locale.ROOT))
                .anyMatch(normalizado::equals);
    }

    /** Identifica el menu del trueque y recuerda que nivel hay en cada hueco. */
    private static final class Holder implements InventoryHolder {
        private final Map<Integer, String> porSlot = new HashMap<>();
        private int slotDeposito = -1;
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
