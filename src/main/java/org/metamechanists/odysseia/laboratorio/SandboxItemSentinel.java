package org.metamechanists.odysseia.laboratorio;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Centinela de seguridad anti-fugas de ítems entre el Laboratorio Creativo y las demás modalidades.
 * 
 * Funcionamiento:
 * 1. Cualquier ítem manipulado, recogido, crafteado o generado dentro del mundo 'laboratorio'
 *    es marcado invisiblemente con una clave NBT persistente (PersistentDataContainer).
 * 2. Si un jugador logra cruzar un ítem con esta marca a cualquier mundo exterior (Survival, Clásico,
 *    SkyBlock, OneBlock), el centinela destruye el ítem instantáneamente, alerta al staff y expulsa
 *    al jugador para purgar residuos sin riesgo de falsos positivos en ítems legítimos del Survival.
 */
public final class SandboxItemSentinel implements Listener {

    private final JavaPlugin plugin;
    private final Set<String> sandboxWorlds;
    private final NamespacedKey keySandbox;
    private final NamespacedKey keyCreator;
    private final NamespacedKey keyTimestamp;

    public SandboxItemSentinel(JavaPlugin plugin, Set<String> sandboxWorlds) {
        this.plugin = plugin;
        this.sandboxWorlds = sandboxWorlds;
        this.keySandbox = new NamespacedKey(plugin, "sandbox_item");
        this.keyCreator = new NamespacedKey(plugin, "sandbox_creator");
        this.keyTimestamp = new NamespacedKey(plugin, "sandbox_time");
    }

    private boolean isSandbox(Location location) {
        return location != null && location.getWorld() != null
                && sandboxWorlds.contains(location.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    /** Marca un ítem con el sello indeleble del Laboratorio Creativo. */
    public void tagItem(ItemStack item, UUID creator) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keySandbox, PersistentDataType.BYTE, (byte) 1);
        if (creator != null) {
            pdc.set(keyCreator, PersistentDataType.STRING, creator.toString());
        }
        pdc.set(keyTimestamp, PersistentDataType.LONG, System.currentTimeMillis());
        item.setItemMeta(meta);
    }

    /** Comprueba si un ítem fue originado en el Laboratorio Creativo. */
    public boolean isSandboxItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(keySandbox, PersistentDataType.BYTE);
    }

    // =========================================================================
    // FASE 1: MARCADO ACTIVO DENTRO DEL LABORATORIO
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClickInSandbox(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isSandbox(player.getLocation())) return;

        if (event.getCurrentItem() != null) tagItem(event.getCurrentItem(), player.getUniqueId());
        if (event.getCursor() != null) tagItem(event.getCursor(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupInSandbox(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isSandbox(player.getLocation())) return;

        tagItem(event.getItem().getItemStack(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropInSandbox(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isSandbox(player.getLocation())) return;

        tagItem(event.getItemDrop().getItemStack(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftInSandbox(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isSandbox(player.getLocation())) return;

        if (event.getCurrentItem() != null) tagItem(event.getCurrentItem(), player.getUniqueId());
    }

    // =========================================================================
    // FASE 2: DETECCIÓN, INCINERACIÓN Y REPORTE FUERA DEL LABORATORIO
    // =========================================================================

    private void inspectAndPurge(Player player) {
        if (isSandbox(player.getLocation())) return;

        boolean violacionDetectada = false;
        ItemStack itemViolacion = null;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isSandboxItem(item)) {
                itemViolacion = item.clone();
                player.getInventory().setItem(i, null);
                violacionDetectada = true;
            }
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (isSandboxItem(item)) {
                if (itemViolacion == null) itemViolacion = item.clone();
                armor[i] = null;
                violacionDetectada = true;
            }
        }
        player.getInventory().setArmorContents(armor);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isSandboxItem(offhand)) {
            if (itemViolacion == null) itemViolacion = offhand.clone();
            player.getInventory().setItemInOffHand(null);
            violacionDetectada = true;
        }

        if (violacionDetectada && itemViolacion != null) {
            String worldName = player.getWorld().getName();
            String itemType = itemViolacion.getType().name();

            // 1. Log crítico de auditoría
            plugin.getLogger().log(Level.SEVERE,
                    "[SENTINEL ALERTA] Violación de aislamiento: " + player.getName()
                            + " detectado con ítem del Laboratorio (" + itemType + " x" + itemViolacion.getAmount()
                            + ") en el mundo " + worldName + "! Ítem purgado.");

            // 2. Alerta a operadores y administradores en línea
            String alertaStaff = ChatColor.translateAlternateColorCodes('&',
                    "&4&l[SENTINEL ALERTA] &c" + player.getName()
                            + " &7intentó cruzar ítem del Laboratorio (&e" + itemType + "&7) a &e" + worldName + "&7. Ítem incinerado.");
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.isOp() || staff.hasPermission("odysseia.admin") || staff.hasPermission("odysseia.alerts")) {
                    staff.sendMessage(alertaStaff);
                }
            }

            // 3. Notificación al jugador y expulsión preventiva
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &c[Seguridad] Se detectó un ítem originario del Laboratorio en tu inventario. "
                            + "&7El ítem ha sido purgado automáticamente."));

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                        "&c[DrakesCraft - Seguridad]\n\n&eSe detectó un ítem no autorizado del Laboratorio en tu inventario.\n"
                                + "&7El ítem ha sido purgado para proteger la economía del Survival.\n"
                                + "&fSi crees que esto fue un fallo del cliente, puedes volver a entrar."));
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        inspectAndPurge(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        inspectAndPurge(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (isSandboxItem(event.getItem())) {
            event.setCancelled(true);
            inspectAndPurge(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isSandboxItem(event.getItemInHand())) {
            event.setCancelled(true);
            inspectAndPurge(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClickOutside(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (isSandbox(player.getLocation())) return;

        if (isSandboxItem(event.getCurrentItem()) || isSandboxItem(event.getCursor())) {
            event.setCancelled(true);
            inspectAndPurge(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropOutside(PlayerDropItemEvent event) {
        if (isSandbox(event.getPlayer().getLocation())) return;

        if (isSandboxItem(event.getItemDrop().getItemStack())) {
            event.getItemDrop().remove();
            inspectAndPurge(event.getPlayer());
        }
    }
}
