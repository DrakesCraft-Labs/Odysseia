package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
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
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.modalities.Modality;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Centinela Universal de Aislamiento de Ítems entre Modalidades de DrakesCraft.
 * 
 * Reglas de Protección:
 * 1. [LABORATORIO]: Ningún ítem originario del Laboratorio Creativo puede cruzar a otra modalidad.
 *    Se destruye inmediatamente, alerta al staff y expulsa al jugador preventivamente.
 * 2. [CLÁSICO]: Ningún ítem de Slimefun (máquinas, componentes, mochilas, reactores, armas SF)
 *    puede existir en los mundos de Clásico (Vanilla). Se purga automáticamente.
 * 3. [INSPECCIÓN PROFUNDA]: Escanea contenedores como Shulker Boxes recursivamente para evitar
 *    que jugadores intenten contrabandear ítems prohibidos dentro de cajas.
 */
public final class UniversalModalitySentinel implements Listener {

    private final Odysseia plugin;
    private final Set<String> sandboxWorlds;
    private final NamespacedKey keySandbox;
    private final NamespacedKey keyCreator;
    private final NamespacedKey keyTimestamp;
    private final NamespacedKey keySlimefun;

    public UniversalModalitySentinel(Odysseia plugin, Set<String> sandboxWorlds) {
        this.plugin = plugin;
        this.sandboxWorlds = sandboxWorlds;
        this.keySandbox = new NamespacedKey(plugin, "sandbox_item");
        this.keyCreator = new NamespacedKey(plugin, "sandbox_creator");
        this.keyTimestamp = new NamespacedKey(plugin, "sandbox_time");
        this.keySlimefun = new NamespacedKey("slimefun", "slimefun_item");
    }

    private boolean isSandbox(Location location) {
        return location != null && location.getWorld() != null
                && sandboxWorlds.contains(location.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    /** Marca un ítem con el sello del Laboratorio Creativo. */
    public void tagSandboxItem(ItemStack item, UUID creator) {
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

    /** Comprueba si un ítem fue creado o manipulado en el Laboratorio Creativo. */
    public boolean isSandboxItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(keySandbox, PersistentDataType.BYTE);
    }

    /** Comprueba con total certeza si un ítem pertenece a Slimefun o sus expansiones. */
    public boolean isSlimefunItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(keySlimefun, PersistentDataType.STRING)) return true;

        // Inspección de respaldo por Slimefun Registry si está activo en runtime
        try {
            Class<?> sfItemClass = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
            var getByItemMethod = sfItemClass.getMethod("getByItem", ItemStack.class);
            Object sfItem = getByItemMethod.invoke(null, item);
            if (sfItem != null) return true;
        } catch (Throwable ignored) {
            // Slimefun no presente o clase no cargada
        }

        return false;
    }

    /**
     * Valida y sanea un ítem según la modalidad donde se encuentra el jugador.
     * Retorna true si el ítem era ilegal y fue destruido/purificado.
     */
    public boolean sanitizeItem(ItemStack item, String modalityId, Player player) {
        if (item == null || item.getType().isAir()) return false;

        // 1. Detección de Ítem de Laboratorio fuera de Laboratorio
        if (!"laboratorio".equalsIgnoreCase(modalityId) && isSandboxItem(item)) {
            item.setAmount(0);
            return true;
        }

        // 2. Detección de Ítem de Slimefun en Clásico Vanilla
        if ("clasico".equalsIgnoreCase(modalityId) && isSlimefunItem(item)) {
            item.setAmount(0);
            return true;
        }

        // 3. Inspección profunda de Shulker Boxes (Anti-Contrabando en contenedores)
        if (item.getItemMeta() instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox shulker) {
            boolean shulkerModified = false;
            ItemStack[] shulkerContents = shulker.getInventory().getContents();
            for (int i = 0; i < shulkerContents.length; i++) {
                ItemStack inner = shulkerContents[i];
                if (inner != null && !inner.getType().isAir()) {
                    if (!"laboratorio".equalsIgnoreCase(modalityId) && isSandboxItem(inner)) {
                        shulkerContents[i] = null;
                        shulkerModified = true;
                    } else if ("clasico".equalsIgnoreCase(modalityId) && isSlimefunItem(inner)) {
                        shulkerContents[i] = null;
                        shulkerModified = true;
                    }
                }
            }
            if (shulkerModified) {
                shulker.getInventory().setContents(shulkerContents);
                bsm.setBlockState(shulker);
                item.setItemMeta(bsm);
                return true;
            }
        }

        return false;
    }

    // =========================================================================
    // FASE 1: MARCADO ACTIVO DENTRO DEL LABORATORIO
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClickInSandbox(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isSandbox(player.getLocation())) return;

