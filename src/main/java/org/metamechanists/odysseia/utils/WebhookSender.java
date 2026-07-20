package org.metamechanists.odysseia.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles secure, asynchronous HTTP POST requests to operator-controlled Discord webhooks.
 * Endured against SSRF attacks and excessive wait times on rate limits.
 */
public final class WebhookSender {

    private static final ThreadPoolExecutor HTTP_EXECUTOR = new ThreadPoolExecutor(
            1, 2, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64),
            new ThreadPoolExecutor.DiscardPolicy());
    private static final AtomicLong LAST_TRANSPORT_WARNING = new AtomicLong();
    private static volatile HttpClient httpClient = newHttpClient();

    private WebhookSender() {}

    public static boolean isDiscordWebhookUrl(String url) {
        if (url == null) {
            return false;
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        return u.contains("discord.com/api/webhooks") || u.contains("discordapp.com/api/webhooks");
    }

    public static boolean isAllowedHttpsUrl(String url) {
        if (url == null || !url.trim().startsWith("https://")) {
            return false;
        }
        try {
            URI u = URI.create(url.trim());
            String host = u.getHost();
            if (host == null) {
                return false;
            }
            String h = host.toLowerCase(Locale.ROOT);
            if (h.equals("localhost") || h.equals("127.0.0.1") || h.endsWith(".local")) {
                return false;
            }
            if (h.startsWith("10.") || h.startsWith("192.168.") || isPrivate172Host(h)) {
                return false;
            }
            return u.getScheme().equalsIgnoreCase("https");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isPrivate172Host(String host) {
        if (!host.startsWith("172.")) {
            return false;
        }
        String[] p = host.split("\\.");
        if (p.length < 2) {
            return false;
        }
        try {
            int oct2 = Integer.parseInt(p[1]);
            return oct2 >= 16 && oct2 <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void sendAsync(JavaPlugin plugin, String url, String jsonBody) {
        if (url == null || url.isBlank() || !isAllowedHttpsUrl(url)) {
            return;
        }
        byte[] bodyUtf8 = jsonBody.getBytes(StandardCharsets.UTF_8);
        String trimmed = url.trim();
        try {
            var req = HttpRequest.newBuilder(URI.create(trimmed))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("User-Agent", "Odysseia/1.0.0")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyUtf8))
                    .build();

            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                if (err != null) {
                    if (isClosedSelector(err)) {
                        // A previous native-thread failure can poison the JDK selector.
                        // Replace it so future events recover after resources return.
                        httpClient = newHttpClient();
                    }
                    warnTransportOnce(plugin, err);
                } else {
                    int code = resp != null ? resp.statusCode() : -1;
                    if (code == 429) {
                        String retryAfterHeader = resp.headers().firstValue("Retry-After").orElse(null);
                        long waitMs = 5000L;
                        if (retryAfterHeader != null) {
                            try {
                                waitMs = (long) (Double.parseDouble(retryAfterHeader) * 1000) + 500;
                            } catch (NumberFormatException ignored) {}
                        }
                        plugin.getLogger().warning("[Odysseia] Discord 429 rate-limit (" + waitMs + " ms). Se descarta este aviso para no bloquear hilos.");
                    } else if (code < 200 || code >= 300) {
                        plugin.getLogger().warning("[Odysseia] Discord responded with HTTP " + code + " for webhook delivery.");
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Odysseia] Could not post to Discord webhook", e);
        }
    }

    /**
     * Best-effort synchronous POST for shutdown (scheduler may already be stopping).
     */
    public static void sendSyncBestEffort(JavaPlugin plugin, String url, String jsonBody) {
        if (url == null || url.isBlank() || !isAllowedHttpsUrl(url)) {
            return;
        }
        byte[] bodyUtf8 = jsonBody.getBytes(StandardCharsets.UTF_8);
        String trimmed = url.trim();
        try {
            var req = HttpRequest.newBuilder(URI.create(trimmed))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("User-Agent", "Odysseia/1.0.0")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyUtf8))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[Odysseia] Shutdown webhook not delivered: " + e.getMessage());
        }
    }

    private static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(HTTP_EXECUTOR)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private static boolean isClosedSelector(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("selector manager closed")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void warnTransportOnce(JavaPlugin plugin, Throwable error) {
        long now = System.currentTimeMillis();
        long previous = LAST_TRANSPORT_WARNING.get();
        if (now - previous < 60_000L || !LAST_TRANSPORT_WARNING.compareAndSet(previous, now)) {
            return;
        }
        plugin.getLogger().log(Level.WARNING, "[Odysseia] Discord webhook HTTP task failed: " + error.getMessage());
    }
}

