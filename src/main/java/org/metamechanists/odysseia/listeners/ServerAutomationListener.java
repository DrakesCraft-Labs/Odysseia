package org.metamechanists.odysseia.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.metamechanists.odysseia.Odysseia;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Provides native baseline automations while remaining compatible with optional Denizen scripts. */
public final class ServerAutomationListener implements Listener {
    private static final long DAY_SECONDS = 86_400L;
    private static final Set<String> HARD_MOBS = Set.of("WARDEN", "ELDER_GUARDIAN", "RAVAGER", "GHAST", "GUARDIAN", "VINDICATOR");
    private static final Set<String> MEDIUM_MOBS = Set.of("CREEPER", "WITCH", "BLAZE", "ENDERMAN", "WITHER_SKELETON", "PIGLIN_BRUTE", "EVOKER", "PHANTOM", "HOGLIN", "MAGMA_CUBE");

    private final Odysseia plugin;
    private final File rewardsFile;
    private final YamlConfiguration rewards;

    public ServerAutomationListener(Odysseia plugin) {
        this.plugin = plugin;
        this.rewardsFile = new File(plugin.getDataFolder(), "daily-rewards.yml");
        this.rewards = YamlConfiguration.loadConfiguration(rewardsFile);
        importLegacyRewards();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBedrockRtp(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().startsWith(".")) return;
        String label = event.getMessage().substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (!Set.of("rtp", "wild", "wildrtp", "wildrtp:rtp", "wildrtp:wild", "wildrtp:wildrtp").contains(label)) return;
        event.setCancelled(true);
        boolean nether = player.getWorld().getName().equalsIgnoreCase("world_nether");
        String world = nether ? "world_nether" : "world";
        int maximum = nether ? 100_000 : 150_000;
        player.sendMessage(color("&c[!] &eRTP Bedrock limitado a " + maximum + " bloques para evitar fallos de renderizado."));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "customrtp " + player.getName() + " " + world + " 5000 " + maximum + " 0 0");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInfernalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Mob mob) || !(event.getEntity() instanceof Player)) return;
        int tier = infernalTier(mob);
        if (tier > 0) event.setDamage(event.getDamage() * (tier == 1 ? 1.2D : 1.4D));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInfernalDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        int tier = infernalTier(event.getEntity());
        if (tier <= 0) return;
        String type = event.getEntityType().name();
        int category = HARD_MOBS.contains(type) ? 2 : MEDIUM_MOBS.contains(type) ? 1 : 0;
        int[][] minimum = {{3, 8, 15}, {20, 30, 50}};
        int[][] maximum = {{8, 15, 25}, {30, 45, 70}};
        int row = Math.min(tier, 2) - 1;
        int levels = ThreadLocalRandom.current().nextInt(minimum[row][category], maximum[row][category] + 1);
        killer.giveExpLevels(levels);
        killer.sendMessage(color((tier == 1 ? "&6[⚔ Infernal] " : "&c[★ Super Infernal] ") + "&e+" + levels + " niveles de XP &8(" + type + ")"));
        if (tier >= 2 && category == 2) {
            Bukkit.broadcast(Component.text(killer.getName() + " eliminó un Super Infernal " + type + " y obtuvo " + levels + " niveles de XP."));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        scheduleTranslation(player);
        deliverStarterKitIfFirstJoin(player);
        String path = "players." + player.getUniqueId();
        long now = Instant.now().getEpochSecond();
        long last = rewards.getLong(path + ".last", 0L);
        if (last != 0L && now - last < DAY_SECONDS) return;
        int streak = last == 0L || now - last > DAY_SECONDS * 2 ? 1 : rewards.getInt(path + ".streak", 0) + 1;
        int amount = streak >= 30 ? 20_000 : streak >= 14 ? 7_500 : streak >= 7 ? 3_000 : streak >= 3 ? 1_000 : 300;
        rewards.set(path + ".last", now);
        rewards.set(path + ".streak", streak);
        saveRewards();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + amount);
        player.sendTitle(color("&6&lRecompensa Diaria"), color("&eDía " + streak + " &7· &a+" + amount + " Dragmas"), 10, 70, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1.2F);
    }

    private void deliverStarterKitIfFirstJoin(Player player) {
        if (!plugin.getConfig().getBoolean("starter-kit.enabled", true)) return;

        NamespacedKey kitKey = new NamespacedKey(plugin, "received_starter_kit");
        if (player.getPersistentDataContainer().has(kitKey, PersistentDataType.BYTE) && !player.isOp()) {
            return; // Ya recibió el kit inicial
        }

        long delayTicks = Math.max(20L, plugin.getConfig().getLong("starter-kit.delay-ticks", 100L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Marcar como entregado
            player.getPersistentDataContainer().set(kitKey, PersistentDataType.BYTE, (byte) 1);

            // Mensaje de bienvenida
            String msg = plugin.getConfig().getString("starter-kit.welcome-message", "");
            if (msg != null && !msg.isBlank()) {
                player.sendMessage(color(msg));
            }

            // Comandos en consola
            List<String> commands = plugin.getConfig().getStringList("starter-kit.commands");
            for (String cmd : commands) {
                if (cmd != null && !cmd.isBlank()) {
                    String formattedCmd = cmd.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                }
            }

            // Entregar items configurados
            List<java.util.Map<?, ?>> itemsConfig = plugin.getConfig().getMapList("starter-kit.items");
            for (java.util.Map<?, ?> itemMap : itemsConfig) {
                try {
                    String matName = String.valueOf(itemMap.get("material")).toUpperCase(Locale.ROOT);
                    org.bukkit.Material mat = org.bukkit.Material.matchMaterial(matName);
                    if (mat == null) continue;

                    int amount = 1;
                    if (itemMap.containsKey("amount")) {
                        amount = Integer.parseInt(String.valueOf(itemMap.get("amount")));
                    }

                    org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(mat, amount);
                    org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();

                    if (meta != null) {
                        if (itemMap.containsKey("name")) {
                            meta.setDisplayName(color(String.valueOf(itemMap.get("name"))));
                        }
                        if (itemMap.containsKey("lore")) {
                            List<?> rawLore = (List<?>) itemMap.get("lore");
                            List<String> formattedLore = new java.util.ArrayList<>();
                            for (Object l : rawLore) {
                                formattedLore.add(color(String.valueOf(l)));
                            }
                            meta.setLore(formattedLore);
                        }

                        if (itemMap.containsKey("enchantments")) {
                            Object rawEnchants = itemMap.get("enchantments");
                            if (rawEnchants instanceof java.util.Map<?, ?> enchMap) {
                                for (java.util.Map.Entry<?, ?> entry : enchMap.entrySet()) {
                                    String enchName = String.valueOf(entry.getKey()).toUpperCase(Locale.ROOT);
                                    org.bukkit.enchantments.Enchantment ench = org.bukkit.enchantments.Enchantment.getByName(enchName);
                                    if (ench == null) {
                                        // Buscar por NamespacedKey
                                        ench = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchName.toLowerCase(Locale.ROOT)));
                                    }
                                    if (ench != null) {
                                        int lvl = Integer.parseInt(String.valueOf(entry.getValue()));
                                        meta.addEnchant(ench, lvl, true);
                                    }
                                }
                            }
                        }
                        stack.setItemMeta(meta);
                    }

                    // Dar item al jugador
                    java.util.Map<Integer, org.bukkit.inventory.ItemStack> leftovers = player.getInventory().addItem(stack);
                    if (!leftovers.isEmpty()) {
                        for (org.bukkit.inventory.ItemStack leftover : leftovers.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("[StarterKit] Error procesando item: " + ex.getMessage());
                }
            }

            plugin.getLogger().info("[SUCCESS] Kit inicial entregado a " + player.getName());
        }, delayTicks);
    }

    /** Configures the translator after login settles without disabling or taking ownership of Denizen. */
    private void scheduleTranslation(Player player) {
        if (!plugin.getConfig().getBoolean("translation.enabled", true)) return;
        long delay = Math.clamp(plugin.getConfig().getLong("translation.join-delay-ticks", 60L), 20L, 200L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || Bukkit.getPluginCommand("wwct") == null) return;
            try {
                String locale = player.getLocale().toLowerCase(Locale.ROOT);
                String language = locale.startsWith("pt") ? "pt" : locale.startsWith("en") ? "en" : "es";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "wwct " + player.getName() + " stop");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "wwct " + player.getName() + " None " + language);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "wwctco " + player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "wwctci " + player.getName());
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("[Translation] No se pudo configurar a " + player.getName()
                        + ": " + exception.getMessage());
            }
        }, delay);
    }

    public void showDaily(Player player) {
        String path = "players." + player.getUniqueId();
        long last = rewards.getLong(path + ".last", 0L);
        int streak = rewards.getInt(path + ".streak", 0);
        long remaining = Math.max(0L, DAY_SECONDS - (Instant.now().getEpochSecond() - last));
        player.sendMessage(color("&8[&6&lDRAKES&8] &eRacha actual: &f" + streak + " días"));
        player.sendMessage(color("&7Próxima recompensa en &f" + remaining / 3600 + "h " + remaining % 3600 / 60 + "m"));
    }

    private int infernalTier(LivingEntity entity) {
        Integer tier = entity.getPersistentDataContainer().get(new NamespacedKey("infernalmobsplus", "tier"), PersistentDataType.INTEGER);
        return tier == null ? 0 : tier;
    }

    private void importLegacyRewards() {
        if (rewardsFile.exists()) return;
        File legacy = new File(plugin.getDataFolder().getParentFile(), "Denizen/data/daily_rewards.yml");
        if (!legacy.isFile()) return;
        YamlConfiguration old = YamlConfiguration.loadConfiguration(legacy);
        if (old.isConfigurationSection("players")) rewards.set("players", old.getConfigurationSection("players"));
        saveRewards();
        plugin.getLogger().info("[SUCCESS] Recompensas diarias importadas desde Denizen.");
    }

    private void saveRewards() {
        try {
            rewards.save(rewardsFile);
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[ERROR] No se pudo guardar daily-rewards.yml", exception);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
