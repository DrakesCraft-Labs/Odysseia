package org.metamechanists.odysseia.services;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

/** Servicio diario para escanear y enviar reportes de vencimiento de rangos VIP a Discord. */
public final class VipExpiryAlertService {
    private static final Set<String> VIP_GROUPS = Set.of(
            "hercules", "hestia", "hermes", "hefesto", "artemisa", "afrodita", "zeus"
    );
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Odysseia plugin;

    public VipExpiryAlertService(Odysseia plugin) {
        this.plugin = plugin;
    }

    public void startScheduler() {
        if (!plugin.getConfig().getBoolean("vipalerts.enabled", true)) {
            plugin.getLogger().info("[VipAlerts] Deshabilitado en config.yml.");
            return;
        }

        // Ejecutar 60s después del arranque y luego cada 24 horas (1728000 ticks)
        long initialDelayTicks = 1200L;
        long periodTicks = 1728000L;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                sendVipExpiryReport();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[VipAlerts] Error al enviar reporte de rangos VIP", e);
            }
        }, initialDelayTicks, periodTicks);

        plugin.getLogger().info("[VipAlerts] Tarea diaria de caducidad VIP programada.");
    }

    public boolean sendVipExpiryReport() {
        List<VipEntry> entries = scanVipExpiries();
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.contains("REPLACE_ME")) {
            plugin.getLogger().warning("[VipAlerts] Webhook de Discord no configurado en config.yml (discord.webhook-url).");
            return false;
        }

        StringBuilder content = new StringBuilder();
        if (entries.isEmpty()) {
            content.append("No hay rangos VIP temporales activos en este momento.");
        } else {
            content.append("Se han detectado **").append(entries.size()).append("** rangos VIP temporales activos:\\n\\n");
            for (VipEntry entry : entries) {
                String capitalizedGroup = entry.group().substring(0, 1).toUpperCase(Locale.ROOT) + entry.group().substring(1);
                boolean warning = entry.daysLeft() <= plugin.getConfig().getInt("vipalerts.warning-threshold-days", 3);
                String prefix = warning ? "⚠️" : "💎";
                String warningTag = warning ? " **[PRÓXIMO A VENCER]**" : "";

                content.append(prefix).append(" **").append(Odysseia.escapeJson(entry.playerName())).append("**")
                       .append(": VIP ").append(capitalizedGroup)
                       .append(" — **").append(entry.daysLeft()).append(" días** restantes")
                       .append(" (Vence: ").append(DATE_FORMATTER.format(Instant.ofEpochSecond(entry.expirySeconds()))).append(")")
                       .append(warningTag).append("\\n");
            }
        }

        String jsonPayload = String.format(
            "{\"username\":\"Odysseia VIP Alerts\",\"embeds\":[{" +
            "\"title\":\"👑 Reporte Diario de Vencimiento de Rangos VIP\"," +
            "\"description\":\"%s\"," +
            "\"color\":16766720," + // Gold
            "\"footer\":{\"text\":\"DrakesCraft · Sistema de Control Odysseia\"}" +
            "}]}",
            content.toString()
        );

        WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
        plugin.getLogger().info("[VipAlerts] Reporte de caducidad VIP enviado a Discord (" + entries.size() + " VIPs procesados).");
        return true;
    }

    public List<VipEntry> scanVipExpiries() {
        List<VipEntry> result = new ArrayList<>();
        File lpUsersDir = new File(plugin.getDataFolder().getParentFile(), "LuckPerms/yaml-storage/users");
        if (!lpUsersDir.exists() || !lpUsersDir.isDirectory()) {
            return result;
        }

        File[] files = lpUsersDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return result;
        }

        long nowSec = System.currentTimeMillis() / 1000L;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String playerName = config.getString("name");
                if (playerName == null || playerName.isBlank()) {
                    continue;
                }

                List<Map<?, ?>> parents = config.getMapList("parents");
                for (Map<?, ?> entry : parents) {
                    for (Map.Entry<?, ?> mapEntry : entry.entrySet()) {
                        String groupName = String.valueOf(mapEntry.getKey()).toLowerCase(Locale.ROOT);
                        if (VIP_GROUPS.contains(groupName)) {
                            if (mapEntry.getValue() instanceof Map<?, ?> details) {
                                Object expiryObj = details.get("expiry");
                                if (expiryObj != null) {
                                    long expirySec = Long.parseLong(expiryObj.toString());
                                    if (expirySec > nowSec) {
                                        long secondsLeft = expirySec - nowSec;
                                        long daysLeft = secondsLeft / 86400L;
                                        result.add(new VipEntry(playerName, groupName, expirySec, daysLeft));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        result.sort(Comparator.comparingLong(VipEntry::daysLeft));
        return result;
    }

    public record VipEntry(String playerName, String group, long expirySeconds, long daysLeft) {}
}
