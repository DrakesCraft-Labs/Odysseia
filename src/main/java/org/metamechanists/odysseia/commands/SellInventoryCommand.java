package org.metamechanists.odysseia.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Permite a los jugadores vender de forma rápida y 100% segura los ítems de recursos
 * de su inventario principal sin arriesgar herramientas, armaduras o ítems de Slimefun.
 */
public final class SellInventoryCommand implements CommandExecutor {

    private final Odysseia plugin;
    private final Map<Material, Double> sellPrices = new EnumMap<>(Material.class);
    private Method slimefunGetByItemMethod;
    private boolean reflectionInitialized = false;

    public SellInventoryCommand(Odysseia plugin) {
        this.plugin = plugin;
        loadDefaultSellPrices();
        initReflection();
    }

    private void initReflection() {
        try {
            Class<?> sfItemClass = Class.forName("com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem");
            this.slimefunGetByItemMethod = sfItemClass.getMethod("getByItem", ItemStack.class);
            this.reflectionInitialized = true;
        } catch (Throwable ignored) {
            this.reflectionInitialized = false;
        }
    }

    private void loadDefaultSellPrices() {
        // Bloques de construcción & recursos básicos
        sellPrices.put(Material.COBBLESTONE, 0.25);
        sellPrices.put(Material.STONE, 0.50);
        sellPrices.put(Material.DEEPSLATE, 0.50);
        sellPrices.put(Material.COBBLED_DEEPSLATE, 0.30);
        sellPrices.put(Material.DIRT, 0.10);
        sellPrices.put(Material.SAND, 0.25);
        sellPrices.put(Material.GRAVEL, 0.25);
        sellPrices.put(Material.GRANITE, 0.40);
        sellPrices.put(Material.DIORITE, 0.40);
        sellPrices.put(Material.ANDESITE, 0.40);
        sellPrices.put(Material.NETHERRACK, 0.20);
        sellPrices.put(Material.BASALT, 0.35);
        sellPrices.put(Material.BLACKSTONE, 0.40);

        // Maderas (Logs)
        sellPrices.put(Material.OAK_LOG, 1.50);
        sellPrices.put(Material.SPRUCE_LOG, 1.50);
        sellPrices.put(Material.BIRCH_LOG, 1.50);
        sellPrices.put(Material.JUNGLE_LOG, 1.50);
        sellPrices.put(Material.ACACIA_LOG, 1.50);
        sellPrices.put(Material.DARK_OAK_LOG, 1.50);
        sellPrices.put(Material.MANGROVE_LOG, 1.50);
        sellPrices.put(Material.CHERRY_LOG, 1.50);
        sellPrices.put(Material.CRIMSON_STEM, 1.80);
        sellPrices.put(Material.WARPED_STEM, 1.80);

        // Minerales y lingotes quedan deliberadamente fuera: Slimefun permite
        // automatizarlos y convertirlos en una fuente ilimitada de Dragmas.

        // Cultivos & agricultura
        sellPrices.put(Material.WHEAT, 1.00);
        sellPrices.put(Material.CARROT, 1.00);
        sellPrices.put(Material.POTATO, 1.00);
        sellPrices.put(Material.BEETROOT, 1.00);
        sellPrices.put(Material.SUGAR_CANE, 1.00);
        sellPrices.put(Material.PUMPKIN, 2.00);
        sellPrices.put(Material.MELON_SLICE, 0.50);
        sellPrices.put(Material.SWEET_BERRIES, 0.80);
        sellPrices.put(Material.GLOW_BERRIES, 1.00);
        sellPrices.put(Material.BAMBOO, 0.50);
        sellPrices.put(Material.CACTUS, 1.00);
        sellPrices.put(Material.NETHER_WART, 1.50);

        // Drops de Mobs
        sellPrices.put(Material.ROTTEN_FLESH, 0.50);
        sellPrices.put(Material.BONE, 1.00);
        sellPrices.put(Material.STRING, 1.00);
        sellPrices.put(Material.SPIDER_EYE, 1.50);
        sellPrices.put(Material.GUNPOWDER, 3.00);
        sellPrices.put(Material.ENDER_PEARL, 5.00);
        sellPrices.put(Material.BLAZE_ROD, 8.00);
        sellPrices.put(Material.SLIME_BALL, 4.00);
        sellPrices.put(Material.FEATHER, 0.50);
        sellPrices.put(Material.LEATHER, 2.00);
        sellPrices.put(Material.PRISMARINE_SHARD, 2.50);
        sellPrices.put(Material.PRISMARINE_CRYSTALS, 3.50);
    }

    private boolean isSlimefunItem(ItemStack item) {
        if (!reflectionInitialized || slimefunGetByItemMethod == null || item == null) {
            return false;
        }
        try {
            return slimefunGetByItemMethod.invoke(null, item) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Visible para pruebas de regresión de la política económica. */
    boolean isSellable(Material material) {
        return sellPrices.containsKey(material);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            player.sendMessage(ChatColor.RED + "⚠ El sistema de economía (Vault) no está disponible en este momento.");
            return true;
        }

        Economy economy = rsp.getProvider();
        double totalEarnings = 0.0;
        int totalItemsSold = 0;

        // Escanear ÚNICAMENTE el inventario principal (ranuras 0 a 35). Omitir armadura y offhand.
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            // BLINDAJE DE SEGURIDAD 1: Nunca vender ítems con metadatos personalizados o encantamientos
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && (meta.hasDisplayName() || meta.hasLore() || meta.hasEnchants()
                        || meta.hasCustomModelData() || !meta.getPersistentDataContainer().isEmpty())) {
                    continue;
                }
            }

            // BLINDAJE DE SEGURIDAD 2: Nunca vender ningún ítem proveniente de Slimefun
            if (isSlimefunItem(item)) {
                continue;
            }

            // BLINDAJE DE SEGURIDAD 3: Verificar si el material tiene precio asignado
            Material type = item.getType();
            if (!sellPrices.containsKey(type)) {
                continue;
            }

            double unitPrice = sellPrices.get(type);
            int amount = item.getAmount();
            double earnings = unitPrice * amount;

            totalEarnings += earnings;
            totalItemsSold += amount;

            // Eliminar ítem del inventario
            player.getInventory().setItem(i, null);
        }

        if (totalItemsSold == 0 || totalEarnings <= 0) {
            player.sendMessage(ChatColor.YELLOW + "💡 No se encontraron recursos vendibles en tu inventario principal.");
            player.sendMessage(ChatColor.GRAY + "Pista: /sellinv solo vende recursos y materiales sin encantamientos ni ítems custom.");
            return true;
        }

        // Acreditar dinero
        economy.depositPlayer(player, totalEarnings);

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "💰 " + ChatColor.BOLD + "¡VENTA DE INVENTARIO COMPLETADA!");
        player.sendMessage(ChatColor.GREEN + "✔ Vendidos: " + ChatColor.WHITE + totalItemsSold + " recursos de supervivencia.");
        player.sendMessage(ChatColor.YELLOW + "💵 Total acreditado: " + ChatColor.GOLD + "+" + String.format("%.2f", totalEarnings) + " Dragmas");
        player.sendMessage("");

        return true;
    }
}
