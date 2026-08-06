package org.metamechanists.odysseia.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
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
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class StoreManager {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static StoreManager instance;
    /** Ver {@link #warnDiscordOnce}: evita repetir el mismo aviso en cada compra. */
    private static volatile boolean discordWarningShown;
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

            // Disparar anuncio en chat, sonido global y webhook de Discord
            announcePurchase(plugin, nick, productName);

            // Confirmar transacción asincrónicamente
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    confirmPurchase(txnId);
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

    /**
     * Dispara el anuncio público de Discord, el anuncio en chat in-game y el sonido global.
     * Estático para poder invocarse desde un comando (entregas vía Tebex) sin depender del hilo de polling.
     *
     * Los tres canales son independientes a proposito. Antes se validaba el webhook de Discord al
     * principio y se cortaba con {@code return}, asi que un webhook sin configurar dejaba al jugador
     * sin su anuncio en el chat aunque el producto se hubiera entregado bien. Discord es el canal
     * prescindible; el que el jugador ve es el del juego.
     */
    public static boolean announcePurchase(Odysseia plugin, String nick, String productName) {
        FileConfiguration config = plugin.getConfig();

        // Apagado a proposito: no hay nada que anunciar y no es un fallo, asi que la accion se da
        // por cumplida. Devolver false aqui la dejaba reintentando para siempre.
        if (!config.getBoolean("purchase-engine.announcements.enabled", false)) {
            return true;
        }

        // 1. Anuncio en el chat local
        String chatAnnounce = config.getString("purchase-engine.announcements.chat-announcement", "");
        if (chatAnnounce != null && !chatAnnounce.isBlank()) {
            String msg = ChatColor.translateAlternateColorCodes('&', chatAnnounce
                    .replace("{player}", nick)
                    .replace("{product}", productName));
            Bukkit.broadcastMessage(msg);
        }

        // 2. Reproducir sonido global si está activado
        if (config.getBoolean("purchase-engine.announcements.global-sound.enabled", true)) {
            String soundName = config.getString("purchase-engine.announcements.global-sound.sound", "UI_TOAST_CHALLENGE_COMPLETE");
            Sound sound = resolverSonido(soundName);
            if (sound != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                }
            } else {
                plugin.getLogger().warning("[Store] Sonido global de tienda inválido: " + soundName);
            }
        }

        // 3. Webhook de Discord (opcional)
        //
        // A partir de aqui nada puede devolver false: el jugador y quienes estaban conectados ya
        // vieron el anuncio, asi que la accion esta cumplida. Que Discord no este configurado es un
        // problema del dueño del servidor, no motivo para reintentar la entrega en bucle.
        String webhookUrl = config.getString("purchase-engine.announcements.webhook-url", "");
        String discordAnnounce = config.getString("purchase-engine.announcements.discord-announcement", "");

        String motivo = motivoDiscordNoDisponible(webhookUrl, discordAnnounce);
        if (motivo != null) {
            warnDiscordOnce(plugin, motivo);
            return true;
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
        plugin.getLogger().info("[Purchase] Anuncio encolado para " + nick + " (" + productName + ").");
        return true;
    }

    /**
     * Busca un sonido aceptando las dos formas con que se escribe en un config.
     *
     * El valor por defecto de la config, {@code UI_TOAST_CHALLENGE_COMPLETE}, es el nombre de la
     * constante de Bukkit; la clave del registro es {@code ui.toast.challenge_complete}. Pasar la
     * primera en minusculas a {@link NamespacedKey#fromString} da una clave que no existe, asi que
     * el sonido del anuncio nunca sono. No se notaba porque el metodo salia antes de llegar aqui.
     *
     * No se puede convertir una forma en otra a ciegas --{@code challenge_complete} lleva guion
     * bajo dentro de la propia palabra--, asi que se compara contra el registro real.
     */
    static Sound resolverSonido(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        String limpio = nombre.trim().toLowerCase(Locale.ROOT);

        NamespacedKey clave = NamespacedKey.fromString(limpio);
        if (clave != null) {
            Sound directo = Registry.SOUNDS.get(clave);
            if (directo != null) return directo;
        }

        for (Sound candidato : Registry.SOUNDS) {
            if (esElMismoSonido(candidato.getKey().getKey(), limpio)) return candidato;
        }
        return null;
    }

    /** True si la clave del registro y lo escrito en el config nombran el mismo sonido. */
    static boolean esElMismoSonido(String claveRegistro, String nombreConfig) {
        return claveRegistro.replace('.', '_').equals(nombreConfig.replace('.', '_'));
    }

    /**
     * Por que no se puede copiar el anuncio a Discord, o {@code null} si si se puede.
     *
     * Separado de {@link #announcePurchase} para poder comprobarlo sin levantar un servidor: es la
     * unica parte de la decision que no toca Bukkit.
     */
    static String motivoDiscordNoDisponible(String webhookUrl, String texto) {
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.startsWith("REPLACE_ME")) {
            return "webhook-url sin configurar";
        }
        if (!WebhookSender.isAllowedHttpsUrl(webhookUrl) || !WebhookSender.isDiscordWebhookUrl(webhookUrl)) {
            return "webhook-url no es una URL de webhook de Discord por HTTPS";
        }
        if (texto == null || texto.isBlank()) {
            return "falta discord-announcement";
        }
        return null;
    }

    /**
     * Avisa una sola vez de que el espejo en Discord no esta configurado.
     *
     * Sin esto el aviso salia en cada compra y se volvia ruido que se ignora. Una vez por arranque
     * basta para que se note, y se reinicia al recargar la config por si se acaba de corregir.
     */
    private static void warnDiscordOnce(Odysseia plugin, String motivo) {
        if (discordWarningShown) return;
        discordWarningShown = true;
        plugin.getLogger().warning("[Purchase] El anuncio salio en el chat del juego, pero no se copiara a Discord: "
                + motivo + " (purchase-engine.announcements). Se avisa una sola vez.");
    }

    /** Permite que el aviso de Discord vuelva a salir tras un /odysseia reload. */
    public static void resetDiscordWarning() {
        discordWarningShown = false;
    }
}
