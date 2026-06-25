package org.metamechanists.odysseia.boss;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.metamechanists.odysseia.boss.instances.ThorBoss;
import org.metamechanists.odysseia.boss.instances.AresBoss;
import org.metamechanists.odysseia.boss.instances.HadesBoss;
import org.metamechanists.odysseia.boss.instances.PoseidonBoss;
import org.metamechanists.odysseia.boss.instances.ZeusBoss;
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
        } else if (type.equalsIgnoreCase("thor")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.PIGLIN_BRUTE);
            boss = new ThorBoss(entity);
        } else if (type.equalsIgnoreCase("ares")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.VINDICATOR);
            boss = new AresBoss(entity);
        } else if (type.equalsIgnoreCase("hades")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
            boss = new HadesBoss(entity);
        } else if (type.equalsIgnoreCase("poseidon")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.DROWNED);
            boss = new PoseidonBoss(entity);
        } else if (type.equalsIgnoreCase("zeus")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
            boss = new ZeusBoss(entity);
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
            OdysseyBoss boss = activeBosses.get(entity.getUniqueId());
            Player killer = entity.getKiller();
            removeBoss(entity.getUniqueId(), killer);
            
            // Custom drops / rewards
            event.getDrops().clear();
            if (boss != null) {
                org.bukkit.inventory.ItemStack dropItem = createCustomDrop(boss.getId());
                if (dropItem != null) {
                    event.getDrops().add(dropItem);
                }
            }
            
            event.setDroppedExp(5000); // 5000 XP
            
            Location loc = entity.getLocation();
            loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    private org.bukkit.inventory.ItemStack createCustomDrop(String bossId) {
        org.bukkit.inventory.ItemStack item = null;
        switch (bossId.toLowerCase()) {
            case "thor":
                item = new org.bukkit.inventory.ItemStack(Material.MACE);
                var metaThor = item.getItemMeta();
                if (metaThor != null) {
                    metaThor.setDisplayName("§6§l✦ Mjolnir ✦");
                    metaThor.setLore(java.util.List.of("§7El Martillo del Trueno de Thor.", "§7Descarga el poder del Olimpo."));
                    metaThor.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
                    metaThor.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2, true);
                    metaThor.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 5, true);
                    item.setItemMeta(metaThor);
                }
                break;
            case "ares":
                item = new org.bukkit.inventory.ItemStack(Material.TRIDENT);
                var metaAres = item.getItemMeta();
                if (metaAres != null) {
                    metaAres.setDisplayName("§c§l✦ Lanza de Ares ✦");
                    metaAres.setLore(java.util.List.of("§7La lanza mítica de Ares.", "§7Infunde el terror de la guerra."));
                    metaAres.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
                    metaAres.addEnchant(org.bukkit.enchantments.Enchantment.IMPALING, 5, true);
                    metaAres.addEnchant(org.bukkit.enchantments.Enchantment.LOYALTY, 3, true);
                    item.setItemMeta(metaAres);
                }
                break;
            case "hades":
                item = new org.bukkit.inventory.ItemStack(Material.NETHERITE_HOE);
                var metaHades = item.getItemMeta();
                if (metaHades != null) {
                    metaHades.setDisplayName("§5§l✦ Guadaña del Inframundo ✦");
                    metaHades.setLore(java.util.List.of("§7La guadaña cosechadora de almas de Hades."));
                    metaHades.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
                    metaHades.addEnchant(org.bukkit.enchantments.Enchantment.LOOTING, 3, true);
                    metaHades.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 2, true);
                    item.setItemMeta(metaHades);
                }
                break;
            case "poseidon":
                item = new org.bukkit.inventory.ItemStack(Material.TRIDENT);
                var metaPoseidon = item.getItemMeta();
                if (metaPoseidon != null) {
                    metaPoseidon.setDisplayName("§9§l✦ Tridente de Poseidón ✦");
                    metaPoseidon.setLore(java.util.List.of("§7El tridente que controla las mareas y tormentas."));
                    metaPoseidon.addEnchant(org.bukkit.enchantments.Enchantment.IMPALING, 5, true);
                    metaPoseidon.addEnchant(org.bukkit.enchantments.Enchantment.RIPTIDE, 3, true);
                    metaPoseidon.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 5, true);
                    item.setItemMeta(metaPoseidon);
                }
                break;
            case "zeus":
                item = new org.bukkit.inventory.ItemStack(Material.TRIDENT);
                var metaZeus = item.getItemMeta();
                if (metaZeus != null) {
                    metaZeus.setDisplayName("§e§l✦ Centella de Zeus ✦");
                    metaZeus.setLore(java.util.List.of("§7El rayo celestial forjado por los Cíclopes."));
                    metaZeus.addEnchant(org.bukkit.enchantments.Enchantment.IMPALING, 5, true);
                    metaZeus.addEnchant(org.bukkit.enchantments.Enchantment.CHANNELING, 1, true);
                    metaZeus.addEnchant(org.bukkit.enchantments.Enchantment.LOYALTY, 3, true);
                    metaZeus.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 5, true);
                    item.setItemMeta(metaZeus);
                }
                break;
        }
        return item;
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
