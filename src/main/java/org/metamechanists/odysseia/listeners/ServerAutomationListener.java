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
import org.metamechanists.odysseia.kits.KitClaimService;
import org.metamechanists.odysseia.kits.StarterKitPolicy;
import org.metamechanists.odysseia.purchase.ActionResult;
import org.metamechanists.odysseia.purchase.KitDeliveryService;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Provides native baseline automations while remaining compatible with optional Denizen scripts. */
public final class ServerAutomationListener implements Listener {
    private static final long DAY_SECONDS = 86_400L;
    private static final Set<String> HARD_MOBS = Set.of("WARDEN", "ELDER_GUARDIAN", "RAVAGER", "GHAST", "GUARDIAN", "VINDICATOR");
    private static final Set<String> MEDIUM_MOBS = Set.of("CREEPER", "WITCH", "BLAZE", "ENDERMAN", "WITHER_SKELETON", "PIGLIN_BRUTE", "EVOKER", "PHANTOM", "HOGLIN", "MAGMA_CUBE");

    private final Odysseia plugin;
    private final File rewardsFile;
    private final YamlConfiguration rewards;
    private final KitDeliveryService kitDelivery;
    private final KitClaimService kitClaims;
    private final NamespacedKey starterPendingKey;
    private final NamespacedKey starterDeliveredKey;
    private final NamespacedKey legacyStarterDeliveredKey;

    public ServerAutomationListener(Odysseia plugin) {
        this.plugin = plugin;
        this.rewardsFile = new File(plugin.getDataFolder(), "daily-rewards.yml");
        this.rewards = YamlConfiguration.loadConfiguration(rewardsFile);
        this.kitDelivery = new KitDeliveryService(plugin);
        this.kitClaims = plugin.getKitClaimService();
        this.starterPendingKey = new NamespacedKey(plugin, "starter_kit_pending");
        this.starterDeliveredKey = new NamespacedKey(plugin, "starter_kit_delivered");
        this.legacyStarterDeliveredKey = new NamespacedKey(plugin, "received_starter_kit");
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
        scheduleStarterKit(player);
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

    private void scheduleStarterKit(Player player) {
        if (!plugin.getConfig().getBoolean("starter-kit.enabled", true)) return;

        String kit = plugin.getConfig().getString("starter-kit.kit", "inicial").toLowerCase(Locale.ROOT);
        String cooldown = plugin.getConfig().getString("kits." + kit + ".cooldown", "-1");
        boolean pending = hasByte(player, starterPendingKey);
        boolean delivered = hasByte(player, starterDeliveredKey) || hasByte(player, legacyStarterDeliveredKey);
        boolean claimed = !kitClaims.state(player.getUniqueId(), kit, cooldown).available();
        if (!StarterKitPolicy.shouldEnroll(player.hasPlayedBefore(), pending, delivered, claimed)) return;

        player.getPersistentDataContainer().set(starterPendingKey, PersistentDataType.BYTE, (byte) 1);
        long delayTicks = Math.clamp(plugin.getConfig().getLong("starter-kit.delay-ticks", 400L), 20L, 2400L);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> attemptStarterKit(player.getUniqueId(), kit, cooldown, 0), delayTicks);
    }

    private void attemptStarterKit(UUID playerId, String kit, String cooldown, int attempt) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline() || !hasByte(player, starterPendingKey)) return;

        if (!kitClaims.state(playerId, kit, cooldown).available()) {
            markStarterKitDelivered(player);
            return;
        }

        String transaction = "starter-kit-" + playerId;
        ActionResult result = kitDelivery.deliver(player, kit, transaction);
        if (result.status() == ActionResult.Status.COMPLETED) {
            boolean claimSaved = kitClaims.record(playerId, kit);
            markStarterKitDelivered(player);
            String message = plugin.getConfig().getString("starter-kit.welcome-message", "");
            if (message != null && !message.isBlank()) player.sendMessage(color(message));
            plugin.getLogger().info("[SUCCESS] Kit inicial " + kit + " entregado a " + player.getName());
            if (!claimSaved) {
                plugin.getLogger().severe("[StarterKit] La entrega quedó protegida por PDC, pero no se pudo guardar kit-claims.yml para "
                        + player.getName());
            }
            return;
        }

        int maxRetries = Math.clamp(plugin.getConfig().getInt("starter-kit.max-retries", 3), 0, 10);
        if ((result.status() == ActionResult.Status.WAITING_FOR_PLAYER
                || result.status() == ActionResult.Status.RETRYABLE_FAILURE) && attempt < maxRetries) {
            long retryTicks = Math.clamp(plugin.getConfig().getLong("starter-kit.retry-delay-ticks", 200L), 20L, 1200L);
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> attemptStarterKit(playerId, kit, cooldown, attempt + 1), retryTicks);
            return;
        }

        plugin.getLogger().warning("[StarterKit] Entrega pendiente para " + player.getName()
                + " (" + result.status() + "): " + result.detail());
    }

    private void markStarterKitDelivered(Player player) {
        player.getPersistentDataContainer().remove(starterPendingKey);
        player.getPersistentDataContainer().set(starterDeliveredKey, PersistentDataType.BYTE, (byte) 1);
    }

    private boolean hasByte(Player player, NamespacedKey key) {
        return player.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Configures the translator after login settles without disabling or taking ownership of Denizen. */
    private void scheduleTranslation(Player player) {
        if (!plugin.getConfig().getBoolean("translation.enabled", true)) return;
        long delay = Math.clamp(plugin.getConfig().getLong("translation.join-delay-ticks", 400L), 20L, 2400L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            try {
                String locale = player.getLocale().toLowerCase(Locale.ROOT);
                String language = locale.startsWith("pt") ? "pt" : locale.startsWith("en") ? "en" : "es";
                configureWorldwideChat(player, language);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                plugin.getLogger().warning("[Translation] No se pudo configurar a " + player.getName()
                        + ": " + exception.getMessage());
            }
        }, delay);
    }

    /**
     * Uses WorldwideChat's public runtime model through reflection so join setup
     * sets exact values instead of flipping the plugin's toggle commands.
     */
    private void configureWorldwideChat(Player player, String language) throws ReflectiveOperationException {
        org.bukkit.plugin.Plugin worldwideChat = Bukkit.getPluginManager().getPlugin("WorldwideChat");
        if (worldwideChat == null || !worldwideChat.isEnabled()) return;

        java.lang.reflect.Method getTranslator = worldwideChat.getClass()
                .getMethod("getActiveTranslator", Player.class);
        Object translator = getTranslator.invoke(worldwideChat, player);
        if (translator == null) {
            if (Bukkit.getPluginCommand("wwct") == null) return;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "wwct " + player.getName() + " None " + language);
            translator = getTranslator.invoke(worldwideChat, player);
        }
        if (translator == null) {
            throw new IllegalStateException("WorldwideChat no creó la sesión del jugador");
        }

        Class<?> type = translator.getClass();
        type.getMethod("setInLangCode", String.class).invoke(translator, "None");
        type.getMethod("setOutLangCode", String.class).invoke(translator, language);
        type.getMethod("setTranslatingChatOutgoing", boolean.class).invoke(translator, false);
        type.getMethod("setTranslatingChatIncoming", boolean.class).invoke(translator, true);
        type.getMethod("setHasBeenSaved", boolean.class).invoke(translator, false);
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
