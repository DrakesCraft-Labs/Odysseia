package org.metamechanists.odysseia.listeners;

import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.utils.WebhookSender;

/** Reports actual moderation actions to the dedicated Discord channel. */
public final class ModerationListener implements Listener {

    private final Odysseia plugin;

    public ModerationListener(Odysseia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        if (!plugin.getConfig().getBoolean("discord.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        String reason = PlainTextComponentSerializer.plainText().serialize(event.reason());
        if (isOperationalKick(reason)) {
            return;
        }

        boolean isBan = event.getCause() == PlayerKickEvent.Cause.BANNED;
        String webhookUrl = moderationWebhook();
        if (webhookUrl == null) {
            return;
        }

        // Keep the in-game feedback for real sanctions, never for maintenance kicks.
        Location location = player.getLocation();
        location.getWorld().strikeLightningEffect(location);
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        location.getWorld().spawnParticle(Particle.EXPLOSION, location.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);

        String serverLabel = serverLabel();
        String title = isBan ? "Sanción aplicada · Baneo" : "Sanción aplicada · Expulsión";
        int color = isBan ? 15105570 : 16750848;
        String action = isBan ? "BANEADO" : "EXPULSADO";
        String jsonPayload = String.format(
                "{\"username\":\"Odysseia Moderación\",\"embeds\":[{"
                        + "\"title\":\"%s\",\"description\":\"Se registró una acción de moderación en DrakesCraft.\","
                        + "\"color\":%d,\"fields\":["
                        + "{\"name\":\"Jugador\",\"value\":\"`%s`\",\"inline\":true},"
                        + "{\"name\":\"Acción\",\"value\":\"%s\",\"inline\":true},"
                        + "{\"name\":\"Motivo\",\"value\":\"%s\",\"inline\":false}],"
                        + "\"footer\":{\"text\":\"%s\"}}]}",
                Odysseia.escapeJson(title), color, Odysseia.escapeJson(player.getName()), action,
                Odysseia.escapeJson(reason), Odysseia.escapeJson(serverLabel));
        WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.getConfig().getBoolean("discord.enabled", true)
                || event.getResult() != PlayerLoginEvent.Result.KICK_BANNED) {
            return;
        }

        String webhookUrl = moderationWebhook();
        if (webhookUrl == null) {
            return;
        }

        String jsonPayload = String.format(
                "{\"username\":\"Odysseia Moderación\",\"embeds\":[{"
                        + "\"title\":\"Acceso bloqueado · Jugador baneado\","
                        + "\"description\":\"Un jugador baneado intentó conectarse a DrakesCraft.\","
                        + "\"color\":15158332,\"fields\":["
                        + "{\"name\":\"Jugador\",\"value\":\"`%s`\",\"inline\":true},"
                        + "{\"name\":\"Mensaje de bloqueo\",\"value\":\"%s\",\"inline\":false}],"
                        + "\"footer\":{\"text\":\"%s\"}}]}",
                Odysseia.escapeJson(event.getPlayer().getName()),
                Odysseia.escapeJson(event.getKickMessage()), Odysseia.escapeJson(serverLabel()));
        WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
    }

    private String moderationWebhook() {
        String url = plugin.getConfig().getString("discord.webhook-moderation-url", "");
        if (url == null || url.isBlank() || url.startsWith("REPLACE_ME")
                || !WebhookSender.isDiscordWebhookUrl(url) || !WebhookSender.isAllowedHttpsUrl(url)) {
            plugin.getLogger().warning("[Moderation] Webhook de moderación inválido o no configurado.");
            return null;
        }
        return url;
    }

    private String serverLabel() {
        String label = plugin.getConfig().getString("presence.server-label", "");
        return label == null || label.isBlank() ? Bukkit.getServer().getName() : label;
    }

    private boolean isOperationalKick(String reason) {
        String normalized = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        return normalized.contains("reinicio") || normalized.contains("restarting")
                || normalized.contains("maintenance") || normalized.contains("mantenimiento");
    }
}
