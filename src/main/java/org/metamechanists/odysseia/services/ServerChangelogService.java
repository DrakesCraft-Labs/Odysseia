package org.metamechanists.odysseia.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.utils.WebhookSender;

/**
 * Servicio de auditoría, detección de cambios (delta) en inicio y despacho de changelog a Discord.
 * Totalmente seguro: no expone credenciales, contraseñas ni contenido sensible.
 */
public final class ServerChangelogService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SNAPSHOT_TYPE = new TypeToken<Map<String, PluginMeta>>() {}.getType();

    private final Odysseia plugin;

    public static class PluginMeta {
        public String name;
        public String version;
        public long fileSize;
        public long lastModified;
        public String jarName;

        public PluginMeta() {}

        public PluginMeta(String name, String version, long fileSize, long lastModified, String jarName) {
            this.name = name;
            this.version = version;
            this.fileSize = fileSize;
            this.lastModified = lastModified;
            this.jarName = jarName;
        }
    }

    public ServerChangelogService(Odysseia plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicia la verificación de delta de plugins en un hilo asíncrono con retraso para esperar que
     * todos los plugins estén listos.
     */
    public void scheduleBootAudit() {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::runAudit, 200L); // 10 segundos después del inicio
    }

    public void runAudit() {
        FileConfiguration config = plugin.getConfig();
        String webhookUrl = config.getString("discord.webhook-changelog-url", "");

        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.startsWith("REPLACE_ME")) {
            return;
        }

        try {
            File cacheDir = new File(plugin.getDataFolder(), "cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File snapshotFile = new File(cacheDir, "plugins_snapshot.json");

            Map<String, PluginMeta> previousSnapshot = new HashMap<>();
            if (snapshotFile.exists()) {
                try (FileReader reader = new FileReader(snapshotFile, StandardCharsets.UTF_8)) {
                    Map<String, PluginMeta> loaded = GSON.fromJson(reader, SNAPSHOT_TYPE);
                    if (loaded != null) {
                        previousSnapshot = loaded;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[Changelog] No se pudo leer snapshot previo: " + e.getMessage());
                }
            }

            // Escanear plugins actuales
            File pluginsDir = plugin.getDataFolder().getParentFile();
            Map<String, PluginMeta> currentSnapshot = scanPlugins(pluginsDir);

            // Si es la primera vez que se ejecuta, guardamos y salimos
            if (previousSnapshot.isEmpty()) {
                saveSnapshot(snapshotFile, currentSnapshot);
                plugin.getLogger().info("[Changelog] Primer snapshot de plugins registrado (" + currentSnapshot.size() + " plugins).");
                return;
            }

            // Calcular diferencias
            List<String> added = new ArrayList<>();
            List<String> updated = new ArrayList<>();
            List<String> removed = new ArrayList<>();

            for (Map.Entry<String, PluginMeta> entry : currentSnapshot.entrySet()) {
                String key = entry.getKey();
                PluginMeta current = entry.getValue();
                PluginMeta prev = previousSnapshot.get(key);

                if (prev == null) {
                    added.add("✨ **" + current.name + "** (v" + current.version + ")");
                } else {
                    boolean versionChanged = !Objects.equals(current.version, prev.version);
                    boolean binaryChanged = current.fileSize != prev.fileSize || Math.abs(current.lastModified - prev.lastModified) > 2000L;

                    if (versionChanged) {
                        updated.add("🔄 **" + current.name + "**: `v" + prev.version + "` ➔ `v" + current.version + "`");
                    } else if (binaryChanged) {
                        updated.add("🔄 **" + current.name + "**: v" + current.version + " *(compilación/parche actualizado)*");
                    }
                }
            }

            for (Map.Entry<String, PluginMeta> entry : previousSnapshot.entrySet()) {
                if (!currentSnapshot.containsKey(entry.getKey())) {
                    removed.add("🗑️ **" + entry.getValue().name + "** (v" + entry.getValue().version + ")");
                }
            }

            // Guardar snapshot actualizado
            saveSnapshot(snapshotFile, currentSnapshot);

            boolean hasChanges = !added.isEmpty() || !updated.isEmpty() || !removed.isEmpty();
            if (!hasChanges) {
                plugin.getLogger().info("[Changelog] Reinicio limpio: sin cambios en la suite de plugins.");
                return;
            }

            // Despachar embed a Discord
            sendDeltaEmbed(webhookUrl, added, updated, removed);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Changelog] Error durante la auditoría de inicio: " + e.getMessage(), e);
        }
    }

    private Map<String, PluginMeta> scanPlugins(File pluginsDir) {
        Map<String, PluginMeta> map = new HashMap<>();
        if (pluginsDir == null || !pluginsDir.isDirectory()) {
            return map;
        }

        File[] files = pluginsDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (files == null) return map;

        for (File jar : files) {
            try (JarFile jarFile = new JarFile(jar)) {
                JarEntry entry = jarFile.getJarEntry("plugin.yml");
                if (entry == null) entry = jarFile.getJarEntry("paper-plugin.yml");

                String name = jar.getName();
                String version = "Desconocida";

                if (entry != null) {
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                        String yName = yaml.getString("name");
                        String yVer = yaml.getString("version");
                        if (yName != null && !yName.isBlank()) name = yName;
                        if (yVer != null && !yVer.isBlank()) version = yVer;
                    }
                }

                String key = name.toLowerCase(Locale.ROOT);
                map.put(key, new PluginMeta(name, version, jar.length(), jar.lastModified(), jar.getName()));
            } catch (Exception ignored) {
                // Archivo no legible como jar de plugin
            }
        }
        return map;
    }

    private void saveSnapshot(File file, Map<String, PluginMeta> snapshot) {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(snapshot, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("[Changelog] No se pudo guardar snapshot: " + e.getMessage());
        }
    }

    private void sendDeltaEmbed(String webhookUrl, List<String> added, List<String> updated, List<String> removed) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm 'UTC'");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Santiago"));
        String dateStr = sdf.format(new Date());

        StringBuilder fields = new StringBuilder();

        if (!updated.isEmpty()) {
            fields.append("{\"name\":\"🔄 Plugins y Módulos Actualizados\",\"value\":\"")
                  .append(Odysseia.escapeJson(String.join("\\n", updated)))
                  .append("\",\"inline\":false},");
        }

        if (!added.isEmpty()) {
            fields.append("{\"name\":\"✨ Nuevos Plugins / Sistemas\",\"value\":\"")
                  .append(Odysseia.escapeJson(String.join("\\n", added)))
                  .append("\",\"inline\":false},");
        }

        if (!removed.isEmpty()) {
            fields.append("{\"name\":\"🗑️ Plugins Retirados\",\"value\":\"")
                  .append(Odysseia.escapeJson(String.join("\\n", removed)))
                  .append("\",\"inline\":false},");
        }

        // Quitar la última coma si existe
        String fieldsJson = fields.toString();
        if (fieldsJson.endsWith(",")) {
            fieldsJson = fieldsJson.substring(0, fieldsJson.length() - 1);
        }

        String jsonPayload = "{\"username\":\"DrakesCraft · Sistema de Parches\","
                + "\"avatar_url\":\"https://web.drakescraft.cl/assets/logo-drakescraft.png\","
                + "\"embeds\":[{"
                + "\"title\":\"🚀 Actualización del Servidor · Registro de Cambios\","
                + "\"description\":\"Se detectó y aplicó un nuevo lote de cambios en el reinicio del servidor.\","
                + "\"color\":9127158," // Morado oficial (#8B5CF6 = 9127158)
                + "\"fields\":[" + fieldsJson + "],"
                + "\"footer\":{\"text\":\"DrakesCraft Network · Parche verificado el " + Odysseia.escapeJson(dateStr) + "\"},"
                + "\"thumbnail\":{\"url\":\"https://web.drakescraft.cl/assets/logo-drakescraft.png\"}"
                + "}]}";

        WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
        plugin.getLogger().info("[Changelog] Notificación de parche enviada a Discord exitosamente.");
    }

    /**
     * Envía un changelog manual con formato a Discord.
     */
    public void postManualChangelog(String title, String details, String author) {
        FileConfiguration config = plugin.getConfig();
        String webhookUrl = config.getString("discord.webhook-changelog-url", "");

        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.startsWith("REPLACE_ME")) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm 'UTC'");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Santiago"));
        String dateStr = sdf.format(new Date());

        String jsonPayload = "{\"username\":\"DrakesCraft · Registro Oficial\","
                + "\"avatar_url\":\"https://web.drakescraft.cl/assets/logo-drakescraft.png\","
                + "\"embeds\":[{"
                + "\"title\":\"🚀 " + Odysseia.escapeJson(title) + "\","
                + "\"description\":\"" + Odysseia.escapeJson(details) + "\","
                + "\"color\":3066993," // Verde esmeralda (#2ecc71 = 3066993)
                + "\"fields\":["
                + "{\"name\":\"👤 Autor / Publicado por\",\"value\":\"`" + Odysseia.escapeJson(author) + "`\",\"inline\":true},"
                + "{\"name\":\"📅 Fecha\",\"value\":\"`" + Odysseia.escapeJson(dateStr) + "`\",\"inline\":true}"
                + "],"
                + "\"footer\":{\"text\":\"DrakesCraft Network · play.drakescraft.cl\"},"
                + "\"thumbnail\":{\"url\":\"https://web.drakescraft.cl/assets/logo-drakescraft.png\"}"
                + "}]}";

        WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
    }
}
