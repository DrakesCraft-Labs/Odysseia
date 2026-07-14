package org.metamechanists.odysseia;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.commands.LenadorCommand;
import org.metamechanists.odysseia.commands.PapaDeMarCommand;
import org.metamechanists.odysseia.commands.ReloadCommand;
import org.metamechanists.odysseia.commands.VanishCommand;
import org.metamechanists.odysseia.commands.BossCommand;
import org.metamechanists.odysseia.boss.BossManager;
import org.metamechanists.odysseia.listeners.ArmorEffectsListener;
import org.metamechanists.odysseia.listeners.ItemConsumeListener;
import org.metamechanists.odysseia.listeners.ModerationListener;
import org.metamechanists.odysseia.listeners.PresenceEventListener;
import org.metamechanists.odysseia.utils.OdysseiaPlaceholderExpansion;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.logging.Level;

public final class Odysseia extends JavaPlugin {

    @Getter
    private static Odysseia instance;

    private VanishCommand vanishCommand;
    private BossManager bossManager;
    private boolean ownerFlip = false;
    private String instanceId = "";
    private int chatGamesCountdown = 0;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Load or create instance ID
        this.instanceId = loadOrCreateInstanceId();

        // Register commands
        this.vanishCommand = new VanishCommand(this);
        getCommand("vanish").setExecutor(vanishCommand);
        getCommand("papademar").setExecutor(new PapaDeMarCommand(this));
        getCommand("lenador").setExecutor(new LenadorCommand(this));

        // Initialize BossManager
        this.bossManager = new BossManager(this);
        BossCommand bossCmd = new BossCommand(this, bossManager);
        getCommand("boss").setExecutor(bossCmd);
        getCommand("boss").setTabCompleter(bossCmd);

        // Register /odysseia reload
        getCommand("odysseia").setExecutor(new ReloadCommand(this));

        // Register listeners
        Bukkit.getPluginManager().registerEvents(vanishCommand, this);
        Bukkit.getPluginManager().registerEvents(bossManager, this);
        Bukkit.getPluginManager().registerEvents(new ArmorEffectsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemConsumeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ModerationListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PresenceEventListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.metamechanists.odysseia.listeners.SFMasterWatcherListener(), this);

