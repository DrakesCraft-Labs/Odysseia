package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.metamechanists.odysseia.modalities.ModalityService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Blindaje activo de jugabilidad vainilla en la modalidad Clásico contra Slimefun y sus addons.
 *
 * <p>En Clásico (clasico, clasico_nether, clasico_the_end) el juego debe mantenerse 100% vainilla.
 * Este listener actúa como salvaguarda integral del servidor:
 * <ul>
 *   <li>Filtra y elimina cualquier drop de ítems de Slimefun o addons al morir entidades en Clásico.</li>
 *   <li>Limpia metadatos residuales en BlockStorage al romper bloques en Clásico para evitar que
 *       máquinas fantasmas o Lucky Blocks activen efectos en el mundo vainilla.</li>
 *   <li>Impide la colocación y el uso interactivo de ítems/herramientas de Slimefun en Clásico.</li>
 *   <li>Purga automáticamente cualquier ítem de Slimefun del inventario, armadura y manos al entrar o interactuar en Clásico.</li>
 * </ul>
 * </p>
 */
public final class ClasicoSlimefunGuardListener implements Listener {

    private static final Set<String> SLIMEFUN_NAMESPACES = Set.of(
            "slimefun",
            "cultivation",
            "slimeframe",
            "dynatech",
            "exoticgarden",
            "alchimiavitae",
            "infinityexpansion",
            "fluffymachines",
            "sensibletoolbox",
            "extraweapons",
            "villagertrade",
            "sfcalc",
            "slimybees",
            "slimytreetaps",
            "equivalencytech"
    );

    private final Plugin plugin;
    private final ModalityService modalityService;
    private Method slimefunGetByItem;
    private Method blockStorageHasBlockInfo;
    private Method blockStorageClearBlockInfo;

    public ClasicoSlimefunGuardListener(Plugin plugin, ModalityService modalityService) {
        this.plugin = plugin;
        this.modalityService = modalityService;
        initSlimefunReflection();
    }

    private void initSlimefunReflection() {
        for (String base : List.of("com.github.drakescraft_labs.slimefun4", "io.github.thebusybiscuit.slimefun4")) {
            try {
                Class<?> itemClass = Class.forName(base + ".api.items.SlimefunItem");
                this.slimefunGetByItem = itemClass.getMethod("getByItem", ItemStack.class);
            } catch (ReflectiveOperationException ignored) {
            }

            for (String bsClass : List.of(base + ".legacy.api.BlockStorage", base + ".api.BlockStorage")) {
                try {
                    Class<?> storageClass = Class.forName(bsClass);
                    this.blockStorageHasBlockInfo = storageClass.getMethod("hasBlockInfo", Block.class);
                    this.blockStorageClearBlockInfo = storageClass.getMethod("clearBlockInfo", Block.class);
                    break;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
    }

    public boolean isClasicoName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.startsWith("clasico")) return true;
        if (modalityService != null) {
            return "clasico".equalsIgnoreCase(modalityService.resolve(lower).id());
        }
        return false;
    }

    /**
     * Determina si el mundo pertenece a la modalidad Clásico.
     */
    public boolean isClasico(World world) {
        if (world == null) return false;
        return isClasicoName(world.getName());
    }

    /**
     * Determina si un ItemStack corresponde a un ítem de Slimefun o de sus addons.
     */
    public boolean isSlimefunOrCustomItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }

        if (slimefunGetByItem != null) {
            try {
                Object sfItem = slimefunGetByItem.invoke(null, item);
                if (sfItem != null) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (NamespacedKey key : pdc.getKeys()) {
                String ns = key.getNamespace().toLowerCase(Locale.ROOT);
                if (SLIMEFUN_NAMESPACES.contains(ns) || ns.startsWith("slimefun") || ns.startsWith("equivalency")) {
                    return true;
                }
            }

            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    for (String line : lore) {
                        if (line == null) continue;
                        String stripped = ChatColor.stripColor(line).toLowerCase(Locale.ROOT);
                        if (stripped.contains("slimefun")
                                || stripped.contains("cultivation")
                                || stripped.contains("equivalency")
                                || stripped.startsWith("id: ")
                                || stripped.contains("generado por sfmaster")) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Purga activamente todos los ítems de Slimefun o addons del inventario del jugador.
     */
    public int purgeSlimefunItems(Player player) {
        if (player == null || !isClasico(player.getWorld())) {
            return 0;
        }
        int removedCount = 0;
        PlayerInventory inv = player.getInventory();

        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (isSlimefunOrCustomItem(contents[i])) {
                inv.setItem(i, null);
                removedCount++;
            }
        }

        ItemStack offHand = inv.getItemInOffHand();
        if (isSlimefunOrCustomItem(offHand)) {
            inv.setItemInOffHand(null);
            removedCount++;
        }

        ItemStack[] armors = inv.getArmorContents();
        boolean armorModified = false;
        for (int i = 0; i < armors.length; i++) {
            if (isSlimefunOrCustomItem(armors[i])) {
                armors[i] = null;
                armorModified = true;
                removedCount++;
            }
        }
        if (armorModified) {
            inv.setArmorContents(armors);
        }

        if (removedCount > 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &cSe han purgado " + removedCount + " objeto(s) de Slimefun de tu inventario (prohibidos en Clásico)."));
        }
        return removedCount;
    }

    private void clearSlimefunBlockInfo(Block block) {
        if (blockStorageHasBlockInfo != null && blockStorageClearBlockInfo != null) {
            try {
                boolean hasInfo = (boolean) blockStorageHasBlockInfo.invoke(null, block);
                if (hasInfo) {
                    blockStorageClearBlockInfo.invoke(null, block);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (isClasico(player.getWorld())) {
            purgeSlimefunItems(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isClasico(player.getWorld())) {
            purgeSlimefunItems(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isClasico(event.getRespawnLocation().getWorld()) && plugin != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> purgeSlimefunItems(player));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isClasico(player.getWorld())) return;

        ItemStack current = event.getCurrentItem();
        if (isSlimefunOrCustomItem(current)) {
            event.setCancelled(true);
            event.setCurrentItem(null);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &cLos objetos de Slimefun no están permitidos en Clásico y han sido removidos."));
            return;
        }

        ItemStack cursor = event.getCursor();
        if (isSlimefunOrCustomItem(cursor)) {
            event.setCancelled(true);
            event.getView().setCursor(null);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &cLos objetos de Slimefun no están permitidos en Clásico y han sido removidos."));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!isClasico(event.getEntity().getWorld())) return;
        if (isSlimefunOrCustomItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!isClasico(event.getPlayer().getWorld())) return;
        if (isSlimefunOrCustomItem(event.getItemDrop().getItemStack())) {
            event.getItemDrop().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isClasico(event.getEntity().getWorld())) return;
        event.getDrops().removeIf(this::isSlimefunOrCustomItem);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isClasico(event.getBlock().getWorld())) return;
        clearSlimefunBlockInfo(event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isClasico(event.getBlock().getWorld())) return;
        if (isSlimefunOrCustomItem(event.getItemInHand())) {
            event.setCancelled(true);
            purgeSlimefunItems(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isClasico(event.getPlayer().getWorld())) return;
        ItemStack item = event.getItem();
        if (isSlimefunOrCustomItem(item)) {
            event.setCancelled(true);
            purgeSlimefunItems(event.getPlayer());
        }
    }
}
