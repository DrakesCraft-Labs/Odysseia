package org.metamechanists.odysseia.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.util.logging.Level;

public final class ModerationListener implements Listener {

    private final Odysseia plugin;

    public ModerationListener(Odysseia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent e) {
        if (!plugin.getConfig().getBoolean("discord.enabled", true)) {
            return;
        }

        Player player = e.getPlayer();
        String reason = PlainTextComponentSerializer.plainText().serialize(e.reason());
        PlayerKickEvent.Cause cause = e.getCause();

        boolean isBan = (cause == PlayerKickEvent.Cause.BANNED);
        String actionType = isBan ? "BANNED" : "KICKED";

        // Creative visual/audio effects at player's location
        Location loc = player.getLocation();
        loc.getWorld().strikeLightningEffect(loc); // Silent visual lightning
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f); // Muffled explosion
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);

        // Send to webhook
        String webhookUrl = plugin.getConfig().getString("discord.webhook-moderation-url", "");
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.equals("REPLACE_ME")) {
            webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        }

        if (webhookUrl != null && !webhookUrl.isBlank() && !webhookUrl.equals("REPLACE_ME")) {
            String serverLabel = plugin.getConfig().getString("presence.server-label", "");
            if (serverLabel == null || serverLabel.isBlank()) {
                serverLabel = Bukkit.getServer().getName();
            }

            String title = isBan ? "Jugador Baneado" : "Jugador Expulsado";
            int color = isBan ? 15105570 : 16750848; // Red for ban, Orange for kick

            String jsonPayload = String.format(
                "{\"username\":\"Odysseia Moderación\",\"embeds\":[{" +
                "\"title\":\"%s\"," +
                "\"description\":\"**%s** ha sido removido del servidor.\"," +
                "\"color\":%d," +
                "\"fields\":[" +
                "{\"name\":\"Jugador\",\"value\":\"%s\",\"inline\":true}," +
                "{\"name\":\"Acción\",\"value\":\"%s\",\"inline\":true}," +
                "{\"name\":\"Razón\",\"value\":\"%s\",\"inline\":false}" +
                "]," +
                "\"footer\":{\"text\":\"%s\"}" +
                "}]}",
                Odysseia.escapeJson(title),
                Odysseia.escapeJson(player.getName()),
                color,
                Odysseia.escapeJson(player.getName()),
                actionType,
                Odysseia.escapeJson(reason),
                Odysseia.escapeJson(serverLabel)
            );

            WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent e) {
        if (!plugin.getConfig().getBoolean("discord.enabled", true)) {
            return;
        }

        if (e.getResult() == PlayerLoginEvent.Result.KICK_BANNED) {
            String webhookUrl = plugin.getConfig().getString("discord.webhook-moderation-url", "");
            if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.equals("REPLACE_ME")) {
                webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
            }

            if (webhookUrl != null && !webhookUrl.isBlank() && !webhookUrl.equals("REPLACE_ME")) {
                String serverLabel = plugin.getConfig().getString("presence.server-label", "");
                if (serverLabel == null || serverLabel.isBlank()) {
                    serverLabel = Bukkit.getServer().getName();
                }

                String reason = e.getKickMessage();
                String jsonPayload = String.format(
                    "{\"username\":\"Odysseia Moderación\",\"embeds\":[{" +
                    "\"title\":\"Intento de Entrada (Baneado)\"," +
                    "\"description\":\"El jugador baneado **%s** intentó conectarse.\"," +
                    "\"color\":15158332," + // Dark Red
                    "\"fields\":[" +
                    "{\"name\":\"Jugador\",\"value\":\"%s\",\"inline\":true}," +
                    "{\"name\":\"IP\",\"value\":\"%s\",\"inline\":true}," +
                    "{\"name\":\"Mensaje de Kick\",\"value\":\"%s\",\"inline\":false}" +
                    "]," +
                    "\"footer\":{\"text\":\"%s\"}" +
                    "}]}",
                    Odysseia.escapeJson(e.getPlayer().getName()),
                    Odysseia.escapeJson(e.getPlayer().getName()),
                    Odysseia.escapeJson(e.getAddress().getHostAddress()),
                    Odysseia.escapeJson(reason),
                    Odysseia.escapeJson(serverLabel)
                );

                WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
            }
        }
    }
}
