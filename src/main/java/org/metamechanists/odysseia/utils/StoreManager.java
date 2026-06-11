package org.metamechanists.odysseia.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.metamechanists.odysseia.Odysseia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class StoreManager {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static StoreManager instance;
    private final Odysseia plugin;
    private final Set<String> inFlightTransactions = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    private StoreManager(Odysseia plugin) {
        this.plugin = plugin;
    }

    public static synchronized void start(Odysseia plugin) {
        if (instance != null) {
            stop();
        }
        instance = new StoreManager(plugin);
        instance.schedule();
    }

    public static synchronized void stop() {
        if (instance != null) {
            instance.cancel();
            instance = null;
        }
    }

    private void schedule() {
        int seconds = Math.max(10, plugin.getConfig().getInt("store.poll-interval-seconds", 60));
        long ticks = seconds * 20L;
        
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkPendingPurchases, 100L, ticks);
        plugin.getLogger().info("[Store] Hilo de entrega de tienda activado. Verificación cada " + seconds + " segundos.");
    }

    private void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
            plugin.getLogger().info("[Store] Hilo de entrega de tienda detenido.");
        }
    }

    private void checkPendingPurchases() {
        FileConfiguration config = plugin.getConfig();
        String apiUrl = config.getString("store.api-url", "");
        String apiKey = config.getString("store.api-key", "");

        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank() || apiKey.startsWith("REPLACE_ME")) {
            return;
        }

        try {
            // Build GET request
            var req = HttpRequest.newBuilder(URI.create(apiUrl + "/pending"))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-API-Key", apiKey)
                    .header("User-Agent", "OdysseiaStore/1.0.0")
                    .GET()
                    .build();

            // Execute request synchronously within this async thread
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();

            if (code == 401) {
                plugin.getLogger().warning("[Store] Error 401 de autenticación. Verifica store.api-key en la config.");
                return;
            } else if (code < 200 || code >= 300) {
                plugin.getLogger().warning("[Store] El servidor de la tienda respondió con HTTP " + code);
                return;
            }

            String body = resp.body();
            if (body == null || body.isBlank() || body.equals("[]")) {
                return; // Sin compras pendientes
            }

            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonArray()) {
                return;
            }

            JsonArray array = root.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement element = array.get(i);
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject obj = element.getAsJsonObject();
                String txnId = obj.has("id") ? obj.get("id").getAsString() : "";
                String nick = obj.has("nick") ? obj.get("nick").getAsString() : "";
                String productId = obj.has("productId") ? obj.get("productId").getAsString() : "";
                String productName = obj.has("productName") ? obj.get("productName").getAsString() : "";

                if (txnId.isEmpty() || nick.isEmpty() || productId.isEmpty()) {
                    continue;
                }

                processPurchase(txnId, nick, productId, productName);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Store] Error al consultar compras pendientes: " + e.getMessage());
        }
    }

    private void processPurchase(String txnId, String nick, String productId, String productName) {
        if (!inFlightTransactions.add(txnId)) {
            return;
        }

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection pkgSection = config.getConfigurationSection("store.packages." + productId);

        if (pkgSection == null) {
            plugin.getLogger().warning("[Store] Compra pendiente para producto desconocido en la configuración: " + productId + " (" + nick + ")");
            inFlightTransactions.remove(txnId);
            return;
        }

        boolean requireOnline = pkgSection.getBoolean("require-online", true);
        Player player = Bukkit.getPlayerExact(nick);

        if (requireOnline && (player == null || !player.isOnline())) {
            // Saltamos esta compra porque el jugador no está conectado, volverá a consultarse en el siguiente check
            inFlightTransactions.remove(txnId);
            return;
        }

        // Ejecutar los comandos en el hilo principal de Bukkit de forma segura
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<String> commands = pkgSection.getStringList("commands");
            plugin.getLogger().info("[Store] Procesando compra para " + nick + ": " + productName + " (" + txnId + ")");
            
            for (String cmd : commands) {
                String formatted = cmd.replace("{player}", nick).replace("{product}", productName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
            }

            // Anuncio en el chat local
            String chatAnnounce = config.getString("store.chat-announcement", "");
            if (chatAnnounce != null && !chatAnnounce.isBlank()) {
                String msg = ChatColor.translateAlternateColorCodes('&', chatAnnounce
                        .replace("{player}", nick)
                        .replace("{product}", productName));
                Bukkit.broadcastMessage(msg);
            }

            // Confirmar transacción asincrónicamente y enviar webhook de Discord
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    confirmPurchase(txnId);
                    sendDiscordNotification(nick, productName);
                } finally {
                    inFlightTransactions.remove(txnId);
                }
            });
        });
    }

    private void confirmPurchase(String txnId) {
        FileConfiguration config = plugin.getConfig();
        String apiUrl = config.getString("store.api-url", "");
        String apiKey = config.getString("store.api-key", "");

        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank() || apiKey.startsWith("REPLACE_ME")) {
            return;
        }

        try {
            String jsonBody = "{\"id\":\"" + Odysseia.escapeJson(txnId) + "\"}";
            byte[] bodyUtf8 = jsonBody.getBytes(StandardCharsets.UTF_8);

            var req = HttpRequest.newBuilder(URI.create(apiUrl + "/confirm"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("X-API-Key", apiKey)
                    .header("User-Agent", "OdysseiaStore/1.0.0")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyUtf8))
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();

            if (code < 200 || code >= 300) {
                plugin.getLogger().warning("[Store] Error al confirmar transaccion " + txnId + ". HTTP " + code);
            } else {
                plugin.getLogger().info("[Store] Transaccion " + txnId + " entregada y confirmada en el backend.");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Store] Error al enviar confirmación de " + txnId + ": " + e.getMessage());
        }
    }

    private void sendDiscordNotification(String nick, String productName) {
        FileConfiguration config = plugin.getConfig();
        String webhookUrl = config.getString("store.announcement-webhook-url", "");
        String discordAnnounce = config.getString("store.discord-announcement", "");

        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.equals("REPLACE_ME") || !WebhookSender.isAllowedHttpsUrl(webhookUrl)) {
            return;
        }

        if (discordAnnounce == null || discordAnnounce.isBlank()) {
            return;
        }

        String cleanText = discordAnnounce.replace("{player}", nick).replace("{product}", productName);
        String jsonPayload = "{\"username\":\"DrakesCraft · Tienda\","
                + "\"avatar_url\":\"https://web.drakescraft.cl/assets/logo-drakescraft.png\","
                + "\"embeds\":[{"
                + "\"title\":\"⚡ ¡Compra Entregada con Éxito! ⚡\","
                + "\"description\":\"" + Odysseia.escapeJson(cleanText) + "\","
                + "\"color\":15844367," // Color dorado (#f1c40f = 15844367)
                + "\"thumbnail\":{\"url\":\"https://web.drakescraft.cl/assets/logo-drakescraft.png\"},"
                + "\"fields\":["
                + "{\"name\":\"🎮 Jugador\",\"value\":\"`" + Odysseia.escapeJson(nick) + "`\",\"inline\":true},"
                + "{\"name\":\"📦 Producto\",\"value\":\"**" + Odysseia.escapeJson(productName) + "**\",\"inline\":true}"
                + "],"
                + "\"footer\":{\"text\":\"DrakesCraft · Tienda Oficial · web.drakescraft.cl\"}"
                + "}]}";

        WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
    }
}
