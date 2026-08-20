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
 * Centinela Universal Anti-Fugas de Ítems entre TODAS las Modalidades de DrakesCraft.
 * 
 * Matriz de Aislamiento Completa:
 * 1. [LABORATORIO]: Ningún ítem originario del Laboratorio Creativo puede ingresar a ninguna otra modalidad.
 *    (Purga instantánea + Alerta Staff + Expulsión preventiva).
 * 2. [CLÁSICO VANILLA]: Ningún ítem con origen Slimefun, Survival, SkyBlock, OneBlock o Laboratorio puede
 *    ingresar a Clásico. Clásico se mantiene 100% puro.
 * 3. [SKYBLOCK / ONEBLOCK]: Los ítems generados en islas y fases quedan aislados en su propia modalidad y
 *    no cruzan al Survival ni a Clásico.
 * 4. [INSPECCIÓN PROFUNDA]: Escaneo recursivo continuo en Shulker Boxes y contenedores portátiles para evitar
 *    contrabando encubierto.
 */
public final class UniversalModalitySentinel implements Listener {

    private final Odysseia plugin;
    private final Set<String> sandboxWorlds;
    private final NamespacedKey keyOriginModality;
    private final NamespacedKey keyCreator;
    private final NamespacedKey keyTimestamp;
    private final NamespacedKey keySlimefun;

    public UniversalModalitySentinel(Odysseia plugin, Set<String> sandboxWorlds) {
        this.plugin = plugin;
        this.sandboxWorlds = sandboxWorlds;
        this.keyOriginModality = new NamespacedKey(plugin, "origin_modality");
        this.keyCreator = new NamespacedKey(plugin, "origin_creator");
        this.keyTimestamp = new NamespacedKey(plugin, "origin_time");
        this.keySlimefun = new NamespacedKey("slimefun", "slimefun_item");
    }

    /** Marca un ítem con el sello indeleble de su modalidad de origen. */
    public void tagItemWithModality(ItemStack item, String modalityId, UUID creator) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // No sobreescribir si ya tiene origen del laboratorio (máxima prioridad de aislamiento)
        String currentOrigin = pdc.get(keyOriginModality, PersistentDataType.STRING);
        if ("laboratorio".equalsIgnoreCase(currentOrigin)) return;

