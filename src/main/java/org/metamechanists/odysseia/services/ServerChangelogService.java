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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    private static final int DISCORD_FIELD_VALUE_LIMIT = 1_000;

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
            ManifestResult manifestResult = publishVerifiedManifest(webhookUrl);
            if (manifestResult == ManifestResult.PUBLISHED || manifestResult == ManifestResult.DUPLICATE) {
                return;
            }

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

            // Sin manifiesto VERIFIED, esto es sólo una observación de binarios tras el boot.
            sendDeltaEmbed(webhookUrl, added, updated, removed);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Changelog] Error durante la auditoría de inicio: " + e.getMessage(), e);
        }
    }

    private ManifestResult publishVerifiedManifest(String webhookUrl) {
        String manifestName = safeDataFileName(
                plugin.getConfig().getString("discord.changelog-manifest-file", "release-manifest.json"));
        String secretName = safeDataFileName(
                plugin.getConfig().getString("discord.changelog-manifest-secret-file", "release-manifest.secret"));
        if (manifestName == null || secretName == null) {
            plugin.getLogger().warning("[Changelog] Nombres de manifiesto o secreto no seguros; se omiten.");
            return ManifestResult.INVALID;
        }

        File manifestFile = new File(plugin.getDataFolder(), manifestName);
        File secretFile = new File(plugin.getDataFolder(), secretName);
        if (!manifestFile.isFile()) {
            return ManifestResult.ABSENT;
        }
        if (!secretFile.isFile()) {
            plugin.getLogger().warning("[Changelog] Hay manifiesto, pero falta su secreto HMAC privado.");
            return ManifestResult.INVALID;
        }

        try {
            byte[] secret = Files.readAllBytes(secretFile.toPath());
            String json = Files.readString(manifestFile.toPath(), StandardCharsets.UTF_8);
            ReleaseManifest.Verification verification = ReleaseManifest.verify(json, secret);
            Arrays.fill(secret, (byte) 0);
            if (verification.status() == ReleaseManifest.Verification.Status.IGNORED) {
                plugin.getLogger().info("[Changelog] Manifiesto no publicable: " + verification.reason());
                return ManifestResult.IGNORED;
            }
            if (verification.status() != ReleaseManifest.Verification.Status.VERIFIED) {
                plugin.getLogger().warning("[Changelog] Manifiesto rechazado: " + verification.reason());
                return ManifestResult.INVALID;
            }

            ReleaseManifest manifest = verification.manifest();
            File deliveredFile = new File(new File(plugin.getDataFolder(), "cache"), "last_release_id.txt");
            if (deliveredFile.isFile()
                    && manifest.releaseId.equals(Files.readString(deliveredFile.toPath(), StandardCharsets.UTF_8).trim())) {
                plugin.getLogger().info("[Changelog] Release ya publicada: " + manifest.releaseId);
                return ManifestResult.DUPLICATE;
            }

            sendVerifiedManifestEmbed(webhookUrl, manifest, deliveredFile);
            plugin.getLogger().info("[Changelog] Manifiesto VERIFIED encolado: " + manifest.releaseId);
            return ManifestResult.PUBLISHED;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Changelog] No se pudo procesar el manifiesto: " + e.getMessage(), e);
            return ManifestResult.INVALID;
        }
    }

    private void sendVerifiedManifestEmbed(String webhookUrl, ReleaseManifest manifest, File deliveredFile) {
        List<String> details = manifest.technicalDetails.stream()
                .map(ReleaseManifest::technicalLine)
                .toList();
        List<String> technicalFields = technicalDetailFields(details);
        StringBuilder fieldsJson = new StringBuilder();
        for (int i = 0; i < technicalFields.size(); i++) {
            if (i > 0) fieldsJson.append(',');
            String name = i == 0 ? "Detalle técnico" : "Detalle técnico (continuación)";
            fieldsJson.append("{\"name\":\"")
                    .append(name)
                    .append("\",\"value\":\"")
                    .append(Odysseia.escapeJson(technicalFields.get(i)))
                    .append("\",\"inline\":false}");
        }
        String jsonPayload = "{\"username\":\"DrakesCraft · Sistema de Parches\","
                + "\"allowed_mentions\":{\"parse\":[]},"
                + "\"embeds\":[{"
                + "\"title\":\"✅ Lote verificado · " + Odysseia.escapeJson(manifest.releaseId) + "\","
                + "\"description\":\"" + Odysseia.escapeJson(manifest.playerSummary) + "\","
                + "\"color\":3066993,"
                + "\"fields\":["
                + fieldsJson + ","
                + "{\"name\":\"Salud postarranque\",\"value\":\"`"
                + Odysseia.escapeJson(manifest.health.status()) + "` · "
                + Odysseia.escapeJson(manifest.health.summary()) + "\",\"inline\":false}"
                + "]}]}";
        WebhookSender.sendAsyncTracked(plugin, webhookUrl, jsonPayload).thenAccept(delivered -> {
            if (!delivered) {
                plugin.getLogger().warning("[Changelog] Manifiesto no entregado; se conserva para reintento: "
                        + manifest.releaseId);
                return;
            }
            try {
                saveDeliveredReleaseId(deliveredFile, manifest.releaseId);
                plugin.getLogger().info("[Changelog] Manifiesto entregado y deduplicado: " + manifest.releaseId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[Changelog] Entregado, pero no se pudo persistir su deduplicacion: " + e.getMessage(), e);
            }
        });
    }

    static List<String> technicalDetailFields(List<String> lines) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (line.length() > DISCORD_FIELD_VALUE_LIMIT) {
                throw new IllegalArgumentException("detalle tecnico individual demasiado largo");
            }
            if (!current.isEmpty() && current.length() + 1 + line.length() > DISCORD_FIELD_VALUE_LIMIT) {
                fields.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append('\n');
            current.append(line);
        }
        if (!current.isEmpty()) fields.add(current.toString());
        return List.copyOf(fields);
    }

    private void saveDeliveredReleaseId(File deliveredFile, String releaseId) throws Exception {
        File parent = deliveredFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("no se pudo crear cache de changelog");
        }
        File temporary = new File(parent, deliveredFile.getName() + ".tmp");
        Files.writeString(temporary.toPath(), releaseId + "\n", StandardCharsets.UTF_8);
        Files.move(temporary.toPath(), deliveredFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String safeDataFileName(String value) {
        if (value == null || value.isBlank() || value.contains("/") || value.contains("\\")
                || value.equals(".") || value.equals("..")) {
            return null;
        }
        return value;
    }

    private enum ManifestResult { ABSENT, IGNORED, INVALID, DUPLICATE, PUBLISHED }

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
                + "\"description\":\"Sincronización de módulos y binarios aplicada durante la secuencia de inicio.\","
                + "\"color\":9127158," // Morado oficial (#8B5CF6 = 9127158)
                + "\"fields\":[" + fieldsJson + "],"
                + "\"footer\":{\"text\":\"DrakesCraft Network · Observado el " + Odysseia.escapeJson(dateStr) + "\"},"
                + "\"thumbnail\":{\"url\":\"https://web.drakescraft.cl/assets/logo-drakescraft.png\"}"
                + "}]}";

        WebhookSender.sendAsync(plugin, webhookUrl, jsonPayload);
        plugin.getLogger().info("[Changelog] Observación de delta binario encolada para Discord.");
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
