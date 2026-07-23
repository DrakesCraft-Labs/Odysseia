package org.metamechanists.odysseia.integrations;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.Odysseia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio de Puente de Traducción Bidireccional entre DiscordSRV y Minecraft
 * utilizando la API de Star Translate (LibreTranslate en https://web.drakescraft.cl/api/translate).
 */
@Log
public class DiscordTranslationBridgeService {

    private final Odysseia plugin;
    private final HttpClient httpClient;

    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private boolean translateDiscordToMc;
    private boolean translateMcToDiscord;
    private String mcTargetLanguage;
    private String discordTargetLanguage;

    private boolean subscribed = false;

    public DiscordTranslationBridgeService(Odysseia plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("discord-translator.enabled", true);
        String rawUrl = config.getString("discord-translator.api-url", "https://web.drakescraft.cl/api/translate");
        if (rawUrl.endsWith("/translate")) {
            this.baseUrl = rawUrl;
        } else {
            this.baseUrl = rawUrl.replaceAll("/+$", "") + "/translate";
        }

        this.apiKey = config.getString("discord-translator.api-key", "d5a53de75eb935d9203fbb302554d18ecad29db08ca46e10b7aee04fea9791ed");
        this.translateDiscordToMc = config.getBoolean("discord-translator.translate-discord-to-mc", true);
        this.translateMcToDiscord = config.getBoolean("discord-translator.translate-mc-to-discord", true);
        this.mcTargetLanguage = config.getString("discord-translator.mc-target-language", "es");
        this.discordTargetLanguage = config.getString("discord-translator.discord-target-language", "en");

        if (enabled && Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            registerDiscordSRV();
        }
    }

    public void registerDiscordSRV() {
        if (!subscribed && Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            try {
                DiscordSRV.api.subscribe(this);
                subscribed = true;
                log.info("[Odysseia] Puente de traducción bidireccional registrado exitosamente con DiscordSRV.");
            } catch (Throwable t) {
                log.warning("[Odysseia] No se pudo suscribir el servicio a DiscordSRV: " + t.getMessage());
            }
        }
    }

    public void unregisterDiscordSRV() {
        if (subscribed && Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            try {
                DiscordSRV.api.unsubscribe(this);
                subscribed = false;
                log.info("[Odysseia] Puente de traducción desuscrito de DiscordSRV.");
            } catch (Throwable t) {
                // Silenciar en apagado
            }
        }
    }

    /**
     * Detección de idioma a través del endpoint Star /detect
     */
    public CompletableFuture<String> detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture("");
        }

        String detectUrl = baseUrl.replaceAll("/translate$", "/detect");
        String jsonPayload = String.format("{\"q\":%s,\"api_key\":%s}",
                escapeJson(text), escapeJson(apiKey));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(detectUrl))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Odysseia/1.1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        // Estructura esperada: [{"confidence":90,"language":"en"}]
                        Pattern pattern = Pattern.compile("\"language\"\\s*:\\s*\"([^\"]+)\"");
                        Matcher matcher = pattern.matcher(body);
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    }
                    return "";
                }).exceptionally(ex -> {
                    log.fine("[Odysseia] Excepción al detectar idioma en Star Translate: " + ex.getMessage());
                    return "";
                });
    }

    /**
     * Traducción asíncrona de texto a través del endpoint Star /translate
     */
    public CompletableFuture<String> translateText(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture("");
        }

        String translateUrl = baseUrl.endsWith("/translate") ? baseUrl : baseUrl + "/translate";
        String jsonPayload = String.format("{\"q\":%s,\"source\":%s,\"target\":%s,\"api_key\":%s}",
                escapeJson(text), escapeJson(sourceLang), escapeJson(targetLang), escapeJson(apiKey));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(translateUrl))
                .timeout(Duration.ofSeconds(4))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Odysseia/1.1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        // Estructura esperada: {"translatedText":"..."}
                        Pattern pattern = Pattern.compile("\"translatedText\"\\s*:\\s*\"([^\"]+)\"");
                        Matcher matcher = pattern.matcher(body);
                        if (matcher.find()) {
                            return unescapeJson(matcher.group(1));
                        }
                    }
                    return "";
                }).exceptionally(ex -> {
                    log.fine("[Odysseia] Excepción al traducir en Star Translate: " + ex.getMessage());
                    return "";
                });
    }

    /**
     * Evento DiscordSRV: Mensaje recibido desde Discord hacia Minecraft
     */
    @Subscribe
    public void onDiscordMessageReceived(DiscordGuildMessageReceivedEvent event) {
        if (!enabled || !translateDiscordToMc) {
            return;
        }

        // Ignorar bots o webhooks
        if (event.getAuthor() == null || event.getAuthor().isBot()) {
            return;
        }

        String messageContent = event.getMessage().getContentDisplay();
        if (messageContent == null || messageContent.trim().isEmpty() || messageContent.startsWith("!")) {
            return;
        }

        String authorName = event.getAuthor().getName();

        // 1. Detectar idioma de origen
        detectLanguage(messageContent).thenAccept(detectedLang -> {
            if (detectedLang.isEmpty() || detectedLang.equalsIgnoreCase(mcTargetLanguage)) {
                return; // Ya está en el idioma destino de Minecraft (ej. español)
            }

            // 2. Traducir al idioma destino de MC
            translateText(messageContent, detectedLang, mcTargetLanguage).thenAccept(translatedText -> {
                if (translatedText.isEmpty() || translatedText.equalsIgnoreCase(messageContent)) {
                    return;
                }

                // 3. Enviar mensaje formateado a los jugadores en Minecraft
                String formattedNotice = ChatColor.translateAlternateColorCodes('&',
                        "&8[&bDiscord&8] &7" + authorName + "&f: " + messageContent +
                                " &8(&eTraducido: &f" + translatedText + "&8)");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(formattedNotice);
                    }
                });
            });
        });
    }

    /**
     * Evento DiscordSRV: Mensaje enviado desde Minecraft hacia Discord
     */
    @Subscribe
    public void onMinecraftMessageToDiscord(GameChatMessagePreProcessEvent event) {
        if (!enabled || !translateMcToDiscord) {
            return;
        }

        String messageContent = event.getMessage();
        if (messageContent == null || messageContent.trim().isEmpty()) {
            return;
        }

        // 1. Detectar idioma del jugador de MC
        detectLanguage(messageContent).thenAccept(detectedLang -> {
            if (detectedLang.isEmpty() || detectedLang.equalsIgnoreCase(discordTargetLanguage)) {
                return; // Ya está en el idioma objetivo de Discord (ej. inglés)
            }

            // 2. Traducir al idioma destino de Discord
            translateText(messageContent, detectedLang, discordTargetLanguage).thenAccept(translatedText -> {
                if (translatedText.isEmpty() || translatedText.equalsIgnoreCase(messageContent)) {
                    return;
                }

                // 3. Enriquecer el mensaje que DiscordSRV enviará a Discord
                String enrichedMessage = messageContent + " *(EN: " + translatedText + ")*";
                event.setMessage(enrichedMessage);
            });
        });
    }

    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