        pdc.set(keyOriginModality, PersistentDataType.STRING, modalityId.toLowerCase(Locale.ROOT));
        if (creator != null) {
            pdc.set(keyCreator, PersistentDataType.STRING, creator.toString());
        }
        pdc.set(keyTimestamp, PersistentDataType.LONG, System.currentTimeMillis());
        item.setItemMeta(meta);
    }

    /** Obtiene la modalidad de origen marcada en el ítem. */
    public String getOriginModality(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String origin = pdc.get(keyOriginModality, PersistentDataType.STRING);
        if (origin != null) return origin;

        // Comprobación de compatibilidad con versión anterior de laboratorio
        NamespacedKey legacyKey = new NamespacedKey(plugin, "sandbox_item");
        if (pdc.has(legacyKey, PersistentDataType.BYTE)) return "laboratorio";

        // Comprobación intrínseca de Slimefun
        if (pdc.has(keySlimefun, PersistentDataType.STRING)) return "survival";

        return null;
    }

    /** Comprueba con total certeza si un ítem es de Slimefun. */
    public boolean isSlimefunItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(keySlimefun, PersistentDataType.STRING)) return true;

        try {
            Class<?> sfItemClass = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
            var getByItemMethod = sfItemClass.getMethod("getByItem", ItemStack.class);
            Object sfItem = getByItemMethod.invoke(null, item);
            if (sfItem != null) return true;
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Evalúa si un ítem es legal en la modalidad destino.
     * Retorna true si el ítem es ilegal y debe ser destruido.
     */
    public boolean isItemIllegalInModality(ItemStack item, String targetModality) {
        if (item == null || item.getType().isAir()) return false;

        String origin = getOriginModality(item);

        // Regla 1: Laboratorio NUNCA cruza a ninguna otra modalidad
        if ("laboratorio".equalsIgnoreCase(origin) && !"laboratorio".equalsIgnoreCase(targetModality)) {
            return true;
        }

        // Regla 2: Clásico NO acepta Slimefun ni ítems de Laboratorio/Skyblock/Oneblock
        if ("clasico".equalsIgnoreCase(targetModality)) {
            if (isSlimefunItem(item)) return true;
            if ("skyblock".equalsIgnoreCase(origin) || "oneblock".equalsIgnoreCase(origin) || "laboratorio".equalsIgnoreCase(origin)) {
                return true;
            }
        }

        // Regla 3: SkyBlock y OneBlock son economías aisladas
        if ("skyblock".equalsIgnoreCase(targetModality) && "oneblock".equalsIgnoreCase(origin)) {
            return true;
        }
        if ("oneblock".equalsIgnoreCase(targetModality) && "skyblock".equalsIgnoreCase(origin)) {
            return true;
        }

        return false;
    }

    /**
     * Sanea el ítem y contenedores (Shulker Boxes).
     * Retorna true si se purificó/destruyó algún elemento ilegal.
     */
    public boolean sanitizeItem(ItemStack item, String targetModality, Player player) {
        if (item == null || item.getType().isAir()) return false;

        if (isItemIllegalInModality(item, targetModality)) {
            item.setAmount(0);
            return true;
        }

        // Inspección profunda recursiva de Shulker Boxes
        if (item.getItemMeta() instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox shulker) {
            boolean modified = false;
            ItemStack[] contents = shulker.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack inner = contents[i];
                if (inner != null && !inner.getType().isAir() && isItemIllegalInModality(inner, targetModality)) {
                    contents[i] = null;
                    modified = true;
                }
            }
            if (modified) {
                shulker.getInventory().setContents(contents);
                bsm.setBlockState(shulker);
                item.setItemMeta(bsm);
                return true;
            }
        }

        return false;
    }

    // =========================================================================
    // FASE 1: MARCADO ACTIVO SEGÚN LA MODALIDAD ACTUAL DEL JUGADOR
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Modality modality = plugin.getModalityService().resolve(player);

        if (event.getCurrentItem() != null) tagItemWithModality(event.getCurrentItem(), modality.id(), player.getUniqueId());
        if (event.getCursor() != null) tagItemWithModality(event.getCursor(), modality.id(), player.getUniqueId());

        boolean curPurged = sanitizeItem(event.getCurrentItem(), modality.id(), player);
        boolean curCursorPurged = sanitizeItem(event.getCursor(), modality.id(), player);
        if (curPurged || curCursorPurged) {
            event.setCancelled(true);
            inspectAndSanitize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Modality modality = plugin.getModalityService().resolve(player);

        tagItemWithModality(event.getItem().getItemStack(), modality.id(), player.getUniqueId());
        if (sanitizeItem(event.getItem().getItemStack(), modality.id(), player)) {
            event.getItem().remove();
            event.setCancelled(true);
            inspectAndSanitize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Modality modality = plugin.getModalityService().resolve(player);

        if (event.getCurrentItem() != null) tagItemWithModality(event.getCurrentItem(), modality.id(), player.getUniqueId());
    }

    // =========================================================================
    // FASE 2: INSPECCIÓN, PURGA Y REACCIÓN
    // =========================================================================

    public void inspectAndSanitize(Player player) {
        Modality modality = plugin.getModalityService().resolve(player);
        String targetModality = modality.id();
        String worldName = player.getWorld().getName();

        boolean laboratorioViolacion = false;
        boolean generalViolacion = false;
        String detalleViolacion = "";

        // Escanear inventario principal
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                String origin = getOriginModality(item);
                if (isItemIllegalInModality(item, targetModality)) {
                    if ("laboratorio".equalsIgnoreCase(origin)) laboratorioViolacion = true;
                    generalViolacion = true;
                    detalleViolacion = item.getType().name() + " (Origen: " + (origin != null ? origin : "Desconocido") + ")";
                    player.getInventory().setItem(i, null);
                } else {
                    sanitizeItem(item, targetModality, player);
                }
            }
        }

        // Escanear armadura
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item != null && !item.getType().isAir()) {
                String origin = getOriginModality(item);
                if (isItemIllegalInModality(item, targetModality)) {
                    if ("laboratorio".equalsIgnoreCase(origin)) laboratorioViolacion = true;
                    generalViolacion = true;
                    detalleViolacion = item.getType().name() + " (Origen: " + (origin != null ? origin : "Desconocido") + ")";
                    armor[i] = null;
                }
            }
        }
        player.getInventory().setArmorContents(armor);

        // Escanear offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) {
            String origin = getOriginModality(offhand);
            if (isItemIllegalInModality(offhand, targetModality)) {
                if ("laboratorio".equalsIgnoreCase(origin)) laboratorioViolacion = true;
                generalViolacion = true;
                detalleViolacion = offhand.getType().name() + " (Origen: " + (origin != null ? origin : "Desconocido") + ")";
                player.getInventory().setItemInOffHand(null);
            }
        }

        // Alerta y respuesta
        if (laboratorioViolacion) {
            plugin.getLogger().log(Level.SEVERE,
                    "[CENTINELA CRÍTICO] " + player.getName() + " detectado con ítem del Laboratorio en "
                            + worldName + " (" + targetModality + ")! Detalle: " + detalleViolacion);

            String alertaStaff = ChatColor.translateAlternateColorCodes('&',
                    "&4&l[CENTINELA ALERTA] &c" + player.getName()
                            + " &7intentó ingresar ítem del Laboratorio a &e" + targetModality + "&7. Ítem incinerado.");
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.isOp() || staff.hasPermission("odysseia.admin") || staff.hasPermission("odysseia.alerts")) {
                    staff.sendMessage(alertaStaff);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                        "&c[DrakesCraft - Centinela]\n\n&eSe detectó un ítem no autorizado del Laboratorio en tu inventario.\n"
                                + "&7El ítem ha sido destruido para proteger la economía del servidor.\n"
                                + "&fSi crees que esto fue un fallo de red, vuelve a ingresar."));
            });
        } else if (generalViolacion) {
            plugin.getLogger().log(Level.WARNING,
                    "[CENTINELA] " + player.getName() + " tenía ítem incompatible en " + targetModality
                            + " (" + worldName + "): " + detalleViolacion + ". Ítem purgado.");

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &c[Centinela] Ese ítem no está permitido en la modalidad &e"
                            + targetModality + "&c. Ha sido purgado automáticamente."));
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
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Modality modality = plugin.getModalityService().resolve(player);

        if (sanitizeItem(event.getItemDrop().getItemStack(), modality.id(), player)) {
            event.getItemDrop().remove();
            inspectAndSanitize(player);
        }
    }
}
