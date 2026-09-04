package org.metamechanists.odysseia.reencarnacion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestiona el ciclo de vida de las sesiones de reencarnacion, codigos de seguridad,
 * entregas pendientes de la capsula y niveles de prestigio.
 */
public final class ReencarnacionManager {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, ReencarnacionSession> SESSIONS_BY_CODE = new ConcurrentHashMap<>();
    private static final Map<UUID, ReencarnacionSession> SESSIONS_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ItemStack>> PENDING_DELIVERIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PRESTIGE_LEVELS = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;
    private final ReencarnacionExecutor executor;
    private final File dataFile;
    private final File deliveriesFile;

    public ReencarnacionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = new ReencarnacionExecutor(plugin);
        this.dataFile = new File(plugin.getDataFolder(), "reencarnaciones_prestige.yml");
        this.deliveriesFile = new File(plugin.getDataFolder(), "reencarnaciones_pendientes.yml");

        loadData();

        // Limpiador periodico de sesiones expiradas cada 60s
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanExpiredSessions, 1200L, 1200L);
    }

    public ReencarnacionSession createSession(Player player, List<ItemStack> capsuleItems) {
        cancelSession(player.getUniqueId());

        String code = generateUniqueCode();
        ReencarnacionSession session = new ReencarnacionSession(player.getUniqueId(), player.getName(), code, capsuleItems);

        SESSIONS_BY_CODE.put(code.toUpperCase(), session);
        SESSIONS_BY_PLAYER.put(player.getUniqueId(), session);

        return session;
    }

    public ReencarnacionSession getSessionByCode(String code) {
        if (code == null) return null;
        ReencarnacionSession session = SESSIONS_BY_CODE.get(code.trim().toUpperCase());
        if (session != null && session.isExpired()) {
            cancelSession(session.getPlayerUuid());
            return null;
        }
        return session;
    }

    public ReencarnacionSession getSessionByPlayer(UUID playerUuid) {
        ReencarnacionSession session = SESSIONS_BY_PLAYER.get(playerUuid);
        if (session != null && session.isExpired()) {
            cancelSession(playerUuid);
            return null;
        }
        return session;
    }

    public void cancelSession(UUID playerUuid) {
        ReencarnacionSession session = SESSIONS_BY_PLAYER.remove(playerUuid);
        if (session != null) {
            SESSIONS_BY_CODE.remove(session.getCode());
        }
    }

    public boolean executeSession(String code) {
        ReencarnacionSession session = getSessionByCode(code);
        if (session == null) {
            return false;
        }

        cancelSession(session.getPlayerUuid());
        return executor.execute(session);
    }

    private void cleanExpiredSessions() {
        List<UUID> toRemove = new ArrayList<>();
        for (ReencarnacionSession session : SESSIONS_BY_PLAYER.values()) {
            if (session.isExpired()) {
                toRemove.add(session.getPlayerUuid());
            }
        }
        for (UUID uuid : toRemove) {
            cancelSession(uuid);
        }
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("RC-");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (SESSIONS_BY_CODE.containsKey(code));
        return code;
    }

    public static Map<UUID, List<ItemStack>> getPendingDeliveries() {
        return PENDING_DELIVERIES;
    }

    public static int getPrestigeLevel(UUID uuid) {
        return PRESTIGE_LEVELS.getOrDefault(uuid, 0);
    }

    public static void setPrestigeLevel(UUID uuid, int level) {
        PRESTIGE_LEVELS.put(uuid, level);
        savePrestigeData();
    }

    private void loadData() {
        try {
            if (dataFile.exists()) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
                for (String key : yaml.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        PRESTIGE_LEVELS.put(uuid, yaml.getInt(key, 0));
                    } catch (Exception ignored) {}
                }
            }
            if (deliveriesFile.exists()) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(deliveriesFile);
                for (String key : yaml.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        List<?> list = yaml.getList(key);
                        if (list != null) {
                            List<ItemStack> items = new ArrayList<>();
                            for (Object o : list) {
                                if (o instanceof ItemStack is) items.add(is);
                            }
                            PENDING_DELIVERIES.put(uuid, items);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Reencarnacion] Error cargando datos persistentes: " + e.getMessage(), e);
        }
    }

    public static void savePrestigeData() {
        try {
            JavaPlugin pl = org.metamechanists.odysseia.Odysseia.getInstance();
            if (pl == null) return;
            File f = new File(pl.getDataFolder(), "reencarnaciones_prestige.yml");
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<UUID, Integer> entry : PRESTIGE_LEVELS.entrySet()) {
                yaml.set(entry.getKey().toString(), entry.getValue());
            }
            yaml.save(f);
        } catch (IOException ignored) {}
    }

    public static void savePendingDeliveries() {
        try {
            JavaPlugin pl = org.metamechanists.odysseia.Odysseia.getInstance();
            if (pl == null) return;
            File f = new File(pl.getDataFolder(), "reencarnaciones_pendientes.yml");
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<UUID, List<ItemStack>> entry : PENDING_DELIVERIES.entrySet()) {
                yaml.set(entry.getKey().toString(), entry.getValue());
            }
            yaml.save(f);
        } catch (IOException ignored) {}
    }
}
