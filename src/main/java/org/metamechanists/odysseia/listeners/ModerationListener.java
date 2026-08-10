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
        Player player = event.getPlayer();
        String reason = PlainTextComponentSerializer.plainText().serialize(event.reason());
        if (isOperationalKick(reason)) {
            return;
        }

        boolean isBan = event.getCause() == PlayerKickEvent.Cause.BANNED;

        // Keep the in-game feedback for real sanctions, never for maintenance kicks.
        Location location = player.getLocation();
        location.getWorld().strikeLightningEffect(location);
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        location.getWorld().spawnParticle(Particle.EXPLOSION, location.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);

        // Va antes que Discord y sin depender de el: si el webhook falta o esta mal puesto, quien
        // esta jugando debe enterarse igual. Antes ambos avisos colgaban del mismo return.
        anunciarEnElServidor(player.getName(), reason, isBan);

        if (!plugin.getConfig().getBoolean("discord.enabled", true)) {
            return;
        }
        String webhookUrl = moderationWebhook();
        if (webhookUrl == null) {
            return;
        }

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

    /**
     * Cuenta la sancion en el chat del servidor.
     *
     * El aviso solo salia por Discord, asi que quien estaba jugando veia desaparecer a alguien sin
     * saber por que. Enterarse dentro es justamente lo que hace que una sancion tenga efecto sobre
     * los demas.
     *
     * Los kicks por inactividad se anuncian con otro tono: son automaticos, pasan a diario y
     * tratarlos como un castigo confundiria a quien los lea.
     */
    private void anunciarEnElServidor(String nombre, String motivo, boolean esBaneo) {
        if (!plugin.getConfig().getBoolean("moderacion.anuncio-ingame.enabled", true)) return;

        boolean porInactividad = motivo != null
                && motivo.toLowerCase(Locale.ROOT).contains("inactiv");
        if (porInactividad && !plugin.getConfig().getBoolean("moderacion.anuncio-ingame.incluir-afk", true)) {
            return;
        }

        String texto;
        if (porInactividad) {
            texto = "&8[&7Servidor&8] &7" + nombre + " &8fue desconectado por inactividad.";
        } else if (esBaneo) {
            texto = "&6DrakesCraft &8· &c&l" + nombre + " &cha sido baneado."
                    + (motivo == null || motivo.isBlank() ? "" : " &7Motivo: &f" + motivo);
        } else {
            texto = "&6DrakesCraft &8· &e" + nombre + " &6ha sido expulsado."
                    + (motivo == null || motivo.isBlank() ? "" : " &7Motivo: &f" + motivo);
        }

        String mensaje = org.bukkit.ChatColor.translateAlternateColorCodes('&', texto);
        Bukkit.getOnlinePlayers().forEach(destinatario -> destinatario.sendMessage(mensaje));
        plugin.getLogger().info("[Moderation] " + org.bukkit.ChatColor.stripColor(mensaje));
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
                || normalized.contains("maintenance") || normalized.contains("mantenimiento")
                // Network disconnects are not moderation actions and must never alert staff.
                || normalized.contains("timed out") || normalized.contains("timeout")
                || normalized.contains("connection reset") || normalized.contains("connection closed")
                || normalized.contains("forcibly closed") || normalized.contains("disconnected");
    }
}
