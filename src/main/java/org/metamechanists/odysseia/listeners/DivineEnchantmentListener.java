package org.metamechanists.odysseia.listeners;

import org.metamechanists.odysseia.util.ParticleCompat;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;

import java.util.*;

/**
 * Motor de Encantamientos y Habilidades Pasivas Mitologicas para Rangos de Pago (Hercules a Titan).
 * Detecta lore, nombres y persistencia de encantamientos custom en armas, armaduras y herramientas.
 */
public final class DivineEnchantmentListener implements Listener {

    private final Odysseia plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> duatCooldowns = new HashMap<>();
    private final Map<UUID, Long> cronosDodgeCooldowns = new HashMap<>();

    public DivineEnchantmentListener(Odysseia plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean hasLoreOrName(ItemStack item, String... keywords) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String displayName = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()).toLowerCase() : "";
        List<String> lore = meta.hasLore() ? meta.getLore() : Collections.emptyList();

        for (String kw : keywords) {
            String cleanKw = kw.toLowerCase();
            if (displayName.contains(cleanKw)) return true;
            for (String line : lore) {
                if (ChatColor.stripColor(line).toLowerCase().contains(cleanKw)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==========================================
    // 1. COMBATE OFENSIVO (Encantamientos en Armas)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponAttack(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() == Material.AIR) return;

                if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof ArmorStand || target.hasMetadata("NPC")) return;
        if (attacker.equals(target)) return;
        Location loc = target.getLocation();

        // 1. HERCULES: Fuerza Herculea IV (Golpe Sismico & Knockback)
        if (hasLoreOrName(weapon, "fuerza hercúlea", "fuerza herculea", "garrote", "mazo de hércules", "mazo de hercules")) {
            event.setDamage(event.getDamage() * 1.25);
            try {
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 2);
                loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
                attacker.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.7f, 0.6f);
                for (Entity e : target.getNearbyEntities(3.5, 2.0, 3.5)) {
                    if (e instanceof LivingEntity near && near != attacker) {
                        near.setVelocity(near.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8).setY(0.4));
                        near.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. HESTIA: Llama Sagrada de Vesta (Incinera & Debilita)
        if (hasLoreOrName(weapon, "llama sagrada", "vesta", "daga del hogar")) {
            target.setFireTicks(160);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0));
            try {
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                attacker.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.2f);
            } catch (Exception ignored) {}
        }

