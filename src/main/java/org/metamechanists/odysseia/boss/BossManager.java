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
import org.metamechanists.odysseia.boss.instances.LokiBoss;
import org.metamechanists.odysseia.boss.instances.OdinBoss;
import org.metamechanists.odysseia.boss.instances.KratosBoss;
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
        } else if (type.equalsIgnoreCase("loki")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.ILLUSIONER);
            boss = new LokiBoss(entity);
        } else if (type.equalsIgnoreCase("odin")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.STRAY);
            boss = new OdinBoss(entity);
        } else if (type.equalsIgnoreCase("kratos")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.PIGLIN_BRUTE);
            boss = new KratosBoss(entity);
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
        // Handle Loki clone hit
        if (event.getEntity() instanceof org.bukkit.entity.Illusioner illusioner) {
            String name = illusioner.getCustomName();
            if (name != null && name.equals("§aIlusión de Loki")) {
                event.setCancelled(true);
                illusioner.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, illusioner.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.05);
                illusioner.getWorld().playSound(illusioner.getLocation(), Sound.ENTITY_BAT_DEATH, 0.8f, 1.2f);
                illusioner.remove();
                return;
            }
        }

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
                event.getDrops().addAll(createCustomDrops(boss.getId()));
            }
            
            event.setDroppedExp(5000); // 5000 XP
            
            Location loc = entity.getLocation();
            loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }


    private java.util.List<org.bukkit.inventory.ItemStack> createCustomDrops(String bossId) {
        java.util.List<org.bukkit.inventory.ItemStack> drops = new java.util.ArrayList<>();
        switch (bossId.toLowerCase()) {
            case "thor": {
                // Mjolnir — el mazo del trueno, no un tridente
                org.bukkit.inventory.ItemStack mjolnir = new org.bukkit.inventory.ItemStack(Material.MACE);
                var meta = mjolnir.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6§l⚡ Mjolnir §r§7[El Martillo del Trueno]");
                    meta.setLore(java.util.List.of(
                        "§8▸ Arma Mítica de Thor",
                        "§7Forjado por los enanos de Nidavellir",
                        "§7en el corazón de una estrella moribunda.",
                        "",
                        "§e§lFURIA DEL TRUENO §r§7— Al golpear,",
                        "§7lanza un rayo sobre el objetivo.",
                        "",
                        "§6§lENCIERRA EL PODER DEL OLIMPO"
                    ));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 4, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                    mjolnir.setItemMeta(meta);
                }
                drops.add(mjolnir);
                break;
            }
            case "ares": {
                // Espada de guerra — Ares es dios de la guerra, no del mar
                org.bukkit.inventory.ItemStack espada = new org.bukkit.inventory.ItemStack(Material.NETHERITE_SWORD);
                var meta = espada.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§c§l⚔ Filo de Ares §r§7[Espada de la Guerra]");
                    meta.setLore(java.util.List.of(
                        "§8▸ Arma Mítica de Ares",
                        "§7Bañada en la sangre de mil batallas.",
                        "§7Quien la empuña entra en frenesí de combate.",
                        "",
                        "§c§lSED DE SANGRE §r§7— +15% de daño",
                        "§7por cada enemigo derrotado (efecto temporal).",
                        "",
                        "§4§lCORRUPTA CON LA ESENCIA DE LA GUERRA"
                    ));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 12, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 3, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.LOOTING, 5, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                    espada.setItemMeta(meta);
                }
                drops.add(espada);
                // También un escudo espartano
                org.bukkit.inventory.ItemStack escudo = new org.bukkit.inventory.ItemStack(Material.SHIELD);
                var metaEscudo = escudo.getItemMeta();
                if (metaEscudo != null) {
                    metaEscudo.setDisplayName("§c§lEscudo Espartano de Ares");
                    metaEscudo.setLore(java.util.List.of(
                        "§8▸ Arma Mítica de Ares",
                        "§7El escudo de los guerreros más feroces.",
                        "",
                        "§c§lBLOQUEO PERFECTO §r§7— Refleja el 20%",
                        "§7del daño bloqueado al atacante."
                    ));
                    metaEscudo.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
                    metaEscudo.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                    escudo.setItemMeta(metaEscudo);
                }
                drops.add(escudo);
                break;
            }
            case "hades": {
                // Guadaña del inframundo
                org.bukkit.inventory.ItemStack guadana = new org.bukkit.inventory.ItemStack(Material.NETHERITE_HOE);
                var meta = guadana.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§5§l☠ Guadaña del Inframundo §r§7[Segadora de Almas]");
                    meta.setLore(java.util.List.of(
                        "§8▸ Arma Mítica de Hades",
                        "§7La herramienta del cosechador de almas.",
                        "§7Cada golpe drena la vida de la víctima.",
                        "",
                        "§5§lDRENAJE DE ALMA §r§7— Roba 3♥ de vida",
                        "§7por golpe y las añade a tu salud.",
                        "",
                        "§8§lCOSECHADOR DE ALMAS §r§7— Looting X"
                    ));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.LOOTING, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 5, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                    guadana.setItemMeta(meta);
                }
                drops.add(guadana);
                break;
            }
            case "poseidon": {
                // El único que merece un tridente — dios del mar
                org.bukkit.inventory.ItemStack tridente = new org.bukkit.inventory.ItemStack(Material.TRIDENT);
                var meta = tridente.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§9§l🔱 Tridente de Poseidón §r§7[Señor del Mar]");
                    meta.setLore(java.util.List.of(
                        "§8▸ Arma Mítica de Poseidón",
                        "§7Forjado en las profundidades del Océano Eterno.",
                        "§7Controla las mareas, las tormentas y los mares.",
                        "",
                        "§9§lTSUNAMI §r§7— Lanza una ola de agua al arrojar",
                        "§7el tridente, empujando a todos los cercanos.",
                        "",
                        "§b§lRIPTIDE V — IMPALING X — LOYALTY V"
                    ));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.IMPALING, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.RIPTIDE, 5, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                    tridente.setItemMeta(meta);
                }
                drops.add(tridente);
                break;
            }
            case "zeus": {
                // Zeus usa un bastón/mace de rayos — NO un tridente
                org.bukkit.inventory.ItemStack rayo = new org.bukkit.inventory.ItemStack(Material.MACE);
                var meta = rayo.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e§l⚡ Rayo de Zeus §r§7[Padre de los Dioses]");
                    meta.setLore(java.util.List.of(
                        "§8▸ Arma Mítica de Zeus",
                        "§7El rayo definitivo forjado por los Cíclopes",
                        "§7en el taller secreto del Olimpo.",
                        "",
                        "§e§lTORMENTA DIVINA §r§7— Al golpear invoca",
                        "§7tres rayos en un radio de 5 bloques.",
                        "",
                        "§e§lPODER DEL OLIMPO — SHARPNESS XII"
                    ));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 12, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 5, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                    rayo.setItemMeta(meta);
                }
                drops.add(rayo);
                break;
            }
            case "loki": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createLokiDagger());
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createLokiScepter());
                break;
            }
            case "odin": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createOdinSpear());
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createOdinHelmet());
                break;
            }
            case "kratos": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createKratosBlade());
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createLeviathanAxe());
                break;
            }
        }
        return drops;
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

    @EventHandler
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.EnderCrystal crystal) {
            NamespacedKey key = new NamespacedKey(plugin, "boss_crystal");
            if (crystal.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                event.blockList().clear();
            }
        }
    }
}
