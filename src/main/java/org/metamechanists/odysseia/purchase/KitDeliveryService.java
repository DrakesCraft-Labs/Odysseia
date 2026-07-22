package org.metamechanists.odysseia.purchase;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
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
        ActionResult protection = deliverConfiguredProtection(player, section);
        if (protection.status() != ActionResult.Status.COMPLETED) return protection;
        for (ItemStack item : items) player.getInventory().addItem(item);
        return ActionResult.completed("kit=" + kitName + ";items=" + items.size());
    }

    /** Valida todas las entradas antes de permitir pruebas o entregas reales. */
    public List<String> validateConfiguration() {
        List<String> errors = new ArrayList<>();
        ConfigurationSection kits = plugin.getConfig().getConfigurationSection("kits");
        if (kits == null) return List.of("Falta la sección kits");
        for (String kit : kits.getKeys(false)) {
            ConfigurationSection section = kits.getConfigurationSection(kit);
            if (section == null) continue;
            String permission = section.getString("permission", "").trim();
            if (permission.isEmpty()) errors.add(kit + ": falta permiso explícito de LuckPerms");
            int index = 0;
            for (Map<?, ?> values : section.getMapList("vanilla-items")) {
                index++;
                String path = kit + ".vanilla-items[" + index + "]";
                Material material = Material.matchMaterial(String.valueOf(values.get("material")));
                if (material == null || !material.isItem()) {
                    errors.add(path + ": material inválido");
                    continue;
                }
                int amount = integer(values.get("amount"), 1);
                if (amount < 1 || amount > 2304) errors.add(path + ": cantidad fuera de 1..2304");
                if (material == Material.WRITTEN_BOOK
                        && !(values.get("pages") instanceof List<?> pages && !pages.isEmpty())) {
                    errors.add(path + ": libro sin páginas");
                }
                Object enchantments = values.get("enchantments");
                if (enchantments instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT)));
                        int level = integer(entry.getValue(), 0);
                        if (enchantment == null) errors.add(path + ": encantamiento desconocido " + entry.getKey());
                        if (level < 1 || level > 255) errors.add(path + ": nivel inválido para " + entry.getKey());
                    }
                }
            }
        }
        return errors;
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
        if (meta instanceof BookMeta bookMeta) {
            bookMeta.setTitle(color(String.valueOf(values.containsKey("book-title")
                    ? values.get("book-title") : "Guía de DrakesCraft")));
            bookMeta.setAuthor(color(String.valueOf(values.containsKey("author")
                    ? values.get("author") : "Staff DrakesCraft")));
            Object pages = values.get("pages");
            if (pages instanceof List<?> list) {
                bookMeta.setPages(list.stream()
                        .map(value -> color(String.valueOf(value).replace("\\n", "\n")))
                        .toList());
            }
        }
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

    /** Delivers only a configured ProtectionStones alias; arbitrary kit commands are never executed. */
    private ActionResult deliverConfiguredProtection(Player player, ConfigurationSection section) {
        String key = section.getString("protection-alias", "").trim();
        if (key.isEmpty()) return ActionResult.completed("no protection");

        String alias = plugin.getConfig().getString("protectionstones.aliases." + key, "").trim();
        int amount = section.getInt("protection-amount", 1);
        if (!key.matches("[A-Za-z0-9_-]+") || !alias.matches("[A-Za-z0-9_-]+") || amount < 1 || amount > 64) {
            return ActionResult.manual("Configuración de ProtectionStone inválida para el kit");
        }
        if (!player.getName().matches("[A-Za-z0-9_.]{1,16}")) {
            return ActionResult.manual("Nombre de jugador inválido para entregar la protección");
        }

        String command = "ps give " + alias + " " + player.getName() + " " + amount;
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
            return ActionResult.retryable("ProtectionStones rechazó la entrega de " + alias);
        }
        return ActionResult.completed("protection=" + alias);
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
