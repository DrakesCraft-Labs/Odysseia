package org.metamechanists.odysseia.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles secure, asynchronous HTTP POST requests to operator-controlled Discord webhooks.
 * Endured against SSRF attacks and excessive wait times on rate limits.
 */
public final class WebhookSender {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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

            HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                if (err != null) {
                    plugin.getLogger().log(Level.WARNING, "[Odysseia] Discord webhook HTTP task failed: " + err.getMessage());
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
                        if (waitMs > 15000L) {
                            plugin.getLogger().warning("[Odysseia] Discord 429 rate-limit too high (" + waitMs + " ms). Dropping webhook message to prevent thread lock.");
                        } else {
                            plugin.getLogger().warning("[Odysseia] Discord 429 rate-limit — retrying in " + waitMs + " ms");
                            try {
                                Thread.sleep(waitMs);
                                sendAsync(plugin, url, jsonBody); // retry once
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
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
            HTTP.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[Odysseia] Shutdown webhook not delivered: " + e.getMessage());
        }
    }
}

