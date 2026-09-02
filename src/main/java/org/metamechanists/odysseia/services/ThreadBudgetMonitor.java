package org.metamechanists.odysseia.services;

import org.bukkit.Bukkit;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Vigila el presupuesto de hilos de la JVM y avisa antes de que el contenedor agote sus PIDs.
 *
 * <p>El ticket 217 documenta tres caidas por {@code OutOfMemoryError: unable to create native thread}:
 * no es falta de heap, sino el techo de ~512 PIDs del contenedor. El pool async de Bukkit
 * (SynchronousQueue sin maximumPoolSize) crea un hilo nuevo por cada tarea que no encuentra worker
 * libre, y los plugins que bloquean workers con Thread.sleep o HTTP sincrono impiden que se reciclen.
 * La causa vive en codigo de terceros, asi que aqui solo se instrumenta: convertir una caida sorpresa
 * en un aviso anticipado con el reparto por familia de hilos, que es lo que hoy solo se obtiene del
 * volcado post-mortem.
 */
public final class ThreadBudgetMonitor {
    private static final String LOG_TAG = "[ThreadBudget]";
    /** Familias con mas hilos que se detallan en el aviso. */
    private static final int TOP_FAMILIES = 8;

    private final Odysseia plugin;
    /** Ultimo conteo que genero aviso; evita repetir la misma linea cada ciclo. */
    private int lastAlertCount = -1;
    private long lastAlertAtMillis;
    private int peakCount;

    public ThreadBudgetMonitor(Odysseia plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("threadbudget.enabled", true)) {
            plugin.getLogger().info(LOG_TAG + " Deshabilitado en config.yml.");
            return;
        }

        long periodTicks = Math.max(20L, plugin.getConfig().getLong("threadbudget.period-seconds", 120L) * 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                sample();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, LOG_TAG + " Error al muestrear el presupuesto de hilos", e);
            }
        }, periodTicks, periodTicks);

        plugin.getLogger().info(LOG_TAG + " Vigilancia de hilos activa (cada "
                + (periodTicks / 20L) + "s, aviso en " + warnThreshold() + ", critico en " + criticalThreshold() + ").");
    }

    /** Muestrea el conteo de hilos y avisa si cruza los umbrales. Visible para pruebas. */
    public void sample() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        int count = threads.getThreadCount();
        peakCount = Math.max(peakCount, count);

        int warn = warnThreshold();
        if (count < warn) {
            lastAlertCount = -1;
            return;
        }

        if (!shouldAlert(count)) {
            return;
        }

        boolean critical = count >= criticalThreshold();
        Map<String, Integer> families = breakdown(threads);
        String detalle = formatBreakdown(families, count);

        lastAlertCount = count;
        lastAlertAtMillis = System.currentTimeMillis();

        plugin.getLogger().log(critical ? Level.SEVERE : Level.WARNING,
                LOG_TAG + (critical ? " CRITICO: " : " ") + count + " hilos vivos (pico " + peakCount
                        + ", aviso " + warn + ", techo estimado del contenedor "
                        + containerPidCeiling() + "). Reparto: " + detalle);

        if (critical) {
            notifyDiscord(count, detalle);
        }
    }

    /**
     * Un aviso por escalon: solo se repite si el conteo sube otro tramo o si vencio el enfriamiento.
     * Sin esto el log se inunda con la misma linea cada ciclo mientras dure la escalada.
     */
    private boolean shouldAlert(int count) {
        if (lastAlertCount < 0) {
            return true;
        }
        int step = Math.max(1, plugin.getConfig().getInt("threadbudget.realert-step", 25));
        if (count >= lastAlertCount + step) {
            return true;
        }
        long cooldownMillis = plugin.getConfig().getLong("threadbudget.realert-cooldown-minutes", 15L) * 60_000L;
        return System.currentTimeMillis() - lastAlertAtMillis >= cooldownMillis;
    }

    /**
     * Agrupa los hilos vivos por familia. Se pide la informacion con profundidad de pila 0 para no
     * pagar el coste de un volcado completo; solo interesan los nombres.
     */
    private Map<String, Integer> breakdown(ThreadMXBean threads) {
        Map<String, Integer> families = new HashMap<>();
        long[] ids = threads.getAllThreadIds();
        ThreadInfo[] infos = threads.getThreadInfo(ids, 0);
        for (ThreadInfo info : infos) {
            // getThreadInfo devuelve null si el hilo murio entre la enumeracion y la consulta.
            if (info == null) {
                continue;
            }
            families.merge(familyOf(info.getThreadName()), 1, Integer::sum);
        }
        return families;
    }

    /** Normaliza "Craft Scheduler Thread - 407" a "Craft Scheduler Thread" para poder agrupar. */
    static String familyOf(String threadName) {
        if (threadName == null || threadName.isBlank()) {
            return "(sin nombre)";
        }
        String name = threadName.trim();
        // Recorta sufijos de indice: " - 12", "-12", " #12", " 12".
        int cut = name.length();
        while (cut > 0 && Character.isDigit(name.charAt(cut - 1))) {
            cut--;
        }
        if (cut == name.length() || cut == 0) {
            return name;
        }
        String head = name.substring(0, cut);
        String trimmed = head.replaceAll("[\\s\\-#_]+$", "");
        return trimmed.isEmpty() ? name : trimmed;
    }

    private String formatBreakdown(Map<String, Integer> families, int total) {
        List<Map.Entry<String, Integer>> top = new ArrayList<>(families.entrySet());
        top.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));

        StringBuilder sb = new StringBuilder();
        int shown = 0;
        int accounted = 0;
        for (Map.Entry<String, Integer> entry : top) {
            if (shown >= TOP_FAMILIES) {
                break;
            }
            if (shown > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            accounted += entry.getValue();
            shown++;
        }
        int resto = total - accounted;
        if (resto > 0) {
            sb.append(", otros=").append(resto);
        }
        return sb.toString();
    }

    private void notifyDiscord(int count, String detalle) {
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.contains("REPLACE_ME")) {
            return;
        }
        String jsonPayload = String.format(
                "{\"username\":\"Odysseia ThreadBudget\",\"embeds\":[{"
                        + "\"title\":\"Presupuesto de hilos en zona critica\","
                        + "\"description\":\"**%d** hilos vivos (techo estimado del contenedor %d).\\n\\n%s\","
                        + "\"color\":15158332,"
                        + "\"footer\":{\"text\":\"DrakesCraft · Sistema de Control Odysseia\"}"
                        + "}]}",
                count, containerPidCeiling(), Odysseia.escapeJson(detalle));
        WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
    }

    private int warnThreshold() {
        return plugin.getConfig().getInt("threadbudget.warn-threshold", 380);
    }

    private int criticalThreshold() {
        return plugin.getConfig().getInt("threadbudget.critical-threshold", 450);
    }

    private int containerPidCeiling() {
        return plugin.getConfig().getInt("threadbudget.container-pid-ceiling", 512);
    }

    public int getPeakCount() {
        return peakCount;
    }
}
