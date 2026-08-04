package org.metamechanists.odysseia.weapons;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Servicio Avanzado de Armas Divinas y Titánicas (Inspirado en Slimefun & FNAmplifications). */
public final class DivineWeaponService implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public DivineWeaponService(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = ChatColor.stripColor(meta.getDisplayName());
        if (name == null) return;

        if (name.contains("Garrote") || name.contains("Hércules")) {
            executeAbility(player, "hercules", 15, () -> executeHerculesAbility(player));
        } else if (name.contains("Daga del Hogar") || name.contains("Hestia")) {
            executeAbility(player, "hestia", 20, () -> executeHestiaAbility(player));
        } else if (name.contains("Caduceo") || name.contains("Hermes")) {
            executeAbility(player, "hermes", 10, () -> executeHermesAbility(player));
        } else if (name.contains("Forja Volcánica") || name.contains("Hefesto")) {
            executeAbility(player, "hefesto", 12, () -> executeHefestoAbility(player));
        } else if (name.contains("Arco de Artemisa") || name.contains("Cacería")) {
            executeAbility(player, "artemisa", 10, () -> executeArtemisaAbility(player));
        } else if (name.contains("Seducción") || name.contains("Afrodita")) {
            executeAbility(player, "afrodita", 18, () -> executeAfroditaAbility(player));
        } else if (name.contains("Espada del Rayo") || name.contains("Zeus")) {
            executeAbility(player, "zeus", 15, () -> executeZeusAbility(player));
        } else if (name.contains("Mjolnir") || name.contains("Thor")) {
            executeAbility(player, "thor", 12, () -> executeThorAbility(player));
        } else if (name.contains("Anubis") || name.contains("Guadaña")) {
            executeAbility(player, "anubis", 20, () -> executeAnubisAbility(player));
        } else if (name.contains("Poseidón") || name.contains("Tridente Abisal")) {
            executeAbility(player, "poseidon", 12, () -> executePoseidonAbility(player));
        } else if (name.contains("Japeto") || name.contains("Hacha Primordial")) {
            executeAbility(player, "japeto", 22, () -> executeJapetoAbility(player));
        } else if (name.contains("Oceanus") || name.contains("Tridente Primordial")) {
            executeAbility(player, "oceanus", 20, () -> executeOceanusAbility(player));
        } else if (name.contains("Hiperión") || name.contains("Espada Solar")) {
            executeAbility(player, "hiperion", 20, () -> executeHiperionAbility(player));
        } else if (name.contains("Cronos") || name.contains("Hoz del Tiempo")) {
            executeAbility(player, "cronos", 30, () -> executeCronosAbility(player));
        } else if (name.contains("Caos") || name.contains("Cetro del Caos")) {
            executeAbility(player, "caos", 25, () -> executeCaosAbility(player));
        }
    }

    private void executeAbility(Player player, String skillId, int cooldownSec, Runnable action) {
        long now = System.currentTimeMillis();
        Map<String, Long> userCds = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        long last = userCds.getOrDefault(skillId, 0L);

        if (now < last + (cooldownSec * 1000L)) {
            long rem = ((last + (cooldownSec * 1000L)) - now) / 1000L;
            player.sendMessage(ChatColor.RED + "[Habilidad] Cooldown activo: " + rem + "s restantes.");
            return;
        }

        userCds.put(skillId, now);
        action.run();
    }

    // 1. Hércules - Onda Expulsora Sísmica
    private void executeHerculesAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0F, 0.5F);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 5);
        player.sendMessage(ChatColor.GOLD + "⚡ Hércules: ¡Golpe Sísmico Leontino!");

        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
            if (e instanceof LivingEntity target && e != player) {
                target.setVelocity(new Vector(0, 1.4, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
            }
        }
    }

    // 2. Hestia - Estación Curativa de Fuego Sagrado
    private void executeHestiaAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
        loc.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 20, 0.8, 0.8, 0.8);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "🔥 Hestia: ¡Fuego Sagrado del Hogar!");
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100, 0));
    }

    // 3. Hermes - Salto Jetpack & Velocidad Mercurial
    private void executeHermesAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_HORSE_GALLOP, 1.0F, 1.8F);
        Vector boost = loc.getDirection().multiply(2.2).setY(1.1);
        player.setVelocity(boost);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 30, 0.5, 0.5, 0.5, 0.1);
        player.sendMessage(ChatColor.YELLOW + "🪽 Hermes: ¡Vuelo & Salto Mercurial!");
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 3));
    }

    // 4. Hefesto - Disparo de Bola de Plasma Magmático (Slimefun Cannon)
    private void executeHefestoAbility(Player player) {
        Location loc = player.getEyeLocation();
        loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0F, 0.8F);
        SmallFireball fireball = player.launchProjectile(SmallFireball.class, loc.getDirection().multiply(2.0));
        fireball.setIsIncendiary(true);
        player.sendMessage(ChatColor.GOLD + "🌋 Hefesto: ¡Canon de Plasma Magmático!");
    }

    // 5. Artemisa - Flecha Teleportadora / Ráfaga Selenita
    private void executeArtemisaAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.6F);
        player.sendMessage(ChatColor.GREEN + "🏹 Artemisa: ¡Lluvia Selenita Perforante!");
        for (int i = -3; i <= 3; i++) {
            Vector dir = loc.getDirection().rotateAroundY(Math.toRadians(i * 8)).multiply(2.0);
            player.launchProjectile(org.bukkit.entity.Arrow.class, dir);
        }
    }

    // 6. Afrodita - Levitación Hipnótica (Mind Control)
    private void executeAfroditaAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ALLAY_ITEM_GIVEN, 1.0F, 0.8F);
        loc.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 40, 1.5, 1.5, 1.5);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "💖 Afrodita: ¡Encanto Hipnótico de Levitación!");

        for (Entity e : player.getNearbyEntities(8, 4, 8)) {
            if (e instanceof LivingEntity target && e != player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 80, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            }
        }
    }

    // 7. Zeus - Cadena de Rayos Celestiales
    private void executeZeusAbility(Player player) {
        Location targetLoc = player.getTargetBlockExact(30) != null ? player.getTargetBlockExact(30).getLocation() : player.getLocation().add(player.getLocation().getDirection().multiply(12));
        targetLoc.getWorld().strikeLightningEffect(targetLoc);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.0F);
        player.sendMessage(ChatColor.AQUA + "⚡ Zeus: ¡Cadena de Rayos Celestiales!");

        for (Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, 6, 6, 6)) {
            if (e instanceof LivingEntity target && e != player) {
                target.damage(12.0, player);
                targetLoc.getWorld().strikeLightningEffect(target.getLocation());
            }
        }
    }

    // 8. Thor - Martillo Bumerán Volador (Flying Boomerang Hammer)
    private void executeThorAbility(Player player) {
        Location loc = player.getEyeLocation();
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THROW, 1.0F, 0.7F);
        player.sendMessage(ChatColor.YELLOW + "🔨 Thor: ¡Lanzamiento Volador de Mjolnir!");

        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());

        Vector dir = loc.getDirection().normalize().multiply(1.2);
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step++ > 15 || !stand.isValid()) {
                    stand.remove();
                    cancel();
                    return;
                }
                stand.teleport(stand.getLocation().add(dir));
                stand.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, stand.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
                for (Entity e : stand.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity target && e != player) {
                        target.damage(18.0, player);
                        target.getWorld().strikeLightningEffect(target.getLocation());
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // 9. Anubis - Invocación Necromántica de Espectros (Summon Minions)
    private void executeAnubisAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1.0F, 0.6F);
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 30, 1.0, 1.0, 1.0, 0.05);
        player.sendMessage(ChatColor.GOLD + "𓀀 Anubis: ¡Invocación Necromántica del Inframundo!");

        for (int i = 0; i < 2; i++) {
            Skeleton skel = (Skeleton) loc.getWorld().spawnEntity(loc.clone().add(i, 0, i), EntityType.SKELETON);
            skel.setCustomName(ChatColor.DARK_GRAY + "Guardia de Anubis (" + player.getName() + ")");
            skel.setCustomNameVisible(true);
            Bukkit.getScheduler().runTaskLater(plugin, skel::remove, 300L);
        }
    }

    // 10. Poseidón - Hydro Jet Flight & Wave
    private void executePoseidonAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0F, 1.0F);
        Vector dir = loc.getDirection().multiply(2.5).setY(1.2);
        player.setVelocity(dir);
        loc.getWorld().spawnParticle(Particle.DRIPPING_WATER, loc, 80, 1.0, 1.0, 1.0, 0.1);
        player.sendMessage(ChatColor.BLUE + "🔱 Poseidón: ¡Chorro Hydro-Jet Abisal!");
    }

    // 11. Titán Japeto - Berserker Knockback Immunity
    private void executeJapetoAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0F, 0.6F);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 40, 0.5, 1.0, 0.5, 0.05);
        player.sendMessage(ChatColor.RED + "⚔ Titán Japeto: ¡Furia Titánica Inquebrantable!");
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 3));
    }

    // 12. Titán Oceanus - Gravitational Vortex
    private void executeOceanusAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_WATER_AMBIENT, 1.0F, 0.5F);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 60, 2.0, 2.0, 2.0, 0.05);
        player.sendMessage(ChatColor.DARK_AQUA + "🌊 Titán Oceanus: ¡Vórtice Abisal Atraedor!");

        for (Entity e : player.getNearbyEntities(12, 5, 12)) {
            if (e instanceof LivingEntity target && e != player) {
                Vector pull = loc.toVector().subtract(target.getLocation().toVector()).normalize().multiply(2.0);
                target.setVelocity(pull);
            }
        }
    }

    // 13. Titán Hiperión - Laser Solar de Inmolación
    private void executeHiperionAbility(Player player) {
        Location loc = player.getEyeLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.5F);
        player.sendMessage(ChatColor.GOLD + "☀️ Titán Hiperión: ¡Rayo Solar de Inmolación!");

        Vector dir = loc.getDirection().normalize();
        for (int i = 1; i <= 15; i++) {
            Location pLoc = loc.clone().add(dir.clone().multiply(i));
            pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 15, 0.2, 0.2, 0.2, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.0F));
            for (Entity e : pLoc.getWorld().getNearbyEntities(pLoc, 1.5, 1.5, 1.5)) {
                if (e instanceof LivingEntity target && e != player) {
                    target.setFireTicks(100);
                    target.damage(10.0, player);
                }
            }
        }
    }

    // 14. Titán Cronos - Time Freeze Anchor
    private void executeCronosAbility(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0F, 0.5F);
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 80, 2.5, 2.5, 2.5, 0.1);
        player.sendMessage(ChatColor.DARK_PURPLE + "⏳ Titán Cronos: ¡Detención del Tiempo!");

        for (Entity e : player.getNearbyEntities(15, 6, 15)) {
            if (e instanceof LivingEntity target && e != player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                target.setVelocity(new Vector(0, 0, 0));
            }
        }
    }

    // 15. Titán Caos - Espada Voladora Autónoma (Floating Autonomous Blade)
    private void executeCaosAbility(Player player) {
        Location loc = player.getEyeLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.8F, 0.5F);
        player.sendMessage(ChatColor.DARK_RED + "🌌 Titán Caos: ¡Espada Voladora Autónoma del Caos!");

        ArmorStand swordStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        swordStand.setVisible(false);
        swordStand.setGravity(false);
        swordStand.setCustomName(ChatColor.RED + "Espada del Caos (" + player.getName() + ")");
        swordStand.setCustomNameVisible(true);
        swordStand.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > 120 || !swordStand.isValid() || !player.isOnline()) {
                    swordStand.remove();
                    cancel();
                    return;
                }

                LivingEntity closest = null;
                double closestDist = 15.0;
                for (Entity e : swordStand.getNearbyEntities(15, 6, 15)) {
                    if (e instanceof LivingEntity target && e != player && !(e instanceof ArmorStand)) {
                        double dist = target.getLocation().distance(swordStand.getLocation());
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = target;
                        }
                    }
                }

                if (closest != null) {
                    Vector dir = closest.getEyeLocation().toVector().subtract(swordStand.getLocation().toVector()).normalize().multiply(0.8);
                    swordStand.teleport(swordStand.getLocation().add(dir));
                    swordStand.getWorld().spawnParticle(Particle.DUST, swordStand.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, new Particle.DustOptions(Color.fromRGB(255, 0, 85), 2.0F));
                    if (closestDist < 2.0) {
                        closest.damage(14.0, player);
                    }
                } else {
                    swordStand.teleport(player.getEyeLocation().add(player.getLocation().getDirection().multiply(2)));
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }
}
