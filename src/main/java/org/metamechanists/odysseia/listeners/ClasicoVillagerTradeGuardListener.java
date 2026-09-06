package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.metamechanists.odysseia.modalities.ModalityService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Blindaje y sanitización activa de aldeanos y vendedores ambulantes en la modalidad Clásico.
 *
 * <p>En Clásico (clasico, clasico_nether, clasico_the_end) el juego debe mantenerse estrictamente
 * vainilla. Algunos addons de Slimefun (como Cultivation, SlimeFrame o VillagerTrade) inyectan
 * recetas comerciales en aldeanos o vendedores ambulantes sin comprobar si el mundo tiene
 * Slimefun activo, y esas ofertas quedan además fijadas en el NBT de las entidades.</p>
 *
 * <p>Este listener:
 * <ul>
 *   <li>Cancela la adquisición de nuevas ofertas comerciales con ítems de Slimefun en Clásico.</li>
 *   <li>Higieniza en tiempo real las recetas de cualquier aldeano o comerciante cuando un jugador
 *       interactúa con él o abre su menú, eliminando únicamente las recetas de Slimefun sin alterar
 *       los intercambios vainilla (reparación/mending, esmeraldas, libreros, etc.).</li>
 *   <li>Limpia de forma asíncrona (1-3 ticks) comerciantes recién aparecidos o aldeanos que hayan
 *       cambiado de profesión.</li>
 * </ul>
 * </p>
 */
public final class ClasicoVillagerTradeGuardListener implements Listener {

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

    public ClasicoVillagerTradeGuardListener(Plugin plugin, ModalityService modalityService) {
        this.plugin = plugin;
        this.modalityService = modalityService;
        initSlimefunReflection();
    }

    private void initSlimefunReflection() {
        for (String base : List.of("com.github.drakescraft_labs.slimefun4", "io.github.thebusybiscuit.slimefun4")) {
            try {
                Class<?> itemClass = Class.forName(base + ".api.items.SlimefunItem");
                this.slimefunGetByItem = itemClass.getMethod("getByItem", ItemStack.class);
                return;
            } catch (ReflectiveOperationException ignored) {
                this.slimefunGetByItem = null;
            }
        }
    }

    /**
     * Determina si el mundo pertenece a la modalidad Clásico.
     */
    public boolean isClasico(World world) {
        if (world == null) return false;
        String name = world.getName().toLowerCase(Locale.ROOT);
        if (name.startsWith("clasico")) return true;
        if (modalityService != null) {
            return "clasico".equalsIgnoreCase(modalityService.resolve(name).id());
        }
        return false;
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

        // 1. Verificación por API de Slimefun (cubre ítems del core y todos los addons registrados)
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
            // 2. Verificación de PersistentDataContainer
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (NamespacedKey key : pdc.getKeys()) {
                String ns = key.getNamespace().toLowerCase(Locale.ROOT);
                if (SLIMEFUN_NAMESPACES.contains(ns) || ns.startsWith("slimefun")) {
                    return true;
                }
            }

            // 3. Verificación de Lore (patrones característicos de Slimefun / Cultivation)
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

    /**
     * Comprueba si una receta de comercio produce o requiere un ítem de Slimefun.
     */
    public boolean isSlimefunTrade(MerchantRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        if (isSlimefunOrCustomItem(recipe.getResult())) {
            return true;
        }
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (isSlimefunOrCustomItem(ingredient)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sanea la lista de recetas de un comerciante, purgando ofertas de Slimefun sin alterar
     * los intercambios vanilla legítimos.
     *
     * @return true si se removió al menos una receta no autorizada.
     */
    public boolean sanitizeRecipes(Merchant trader) {
        if (trader == null) {
            return false;
        }
        List<MerchantRecipe> recipes = trader.getRecipes();
        if (recipes == null || recipes.isEmpty()) {
            return false;
        }
        List<MerchantRecipe> cleanRecipes = new ArrayList<>(recipes);
        boolean modified = cleanRecipes.removeIf(this::isSlimefunTrade);
        if (modified) {
            trader.setRecipes(cleanRecipes);
        }
        return modified;
    }

    /**
     * Evita que un aldeano en Clásico adquiera un nuevo trade de Slimefun.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!isClasico(event.getEntity().getWorld())) {
            return;
        }
        if (isSlimefunTrade(event.getRecipe())) {
            event.setCancelled(true);
        }
    }

    /**
     * Al interactuar con un aldeano o vendedor ambulante en Clásico, purga cualquier trade corrupto
     * existente en su NBT antes de que se abra la ventana comercial.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!isClasico(event.getPlayer().getWorld())) {
            return;
        }
        if (event.getRightClicked() instanceof AbstractVillager trader) {
            if (sanitizeRecipes(trader)) {
                plugin.getLogger().info("[ClasicoTradeGuard] Purga de trade Slimefun ejecutada en aldeano ("
                        + trader.getLocation().getBlockX() + ", "
                        + trader.getLocation().getBlockY() + ", "
                        + trader.getLocation().getBlockZ() + ") en "
                        + trader.getWorld().getName());
            }
        }
    }

    /**
     * Barrera secundaria al abrir cualquier inventario de comerciante.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isClasico(event.getPlayer().getWorld())) {
            return;
        }
        if (event.getInventory() instanceof MerchantInventory merchantInventory) {
            Merchant merchant = merchantInventory.getMerchant();
            if (merchant != null) {
                sanitizeRecipes(merchant);
            }
        }
    }

    /**
     * Higieniza vendedores ambulantes u otros aldeanos tras su aparición.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!isClasico(event.getLocation().getWorld())) {
            return;
        }
        if (event.getEntity() instanceof AbstractVillager trader) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (trader.isValid()) {
                    sanitizeRecipes(trader);
                }
            }, 2L);
        }
    }

    /**
     * Higieniza aldeanos que acaban de cambiar de profesión cuando los plugins de addons
     * programan sus inyecciones en el tick siguiente.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerCareerChange(VillagerCareerChangeEvent event) {
        if (!isClasico(event.getEntity().getWorld())) {
            return;
        }
        Villager villager = event.getEntity();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (villager.isValid()) {
                sanitizeRecipes(villager);
            }
        }, 3L);
    }
}
