package org.metamechanists.odysseia.events.pvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Almacena de forma 100% segura y atómica el inventario real de un jugador durante eventos PvP.
 * Utiliza SQLite WAL con PRAGMA synchronous=FULL para garantizar tolerancia a caídas.
 */
public class EventPvPStashRepository implements AutoCloseable {

    public record Stash(ItemStack[] contents, ItemStack[] armor, ItemStack offhand,
                        int level, float exp, String gameMode, String locationStr) {}

    private final Connection connection;
    private final Logger logger;

    public EventPvPStashRepository(File databaseFile, Logger logger) throws SQLException {
        this.logger = logger;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=5000");
            stmt.execute("PRAGMA synchronous=FULL");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS odysseia_pvp_stash (
                    player_uuid TEXT PRIMARY KEY,
                    contents TEXT,
                    armor TEXT,
                    offhand TEXT,
                    level INTEGER NOT NULL,
                    exp REAL NOT NULL,
                    game_mode TEXT NOT NULL,
                    location TEXT NOT NULL,
                    stored_at TEXT NOT NULL
                )
            """);
        }
    }

    public synchronized boolean stashPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        String locStr = loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();

        Stash stash = new Stash(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getInventory().getItemInOffHand(),
                player.getLevel(),
                player.getExp(),
                player.getGameMode().name(),
                locStr
        );

        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO odysseia_pvp_stash(player_uuid, contents, armor, offhand, level, exp, game_mode, location, stored_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                contents=excluded.contents,
                armor=excluded.armor,
                offhand=excluded.offhand,
                level=excluded.level,
                exp=excluded.exp,
                game_mode=excluded.game_mode,
                location=excluded.location,
                stored_at=excluded.stored_at
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serialize(stash.contents()));
            ps.setString(3, serialize(stash.armor()));
            ps.setString(4, serialize(new ItemStack[]{stash.offhand()}));
            ps.setInt(5, stash.level());
            ps.setFloat(6, stash.exp());
            ps.setString(7, stash.gameMode());
            ps.setString(8, stash.locationStr());
            ps.setString(9, Instant.now().toString());
            ps.executeUpdate();

            // Verificación inmediata antes de vaciar el inventario
            if (hasStash(uuid)) {
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.getInventory().setItemInOffHand(null);
                player.setLevel(0);
                player.setExp(0);
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[PvPStash] Error crítico guardando inventario de " + player.getName(), e);
        }
        return false;
    }

    public synchronized boolean restorePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Stash stash = load(uuid);
        if (stash == null) return false;

        try {
            player.getInventory().clear();
            if (stash.contents() != null) {
                player.getInventory().setContents(stash.contents());
            }
            if (stash.armor() != null) {
                player.getInventory().setArmorContents(stash.armor());
            }
            if (stash.offhand() != null) {
                player.getInventory().setItemInOffHand(stash.offhand());
            }
            player.setLevel(stash.level());
            player.setExp(stash.exp());

            try {
                player.setGameMode(org.bukkit.GameMode.valueOf(stash.gameMode()));
            } catch (Exception ignored) {}

            Location prevLoc = deserializeLocation(stash.locationStr());
            if (prevLoc != null) {
                player.teleport(prevLoc);
            }

            clear(uuid);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[PvPStash] Error restaurando inventario de " + player.getName(), e);
            return false;
        }
    }

    public synchronized boolean hasStash(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM odysseia_pvp_stash WHERE player_uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized Stash load(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM odysseia_pvp_stash WHERE player_uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ItemStack[] offhand = deserialize(rs.getString("offhand"));
                return new Stash(
                        deserialize(rs.getString("contents")),
                        deserialize(rs.getString("armor")),
                        offhand.length > 0 ? offhand[0] : null,
                        rs.getInt("level"),
                        rs.getFloat("exp"),
                        rs.getString("game_mode"),
                        rs.getString("location")
                );
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PvPStash] Error cargando stash de " + uuid, e);
            return null;
        }
    }

    public synchronized void clear(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM odysseia_pvp_stash WHERE player_uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PvPStash] Error eliminando stash de " + uuid, e);
        }
    }

    public synchronized List<UUID> getAllStashedPlayers() {
        List<UUID> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_uuid FROM odysseia_pvp_stash")) {
            while (rs.next()) {
                try {
                    list.add(UUID.fromString(rs.getString("player_uuid")));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PvPStash] Error listando stashes pendientes", e);
        }
        return list;
    }

    private static Location deserializeLocation(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] p = raw.split(";");
        if (p.length < 6) return null;
        var world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        try {
            double x = Double.parseDouble(p[1]);
            double y = Double.parseDouble(p[2]);
            double z = Double.parseDouble(p[3]);
            float yaw = Float.parseFloat(p[4]);
            float pitch = Float.parseFloat(p[5]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String serialize(ItemStack[] contents) {
        if (contents == null) contents = new ItemStack[0];
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             BukkitObjectOutputStream data = new BukkitObjectOutputStream(output)) {
            data.writeInt(contents.length);
            for (ItemStack item : contents) {
                data.writeObject(item);
            }
            data.flush();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Error serializando stash PvP", e);
        }
    }

    private static ItemStack[] deserialize(String raw) {
        if (raw == null || raw.isBlank()) return new ItemStack[0];
        byte[] bytes = Base64.getDecoder().decode(raw);
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream data = new BukkitObjectInputStream(input)) {
            int length = data.readInt();
            ItemStack[] contents = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                contents[i] = (ItemStack) data.readObject();
            }
            return contents;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Error deserializando stash PvP", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[PvPStash] Error cerrando conexión SQLite", e);
        }
    }
}
