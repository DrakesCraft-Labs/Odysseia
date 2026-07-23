package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.metamechanists.odysseia.Odysseia;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Escucha comandos de DiscordSRV (como tps, !tps, list, lista) y responde
 * con el rendimiento del servidor (TPS 1m, 5m, 15m, RAM y jugadores en línea).
 */
public final class DiscordSRVCommandListener implements Listener {

    private final Odysseia plugin;

    public DiscordSRVCommandListener(Odysseia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDiscordMessage(org.bukkit.event.Event event) {
        // Soporta DiscordGuildMessageReceivedEvent y DiscordChatMessageEvent vía reflexión
        String eventClassName = event.getClass().getName();
        if (!eventClassName.contains("DiscordGuildMessageReceivedEvent")
                && !eventClassName.contains("DiscordChatMessageEvent")) {
            return;
        }

        try {
            Method getMessageMethod = event.getClass().getMethod("getMessage");
            Object messageObj = getMessageMethod.invoke(event);
            if (messageObj == null) return;

            Method getContentRawMethod = messageObj.getClass().getMethod("getContentRaw");
            String rawContent = (String) getContentRawMethod.invoke(messageObj);
            if (rawContent == null) return;

            String cleaned = rawContent.trim().toLowerCase(Locale.ROOT);
            if (cleaned.startsWith("!")) cleaned = cleaned.substring(1);
            if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);

            if (cleaned.equals("tps") || cleaned.equals("list") || cleaned.equals("lista") || cleaned.equals("players")) {
                Method getChannelMethod = messageObj.getClass().getMethod("getChannel");
                Object channelObj = getChannelMethod.invoke(messageObj);
                if (channelObj == null) return;

                String response = buildStatusResponse();
                Method sendMessageMethod = channelObj.getClass().getMethod("sendMessage", CharSequence.class);
                Object action = sendMessageMethod.invoke(channelObj, response);
                if (action != null) {
                    try {
                        Method queueMethod = action.getClass().getMethod("queue");
                        queueMethod.invoke(action);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().fine("[DiscordSRVListener] Evento omitido: " + ex.getMessage());
        }
    }

    public static String buildStatusResponse() {
        double[] tps = Bukkit.getTPS();
        double tps1m = Math.min(20.0, Math.max(0.0, tps[0]));
        double tps5m = Math.min(20.0, Math.max(0.0, tps[1]));
        double tps15m = Math.min(20.0, Math.max(0.0, tps[2]));

        String statusIndicator = tps1m >= 19.5 ? "🟢 Excelente" : (tps1m >= 17.0 ? "🟡 Aceptable" : "🔴 Carga Alta");

        Runtime runtime = Runtime.getRuntime();
        long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMem = runtime.maxMemory() / (1024 * 1024);

        int onlineCount = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();

        StringBuilder sb = new StringBuilder();
        sb.append("📊 **Estado del Servidor DrakesCraft**\n");
        sb.append("• **Rendimiento:** ").append(statusIndicator).append("\n");
        sb.append("• **TPS (1m / 5m / 15m):** `")
          .append(String.format(Locale.ROOT, "%.2f", tps1m)).append("` | `")
          .append(String.format(Locale.ROOT, "%.2f", tps5m)).append("` | `")
          .append(String.format(Locale.ROOT, "%.2f", tps15m)).append("`\n");
        sb.append("• **Memoria RAM:** `").append(usedMem).append(" MB / ").append(maxMem).append(" MB`\n");
        sb.append("• **Jugadores Conectados:** `").append(onlineCount).append(" / ").append(maxPlayers).append("`\n");

        if (onlineCount > 0) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            sb.append("👥 **Lista:** ").append(String.join(", ", names));
        } else {
            sb.append("👥 **Lista:** *No hay jugadores conectados en este momento.*");
        }

        return sb.toString();
    }
}
