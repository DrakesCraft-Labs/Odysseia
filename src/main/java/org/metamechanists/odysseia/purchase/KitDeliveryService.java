package org.metamechanists.odysseia.purchase;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.metamechanists.odysseia.Odysseia;

import java.util.*;

/** Construye kits desde la configuración activa sin ejecutar dinero ni comandos embebidos. */
public final class KitDeliveryService {
    private final Odysseia plugin;
    private final NamespacedKey transactionKey;

    public KitDeliveryService(Odysseia plugin) {
        this.plugin = plugin;
        this.transactionKey = new NamespacedKey(plugin, "purchase_transaction");
    }

    public ActionResult deliver(Player player, String kitName, String transaction) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("kits." + kitName);
        if (section == null) return ActionResult.manual("Kit ausente en configuración: " + kitName);
        if (containsTransaction(player, transaction)) return ActionResult.manual("Se detectaron ítems de esta transacción; requiere reconciliación");
        List<ItemStack> items = new ArrayList<>();
        for (Map<?, ?> values : section.getMapList("vanilla-items")) {
            List<ItemStack> stacks = createItems(values, transaction);
            if (stacks == null) return ActionResult.manual("Item inválido en kit " + kitName + ": " + values.get("material"));
            items.addAll(stacks);
        }
        if (!fits(player.getInventory(), items)) return ActionResult.waiting("Inventario sin espacio suficiente");
        for (ItemStack item : items) player.getInventory().addItem(item);
        return ActionResult.completed("kit=" + kitName + ";items=" + items.size());
    }

    private List<ItemStack> createItems(Map<?, ?> values, String transaction) {
        Material material = Material.matchMaterial(String.valueOf(values.get("material")));
        if (material == null) return null;
        int amount = integer(values.get("amount"), 1);
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return List.of(item);
        Object name = values.get("name");
        if (name != null) meta.setDisplayName(color(String.valueOf(name)));
        Object loreValue = values.get("lore");
        if (loreValue instanceof List<?> lore) meta.setLore(lore.stream().map(value -> color(String.valueOf(value))).toList());
        Object enchantments = values.get("enchantments");
        if (enchantments instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT)));
                if (enchantment != null) meta.addEnchant(enchantment, integer(entry.getValue(), 1), true);
            }
        }
        if (Boolean.parseBoolean(String.valueOf(values.containsKey("soulbound") ? values.get("soulbound") : false))) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "soulbound"), PersistentDataType.BYTE, (byte) 1);
        }
        meta.getPersistentDataContainer().set(transactionKey, PersistentDataType.STRING, transaction);
        item.setItemMeta(meta);
        List<ItemStack> stacks = new ArrayList<>();
        for (int remaining = Math.max(1, amount); remaining > 0; remaining -= material.getMaxStackSize()) {
            ItemStack stack = item.clone();
            stack.setAmount(Math.min(remaining, material.getMaxStackSize()));
            stacks.add(stack);
        }
        return stacks;
    }

    private boolean containsTransaction(Player player, String transaction) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            if (transaction.equals(item.getItemMeta().getPersistentDataContainer().get(transactionKey, PersistentDataType.STRING))) return true;
        }
        return false;
    }

    private boolean fits(PlayerInventory inventory, List<ItemStack> items) {
        ItemStack[] snapshot = inventory.getStorageContents();
        int free = 0;
        for (ItemStack item : snapshot) if (item == null || item.getType().isAir()) free++;
        return free >= items.size();
    }
    private int integer(Object value, int fallback) { try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { return fallback; } }
    private String color(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
}
