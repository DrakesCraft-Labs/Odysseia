package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
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
            "slimytreetaps"
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
                if (SLIMEFUN_NAMESPACES.contains(ns) || ns.startsWith("slimefun")) {
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
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &cLos bloques y objetos de Slimefun no están permitidos en Clásico."));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isClasico(event.getPlayer().getWorld())) return;
        ItemStack item = event.getItem();
        if (isSlimefunOrCustomItem(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &cLas herramientas y objetos de Slimefun no están permitidos en Clásico."));
        }
    }
}
