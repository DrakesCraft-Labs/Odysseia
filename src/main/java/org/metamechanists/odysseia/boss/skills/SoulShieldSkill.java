package org.metamechanists.odysseia.boss.skills;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.instances.DiosCorruptoBoss;

public class SoulShieldSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (!(boss instanceof DiosCorruptoBoss dios)) {
            return;
        }

        LivingEntity entity = dios.getEntity();
        if (entity == null || entity.isDead()) {
            return;
        }

        double currentHealth = entity.getHealth();
        double maxHealth = dios.getMaxHealth();

        // Only activate if health is below 50% and shield has not been triggered yet
        if (currentHealth < maxHealth * 0.5 && !dios.isShieldActivated()) {
            dios.setShieldActivated(true);
            Location loc = entity.getLocation();

            // Spawn 3 crystals in a triangular layout
            double radius = 4.0;
            for (int i = 0; i < 3; i++) {
                double angle = i * (2 * Math.PI / 3);
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location crystalLoc = loc.clone().add(x, 1.5, z);

                org.bukkit.entity.EnderCrystal crystal = (org.bukkit.entity.EnderCrystal) loc.getWorld().spawnEntity(crystalLoc, EntityType.END_CRYSTAL);
                crystal.setShowingBottom(false);
                crystal.setBeamTarget(loc.clone().add(0, 1.5, 0));
                dios.getActiveCrystals().add(crystal);
            }

            // Audio-visuals
            loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.5f, 0.7f);
            loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 1.0f, 0.8f);
            loc.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH, loc.add(0, 1.5, 0), 100, 1.0, 1.0, 1.0, 0.1);

            // Alert nearby players
            loc.getWorld().getNearbyEntities(loc, 25.0, 25.0, 25.0).forEach(e -> {
                if (e instanceof Player p) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        "&c&l¡El Dios Corrupto invoca un Escudo de Almas! &7¡Destruye los Cristales de Ender para dañarlo!"));
                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&c&lESCUDO DE ALMAS"), 
                               ChatColor.translateAlternateColorCodes('&', "&7¡Destruye los Cristales!"), 10, 40, 10);
                }
            });
        }
    }
}
