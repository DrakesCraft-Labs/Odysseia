package org.metamechanists.odysseia.integrations;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optional bridge to DiosesDrakes. Reflection deliberately keeps Odysseia loadable when the
 * progression plugin is absent and prevents an API dependency cycle between both plugins.
 */
public final class DiosesDrakesBossBridge {
    private static final String ACCESS_CLASS = "cl.drakescraft.diosesdrakes.api.DivineAccess";
    private static final String VICTORY_CLASS = "cl.drakescraft.diosesdrakes.api.DivineBossVictory";

    private final Odysseia plugin;
    private volatile boolean warnedUnavailable;

    public DiosesDrakesBossBridge(Odysseia plugin) {
        this.plugin = plugin;
    }

    /** Sends one idempotent favor request per eligible participant after a boss death. */
    public void rewardParticipants(UUID bossInstanceId, OdysseyBoss boss, List<Player> participants,
                                   Map<UUID, Double> contributions) {
        if (!plugin.getConfig().getBoolean("integrations.diosesdrakes.boss-rewards.enabled", true)
                || participants.isEmpty()) {
            return;
        }

        try {
            Class<?> accessClass = Class.forName(ACCESS_CLASS);
            @SuppressWarnings({"rawtypes", "unchecked"})
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration((Class) accessClass);
            if (provider == null || provider.getProvider() == null) {
                warnUnavailable("DiosesDrakes no registro DivineAccess; se omiten recompensas divinas.");
                return;
            }

            Class<?> victoryClass = Class.forName(VICTORY_CLASS);
            Constructor<?> constructor = victoryClass.getConstructor(UUID.class, UUID.class, String.class,
                    double.class, double.class, int.class, Instant.class);
            Method reward = accessClass.getMethod("rewardBossVictory", victoryClass);
            double totalContribution = contributions.values().stream().mapToDouble(Double::doubleValue).sum();
            Instant defeatedAt = Instant.now();

            for (Player participant : participants) {
                Object victory = constructor.newInstance(bossInstanceId, participant.getUniqueId(), boss.getId(),
                        contributions.getOrDefault(participant.getUniqueId(), 0.0D), totalContribution,
                        participants.size(), defeatedAt);
                reward.invoke(provider.getProvider(), victory);
            }
            warnedUnavailable = false;
        } catch (ClassNotFoundException exception) {
            warnUnavailable("DiosesDrakes no esta instalado; se omiten recompensas divinas.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("[DiosesDrakes] No se pudo entregar favor de boss: "
                    + exception.getClass().getSimpleName());
        }
    }

    private void warnUnavailable(String message) {
        if (!warnedUnavailable) {
            warnedUnavailable = true;
            plugin.getLogger().info("[DiosesDrakes] " + message);
        }
    }
}
