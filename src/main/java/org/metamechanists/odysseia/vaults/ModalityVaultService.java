package org.metamechanists.odysseia.vaults;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.modalities.Modality;
import org.metamechanists.odysseia.modalities.ModalityService;

import java.io.File;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Bovedas personales separadas por modalidad.
 *
 * PlayerVaultZ guarda todo en una unica base global y no tiene ninguna nocion de mundo, asi que
 * en las islas sirve de puente para mover items desde el survival. Este servicio atiende /pv
 * dentro de las modalidades de isla con su propio almacen, y deja PlayerVaultZ intacto para el
 * survival, donde ya viven las bovedas historicas de los jugadores.
 */
public final class ModalityVaultService implements Listener {

    private static final String PERMISSION_PREFIX = "odysseia.bovedas.";

    private final JavaPlugin plugin;
    private final ModalityService modalities;
    private final ModalityVaultRepository repository;
    private final int rows;
    private final int defaultVaults;

    public ModalityVaultService(JavaPlugin plugin, ModalityService modalities) throws SQLException {
        this.plugin = plugin;
        this.modalities = modalities;
        this.repository = new ModalityVaultRepository(new File(plugin.getDataFolder(), "modality-vaults.db"));
        this.rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("modalidades.bovedas.filas", 6)));
        this.defaultVaults = Math.max(1, plugin.getConfig().getInt("modalidades.bovedas.cantidad-base", 3));
    }

    /** Cuantas bovedas puede abrir el jugador: el mayor valor entre el base y sus permisos. */
    public int allowance(Player player) {
        int allowed = defaultVaults;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String node = info.getPermission();
            if (!info.getValue() || !node.startsWith(PERMISSION_PREFIX)) continue;
            try {
                allowed = Math.max(allowed, Integer.parseInt(node.substring(PERMISSION_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                // nodo con sufijo no numerico, se ignora
            }
        }
        return allowed;
    }

    /** Abre una boveda de la modalidad en la que esta el jugador. */
    public void open(Player player, int vault) {
        Modality modality = modalities.resolve(player);
        int allowed = allowance(player);
        if (vault < 1 || vault > allowed) {
            player.sendMessage(color("&6DrakesCraft &8· &7Tienes &e" + allowed + "&7 bovedas en &e"
                    + ChatColor.stripColor(color(modality.displayName())) + "&7. Usa &e/pv 1&7 a &e/pv " + allowed + "&7."));
            return;
        }
        try {
            int size = rows * 9;
            ItemStack[] contents = repository.load(player.getUniqueId(), modality.id(), vault, size);
            VaultHolder holder = new VaultHolder(player.getUniqueId(), modality.id(), vault);
            Inventory inventory = Bukkit.createInventory(holder, size,
                    color("&8Boveda &e" + vault + " &8· " + modality.displayName()));
            holder.inventory = inventory;
            inventory.setContents(contents);
            player.openInventory(inventory);
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "[Bovedas] No se pudo abrir la boveda " + vault
                    + " de " + player.getName(), error);
            player.sendMessage(color("&cNo se pudo abrir la boveda. Avisa al staff."));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof VaultHolder holder)) return;
        persist(holder, event.getInventory().getContents());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Si el jugador se desconecta con la boveda abierta, Bukkit no siempre dispara el cierre.
        if (event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof VaultHolder holder) {
            persist(holder, event.getPlayer().getOpenInventory().getTopInventory().getContents());
        }
    }

    private void persist(VaultHolder holder, ItemStack[] contents) {
        try {
            repository.save(holder.owner, holder.modality, holder.vault, contents);
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE, "[Bovedas] No se pudo guardar la boveda "
                    + holder.vault + " (" + holder.modality + ") de " + holder.owner, error);
        }
    }

    /** Guarda las bovedas que sigan abiertas; se llama al apagar el plugin. */
    public void flushOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof VaultHolder holder) persist(holder, top.getContents());
        }
    }

    public void close() {
        flushOpen();
        try {
            repository.close();
        } catch (SQLException error) {
            plugin.getLogger().log(Level.WARNING, "[Bovedas] Error al cerrar la base de bovedas", error);
        }
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /** Identifica a que boveda pertenece un inventario abierto. */
    private static final class VaultHolder implements InventoryHolder {
        private final UUID owner;
        private final String modality;
        private final int vault;
        private Inventory inventory;

        private VaultHolder(UUID owner, String modality, int vault) {
            this.owner = owner;
            this.modality = modality;
            this.vault = vault;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
