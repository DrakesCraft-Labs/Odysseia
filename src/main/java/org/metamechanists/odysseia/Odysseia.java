package org.metamechanists.odysseia;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.metamechanists.odysseia.commands.LenadorCommand;
import org.metamechanists.odysseia.commands.PapaDeMarCommand;
import org.metamechanists.odysseia.commands.ReloadCommand;
import org.metamechanists.odysseia.commands.VanishCommand;
import org.metamechanists.odysseia.commands.BossCommand;
import org.metamechanists.odysseia.boss.BossManager;
import org.metamechanists.odysseia.kits.KitClaimService;
import org.metamechanists.odysseia.listeners.ArmorEffectsListener;
import org.metamechanists.odysseia.listeners.ItemConsumeListener;
import org.metamechanists.odysseia.listeners.ModerationListener;
import org.metamechanists.odysseia.listeners.PresenceEventListener;
import org.metamechanists.odysseia.utils.OdysseiaPlaceholderExpansion;
import org.metamechanists.odysseia.utils.WebhookSender;
import org.metamechanists.odysseia.purchase.PurchaseEngine;
import org.metamechanists.odysseia.integrations.StarTelemetryPublisher;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class Odysseia extends JavaPlugin {

    @Getter
    private static Odysseia instance;

    private VanishCommand vanishCommand;
    private BossManager bossManager;
    private KitClaimService kitClaimService;
    private boolean ownerFlip = false;
    private String instanceId = "";
    private int chatGamesCountdown = 0;
    private PurchaseEngine purchaseEngine;
    private StarTelemetryPublisher starTelemetry;
    private final List<BukkitTask> runtimeTasks = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Load or create instance ID
        this.instanceId = loadOrCreateInstanceId();
        this.kitClaimService = new KitClaimService(this);

        // Register commands
        this.vanishCommand = new VanishCommand(this);
        getCommand("vanish").setExecutor(vanishCommand);
        getCommand("papademar").setExecutor(new PapaDeMarCommand(this));
        getCommand("lenador").setExecutor(new LenadorCommand(this));
        ReloadCommand reloadCommand = new ReloadCommand(this);
        getCommand("odysseia").setExecutor(reloadCommand);
        getCommand("odysseia").setTabCompleter(reloadCommand);
        getCommand("odysseiaannounce").setExecutor(new org.metamechanists.odysseia.commands.StoreAnnounceCommand(this));
        this.purchaseEngine = new PurchaseEngine(this);
        this.starTelemetry = new StarTelemetryPublisher(this);
        this.purchaseEngine.setTelemetry(telemetry -> starTelemetry.publishPurchase(purchaseEngine, telemetry));
        getCommand("odysseiapurchase").setExecutor(new org.metamechanists.odysseia.commands.PurchaseCommand(purchaseEngine));
        org.metamechanists.odysseia.listeners.ServerAutomationListener automation = new org.metamechanists.odysseia.listeners.ServerAutomationListener(this);
        org.metamechanists.odysseia.purchase.PendingKitService pendingKits = new org.metamechanists.odysseia.purchase.PendingKitService(this);
        org.metamechanists.odysseia.listeners.ChatFilterListener chatFilter = new org.metamechanists.odysseia.listeners.ChatFilterListener(this);
        getCommand("daily").setExecutor(new org.metamechanists.odysseia.commands.DailyCommand(automation));
        getCommand("restart30").setExecutor(new org.metamechanists.odysseia.commands.SafeRestartCommand(this));
        getCommand("odysseiapendingkit").setExecutor(pendingKits);
        getCommand("drakeswarn").setExecutor(chatFilter);
        org.metamechanists.odysseia.commands.KitGiveCommand kitGive = new org.metamechanists.odysseia.commands.KitGiveCommand(this);
        getCommand("kitgive").setExecutor(kitGive);
        getCommand("kitgive").setTabCompleter(kitGive);
        org.metamechanists.odysseia.commands.KitCommand kitCommand = new org.metamechanists.odysseia.commands.KitCommand(this);
        getCommand("kit").setExecutor(kitCommand);
        getCommand("kit").setTabCompleter(kitCommand);
        org.metamechanists.odysseia.menus.ShopMenuService shopMenu = new org.metamechanists.odysseia.menus.ShopMenuService(this);
        getCommand("drakestienda").setExecutor(shopMenu);
        java.util.List<String> kitErrors = kitGive.validateConfiguration();
        if (kitErrors.isEmpty()) {
            getLogger().info("[SUCCESS] Configuración de kits validada.");
        } else {
            kitErrors.forEach(error -> getLogger().severe("[Kits] " + error));
        }
        Bukkit.getPluginManager().registerEvents(purchaseEngine, this);
        Bukkit.getPluginManager().registerEvents(pendingKits, this);
        Bukkit.getPluginManager().registerEvents(chatFilter, this);
        Bukkit.getPluginManager().registerEvents(shopMenu, this);

        // Initialize BossManager
        this.bossManager = new BossManager(this);
        BossCommand bossCmd = new BossCommand(this, bossManager);
        getCommand("boss").setExecutor(bossCmd);
        getCommand("boss").setTabCompleter(bossCmd);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(vanishCommand, this);
        Bukkit.getPluginManager().registerEvents(bossManager, this);
        Bukkit.getPluginManager().registerEvents(new ArmorEffectsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemConsumeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ModerationListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PresenceEventListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.metamechanists.odysseia.listeners.BossItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.metamechanists.odysseia.listeners.SFMasterWatcherListener(this), this);
        Bukkit.getPluginManager().registerEvents(automation, this);

        // Register PlaceholderAPI expansion if present
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OdysseiaPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully.");
        }

        // Start Tasks
        startSchedulers();
        startStarTelemetry();

        // Send startup webhook
        sendStartupWebhook();

        getLogger().info("Odysseia v" + getPluginMeta().getVersion() + " habilitado correctamente.");
    }

    public KitClaimService getKitClaimService() {
        return kitClaimService;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public int getPurchaseEngineProductCount() {
        return purchaseEngine == null ? 0 : purchaseEngine.catalogProductCount();
    }

    public List<String> reloadRuntime() {
        List<String> errors = new ArrayList<>();
        reloadConfig();
        cancelRuntimeTasks();

        if (purchaseEngine != null) {
            HandlerList.unregisterAll(purchaseEngine);
            purchaseEngine.close();
        }
        this.purchaseEngine = new PurchaseEngine(this);
        if (starTelemetry == null) {
            this.starTelemetry = new StarTelemetryPublisher(this);
        }
        this.purchaseEngine.setTelemetry(telemetry -> starTelemetry.publishPurchase(purchaseEngine, telemetry));
        getCommand("odysseiapurchase").setExecutor(new org.metamechanists.odysseia.commands.PurchaseCommand(purchaseEngine));
        Bukkit.getPluginManager().registerEvents(purchaseEngine, this);
        if (!purchaseEngine.isReady()) {
            errors.add("purchase-engine: " + purchaseEngine.startupError());
        }

        org.metamechanists.odysseia.commands.KitGiveCommand validator = new org.metamechanists.odysseia.commands.KitGiveCommand(this);
        errors.addAll(validator.validateConfiguration().stream().map(error -> "kits: " + error).toList());

        startSchedulers();
        startStarTelemetry();
        getLogger().info("[Reload] Runtime recargado: config.yml, purchases.yml, schedulers y purchase engine.");
        return errors;
    }

    @Override
    public void onDisable() {
        if (vanishCommand != null) {
            vanishCommand.revealAll();
        }

        // Send shutdown webhook synchronously
        sendShutdownWebhookSync();

        if (purchaseEngine != null) purchaseEngine.close();

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

    private void trackRuntimeTask(BukkitTask task) {
        runtimeTasks.add(task);
    }

    private void cancelRuntimeTasks() {
        for (BukkitTask task : runtimeTasks) {
            if (task != null) {
                task.cancel();
            }
        }
        runtimeTasks.clear();
    }

    private void startStarTelemetry() {
        if (starTelemetry == null || !getConfig().getBoolean("star-monitor.enabled", false)) {
            return;
        }
        int intervalSeconds = Math.max(30, getConfig().getInt("star-monitor.heartbeat-interval-seconds", 60));
        trackRuntimeTask(Bukkit.getScheduler().runTaskLater(this, () -> starTelemetry.publishStartup(purchaseEngine), 40L));
        trackRuntimeTask(Bukkit.getScheduler().runTaskTimer(this, () -> starTelemetry.publishHeartbeat(purchaseEngine), intervalSeconds * 20L, intervalSeconds * 20L));
    }

    private void startOwnerCycleScheduler() {
        if (!getConfig().getBoolean("owner-cycle.enabled", true)) {
            return;
        }
        int intervalMinutes = getConfig().getInt("owner-cycle.interval-minutes", 30);
        long periodTicks = intervalMinutes * 60L * 20L;
        trackRuntimeTask(Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (isOwnerOnline()) {
                ownerFlip = !ownerFlip;
            }
        }, periodTicks, periodTicks));
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
        if (!getConfig().getBoolean("chatgames.enabled", false)) {
            getLogger().info("[ChatGames] Scheduler deshabilitado (chatgames.enabled: false).");
            return;
        }
        String command = getConfig().getString("chatgames.command", "chatgames force");
        chatGamesCountdown = getRandomChatGamesInterval();
        trackRuntimeTask(Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            chatGamesCountdown--;
            if (chatGamesCountdown <= 0) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                chatGamesCountdown = getRandomChatGamesInterval();
            }
        }, 1200L, 1200L));
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
        if (!getConfig().getBoolean("lenador-loco.enabled", true)) {
            return;
        }
        long intervalTicks = Math.max(1, getConfig().getLong("lenador-loco.check-interval-seconds", 5)) * 20L;
        String targetName = getConfig().getString("lenador-loco.target-name", "Leñador Loco");
        trackRuntimeTask(Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.entity.LivingEntity entity : world.getLivingEntities()) {
                    String name = entity.getCustomName();
                    if (name != null) {
                        String stripped = ChatColor.stripColor(name);
                        if (stripped.equalsIgnoreCase(targetName)) {
                            applyConfiguredLenadorEffects(entity);
                        }
                    }
                }
            }
        }, intervalTicks, intervalTicks));
    }

    private void applyConfiguredLenadorEffects(org.bukkit.entity.LivingEntity entity) {
        List<java.util.Map<?, ?>> effects = getConfig().getMapList("lenador-loco.effects");
        if (effects.isEmpty()) {
            effects = List.of(
                    java.util.Map.of("type", "RESISTANCE", "duration-seconds", 12, "amplifier", 1),
                    java.util.Map.of("type", "REGENERATION", "duration-seconds", 12, "amplifier", 1),
                    java.util.Map.of("type", "STRENGTH", "duration-seconds", 12, "amplifier", 0),
                    java.util.Map.of("type", "SPEED", "duration-seconds", 12, "amplifier", 0)
            );
        }
        for (java.util.Map<?, ?> raw : effects) {
            String typeName = text(raw.get("type"), "").toUpperCase(Locale.ROOT);
            org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(typeName);
            if (type == null) {
                getLogger().warning("[Lenador] Efecto inválido en config: " + typeName);
                continue;
            }
            int duration = Math.max(1, parseInt(raw.get("duration-seconds"), 12)) * 20;
            int amplifier = Math.max(0, parseInt(raw.get("amplifier"), 0));
            entity.addPotionEffect(new org.bukkit.potion.PotionEffect(type, duration, amplifier, true, false, false));
        }
    }

    private int parseInt(Object value, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String text(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private void startPapaDeMarDeliveryScheduler() {
        if (!getConfig().getBoolean("papa-de-mar.delivery.enabled", true)) {
            return;
        }
        int deliveryMinutes = getConfig().getInt("papa-de-mar.delivery.interval-minutes",
                getConfig().getInt("papa-de-mar.delivery-interval-minutes", 15));
        long periodTicks = deliveryMinutes * 60L * 20L;
        trackRuntimeTask(Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            org.bukkit.inventory.ItemStack potato = createPapaDeMarItem();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().addItem(potato);
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        getConfig().getString("papa-de-mar.delivery.message", "&eRecibiste la &6&l✦ Papa de mar ✦&e.")));
            }
        }, periodTicks, periodTicks));
    }

    public org.bukkit.inventory.ItemStack createPapaDeMarItem() {
        String materialName = getConfig().getString("papa-de-mar.item.material", "BAKED_POTATO");
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName == null ? "BAKED_POTATO" : materialName);
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material == null ? org.bukkit.Material.BAKED_POTATO : material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("papa-de-mar.item.name", "&6&l✦ Papa de mar ✦")));
            List<String> lore = getConfig().getStringList("papa-de-mar.item.lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toList();
            if (lore.isEmpty()) {
                lore = List.of(
                        ChatColor.translateAlternateColorCodes('&', "&7Relicario legendario de Drakes."),
                        ChatColor.translateAlternateColorCodes('&', "&7Ha cruzado temporadas, guerras y treguas."),
                        ChatColor.translateAlternateColorCodes('&', "&7A veces se canjeó por llaves ante aldeanos."),
                        ChatColor.translateAlternateColorCodes('&', "&7A veces fue comida, spam y pura leyenda."),
                        ChatColor.translateAlternateColorCodes('&', "&8Dicen que con esta papa terminó una guerra."),
                        ChatColor.translateAlternateColorCodes('&', "&8Y que quien la porta carga historia en las manos.")
                );
            }
            meta.setLore(lore);
            for (String raw : getConfig().getStringList("papa-de-mar.item.enchantments")) {
                String[] parts = raw.split(":", 2);
                org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByName(parts[0].toUpperCase(Locale.ROOT));
                if (enchantment == null) {
                    getLogger().warning("[PapaDeMar] Encantamiento inválido en config: " + raw);
                    continue;
                }
                int level = parts.length > 1 ? parseInt(parts[1], 1) : 1;
                meta.addEnchant(enchantment, level, true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void startHeartbeatScheduler() {
        if (!getConfig().getBoolean("presence.enabled", true)
                || !getConfig().getBoolean("presence.heartbeat.enabled", false)) {
            return;
        }
        String url = getConfig().getString("discord.webhook-url", "");
        if (url == null || url.isBlank() || url.equals("REPLACE_ME") || !WebhookSender.isAllowedHttpsUrl(url)) {
            return;
        }
        int minutes = Math.max(15, getConfig().getInt("presence.heartbeat.interval-minutes", 15));
        long periodTicks = minutes * 60L * 20L;
        long delayTicks = Math.max(20L, getConfig().getInt("presence.startup-delay-seconds", 60) * 20L);

        trackRuntimeTask(Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
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
        }, delayTicks, periodTicks));
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
                trackRuntimeTask(Bukkit.getScheduler().runTaskLater(this, () ->
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg)), warnTicks));
            }
        }

        final DayOfWeek finalDay = targetDay;
        final int finalHour = hour;
        final int finalMin = minute;
        final ZoneId finalZone = zone;
        trackRuntimeTask(Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("[Restart] Ejecutando reinicio semanal programado.");
            Bukkit.broadcastMessage(ChatColor.RED + "⚡ Reiniciando servidor ahora...");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
        }, delayTicks));
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

    public BossManager getBossManager() {
        return this.bossManager;
    }
}
