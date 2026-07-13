package org.metamechanists.odysseia.integrations;

import org.bukkit.Bukkit;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.purchase.PurchaseEngine;
import org.metamechanists.odysseia.purchase.PurchaseService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/** Publishes signed, non-sensitive operational telemetry without blocking Minecraft work. */
public final class StarTelemetryPublisher {
    private final Odysseia plugin;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public StarTelemetryPublisher(Odysseia plugin) {
        this.plugin = plugin;
    }

    public void publishStartup(PurchaseEngine engine) {
        publish("STARTED", engine, null, null);
    }

    public void publishHeartbeat(PurchaseEngine engine) {
        publish("HEARTBEAT", engine, null, null);
    }

    public void publishPurchase(PurchaseEngine engine, PurchaseService.Telemetry telemetry) {
        publish("PURCHASE_STATE", engine, telemetry.productId(), telemetry.state().name());
    }

    private void publish(String type, PurchaseEngine engine, String productId, String purchaseState) {
        if (!plugin.getConfig().getBoolean("star-monitor.enabled", false)) return;
        String endpoint = plugin.getConfig().getString("star-monitor.endpoint", "");
        String secret = resolveSecret();
        if (endpoint == null || endpoint.isBlank() || secret == null || secret.isBlank() || secret.equals("REPLACE_ME")) {
            plugin.getLogger().warning("[Star] Telemetría habilitada sin endpoint o secreto válido.");
            return;
        }
        String payload = payload(type, engine, productId, purchaseState);
        long timestamp = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> send(endpoint, secret, timestamp, payload));
    }

    private void send(String endpoint, String secret, long timestamp, String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Odysseia-Timestamp", Long.toString(timestamp))
                    .header("X-Odysseia-Signature", hmac(secret, timestamp + "." + payload))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            plugin.getLogger().warning("[Star] Telemetría rechazada: HTTP " + response.statusCode());
                        }
                    })
                    .exceptionally(error -> {
                        plugin.getLogger().warning("[Star] Telemetría no disponible: " + error.getClass().getSimpleName());
                        return null;
                    });
        } catch (Exception error) {
            plugin.getLogger().warning("[Star] Telemetría inválida: " + error.getMessage());
        }
    }

    private String payload(String type, PurchaseEngine engine, String productId, String purchaseState) {
        StringBuilder json = new StringBuilder("{")
                .append("\"eventId\":\"").append(UUID.randomUUID()).append("\",")
                .append("\"type\":\"").append(type).append("\",")
                .append("\"instanceId\":\"").append(escape(plugin.getInstanceId())).append("\",")
                .append("\"purchaseEngineReady\":").append(engine.isReady()).append(',')
                .append("\"catalogProducts\":").append(engine.catalogProductCount()).append(',')
                .append("\"sentAt\":").append(System.currentTimeMillis());
        if (productId != null) json.append(",\"productId\":\"").append(escape(productId)).append("\"");
        if (purchaseState != null) json.append(",\"purchaseState\":\"").append(escape(purchaseState)).append("\"");
        return json.append('}').toString();
    }

    private String resolveSecret() {
        String configured = plugin.getConfig().getString("star-monitor.signing-secret", "");
        if (configured != null && !configured.isBlank() && !configured.equals("REPLACE_ME")) {
            return configured;
        }
        String fileName = plugin.getConfig().getString("star-monitor.signing-secret-file", "star-monitor.secret");
        try {
            return Files.readString(plugin.getDataFolder().toPath().resolve(fileName)).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String hmac(String secret, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder(bytes.length * 2);
        for (byte valueByte : bytes) output.append(String.format("%02x", valueByte));
        return output.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
