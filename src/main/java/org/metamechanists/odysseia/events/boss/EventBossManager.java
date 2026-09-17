package org.metamechanists.odysseia.events.boss;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestiona eventos de jefes mundiales asegurando reparto proporcional de recompensas por daño.
 * Evita el robo de loot por "último golpe" (last hit).
 */
public class EventBossManager implements Listener {

    private final JavaPlugin plugin;
    private final String webhookUrl;
    // Boss UUID -> (Player UUID -> Total Damage Dealt)
    private final Map<UUID, Map<UUID, Double>> bossDamageMap = new ConcurrentHashMap<>();
    // Boss UUID -> Boss Display Name
    private final Map<UUID, String> activeBossNames = new ConcurrentHashMap<>();

    public EventBossManager(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
    }

    public void registerBoss(UUID bossUuid, String bossName) {
        bossDamageMap.put(bossUuid, new ConcurrentHashMap<>());
        activeBossNames.put(bossUuid, bossName);
    }

    public void spawnEventBoss(String bossType, Location location, Player summoner) {
        // Ejecuta el comando de DrakesBosses para invocar el jefe con todas sus habilidades personalizadas
        String locStr = location.getWorld().getName() + " " +
                location.getBlockX() + " " +
                location.getBlockY() + " " +
                location.getBlockZ();
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "boss spawn " + bossType + " " + locStr);
        
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&4☠ &c&lJEFE DE EVENTO INVOCADO &8» &f¡Un poderoso &e" + bossType.toUpperCase(Locale.ROOT) +
                " &fha descendido sobre el mundo!\n" +
                "  &7• Ubicación aproximada: &ex=" + location.getBlockX() + " z=" + location.getBlockZ() + " &7(" + location.getWorld().getName() + ")\n" +
                "  &7• ¡Recompensas proporcionales al daño causado!\n"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
        }

        sendDiscordSpawnWebhook(bossType, location);
    }

    private void sendDiscordSpawnWebhook(String bossType, Location loc) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String json = """
        {
          "content": "<@&1539644230941806602>",
          "embeds": [{
            "title": "☠️ ¡JEFE MUNDIAL HA APARECIDO EN DRAKES! ☠️",
            "description": "**Un temible jefe ha emergido y amenaza el reino.**\\n\\n• **Jefe:** `%s`\\n• **Mundo:** `%s`\\n• **Coordenadas aprox:** `X: %d, Z: %d`\\n• **Sistema de Botín:** ⚔️ Recompensas proporcionales al daño (sin robo por último golpe).\\n\\n¡Reúne a tu clan y viaja a combatirlo ahora en `mc.drakescraft.net`!",
            "color": 11141120,
            "footer": { "text": "DrakesCraft Event Suite • DrakesBosses Integration" },
            "timestamp": "%s"
          }]
        }
        """.formatted(
                bossType.toUpperCase(Locale.ROOT),
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockZ(),
                java.time.Instant.now().toString()
        );
        WebhookSender.sendAsync(plugin, webhookUrl, json);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        Player damager = null;
        if (event.getDamager() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            damager = p;
        }

        if (damager == null) return;

        UUID entityId = victim.getUniqueId();
        String customName = victim.getCustomName();
        // Si no estaba registrado pero tiene nombre de jefe o es una entidad jefe
        if (!bossDamageMap.containsKey(entityId) && customName != null &&
                (customName.contains("☠") || customName.contains("BOSS") || customName.contains("Jefe") ||
                 victim.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "drakes_boss_id")))) {
            registerBoss(entityId, ChatColor.stripColor(customName));
        }

        Map<UUID, Double> damageMap = bossDamageMap.get(entityId);
        if (damageMap != null) {
            double finalDmg = event.getFinalDamage();
            damageMap.merge(damager.getUniqueId(), finalDmg, Double::sum);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        UUID entityId = entity.getUniqueId();
        Map<UUID, Double> damages = bossDamageMap.remove(entityId);
        String bossName = activeBossNames.remove(entityId);

        if (damages == null || damages.isEmpty()) return;

        distributeRewards(bossName != null ? bossName : "Jefe", damages, entity.getLocation());
    }

    public void processExternalVictory(String bossName, Map<UUID, Double> contributions, Location location) {
        distributeRewards(bossName, contributions, location);
    }

    private void distributeRewards(String bossName, Map<UUID, Double> damages, Location loc) {
        double totalDamage = damages.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalDamage <= 0) return;

        List<Map.Entry<UUID, Double>> sorted = damages.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        Economy econ = getEconomy();

        StringBuilder sb = new StringBuilder();
        sb.append("\n&4☠ &c&lJEFE DERROTADO &8» &e").append(bossName).append(" &fha caído!\n");
        sb.append("&6&lPODIO DE DAÑO (Recompensas Proporcionales):\n");

        int rank = 1;
        double baseRewardPool = 25000.0; // Fondo base de dinero por derrotar un jefe

        for (Map.Entry<UUID, Double> entry : sorted) {
            UUID pUuid = entry.getKey();
            double dmg = entry.getValue();
            double percent = (dmg / totalDamage) * 100.0;
            Player p = Bukkit.getPlayer(pUuid);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(pUuid).getName();
            if (name == null) name = "Guerrero";

            double rewardMoney = Math.round((dmg / totalDamage) * baseRewardPool);

            if (econ != null && rewardMoney > 0) {
                econ.depositPlayer(Bukkit.getOfflinePlayer(pUuid), rewardMoney);
            }

            if (rank <= 3) {
                String medal = rank == 1 ? "&e🥇" : (rank == 2 ? "&f🥈" : "&6🥉");
                sb.append("  ").append(medal).append(" &e#").append(rank).append(" &f").append(name)
                        .append(" &8- &c").append(String.format(Locale.ROOT, "%.1f%%", percent))
                        .append(" daño &8(&a+$").append(String.format(Locale.ROOT, "%,.0f", rewardMoney)).append("&8)\n");
            }

            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6⚔ &a¡Tu aporte de daño (&e" + String.format(Locale.ROOT, "%.1f%%", percent) +
                        "&a) te otorgó &e$" + String.format(Locale.ROOT, "%,.0f", rewardMoney) + " monedas&a!"));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }

            rank++;
        }

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', sb.toString()));
        sendDiscordDefeatWebhook(bossName, sorted, totalDamage);
    }

    private void sendDiscordDefeatWebhook(String bossName, List<Map.Entry<UUID, Double>> ranking, double totalDmg) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        StringBuilder topText = new StringBuilder();
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : ranking) {
            if (rank > 5) break;
            String medal = rank == 1 ? "🥇" : (rank == 2 ? "🥈" : (rank == 3 ? "🥉" : "⚔️"));
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            double pct = (entry.getValue() / totalDmg) * 100.0;
            topText.append(String.format(Locale.ROOT, "%s #%d **%s** — %.1f%% de daño\\n",
                    medal, rank, name != null ? name : "Desconocido", pct));
            rank++;
        }

        String json = """
        {
          "content": "<@&1539644230941806602>",
          "embeds": [{
            "title": "🏆 ¡JEFE DERROTADO! 🏆",
            "description": "**El jefe %s ha sido vencido.**\\n\\n**Top Contribuyentes:**\\n%s\\n¡Recompensas entregadas a todos los participantes proporcionalmente a su esfuerzo!",
            "color": 3066993,
            "footer": { "text": "DrakesCraft Event Suite • DrakesBosses Integration" },
            "timestamp": "%s"
          }]
        }
        """.formatted(
                bossName,
                topText.toString(),
                java.time.Instant.now().toString()
        );
        WebhookSender.sendAsync(plugin, webhookUrl, json);
    }

    private Economy getEconomy() {
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            return rsp != null ? rsp.getProvider() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
