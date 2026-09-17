package org.metamechanists.odysseia.events.boost;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.events.model.ActiveEvent;
import org.metamechanists.odysseia.events.model.EventType;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Gestiona los potenciadores globales del servidor (Economía, XP, Slimefun, Dracmas).
 * IMPORTANTE: El sistema de votos (/votar) queda estrictamente excluido por configuraciones pendientes.
 */
public class ServerBoostManager {

    public enum BoostCategory {
        ECONOMIA("Economía", "💰", BarColor.YELLOW),
        XP("Experiencia", "✨", BarColor.GREEN),
        SLIMEFUN("Slimefun", "⚡", BarColor.PURPLE),
        DRACMAS("Dracmas", "🪙", BarColor.BLUE);

        @Getter private final String displayName;
        @Getter private final String icon;
        @Getter private final BarColor barColor;

        BoostCategory(String displayName, String icon, BarColor barColor) {
            this.displayName = displayName;
            this.icon = icon;
            this.barColor = barColor;
        }

        public static BoostCategory fromString(String name) {
            if (name == null) return null;
            String clean = name.trim().toUpperCase(Locale.ROOT);
            for (BoostCategory cat : values()) {
                if (cat.name().equalsIgnoreCase(clean) || cat.displayName.equalsIgnoreCase(clean)) {
                    return cat;
                }
            }
            return null;
        }
    }

    public static class ActiveBoost implements ActiveEvent {
        @Getter private final BoostCategory category;
        @Getter private final double multiplier;
        @Getter private final long durationSeconds;
        private long remainingSeconds;
        private boolean active = false;
        private final BossBar bossBar;
        private final JavaPlugin plugin;
        private final String webhookUrl;

        public ActiveBoost(JavaPlugin plugin, BoostCategory category, double multiplier, long durationSeconds, String webhookUrl) {
            this.plugin = plugin;
            this.category = category;
            this.multiplier = multiplier;
            this.durationSeconds = durationSeconds;
            this.remainingSeconds = durationSeconds;
            this.webhookUrl = webhookUrl;
            this.bossBar = Bukkit.createBossBar(
                    formatTitle(),
                    category.getBarColor(),
                    BarStyle.SEGMENTED_10
            );
        }

        private String formatTitle() {
            long mins = remainingSeconds / 60;
            long secs = remainingSeconds % 60;
            return ChatColor.translateAlternateColorCodes('&',
                    "&6&lBOOST GLOBAL &8| " + category.getIcon() + " &e" + category.getDisplayName() +
                    " &a" + String.format(Locale.ROOT, "%.1fx", multiplier) +
                    " &8| &f⏳ " + String.format(Locale.ROOT, "%02d:%02d", mins, secs));
        }

        @Override
        public EventType getEventType() {
            return EventType.GLOBAL_BOOST;
        }

        @Override
        public String getName() {
            return "Boost " + category.getDisplayName() + " " + multiplier + "x";
        }

        @Override
        public String getStatusSummary() {
            return "Boost " + category.getDisplayName() + " " + multiplier + "x (" + remainingSeconds + "s)";
        }

        @Override
        public void stop(String reason) {
            stop();
        }

        @Override
        public void start() {
            this.active = true;
            bossBar.setProgress(1.0);
            bossBar.setVisible(true);
            for (Player p : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(p);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            }

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&e⚡ &6&lDRAKES BOOSTER &8» &f¡Se ha activado un &eBoost Global&f!\n" +
                    "  &7• Multiplicador: &a&l" + String.format(Locale.ROOT, "%.1fx", multiplier) + " en " + category.getDisplayName() + "\n" +
                    "  &7• Duración: &b" + (durationSeconds / 60) + " minutos\n" +
                    "  &7• ¡Aprovecha la bonificación en todo el servidor!\n"));

            sendDiscordWebhook();
        }

        private void sendDiscordWebhook() {
            if (webhookUrl == null || webhookUrl.isBlank()) return;
            String json = """
            {
              "content": "<@&1539644230941806602>",
              "embeds": [{
                "title": "⚡ BOOST GLOBAL ACTIVADO EN DRAKES ⚡",
                "description": "**¡El servidor está bajo un potenciador global!**\\n\\n• **Categoría:** %s %s\\n• **Multiplicador:** **%.1fx**\\n• **Duración:** **%d minutos**\\n\\n¡Conéctate ahora a `mc.drakescraft.net` y aprovecha el bono!",
                "color": 16766720,
                "footer": { "text": "DrakesCraft Event Suite • Odysseia Core" },
                "timestamp": "%s"
              }]
            }
            """.formatted(
                    category.getIcon(),
                    category.getDisplayName(),
                    multiplier,
                    durationSeconds / 60,
                    java.time.Instant.now().toString()
            );
            WebhookSender.sendAsync(plugin, webhookUrl, json);
        }

        @Override
        public void tick() {
            if (!active) return;
            remainingSeconds--;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!bossBar.getPlayers().contains(p)) {
                    bossBar.addPlayer(p);
                }
            }

            if (remainingSeconds <= 0) {
                stop();
                return;
            }

            double progress = Math.max(0.0, Math.min(1.0, (double) remainingSeconds / durationSeconds));
            bossBar.setProgress(progress);
            bossBar.setTitle(formatTitle());
        }

        @Override
        public void stop() {
            if (!active) return;
            active = false;
            bossBar.removeAll();
            bossBar.setVisible(false);

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e⚡ &6&lDRAKES BOOSTER &8» &7El Boost Global de &e" + category.getDisplayName() + " &7ha finalizado."));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public long getTimeRemainingSeconds() {
            return remainingSeconds;
        }
    }

    private final JavaPlugin plugin;
    private final String webhookUrl;
    private final ConcurrentMap<BoostCategory, ActiveBoost> activeBoosts = new ConcurrentHashMap<>();

    public ServerBoostManager(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
    }

    public ActiveBoost startBoost(BoostCategory category, double multiplier, long durationSeconds) {
        if (category == null || multiplier <= 1.0 || durationSeconds <= 0) return null;

        ActiveBoost existing = activeBoosts.get(category);
        if (existing != null && existing.isActive()) {
            existing.stop();
        }

        ActiveBoost boost = new ActiveBoost(plugin, category, multiplier, durationSeconds, webhookUrl);
        activeBoosts.put(category, boost);
        boost.start();
        return boost;
    }

    public void stopBoost(BoostCategory category) {
        ActiveBoost boost = activeBoosts.remove(category);
        if (boost != null) {
            boost.stop();
        }
    }

    public void stopAll() {
        for (ActiveBoost boost : activeBoosts.values()) {
            boost.stop();
        }
        activeBoosts.clear();
    }

    public void tick() {
        activeBoosts.values().removeIf(boost -> {
            if (boost.isActive()) {
                boost.tick();
                return !boost.isActive();
            }
            return true;
        });
    }

    public double getMultiplier(BoostCategory category) {
        ActiveBoost boost = activeBoosts.get(category);
        if (boost != null && boost.isActive()) {
            return boost.getMultiplier();
        }
        return 1.0;
    }

    public boolean hasActiveBoost(BoostCategory category) {
        ActiveBoost boost = activeBoosts.get(category);
        return boost != null && boost.isActive();
    }

    public ConcurrentMap<BoostCategory, ActiveBoost> getActiveBoosts() {
        return activeBoosts;
    }
}
