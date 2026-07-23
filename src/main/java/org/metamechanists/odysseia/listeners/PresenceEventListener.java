package org.metamechanists.odysseia.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PresenceEventListener implements Listener {

    private final Odysseia plugin;
    private int countInWindow;
    private long windowStartMs;

    public PresenceEventListener(Odysseia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("presence.enabled", true)
                || !plugin.getConfig().getBoolean("presence.events.player-join", false)) {
            return;
        }

        Player p = event.getPlayer();
        if (plugin.isVanished(p)) {
            return; // Don't broadcast vanished staff joining
        }

        if (!allowRate()) {
            return;
        }

        String serverLabel = getServerLabel();
        String jsonPayload = String.format(
            "{\"username\":\"Odysseia Presencia\",\"embeds\":[{" +
            "\"title\":\"Entrada\"," +
            "\"description\":\"**%s** entró al servidor.\"," +
            "\"color\":3447003," + // Green
            "\"footer\":{\"text\":\"%s\"}" +
            "}]}",
            Odysseia.escapeJson(p.getName()),
            Odysseia.escapeJson(serverLabel)
        );

        sendWebhook(jsonPayload);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("presence.enabled", true)
                || !plugin.getConfig().getBoolean("presence.events.player-quit", false)) {
            return;
        }

        Player p = event.getPlayer();
        if (plugin.isVanished(p)) {
            return; // Don't broadcast vanished staff quitting
        }

        if (!allowRate()) {
            return;
        }

        String serverLabel = getServerLabel();
        String jsonPayload = String.format(
            "{\"username\":\"Odysseia Presencia\",\"embeds\":[{" +
            "\"title\":\"Salida\"," +
            "\"description\":\"**%s** salió del servidor.\"," +
            "\"color\":9807270," + // Grey/Red
            "\"footer\":{\"text\":\"%s\"}" +
            "}]}",
            Odysseia.escapeJson(p.getName()),
            Odysseia.escapeJson(serverLabel)
        );

        sendWebhook(jsonPayload);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("presence.enabled", true)
                || !plugin.getConfig().getBoolean("presence.events.player-death", true)) {
            return;
        }

        Player p = event.getEntity();
        if (plugin.isVanished(p)) {
            return; // Don't broadcast vanished staff deaths
        }

        if (!allowRate()) {
            return;
        }

        String msg = "";
        if (event.deathMessage() != null) {
            msg = PlainTextComponentSerializer.plainText().serialize(event.deathMessage());
        } else {
            msg = p.getName() + " murió.";
        }

        String serverLabel = getServerLabel();
        String jsonPayload = String.format(
            "{\"username\":\"Odysseia Presencia\",\"embeds\":[{" +
            "\"title\":\"Muerte\"," +
            "\"description\":\"%s\"," +
            "\"color\":15158332," + // Dark Red
            "\"footer\":{\"text\":\"%s\"}" +
            "}]}",
            Odysseia.escapeJson(msg),
            Odysseia.escapeJson(serverLabel)
        );

        sendWebhook(jsonPayload);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!plugin.getConfig().getBoolean("presence.enabled", true)
                || !plugin.getConfig().getBoolean("presence.events.advancement", true)) {
            return;
        }

        Player p = event.getPlayer();
        if (plugin.isVanished(p)) {
            return;
        }

        // Hardening: Only announce achievements/advancements that have display info (ignoring recipes/internal keys)
        if (event.getAdvancement().getDisplay() == null) {
            return;
        }

        if (!allowRate()) {
            return;
        }

        String advancementTitle = PlainTextComponentSerializer.plainText().serialize(event.getAdvancement().getDisplay().title());
        String serverLabel = getServerLabel();

        String jsonPayload = String.format(
            "{\"username\":\"Odysseia Presencia\",\"embeds\":[{" +
            "\"title\":\"Logro Completado\"," +
            "\"description\":\"**%s** ha completado el logro **%s**.\"," +
            "\"color\":10181046," + // Purple
            "\"footer\":{\"text\":\"%s\"}" +
            "}]}",
            Odysseia.escapeJson(p.getName()),
            Odysseia.escapeJson(advancementTitle),
            Odysseia.escapeJson(serverLabel)
        );

        sendWebhook(jsonPayload);
    }

    private void sendWebhook(String jsonPayload) {
        String url = plugin.getConfig().getString("discord.webhook-url", "");
        if (url != null && !url.isBlank() && !url.equals("REPLACE_ME")) {
            WebhookSender.sendAsync(plugin, url, jsonPayload);
        }
    }

    private String getServerLabel() {
        String serverLabel = plugin.getConfig().getString("presence.server-label", "");
        if (serverLabel == null || serverLabel.isBlank()) {
            serverLabel = Bukkit.getServer().getName();
        }
        return serverLabel;
    }

    private synchronized boolean allowRate() {
        int max = plugin.getConfig().getInt("presence.events.rate-limit-per-minute", 12);
        if (max <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - windowStartMs > 60_000L) {
            windowStartMs = now;
            countInWindow = 0;
        }
        if (countInWindow >= max) {
            return false;
        }
        countInWindow++;
        return true;
    }
}
