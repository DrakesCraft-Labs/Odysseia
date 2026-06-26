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
import org.metamechanists.odysseia.boss.instances.HeimdallBoss;
import org.metamechanists.odysseia.boss.instances.HidraBoss;
import org.metamechanists.odysseia.boss.instances.CerberoBoss;
import org.metamechanists.odysseia.boss.instances.ArtemisaBoss;
import org.metamechanists.odysseia.boss.instances.TifonBoss;
import org.metamechanists.odysseia.boss.instances.PrometeoBoss;
import org.metamechanists.odysseia.boss.skills.PolymorphSkill;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossManager implements Listener {

    private final Odysseia plugin;
    private final Map<UUID, OdysseyBoss> activeBosses = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> naturalBosses = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private BukkitTask updateTask;
    private BukkitTask skillTask;
    private BukkitTask naturalSpawnTask;

    /** Jefes elegibles para spawn natural por defecto (si la config no especifica lista). */
    private static final java.util.List<String> DEFAULT_NATURAL_BOSSES = java.util.List.of(
            "thor", "ares", "hades", "poseidon", "zeus", "loki", "odin", "kratos",
            "heimdall", "hidra", "cerbero", "artemisa", "tifon", "prometeo"
    );

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
                boss.checkPhases();
                boss.tickAura();
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

        startNaturalSpawnTask();
    }

    /**
     * Spawn natural: cada cierto intervalo, con una probabilidad configurable, invoca un jefe
     * aleatorio cerca de un jugador (en la superficie). No genera estructuras.
     * Config en config.yml → sección "natural-spawn".
     */
    private void startNaturalSpawnTask() {
        if (!plugin.getConfig().getBoolean("natural-spawn.enabled", false)) {
            return;
        }
        int intervalSeconds = Math.max(60, plugin.getConfig().getInt("natural-spawn.interval-seconds", 1800));
        long period = intervalSeconds * 20L;
        naturalSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tryNaturalSpawn, period, period);
        plugin.getLogger().info("[NaturalSpawn] Spawn natural de jefes activado (cada " + intervalSeconds + "s).");
    }

    private void tryNaturalSpawn() {
        try {
            var cfg = plugin.getConfig();
            if (Math.random() > cfg.getDouble("natural-spawn.chance", 0.25)) {
                return;
            }
            // Respeta el máximo de jefes naturales activos
            naturalBosses.removeIf(uuid -> !activeBosses.containsKey(uuid));
            if (naturalBosses.size() >= cfg.getInt("natural-spawn.max-active", 2)) {
                return;
            }

            java.util.List<String> allowedWorlds = cfg.getStringList("natural-spawn.worlds");
            java.util.List<String> bosses = cfg.getStringList("natural-spawn.bosses");
            if (bosses.isEmpty()) {
                bosses = DEFAULT_NATURAL_BOSSES;
            }

            // Candidatos: jugadores vivos en mundos permitidos (lista vacía = todos)
            java.util.List<Player> candidates = new java.util.ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isDead() || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                if (allowedWorlds.isEmpty() || allowedWorlds.contains(p.getWorld().getName())) {
                    candidates.add(p);
                }
            }
            if (candidates.isEmpty()) {
                return;
            }

            java.util.Random rnd = new java.util.Random();
            Player anchor = candidates.get(rnd.nextInt(candidates.size()));
            double minDist = cfg.getDouble("natural-spawn.min-distance", 30);
            double maxDist = cfg.getDouble("natural-spawn.max-distance", 60);
            double dist = minDist + rnd.nextDouble() * Math.max(0, maxDist - minDist);
            double angle = rnd.nextDouble() * Math.PI * 2;

            Location base = anchor.getLocation().clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            int y = base.getWorld().getHighestBlockYAt(base);
            Location spawnLoc = new Location(base.getWorld(), base.getBlockX() + 0.5, y + 1, base.getBlockZ() + 0.5);

            String type = bosses.get(rnd.nextInt(bosses.size()));
            OdysseyBoss boss = spawnBoss(type, spawnLoc);
            if (boss != null) {
                naturalBosses.add(boss.getEntity().getUniqueId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NaturalSpawn] Error al intentar spawn natural: " + e.getMessage());
        }
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
        } else if (type.equalsIgnoreCase("heimdall")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.STRAY);
            boss = new HeimdallBoss(entity);
        } else if (type.equalsIgnoreCase("hidra")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.RAVAGER);
            boss = new HidraBoss(entity);
        } else if (type.equalsIgnoreCase("cerbero")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.RAVAGER);
            boss = new CerberoBoss(entity);
        } else if (type.equalsIgnoreCase("artemisa")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
            boss = new ArtemisaBoss(entity);
        } else if (type.equalsIgnoreCase("tifon") || type.equalsIgnoreCase("tifón")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.GIANT);
            boss = new TifonBoss(entity);
        } else if (type.equalsIgnoreCase("prometeo")) {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
            boss = new PrometeoBoss(entity);
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
        naturalBosses.remove(uuid);
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
        if (naturalSpawnTask != null) naturalSpawnTask.cancel();

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
            } else if (boss instanceof PrometeoBoss prometeo && prometeo.isInvulnerable()) {
                // Fénix: invulnerable durante la resurrección
                event.setCancelled(true);
                prometeo.getEntity().getWorld().spawnParticle(org.bukkit.Particle.FLAME, prometeo.getEntity().getLocation().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.05);
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
            
            // Custom drops / rewards — se sueltan manualmente en el suelo para
            // garantizar que nunca se pierdan (otros plugins pueden limpiar getDrops()).
            event.getDrops().clear();
            if (boss != null) {
                Location dropLocation = entity.getLocation();
                for (org.bukkit.inventory.ItemStack drop : createCustomDrops(boss.getId())) {
                    if (drop != null && dropLocation.getWorld() != null) {
                        dropLocation.getWorld().dropItemNaturally(dropLocation, drop);
                    }
                }
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
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createMjolnir());
                break;
            }
            case "ares": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createAresBlade());
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createAresShield());
                break;
            }
            case "hades": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createHadesScythe());
                break;
            }
            case "poseidon": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createPoseidonTrident());
                break;
            }
            case "zeus": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createZeusMace());
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
            case "heimdall": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createGjallarhorn());
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createBifrostWings());
                break;
            }
            case "hidra": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createHydraFang());
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createHydraScale());
                break;
            }
            case "cerbero": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createCerberoHide());
                break;
            }
            case "artemisa": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createArtemisBow());
                break;
            }
            case "tifon": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createTifonClaw());
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createTifonChestplate());
                break;
            }
            case "prometeo": {
                drops.add(org.metamechanists.odysseia.items.OdysseyItemManager.createPrometeoFlame());
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
