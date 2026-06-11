package org.metamechanists.odysseia.boss;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.instances.CirceBoss;
import org.metamechanists.odysseia.boss.instances.PolifemoBoss;
import org.metamechanists.odysseia.boss.instances.DiosCorruptoBoss;
import org.metamechanists.odysseia.boss.skills.PolymorphSkill;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossManager implements Listener {

    private final Odysseia plugin;
    private final Map<UUID, OdysseyBoss> activeBosses = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private BukkitTask skillTask;

    public BossManager(Odysseia plugin) {
        this.plugin = plugin;
        startTasks();
    }

    private void startTasks() {
        // Update BossBars and Pathfinding every 1 second (20 ticks)
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (OdysseyBoss boss : activeBosses.values()) {
                if (boss.getEntity() == null || boss.getEntity().isDead() || !boss.getEntity().isValid()) {
                    removeBoss(boss.getEntity().getUniqueId(), null);
                    continue;
                }
                boss.updateBossBar();
                if (boss instanceof PolifemoBoss polifemo) {
                    polifemo.updatePathfinding();
                }
            }
        }, 20L, 20L);

        // Execute skills every 5 seconds (100 ticks)
        skillTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (OdysseyBoss boss : activeBosses.values()) {
                boss.executeSkillsRotation();
            }
        }, 100L, 100L);
    }

    public OdysseyBoss spawnBoss(String type, Location loc) {
        LivingEntity entity;
        OdysseyBoss boss;

        if (type.equalsIgnoreCase("circe")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITCH);
            boss = new CirceBoss(entity);
        } else if (type.equalsIgnoreCase("polifemo")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.GIANT);
            boss = new PolifemoBoss(entity);
        } else if (type.equalsIgnoreCase("dios_corrupto") || type.equalsIgnoreCase("dios-corrupto")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
            boss = new DiosCorruptoBoss(entity);
        } else {
            return null;
        }

        activeBosses.put(entity.getUniqueId(), boss);
        broadcastSpawn(boss);
        sendDiscordWebhook(boss, true, null);
        return boss;
    }

    public void removeBoss(UUID uuid, Player killer) {
        OdysseyBoss boss = activeBosses.remove(uuid);
        if (boss != null) {
            boss.cleanup();
            if (killer != null) {
                broadcastDeath(boss, killer);
                sendDiscordWebhook(boss, false, killer);
            }
        }
    }

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        if (skillTask != null) skillTask.cancel();

        // Clean up active bosses
        for (OdysseyBoss boss : activeBosses.values()) {
            boss.cleanup();
        }
        activeBosses.clear();

        // Restore any polymorphed players
        for (UUID uuid : PolymorphSkill.getPolymorphedHelmets().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                PolymorphSkill.restorePlayer(p);
            }
        }
        PolymorphSkill.getPolymorphedHelmets().clear();
    }

    private void broadcastSpawn(OdysseyBoss boss) {
        String msg = ChatColor.translateAlternateColorCodes('&',
                "&c&l[MÍTICO] &f¡El jefe ancestral " + boss.getDisplayName() + " &fha aparecido en el mundo!");
        Bukkit.broadcastMessage(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }
    }

    private void broadcastDeath(OdysseyBoss boss, Player killer) {
        String msg = ChatColor.translateAlternateColorCodes('&',
                "&c&l[MÍTICO] &f¡El jefe " + boss.getDisplayName() + " &fha sido derrotado por &a" + killer.getName() + "&f!");
        Bukkit.broadcastMessage(msg);
    }

    private void sendDiscordWebhook(OdysseyBoss boss, boolean isSpawn, Player killer) {
        String url = plugin.getConfig().getString("discord.webhook-url", "");
        if (url == null || url.isBlank() || url.equals("REPLACE_ME") || !WebhookSender.isAllowedHttpsUrl(url)) {
            return;
        }

        String title = isSpawn ? "⚔️ ¡Jefe Mítico Invocado!" : "💀 ¡Jefe Mítico Derrotado!";
        String desc = isSpawn
                ? "El jefe **" + ChatColor.stripColor(boss.getDisplayName()) + "** ha despertado en las coordenadas: " +
                  "`X: " + boss.getEntity().getLocation().getBlockX() +
                  ", Y: " + boss.getEntity().getLocation().getBlockY() +
                  ", Z: " + boss.getEntity().getLocation().getBlockZ() + "`."
                : "El jefe **" + ChatColor.stripColor(boss.getDisplayName()) + "** fue derrotado por **" + killer.getName() + "**.";
        int color = isSpawn ? 16753920 : 3066993; // Orange for spawn, Green for death

        String json = "{\"username\":\"Odysseia Mitología\",\"embeds\":[{\"title\":\"" + title + "\",\"description\":\""
                + Odysseia.escapeJson(desc) + "\",\"color\":" + color + "}]}";

        WebhookSender.sendAsync(plugin, url, json);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (activeBosses.containsKey(event.getEntity().getUniqueId())) {
            OdysseyBoss boss = activeBosses.get(event.getEntity().getUniqueId());
            if (boss instanceof DiosCorruptoBoss dios && dios.isShieldActive()) {
                event.setCancelled(true);
                dios.getEntity().getWorld().playSound(dios.getEntity().getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, 1.0f, 1.5f);
                dios.getEntity().getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, dios.getEntity().getLocation().add(0, 1.5, 0), 15, 0.5, 0.5, 0.5, 0.05);
            } else {
                // Update boss bar instantly on damage
                Bukkit.getScheduler().runTaskLater(plugin, boss::updateBossBar, 1L);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (activeBosses.containsKey(entity.getUniqueId())) {
            Player killer = entity.getKiller();
            removeBoss(entity.getUniqueId(), killer);
            
            // Custom drops / rewards
            event.getDrops().clear();
            event.setDroppedExp(1000); // Massive XP
            
            Location loc = entity.getLocation();
            loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PolymorphSkill.restorePlayer(player);
        
        // Remove from boss bars
        for (OdysseyBoss boss : activeBosses.values()) {
            boss.getBossBar().removePlayer(player);
            boss.getPlayersWatching().remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        PolymorphSkill.restorePlayer(player);
        
        // Remove from boss bars
        for (OdysseyBoss boss : activeBosses.values()) {
            boss.getBossBar().removePlayer(player);
            boss.getPlayersWatching().remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock fallingBlock) {
            NamespacedKey key = new NamespacedKey(plugin, "boss_rock");
            if (fallingBlock.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
        }
    }
}