        // Register PlaceholderAPI expansion if present
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OdysseiaPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully.");
        }

        // Start Tasks
        startSchedulers();

        // Start StoreManager
        if (getConfig().getBoolean("store.enabled", true)) {
            org.metamechanists.odysseia.utils.StoreManager.start(this);
        }

        // Send startup webhook
        sendStartupWebhook();

        getLogger().info("Odysseia v" + getPluginMeta().getVersion() + " habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        // Send shutdown webhook synchronously
        sendShutdownWebhookSync();

        // Stop StoreManager
        org.metamechanists.odysseia.utils.StoreManager.stop();

        // Shutdown BossManager
        if (bossManager != null) {
            bossManager.shutdown();
        }

        getLogger().info("Odysseia v" + getPluginMeta().getVersion() + " deshabilitado correctamente.");
    }

    public boolean isVanished(Player player) {
        return vanishCommand != null && vanishCommand.isVanished(player);
    }

    // Owner Cycle Prefix Support
    public String getCurrentOdiseoPrefix() {
        if (!getConfig().getBoolean("owner-cycle.enabled", true)) {
            return getConfig().getString("owner-cycle.odiseo.prefix-a", "&8✠ &6&lᴏᴅɪsᴇᴏ &8✦ &6&o");
        }
        return ownerFlip
                ? getConfig().getString("owner-cycle.odiseo.prefix-b", "&8✠ &4&lᴅᴜᴇñᴏ &8✦ &4")
                : getConfig().getString("owner-cycle.odiseo.prefix-a", "&8✠ &6&lᴏᴅɪsᴇᴏ &8✦ &6&o");
    }

    public String getCurrentPenelopePrefix() {
        if (!getConfig().getBoolean("owner-cycle.enabled", true)) {
            return getConfig().getString("owner-cycle.penelope.prefix-a", "&d✦ &d&lᴘᴇɴᴇʟᴏᴘᴇ &8✦ &d");
        }
        return ownerFlip
                ? getConfig().getString("owner-cycle.penelope.prefix-b", "&d✦ &d&lᴅᴜᴇñᴀ &8✦ &d")
                : getConfig().getString("owner-cycle.penelope.prefix-a", "&d✦ &d&lᴘᴇɴᴇʟᴏᴘᴇ &8✦ &d");
    }

    public static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String loadOrCreateInstanceId() {
        java.io.File file = new java.io.File(getDataFolder(), "data.yml");
        org.bukkit.configuration.file.YamlConfiguration data = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        String id = data.getString("instance-id");
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
            data.set("instance-id", id);
            try {
                data.save(file);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error al guardar data.yml", e);
            }
        }
        return id;
    }

    private void startSchedulers() {
        startOwnerCycleScheduler();
        startChatGamesScheduler();
        startLenadorLocoScheduler();
        startPapaDeMarDeliveryScheduler();
        startHeartbeatScheduler();
        startRestartScheduler();
    }

    private void startOwnerCycleScheduler() {
        if (!getConfig().getBoolean("owner-cycle.enabled", true)) {
            return;
        }
        int intervalMinutes = getConfig().getInt("owner-cycle.interval-minutes", 30);
        long periodTicks = intervalMinutes * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (isOwnerOnline()) {
                ownerFlip = !ownerFlip;
            }
        }, periodTicks, periodTicks);
    }

    private boolean isOwnerOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("group.odiseo") || p.hasPermission("group.penelope")) {
                return true;
            }
            String name = p.getName().toLowerCase();
            if (name.equals("odiseo") || name.equals("penelope")) {
                return true;
            }
        }
        return false;
    }

    private void startChatGamesScheduler() {
        chatGamesCountdown = getRandomChatGamesInterval();
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            chatGamesCountdown--;
            if (chatGamesCountdown <= 0) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "chatgames start");
                chatGamesCountdown = getRandomChatGamesInterval();
            }
        }, 1200L, 1200L); // Check every minute (1200 ticks)
    }

    private int getRandomChatGamesInterval() {
        int min = getConfig().getInt("chatgames.min-interval", 15);
        int max = getConfig().getInt("chatgames.max-interval", 30);
        if (min >= max) {
            return min;
        }
        return min + new java.util.Random().nextInt(max - min + 1);
    }

    private void startLenadorLocoScheduler() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.entity.LivingEntity entity : world.getLivingEntities()) {
                    String name = entity.getCustomName();
                    if (name != null) {
                        String stripped = ChatColor.stripColor(name);
                        if (stripped.equalsIgnoreCase("Leñador Loco")) {
                            // Apply effects for 12 seconds (240 ticks) without particles/icons
                            entity.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 240, 1, true, false, false));
                            entity.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 240, 1, true, false, false));
                            entity.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 240, 0, true, false, false));
                            entity.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 240, 0, true, false, false));
                        }
                    }
                }
            }
        }, 100L, 100L); // Every 5 seconds (100 ticks)
    }

    private void startPapaDeMarDeliveryScheduler() {
        int deliveryMinutes = getConfig().getInt("papa-de-mar.delivery-interval-minutes", 15);
        long periodTicks = deliveryMinutes * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            org.bukkit.inventory.ItemStack potato = createPapaDeMarItem();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().addItem(potato);
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eRecibiste la &6&l✦ Papa de mar ✦&e."));
            }
        }, periodTicks, periodTicks);
    }

    public org.bukkit.inventory.ItemStack createPapaDeMarItem() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BAKED_POTATO);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&l✦ Papa de mar ✦"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Relicario legendario de Drakes."));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Ha cruzado temporadas, guerras y treguas."));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7A veces se canjeó por llaves ante aldeanos."));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7A veces fue comida, spam y pura leyenda."));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&8Dicen que con esta papa terminó una guerra."));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&8Y que quien la porta carga historia en las manos."));
            meta.setLore(lore);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.VANISHING_CURSE, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void startHeartbeatScheduler() {
        if (!getConfig().getBoolean("presence.enabled", true)) {
            return;
        }
        String url = getConfig().getString("discord.webhook-url", "");
        if (url == null || url.isBlank() || url.equals("REPLACE_ME") || !WebhookSender.isAllowedHttpsUrl(url)) {
            return;
        }
        int minutes = Math.max(15, getConfig().getInt("presence.heartbeat-minutes", 15));
        long periodTicks = minutes * 60L * 20L;
        long delayTicks = Math.max(20L, getConfig().getInt("presence.startup-delay-seconds", 60) * 20L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                String serverLabel = getConfig().getString("presence.server-label", "");
                if (serverLabel == null || serverLabel.isBlank()) {
                    serverLabel = Bukkit.getServer().getName();
                }
                boolean sf = Bukkit.getPluginManager().getPlugin("Slimefun") != null;
                String sfVer = "";
                if (sf) {
                    var pl = Bukkit.getPluginManager().getPlugin("Slimefun");
                    if (pl != null) {
                        sfVer = pl.getPluginMeta().getVersion();
                    }
                }
                String paperMc = Bukkit.getMinecraftVersion();
                String ts = java.time.Instant.now().toString();
                
                String desc = "**instanceId:** `" + instanceId + "` | **MC:** " + paperMc
                        + " | **Slimefun:** " + (sf ? "sí (`" + sfVer + "`)" : "no")
                        + " | **Plugin:** `Odysseia v" + getPluginMeta().getVersion() + "` | **UTC:** " + ts;
                
                String json = "{\"username\":\"Odysseia Heartbeat\",\"embeds\":[{\"title\":\"Drakes lab heartbeat\",\"description\":\""
                        + escapeJson(desc) + "\",\"color\":5814783,\"fields\":[{\"name\":\"Servidor\",\"value\":\""
                        + escapeJson(serverLabel.length() <= 900 ? serverLabel : serverLabel.substring(0, 900) + "…") + "\",\"inline\":false}]}]}";

                WebhookSender.sendAsync(this, url, json);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error al enviar latido a Discord", e);
            }
        }, delayTicks, periodTicks);
    }

    private void startRestartScheduler() {
        if (!getConfig().getBoolean("restart.enabled", false)) {
            return;
        }
        String dayStr = getConfig().getString("restart.day", "MONDAY").toUpperCase();
        int hour = getConfig().getInt("restart.hour", 5);
        int minute = getConfig().getInt("restart.minute", 0);
        String timezone = getConfig().getString("restart.timezone", "America/Santiago");

        DayOfWeek targetDay;
        try {
            targetDay = DayOfWeek.valueOf(dayStr);
        } catch (IllegalArgumentException e) {
            getLogger().warning("[Restart] Día inválido en config: " + dayStr + ". Usando MONDAY.");
            targetDay = DayOfWeek.MONDAY;
        }

        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);
        } catch (Exception e) {
            getLogger().warning("[Restart] Timezone inválido: " + timezone + ". Usando America/Santiago.");
            zone = ZoneId.of("America/Santiago");
        }

        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.with(TemporalAdjusters.nextOrSame(targetDay))
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.with(TemporalAdjusters.next(targetDay));
        }

        long delaySeconds = java.time.Duration.between(now, next).getSeconds();
        long delayTicks = delaySeconds * 20L;

        getLogger().info(String.format("[Restart] Próximo reinicio: %s (%s) en %d horas.",
                next, timezone, delaySeconds / 3600));

        // Avisos previos: 10 min, 5 min, 1 min antes
        long[] warnOffsets = {10 * 60 * 20L, 5 * 60 * 20L, 60 * 20L};
        String[] warnMessages = {
            "&6&l⚡ &eel servidor se reiniciará en &6&l10 minutos&e. Guarda tus cosas.",
            "&c&l⚡ &cel servidor se reiniciará en &c&l5 minutos&c.",
            "&4&l⚡ &4Reinicio en &4&l1 minuto&4. Prepárate."
        };
        for (int i = 0; i < warnOffsets.length; i++) {
            long warnTicks = delayTicks - warnOffsets[i];
            if (warnTicks > 0) {
                final String msg = ChatColor.translateAlternateColorCodes('&', warnMessages[i]);
                Bukkit.getScheduler().runTaskLater(this, () ->
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg)), warnTicks);
            }
        }

        final DayOfWeek finalDay = targetDay;
        final int finalHour = hour;
        final int finalMin = minute;
        final ZoneId finalZone = zone;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("[Restart] Ejecutando reinicio semanal programado.");
            Bukkit.broadcastMessage(ChatColor.RED + "⚡ Reiniciando servidor ahora...");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
        }, delayTicks);
    }

    private void sendStartupWebhook() {
        if (!getConfig().getBoolean("presence.enabled", true)
                || !getConfig().getBoolean("presence.events.server-startup", true)) {
            return;
        }
        String url = getConfig().getString("discord.webhook-url", "");
        if (url == null || url.isBlank() || url.equals("REPLACE_ME") || !WebhookSender.isAllowedHttpsUrl(url)) {
            return;
        }
        long delayTicks = Math.max(20L, getConfig().getInt("presence.startup-delay-seconds", 60) * 20L);
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            String serverLabel = getConfig().getString("presence.server-label", "");
            if (serverLabel == null || serverLabel.isBlank()) {
                serverLabel = Bukkit.getServer().getName();
            }
            String mc = Bukkit.getMinecraftVersion();
            boolean sf = Bukkit.getPluginManager().getPlugin("Slimefun") != null;
            String desc = "Servidor **" + serverLabel + "** en línea · MC `" + mc + "` · Slimefun: " + (sf ? "sí" : "no");
            
            String json = "{\"username\":\"Odysseia\",\"embeds\":[{\"title\":\"Arranque\",\"description\":\""
                    + escapeJson(desc) + "\",\"color\":3066993,\"footer\":{\"text\":\""
                    + escapeJson(instanceId + " · " + serverLabel) + "\"}}]}";
            
            WebhookSender.sendAsync(this, url, json);
        }, delayTicks);
    }

    private void sendShutdownWebhookSync() {
        if (!getConfig().getBoolean("presence.enabled", true)
                || !getConfig().getBoolean("presence.events.server-shutdown", true)) {
            return;
        }
        String url = getConfig().getString("discord.webhook-url", "");
        if (url == null || url.isBlank() || url.equals("REPLACE_ME") || !WebhookSender.isAllowedHttpsUrl(url)) {
            return;
        }
        String serverLabel = getConfig().getString("presence.server-label", "");
        if (serverLabel == null || serverLabel.isBlank()) {
            serverLabel = Bukkit.getServer().getName();
        }
        String desc = "Apagado o recarga de **" + serverLabel + "**.";
        
        String json = "{\"username\":\"Odysseia\",\"embeds\":[{\"title\":\"Apagado\",\"description\":\""
                + escapeJson(desc) + "\",\"color\":15105570,\"footer\":{\"text\":\""
                + escapeJson(instanceId + " · " + serverLabel) + "\"}}]}";
        
        WebhookSender.sendSyncBestEffort(this, url, json);
    }
}
