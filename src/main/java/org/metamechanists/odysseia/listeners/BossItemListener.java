package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.items.OdysseyItemManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BossItemListener implements Listener {

    private final Odysseia plugin;
    private final NamespacedKey kratosTempOffhandKey;
    private final Map<UUID, Long> scepterCooldowns = new HashMap<>();
    private static final Map<UUID, ItemStack> savedOffhands = new HashMap<>();

    public BossItemListener(Odysseia plugin) {
        this.plugin = plugin;
        this.kratosTempOffhandKey = new NamespacedKey(plugin, "kratos_temp_offhand");

        // Periodic check for Odin's Helmet every 2 seconds (40 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkOdinHelmet, 40L, 40L);
    }

    private void checkOdinHelmet() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack helmet = p.getInventory().getHelmet();
            if (helmet != null && helmet.hasItemMeta()) {
                ItemMeta meta = helmet.getItemMeta();
                String type = meta.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if ("odin_helmet".equals(type)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, true, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, true, false, false));
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity targetEntity = event.getEntity();

        if (!(targetEntity instanceof LivingEntity target)) return;

        // 1. Direct melee hit by player
        if (damagerEntity instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String type = meta.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if (type != null) {
                    if (type.equals("kratos_blade") && player.getHealth() < 12.0) {
                        event.setDamage(event.getDamage() * 1.30);
                    }
                    handleCustomMeleeHit(player, target, type);
                }
            }
        }

        // 2. Projectile hit (custom tridents)
        if (damagerEntity instanceof Trident trident) {
            if (trident.getPersistentDataContainer().has(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING)) {
                String type = trident.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if ("odin_spear".equals(type)) {
                    Location loc = target.getLocation();
                    loc.getWorld().strikeLightning(loc);
                    loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                } else if ("poseidon_trident".equals(type)) {
                    triggerTsunami(target.getLocation(), trident.getShooter() instanceof Entity s ? s : null);
                }
            }
        }

        // 2.5 Projectile hit (Artemis Bow)
        if (damagerEntity instanceof org.bukkit.entity.Arrow arrow && arrow.hasMetadata("artemis_arrow")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
            Location tLoc = target.getLocation();
            tLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, tLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
            tLoc.getWorld().playSound(tLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);
        }

        // 3. Escudo Espartano de Ares — refleja el 20% del daño bloqueado al atacante
        if (targetEntity instanceof Player blocker && blocker.isBlocking() && isHoldingItem(blocker, "ares_shield")) {
            if (damagerEntity instanceof LivingEntity attacker && !attacker.equals(blocker)) {
                double reflected = event.getDamage() * 0.20;
                if (reflected > 0) {
                    attacker.damage(reflected, blocker);
                    Location aLoc = attacker.getLocation();
                    aLoc.getWorld().spawnParticle(Particle.CRIT, aLoc.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.1);
                    aLoc.getWorld().playSound(aLoc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.7f);
                }
            }
        }
    }

    /** Ola de Poseidón: empuja a todos los enemigos cercanos al punto de impacto. */
    private void triggerTsunami(Location center, Entity source) {
        center.getWorld().spawnParticle(Particle.SPLASH, center, 80, 3.0, 1.0, 3.0, 0.2);
        center.getWorld().spawnParticle(Particle.BUBBLE, center, 60, 3.0, 1.0, 3.0, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.6f);
        for (Entity e : center.getWorld().getNearbyEntities(center, 6, 4, 6)) {
            if (e instanceof LivingEntity victim && !e.equals(source)) {
                Vector push = victim.getLocation().toVector().subtract(center.toVector());
                if (push.lengthSquared() < 0.01) {
                    push = new Vector(0, 1, 0);
                }
                push.normalize().multiply(1.5).setY(0.8);
                victim.setVelocity(push);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
            }
        }
    }

    /** Comprueba si el jugador sostiene (en cualquier mano) un item custom con el typeId dado. */
    private boolean isHoldingItem(Player player, String typeId) {
        return matchesType(player.getInventory().getItemInMainHand(), typeId)
                || matchesType(player.getInventory().getItemInOffHand(), typeId);
    }

    private boolean matchesType(ItemStack item, String typeId) {
        if (item == null || !item.hasItemMeta()) return false;
        String type = item.getItemMeta().getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
        return typeId.equals(type);
    }

    private String getCustomItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
    }

    private boolean isKratosBlade(ItemStack item) {
        return "kratos_blade".equals(getCustomItemType(item));
    }

    private boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private boolean isKratosTempOffhand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(kratosTempOffhandKey, PersistentDataType.BYTE);
    }

    private void synchronizeKratosBlades(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        UUID playerId = player.getUniqueId();

        if (isKratosBlade(mainHand)) {
            if (isKratosBlade(offHand) && !isKratosTempOffhand(offHand)) {
                return;
            }

            if (!savedOffhands.containsKey(playerId)) {
                savedOffhands.put(playerId, isAir(offHand) ? null : offHand.clone());
            }

            if (!isKratosTempOffhand(offHand)) {
                ItemStack visualClone = mainHand.clone();
                ItemMeta cloneMeta = visualClone.getItemMeta();
                if (cloneMeta != null) {
                    cloneMeta.getPersistentDataContainer().set(kratosTempOffhandKey, PersistentDataType.BYTE, (byte) 1);
                    visualClone.setItemMeta(cloneMeta);
                }
                player.getInventory().setItemInOffHand(visualClone);
            }
            return;
        }

        restoreKratosOffhand(player, false);
    }

    private void restoreKratosOffhand(Player player, boolean forceRestore) {
        UUID playerId = player.getUniqueId();
        ItemStack saved = savedOffhands.get(playerId);
        if (saved == null && !savedOffhands.containsKey(playerId)) {
            return;
        }

        ItemStack currentOffHand = player.getInventory().getItemInOffHand();
        if (!forceRestore && !isKratosTempOffhand(currentOffHand)) {
            return;
        }

        savedOffhands.remove(playerId);
        player.getInventory().setItemInOffHand(isAir(saved) ? null : saved.clone());
    }

    private void handleCustomMeleeHit(Player player, LivingEntity target, String itemType) {
        Location targetLoc = target.getLocation();

        switch (itemType) {
            case "loki_dagger":
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 2, false, true));
                targetLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, targetLoc.add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 1.2f);
                // 20% probabilidad de volverse invisible por 3 segundos
                if (Math.random() < 0.20) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false, true));
                    player.sendMessage("§a§o[Loki] Te has desvanecido en las sombras...");
                }
                break;

            case "kratos_blade":
                target.setFireTicks(100);
                // Pull target toward player
                Vector pull = player.getLocation().toVector().subtract(targetLoc.toVector());
                if (pull.lengthSquared() > 0.01) {
                    pull.normalize().multiply(0.8).setY(0.3);
                    target.setVelocity(pull);
                }
                // Dibujar línea de partículas de fuego (simula las cadenas de Kratos)
                Location start = player.getEyeLocation().subtract(0, 0.3, 0);
                Location end = target.getLocation().add(0, 1.0, 0);
                Vector direction = end.toVector().subtract(start.toVector());
                double distance = direction.length();
                if (distance > 0.1) {
                    direction.normalize();
                    for (double d = 0; d < distance; d += 0.25) {
                        Location point = start.clone().add(direction.clone().multiply(d));
                        point.getWorld().spawnParticle(Particle.FLAME, point, 1, 0.0, 0.0, 0.0, 0.0);
                        point.getWorld().spawnParticle(Particle.DUST, point, 1, 0.0, 0.0, 0.0,
                                new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 50, 0), 0.7f));
                    }
                }
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc.add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
                targetLoc.getWorld().playSound(targetLoc, Sound.ITEM_FIRECHARGE_USE, 0.8f, 0.9f);
                break;

            case "leviathan_axe":
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, false, true));
                targetLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, targetLoc.add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.05);
                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
                break;

            case "odin_spear":
                targetLoc.getWorld().strikeLightning(targetLoc);
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                break;

            case "mjolnir":
                // Furia del Trueno — rayo directo sobre el objetivo
                targetLoc.getWorld().strikeLightning(targetLoc);
                targetLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.1f);
                break;

            case "zeus_mace": {
                // Tormenta Divina — 3 rayos en radio 5 alrededor del objetivo
                for (int i = 0; i < 3; i++) {
                    double ox = (Math.random() - 0.5) * 10;
                    double oz = (Math.random() - 0.5) * 10;
                    targetLoc.getWorld().strikeLightning(targetLoc.clone().add(ox, 0, oz));
                }
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
                break;
            }

            case "hades_scythe": {
                // Drenaje de Alma — roba 3♥ (6 HP) y los suma a la salud del jugador
                double maxHp = 20.0;
                var maxAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                if (maxAttr != null) {
                    maxHp = maxAttr.getValue();
                }
                player.setHealth(Math.min(maxHp, player.getHealth() + 6.0));
                targetLoc.getWorld().spawnParticle(Particle.SOUL, targetLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                targetLoc.getWorld().playSound(targetLoc, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 0.6f);
                break;
            }

            case "gjallarhorn": {
                // Onda de choque sónica - fuerza de los 9 mundos
                targetLoc.getWorld().playSound(targetLoc, Sound.EVENT_RAID_HORN, 1.2f, 1.5f);
                targetLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, targetLoc, 1);
                for (Entity e : target.getNearbyEntities(4, 3, 4)) {
                    if (e instanceof LivingEntity victim && !victim.equals(player)) {
                        victim.damage(8.0, player);
                        victim.setVelocity(victim.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2).setY(0.4));
                    }
                }
                break;
            }

            case "hydra_fang": {
                // Veneno de Lerna
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 4, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 4, false, true));
                targetLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, targetLoc, 20, 0.4, 0.4, 0.4, 0.05);
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_SPIDER_DEATH, 0.8f, 0.5f);
                break;
            }

            case "tifon_claw": {
                // Furia Primordial - Erupción volcánica y daño porcentual
                target.setFireTicks(120);
                target.setVelocity(new Vector(0, 1.2, 0));
                double eruptionDamage = target.getHealth() * 0.10;
                target.damage(Math.max(4.0, eruptionDamage), player);
                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 20, 0.3, 0.3, 0.3, 0.1);
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
                break;
            }

            case "prometeo_flame": {
                // Chispa Divina - Incendio en área
                for (Entity e : target.getNearbyEntities(4, 3, 4)) {
                    if (e instanceof LivingEntity victim && !victim.equals(player)) {
                        victim.setFireTicks(120);
                        victim.damage(4.0, player);
                    }
                }
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 35, 1.5, 0.5, 1.5, 0.05);
                targetLoc.getWorld().playSound(targetLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
                break;
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        if (proj instanceof Trident trident && trident.getShooter() instanceof Player player) {
            // Propaga el typeId del tridente custom (mano o secundaria) al proyectil lanzado
            String type = tridentType(player.getInventory().getItemInMainHand());
            if (type == null) {
                type = tridentType(player.getInventory().getItemInOffHand());
            }
            if ("odin_spear".equals(type) || "poseidon_trident".equals(type)) {
                trident.getPersistentDataContainer().set(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING, type);
            }
        }
    }

    private String tridentType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemMeta meta = item.getItemMeta();
        
        // ── Invocador de Jefes Celestiales ──
        NamespacedKey summonKey = new NamespacedKey(plugin, "boss_summoner");
        String bossId = meta.getPersistentDataContainer().get(summonKey, PersistentDataType.STRING);
        if (bossId != null) {
            event.setCancelled(true);
            
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
                return;
            }

            // Consumir el invocador (1 unidad)
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItem(event.getHand(), null);
            }

            Location spawnLoc = event.getClickedBlock().getLocation().clone().add(0.5, 1.0, 0.5);
            org.bukkit.World world = spawnLoc.getWorld();
            
            // Efectos celestiales de invocación
            if (world != null) {
                world.strikeLightningEffect(spawnLoc);
                world.spawnParticle(Particle.EXPLOSION_EMITTER, spawnLoc, 5, 0.5, 0.5, 0.5, 0.1);
                world.playSound(spawnLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
                world.playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
            }

            // Invocar al jefe
            plugin.getBossManager().spawnBoss(bossId, spawnLoc);
            return;
        }

        String type = meta.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);

        if ("loki_scepter".equals(type)) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            long lastUse = scepterCooldowns.getOrDefault(player.getUniqueId(), 0L);
            long diff = now - lastUse;

            if (diff < 5000) {
                double remaining = (5000 - diff) / 1000.0;
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c&l[COOLDOWN] &eEl Cetro de Loki está recargando. Espera &c" + String.format("%.1f", remaining) + "s&e."));
                return;
            }

            scepterCooldowns.put(player.getUniqueId(), now);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 1.1f);

            Snowball ball = player.launchProjectile(Snowball.class);
            ball.setMetadata("loki_magic", new FixedMetadataValue(plugin, true));

            // Green particle trail task
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (ball.isDead() || !ball.isValid()) {
                    task.cancel();
                    return;
                }
                ball.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ball.getLocation(), 3, 0.1, 0.1, 0.1, 0.02);
            }, 1L, 1L);
        }

        // ── Hacha Leviatán: Lanzamiento y Reclamación Rúnica ──
        if ("leviathan_axe".equals(type)) {
            event.setCancelled(true);

            // Cooldown de 2 segundos para evitar spam de lanzamiento
            long now = System.currentTimeMillis();
            long lastUse = scepterCooldowns.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastUse < 2000L) {
                return;
            }
            scepterCooldowns.put(player.getUniqueId(), now);

            ItemStack axeItem = item.clone();
            
            // Remover temporalmente del inventario (simula lanzamiento)
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 1.0f, 0.5f);

            // Disparar proyectil físico (bola de nieve pesada)
            Snowball ball = player.launchProjectile(Snowball.class);
            ball.setMetadata("kratos_axe", new FixedMetadataValue(plugin, true));

            // Espiral de partículas de nieve siguiendo al proyectil
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (ball.isDead() || !ball.isValid()) {
                    task.cancel();
                    return;
                }
                ball.getWorld().spawnParticle(Particle.SNOWFLAKE, ball.getLocation(), 4, 0.1, 0.1, 0.1, 0.01);
            }, 1L, 1L);

            // Devolver automáticamente el hacha a la mano en 1 segundo (20 ticks)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(axeItem);
                    } else {
                        player.getInventory().setItemInOffHand(axeItem);
                    }
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0f, 1.2f);
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                    player.sendMessage("§b§o[Leviatán] El hacha ha regresado a tu mano.");
                }
            }, 20L);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();

        // Tsunami del Tridente de Poseidón al impactar (bloque o entidad)
        if (proj instanceof Trident trident
                && "poseidon_trident".equals(trident.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING))) {
            Location loc = event.getHitBlock() != null
                    ? event.getHitBlock().getLocation().add(0.5, 1, 0.5)
                    : trident.getLocation();
            triggerTsunami(loc, trident.getShooter() instanceof Entity s ? s : null);
        }

        if (proj instanceof Snowball ball) {
            if (ball.hasMetadata("loki_magic")) {
                Location loc = ball.getLocation();
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 35, 1.0, 1.0, 1.0, 0.1);
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 15, 0.5, 0.5, 0.5, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);

                if (event.getHitEntity() instanceof LivingEntity victim) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 2, false, true));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true));
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BAT_DEATH, 0.8f, 0.5f);
                }
            } else if (ball.hasMetadata("kratos_axe")) {
                Location loc = ball.getLocation();
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 30, 0.5, 0.5, 0.5, 0.1);
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

                if (event.getHitEntity() instanceof LivingEntity victim) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 5, false, true));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
                    if (ball.getShooter() instanceof org.bukkit.entity.Entity shooter) {
                        victim.damage(10.0, shooter);
                    } else {
                        victim.damage(10.0);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        // Sed de Sangre del Filo de Ares — cada víctima otorga Fuerza acumulable (hasta V)
        Player killer = event.getEntity().getKiller();
        if (killer == null || !isHoldingItem(killer, "ares_blade")) {
            return;
        }
        int nextAmplifier = 0;
        PotionEffect current = killer.getPotionEffect(PotionEffectType.STRENGTH);
        if (current != null) {
            nextAmplifier = Math.min(current.getAmplifier() + 1, 4); // cap Fuerza V (amplifier 4)
        }
        killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, nextAmplifier, false, true)); // 30s, refresca
        Location kLoc = killer.getLocation();
        kLoc.getWorld().spawnParticle(Particle.DUST, kLoc.clone().add(0, 1, 0), 15, 0.4, 0.6, 0.4,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 1.5f));
        kLoc.getWorld().playSound(kLoc, Sound.ENTITY_WITHER_HURT, 0.5f, 1.4f);
    }

    @EventHandler
    public void onBowShoot(org.bukkit.event.entity.EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack bow = event.getBow();
            if (bow != null && bow.hasItemMeta()) {
                String type = bow.getItemMeta().getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if ("artemis_bow".equals(type)) {
                    // Marcar la flecha
                    event.getProjectile().setMetadata("artemis_arrow", new FixedMetadataValue(plugin, true));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> synchronizeKratosBlades(event.getPlayer()));
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> synchronizeKratosBlades(event.getPlayer()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getSlot() == 40 && isKratosTempOffhand(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> synchronizeKratosBlades(player));
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isKratosBlade(event.getMainHandItem())
                || isKratosBlade(event.getOffHandItem())
                || isKratosTempOffhand(event.getOffHandItem())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> synchronizeKratosBlades(event.getPlayer()));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        restoreKratosOffhand(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        if (!savedOffhands.containsKey(playerId)) {
            return;
        }

        ItemStack originalOffHand = savedOffhands.remove(playerId);
        event.getDrops().removeIf(this::isKratosTempOffhand);

        if (event.getKeepInventory()) {
            player.getInventory().setItemInOffHand(isAir(originalOffHand) ? null : originalOffHand.clone());
            return;
        }

        if (!isAir(originalOffHand)) {
            event.getDrops().add(originalOffHand.clone());
        }
    }
}