        if (event.getCurrentItem() != null) tagSandboxItem(event.getCurrentItem(), player.getUniqueId());
        if (event.getCursor() != null) tagSandboxItem(event.getCursor(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupInSandbox(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isSandbox(player.getLocation())) return;

        tagSandboxItem(event.getItem().getItemStack(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropInSandbox(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isSandbox(player.getLocation())) return;

        tagSandboxItem(event.getItemDrop().getItemStack(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftInSandbox(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isSandbox(player.getLocation())) return;

        if (event.getCurrentItem() != null) tagSandboxItem(event.getCurrentItem(), player.getUniqueId());
    }

    // =========================================================================
    // FASE 2: INSPECCIÓN, PURGA Y AUDITORÍA UNIVERSAL
    // =========================================================================

    public void inspectAndSanitize(Player player) {
        Modality modality = plugin.getModalityService().resolve(player);
        String modalityId = modality.id();
        String worldName = player.getWorld().getName();

        boolean sandboxViolacion = false;
        boolean slimefunEnClasico = false;

        // Escanear inventario principal
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                if (!"laboratorio".equalsIgnoreCase(modalityId) && isSandboxItem(item)) {
                    sandboxViolacion = true;
                    player.getInventory().setItem(i, null);
                } else if ("clasico".equalsIgnoreCase(modalityId) && isSlimefunItem(item)) {
                    slimefunEnClasico = true;
                    player.getInventory().setItem(i, null);
                } else {
                    sanitizeItem(item, modalityId, player);
                }
            }
        }

        // Escanear armadura
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item != null && !item.getType().isAir()) {
                if (!"laboratorio".equalsIgnoreCase(modalityId) && isSandboxItem(item)) {
                    sandboxViolacion = true;
                    armor[i] = null;
                } else if ("clasico".equalsIgnoreCase(modalityId) && isSlimefunItem(item)) {
                    slimefunEnClasico = true;
                    armor[i] = null;
                }
            }
        }
        player.getInventory().setArmorContents(armor);

        // Escanear offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) {
            if (!"laboratorio".equalsIgnoreCase(modalityId) && isSandboxItem(offhand)) {
                sandboxViolacion = true;
                player.getInventory().setItemInOffHand(null);
            } else if ("clasico".equalsIgnoreCase(modalityId) && isSlimefunItem(offhand)) {
                slimefunEnClasico = true;
                player.getInventory().setItemInOffHand(null);
            }
        }

        // Reacción 1: Violación del Laboratorio (Crítica)
        if (sandboxViolacion) {
            plugin.getLogger().log(Level.SEVERE,
                    "[SENTINEL CRÍTICO] Violación de Aislamiento: " + player.getName()
                            + " detectado con ítem del Laboratorio en mundo " + worldName + " (" + modalityId + ")! Ítem incinerado.");

            String alertaStaff = ChatColor.translateAlternateColorCodes('&',
                    "&4&l[SENTINEL ALERTA] &c" + player.getName()
                            + " &7intentó ingresar ítem del Laboratorio a &e" + worldName + "&7. Ítem incinerado.");
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.isOp() || staff.hasPermission("odysseia.admin") || staff.hasPermission("odysseia.alerts")) {
                    staff.sendMessage(alertaStaff);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                        "&c[DrakesCraft - Seguridad]\n\n&eSe detectó un ítem no autorizado del Laboratorio en tu inventario.\n"
                                + "&7El ítem ha sido destruido para proteger la economía del servidor.\n"
                                + "&fSi crees que esto fue un fallo de red, vuelve a ingresar."));
            });
        }

        // Reacción 2: Violación de Slimefun en Clásico
        if (slimefunEnClasico) {
            plugin.getLogger().log(Level.WARNING,
                    "[SENTINEL] Purga de Slimefun en Clásico: " + player.getName()
                            + " tenía ítem de Slimefun en Clásico (" + worldName + "). Ítem retirado.");

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &c[Clásico] En esta modalidad no se permiten ítems tecnológicos de Slimefun. "
                            + "&7El ítem ha sido purgado."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        inspectAndSanitize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        inspectAndSanitize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Modality modality = plugin.getModalityService().resolve(player);
        if (sanitizeItem(event.getItem(), modality.id(), player)) {
            event.setCancelled(true);
            inspectAndSanitize(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Modality modality = plugin.getModalityService().resolve(player);
        if (sanitizeItem(event.getItemInHand(), modality.id(), player)) {
            event.setCancelled(true);
            inspectAndSanitize(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Modality modality = plugin.getModalityService().resolve(player);

        boolean currentPurged = sanitizeItem(event.getCurrentItem(), modality.id(), player);
        boolean cursorPurged = sanitizeItem(event.getCursor(), modality.id(), player);

        if (currentPurged || cursorPurged) {
            event.setCancelled(true);
            inspectAndSanitize(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Modality modality = plugin.getModalityService().resolve(player);

        if (sanitizeItem(event.getItemDrop().getItemStack(), modality.id(), player)) {
            event.getItemDrop().remove();
            inspectAndSanitize(player);
        }
    }
}
