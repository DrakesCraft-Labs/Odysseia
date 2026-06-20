package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.metamechanists.odysseia.Odysseia;

import java.io.*;
import java.util.*;

/**
 * Entrega kit de inicio a jugadores nuevos al primer join.
 * Toda la configuración se lee de config.yml (sección starter-kit).
 * Persiste UUIDs en plugins/Odysseia/starter_kit_given.txt.
 */
public class StarterKitListener implements Listener {

    private final Odysseia plugin;
    private final Set<UUID> received = new HashSet<>();
    private final File dataFile;

    public StarterKitListener(Odysseia plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "starter_kit_given.txt");
        load();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("starter-kit.enabled", true)) return;

        Player player = event.getPlayer();
        if (received.contains(player.getUniqueId())) return;

        int delayTicks = plugin.getConfig().getInt("starter-kit.delay-ticks", 100);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            give(player);
        }, delayTicks);
    }

    private void give(Player player) {
        List<ItemStack> items = buildKit();

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        received.add(player.getUniqueId());
        save();

        // Ejecutar comandos post-kit definidos en config
        List<String> commands = plugin.getConfig().getStringList("starter-kit.commands");
        for (String cmd : commands) {
            String resolved = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }

        String msg = plugin.getConfig().getString("starter-kit.welcome-message",
                "&8[&6&lDrakesCraft&8] &eKit de inicio entregado. ¡Bienvenido a la Odisea!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    private List<ItemStack> buildKit() {
        List<ItemStack> result = new ArrayList<>();
        List<Map<?, ?>> itemDefs = plugin.getConfig().getMapList("starter-kit.items");

        for (Map<?, ?> def : itemDefs) {
            String matName = String.valueOf(def.get("material"));
            Material material = Material.matchMaterial(matName);
            if (material == null) {
                plugin.getLogger().warning("[StarterKit] Material desconocido: " + matName);
                continue;
            }

            int amount = def.containsKey("amount") ? ((Number) def.get("amount")).intValue() : 1;
            ItemStack item = new ItemStack(material, amount);

            // Enchantments
            if (def.containsKey("enchantments")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> enchants = (Map<String, Object>) def.get("enchantments");
                for (Map.Entry<String, Object> entry : enchants.entrySet()) {
                    Enchantment ench = resolveEnchantment(entry.getKey());
                    if (ench == null) {
                        plugin.getLogger().warning("[StarterKit] Encantamiento desconocido: " + entry.getKey());
                        continue;
                    }
                    int level = ((Number) entry.getValue()).intValue();
                    item.addUnsafeEnchantment(ench, level);
                }
            }

            // Display name
            if (def.containsKey("name")) {
                var meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                            String.valueOf(def.get("name"))));
                    item.setItemMeta(meta);
                }
            }

            // Lore
            if (def.containsKey("lore")) {
                @SuppressWarnings("unchecked")
                List<String> loreRaw = (List<String>) def.get("lore");
                var meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    for (String line : loreRaw) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }

            result.add(item);
        }

        return result;
    }

    /** Busca encantamiento por nombre (KEY o DISPLAY_NAME, case-insensitive). */
    private Enchantment resolveEnchantment(String name) {
        // Intento directo via NamespacedKey minecraft:xxx
        String key = name.toLowerCase().replace(" ", "_");
        Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
        if (ench != null) return ench;
        // Búsqueda por nombre en todos los registrados
        for (Enchantment e : Registry.ENCHANTMENT) {
            if (e.getKey().getKey().equalsIgnoreCase(key)) return e;
        }
        return null;
    }

    // ── Persistencia ──────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(dataFile))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try { received.add(UUID.fromString(line)); } catch (Exception ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[StarterKit] Error al cargar datos: " + e.getMessage());
        }
    }

    private void save() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(dataFile))) {
            for (UUID uuid : received) {
                w.write(uuid.toString());
                w.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[StarterKit] Error al guardar datos: " + e.getMessage());
        }
    }
}