        // 3. HERMES: Caduceo Sanador (Speed al atacante & Purga de Debuffs)
        if (hasLoreOrName(weapon, "caduceo", "mercurial", "zancada")) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false, true));
            attacker.removePotionEffect(PotionEffectType.BLINDNESS);
            attacker.removePotionEffect(PotionEffectType.SLOWNESS);
            attacker.removePotionEffect(PotionEffectType.POISON);
            attacker.removePotionEffect(PotionEffectType.WITHER);
            try {
                attacker.getWorld().spawnParticle(Particle.CLOUD, attacker.getLocation().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.05);
                attacker.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.8f);
            } catch (Exception ignored) {}
        }

        // 4. ARTEMISA: Flecha Lunar Infalible / Cazadora (Perforacion Absoluta & Marcador)
        if (hasLoreOrName(weapon, "flecha lunar", "cacería", "caceria", "artemisa")) {
            event.setDamage(event.getDamage() + 6.0);
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            try {
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 15, 0.2, 0.4, 0.2, 0.08);
                attacker.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
            } catch (Exception ignored) {}
        }

        // 5. AFRODITA: Seduccion Fatal (Pacificacion temporal & Lifesteal)
        if (hasLoreOrName(weapon, "seducción", "seduccion", "afrodita")) {
            if (random.nextDouble() < 0.25) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 2));
                double maxHp = 20.0;
                try {
                    if (attacker.getAttribute(Attribute.MAX_HEALTH) != null) {
                        maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
                    }
                } catch (Throwable ignored) {}
                attacker.setHealth(Math.min(maxHp, attacker.getHealth() + 3.0));
                try {
                    loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1.5, 0), 12, 0.4, 0.4, 0.4, 0.1);
                    attacker.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                } catch (Exception ignored) {}
            }
        }

        // 6. ATENEA: Sabiduria Tactica de Palas (Contraataque critico)
        if (hasLoreOrName(weapon, "sabiduría táctica", "sabiduria tactica", "palas", "atenea")) {
            if (random.nextDouble() < 0.25) {
                event.setDamage(event.getDamage() * 1.5);
                try {
                    loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 2);
                    loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                    attacker.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
                } catch (Exception ignored) {}
            }
        }

        // 7. ZEUS: Ira del Olimpo (Rayo en cadena a 3 entidades)
        if (hasLoreOrName(weapon, "ira del olimpo", "espada del rayo", "zeus")) {
            if (random.nextDouble() < 0.30) {
                try {
                    loc.getWorld().strikeLightningEffect(loc);
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                    attacker.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);

                    int chained = 0;
                    for (Entity e : target.getNearbyEntities(6.0, 3.0, 6.0)) {
                        if (e instanceof LivingEntity near && near != attacker && near != target) {
                            near.damage(10.0, attacker);
                            near.getWorld().strikeLightningEffect(near.getLocation());
                            if (++chained >= 3) break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // 8. THOR: Impacto Sismico de Mjolnir (Onda destructora de area)
        if (hasLoreOrName(weapon, "impacto de mjolnir", "mjolnir", "thor")) {
            try {
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 0.5, 0), 3);
                attacker.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                for (Entity e : target.getNearbyEntities(6.0, 3.0, 6.0)) {
                    if (e instanceof LivingEntity near && near != attacker) {
                        near.damage(8.0, attacker);
                        near.setVelocity(new Vector(0, 0.8, 0));
                        near.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                    }
                }
            } catch (Exception ignored) {}
        }

        // 9. ANUBIS: Pesaje del Corazon (Necrosis del Duat & Lifesteal)
        if (hasLoreOrName(weapon, "pesaje del corazón", "pesaje del corazon", "guadaña", "guadana", "anubis")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2));
            double maxHp = 20.0;
            try {
                if (attacker.getAttribute(Attribute.MAX_HEALTH) != null) {
                    maxHp = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
                }
            } catch (Throwable ignored) {}
            attacker.setHealth(Math.min(maxHp, attacker.getHealth() + 4.0));
            try {
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.05);
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.03);
                attacker.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.9f);
            } catch (Exception ignored) {}
        }

        // 10. POSEIDON: Tsunami Abisal (Poder en agua/lluvia & Disparo Hidraulico)
        if (hasLoreOrName(weapon, "tsunami abisal", "tridente abisal", "poseidón", "poseidon")) {
            boolean inWater = loc.getBlock().getType() == Material.WATER || loc.getWorld().hasStorm();
            if (inWater) {
                event.setDamage(event.getDamage() * 1.5);
            }
            Vector blast = loc.toVector().subtract(attacker.getLocation().toVector()).normalize().multiply(1.5).setY(0.5);
            target.setVelocity(blast);
            try {
                loc.getWorld().spawnParticle(Particle.SPLASH, loc.clone().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.2);
                loc.getWorld().spawnParticle(Particle.BUBBLE_POP, loc.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
                attacker.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.9f, 1.0f);
            } catch (Exception ignored) {}
        }

        // 11. TITAN JAPETO: Empalamiento Titanico (Armor Penetration)
        if (hasLoreOrName(weapon, "empalamiento titánico", "empalamiento titanico", "furia titánica", "furia titanica", "japeto")) {
            event.setDamage(event.getDamage() + 8.0);
            try {
                loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.15);
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
                attacker.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 0.6f);
            } catch (Exception ignored) {}
        }

        // 12. TITAN OCEANUS: Vortice Cosmico (Atraccion Masiva)
        if (hasLoreOrName(weapon, "vórtice cósmico", "vortice cosmico", "tridente primordial", "oceanus")) {
            try {
                loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.1);
                attacker.playSound(loc, Sound.BLOCK_CONDUIT_DEACTIVATE, 1.0f, 0.8f);
                for (Entity e : target.getNearbyEntities(10.0, 4.0, 10.0)) {
                    if (e instanceof LivingEntity near && near != attacker) {
                        Vector pull = loc.toVector().subtract(near.getLocation().toVector()).normalize().multiply(1.2);
                        near.setVelocity(pull);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 13. TITAN HIPERION: Fulguracion Solar Divina (Rayo Solar e Incineracion)
        if (hasLoreOrName(weapon, "fulguración solar", "fulguracion solar", "espada solar", "hiperión", "hiperion")) {
            target.setFireTicks(240);
            event.setDamage(event.getDamage() + 10.0);
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            try {
                loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 30, 0.4, 0.4, 0.4, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.0F));
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                attacker.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            } catch (Exception ignored) {}
        }

        // 14. TITAN CRONOS: Detencion Temporal de Cronos (Slowness VI & Congelacion)
        if (hasLoreOrName(weapon, "detención temporal", "detencion temporal", "hoz del tiempo", "cronos")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 5));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 3));
            target.setVelocity(new Vector(0, 0, 0));
            try {
                loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 60, 0.8, 0.8, 0.8, 0.2);
                attacker.playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 0.5f);
            } catch (Exception ignored) {}
        }

        // 15. TITAN CAOS: Aniquilacion del Vacio (Drenaje del 25% Vida Actual & Singularidad)
        if (hasLoreOrName(weapon, "aniquilación del vacío", "aniquilacion del vacio", "cetro del caos", "caos")) {
            if (random.nextDouble() < 0.20) {
                double currentHp = target.getHealth();
                double voidDamage = Math.max(8.0, currentHp * 0.25);
                event.setDamage(event.getDamage() + voidDamage);
                try {
                    loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);
                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.08);
                    ParticleCompat.spawnDragonBreath(loc.getWorld(), loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05, 0.0f);
                    attacker.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.6f);
                    attacker.sendTitle("§4§l¡ANIQUILACION DEL CAOS!", "§cDesgarro del vacio desatado (-25% vida)", 5, 25, 5);
                } catch (Exception ignored) {}
            }
        }
    }

    // ==========================================
    // 2. HERRAMIENTAS: AUTO-SMELT (Hefesto)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToolBreakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

        if (hasLoreOrName(tool, "forja volcánica", "forja volcanica", "hefesto", "pico de hefesto", "hacha de hefesto", "pala de hefesto")) {
            Block block = event.getBlock();
            Material type = block.getType();
            Material smelted = null;

            switch (type) {
                case RAW_IRON_BLOCK: smelted = Material.IRON_BLOCK; break;
                case RAW_COPPER_BLOCK: smelted = Material.COPPER_BLOCK; break;
                case RAW_GOLD_BLOCK: smelted = Material.GOLD_BLOCK; break;
                case IRON_ORE:
                case DEEPSLATE_IRON_ORE: smelted = Material.IRON_INGOT; break;
                case COPPER_ORE:
                case DEEPSLATE_COPPER_ORE: smelted = Material.COPPER_INGOT; break;
                case GOLD_ORE:
                case DEEPSLATE_GOLD_ORE: smelted = Material.GOLD_INGOT; break;
                case ANCIENT_DEBRIS: smelted = Material.NETHERITE_SCRAP; break;
                case OAK_LOG:
                case SPRUCE_LOG:
                case BIRCH_LOG:
                case JUNGLE_LOG:
                case ACACIA_LOG:
                case DARK_OAK_LOG:
                case MANGROVE_LOG:
                case CHERRY_LOG: smelted = Material.CHARCOAL; break;
                default: break;
            }

            if (smelted != null) {
                event.setDropItems(false);
                int amount = 1;
                if (random.nextDouble() < 0.30) {
                    amount = 2;
                }
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(smelted, amount));
                try {
                    block.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.05);
                    block.getWorld().spawnParticle(Particle.LAVA, block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.4f, 1.8f);
                } catch (Exception ignored) {}
            }
        }
    }

    // ==========================================
    // 3. DEFENSA: RESISTENCIA Y REFLEJO DE ARMADURAS
    // ==========================================
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArmorDefense(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;

        ItemStack chest = defender.getInventory().getChestplate();

        // 1. Hercules: Piel de Nemea (-20% daño)
        if (hasLoreOrName(chest, "nemea", "hércules", "hercules")) {
            event.setDamage(event.getDamage() * 0.80);
        }

        // 2. Atenea: Egida de Palas (Reflejo del 35% de daño)
        if (hasLoreOrName(chest, "égida", "egida", "palas", "atenea")) {
            if (event.getDamager() instanceof LivingEntity attacker) {
                attacker.damage(event.getDamage() * 0.35, defender);
                try {
                    defender.getWorld().spawnParticle(Particle.CRIT, defender.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                    defender.playSound(defender.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.5f);
                } catch (Exception ignored) {}
            }
        }

        // 3. Afrodita: Gracia Protectora (Ceguera y lentitud al agresor)
        if (hasLoreOrName(chest, "gracia protectora", "afrodita")) {
            if (event.getDamager() instanceof LivingEntity attacker) {
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            }
        }

        // 4. Zeus: Soberania Celestial (Choque electrico a quien golpee)
        if (hasLoreOrName(chest, "soberanía", "soberania", "zeus")) {
            if (event.getDamager() instanceof LivingEntity attacker) {
                attacker.setVelocity(attacker.getLocation().toVector().subtract(defender.getLocation().toVector()).normalize().multiply(1.0).setY(0.3));
                attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, attacker.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
            }
        }

        // 5. Titan Cronos: Evasion Temporal (20% de probabilidad de anular ataque)
        if (hasLoreOrName(chest, "reloj del universo", "cronos")) {
            long now = System.currentTimeMillis();
            long last = cronosDodgeCooldowns.getOrDefault(defender.getUniqueId(), 0L);
            if (now - last > 5000L && random.nextDouble() < 0.20) {
                cronosDodgeCooldowns.put(defender.getUniqueId(), now);
                event.setCancelled(true);
                try {
                    defender.getWorld().spawnParticle(Particle.REVERSE_PORTAL, defender.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                    defender.playSound(defender.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                    defender.sendMessage("§5[Cronos] §d¡Has rebobinado el tiempo evitando el ataque enemigo!");
                } catch (Exception ignored) {}
                return;
            }
        }

        // 6. Anubis: Invocacion Espectral ante Golpe Critico
        if (hasLoreOrName(chest, "manto del duat", "anubis")) {
            if (defender.getHealth() - event.getFinalDamage() <= 6.0) {
                long now = System.currentTimeMillis();
                long last = duatCooldowns.getOrDefault(defender.getUniqueId(), 0L);
                if (now - last > 120000L) {
                    duatCooldowns.put(defender.getUniqueId(), now);
                    Location loc = defender.getLocation();
                    try {
                        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 40, 1.0, 1.0, 1.0, 0.05);
                        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.6f);
                        defender.sendMessage("§6[Anubis] §f¡Los guardianes del Duat han emergido para protegerte!");

                        for (int i = 0; i < 2; i++) {
                            Skeleton skel = (Skeleton) loc.getWorld().spawnEntity(loc.clone().add(i, 0, i), EntityType.SKELETON);
                            skel.setCustomName("§8Guardián del Duat (§e" + defender.getName() + "§8)");
                            skel.setCustomNameVisible(true);
                            Bukkit.getScheduler().runTaskLater(plugin, skel::remove, 300L);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    // ==========================================
    // 4. CONSUMIBLES DIVINOS (Ambrosia)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onAmbrosiaConsume(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String name = ChatColor.stripColor(meta.getDisplayName());

        boolean isOlimpica = name.contains("Ambrosía Olímpica") || name.contains("Ambrosia Olimpica") || name.contains("Néctar de los Dioses") || name.contains("Nectar de los Dioses");
        boolean isCaos = name.contains("Ambrosía del Caos") || name.contains("Ambrosia del Caos") || name.contains("Ambrosía Primordial") || name.contains("Ambrosia Primordial");

        if (!isOlimpica && !isCaos) return;

        event.setCancelled(true);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        double maxHp = 20.0;
        try {
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            }
        } catch (Throwable ignored) {}
        player.setHealth(maxHp);

        player.setFoodLevel(20);
        player.setSaturation(20.0f);

        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.DARKNESS);

        Location loc = player.getLocation();
        if (isCaos) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 3));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 3));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0));

            try {
                loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 0.8f);
                loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1.0, 0), 40, 0.4, 0.6, 0.4, 0.15);
                ParticleCompat.spawnDragonBreath(loc.getWorld(), loc.clone().add(0, 1.0, 0), 25, 0.3, 0.5, 0.3, 0.05, 0.0f);
            } catch (Exception ignored) {}

            player.sendTitle("§4§l¡AMBROSIA DEL CAOS!", "§ePoder primordial restaurado al 100%", 5, 40, 10);
            player.sendMessage("§4[Caos] §f¡Has ingerido la §cAmbrosía Primordial del Caos§f! Tu cuerpo trasciende todo daño mortal.");
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));

            try {
                loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 0.8f, 1.4f);
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1.0, 0), 25, 0.3, 0.5, 0.3, 0.1);
                loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1.5, 0), 10, 0.4, 0.4, 0.4, 0.1);
            } catch (Exception ignored) {}

            player.sendTitle("§6§l¡AMBROSIA OLIMPICA!", "§eEnergía divina restaurada al 100%", 5, 35, 10);
            player.sendMessage("§6[Olimpo] §f¡Has consumido la §eAmbrosía Olímpica§f! Bendición divina y vitalidad celestial activa.");
        }
    }
}
