package org.metamechanists.odysseia.laboratorio;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Guarda el inventario real de un jugador mientras esta dentro del laboratorio.
 *
 * Es un deposito, no una boveda: solo hay una fila por jugador y existe unicamente entre que
 * entra al laboratorio y que sale. Se persiste en SQLite y no en memoria a proposito, porque el
 * caso que importa es justo el que la memoria no sobrevive: si el servidor se cae con gente
 * dentro, al volver el inventario sigue estando aqui y se puede devolver.
 */
public final class SandboxStashRepository implements AutoCloseable {

    /** Lo que un jugador deja en consigna al entrar al laboratorio. */
    public record Stash(ItemStack[] contents, ItemStack[] armor, ItemStack offhand,
                        int level, float exp, String gameMode) {}

    private final Connection connection;

    public SandboxStashRepository(File database) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA busy_timeout=5000");
            // synchronous=FULL: este deposito guarda el inventario real de un jugador que esta a
            // punto de quedarse sin el. Merece la pena pagar el fsync por cada entrada.
            statement.execute("PRAGMA synchronous=FULL");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS sandbox_stash(
                      player_uuid TEXT PRIMARY KEY,
                      contents TEXT,
                      armor TEXT,
                      offhand TEXT,
                      level INTEGER NOT NULL,
                      exp REAL NOT NULL,
                      game_mode TEXT NOT NULL,
                      stored_at TEXT NOT NULL)""");
        }
    }

    public synchronized void save(UUID player, Stash stash) throws SQLException {
        try (PreparedStatement upsert = connection.prepareStatement("""
                INSERT INTO sandbox_stash(player_uuid,contents,armor,offhand,level,exp,game_mode,stored_at)
                VALUES(?,?,?,?,?,?,?,?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                  contents=excluded.contents, armor=excluded.armor, offhand=excluded.offhand,
                  level=excluded.level, exp=excluded.exp, game_mode=excluded.game_mode,
                  stored_at=excluded.stored_at""")) {
            upsert.setString(1, player.toString());
            upsert.setString(2, serialize(stash.contents()));
            upsert.setString(3, serialize(stash.armor()));
            upsert.setString(4, serialize(new ItemStack[] { stash.offhand() }));
            upsert.setInt(5, stash.level());
            upsert.setFloat(6, stash.exp());
            upsert.setString(7, stash.gameMode());
            upsert.setString(8, Instant.now().toString());
            upsert.executeUpdate();
        }
    }

    /** Lo depositado, o null si el jugador no tiene nada en consigna. */
    public synchronized Stash load(UUID player) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT * FROM sandbox_stash WHERE player_uuid=?")) {
            query.setString(1, player.toString());
            ResultSet row = query.executeQuery();
            if (!row.next()) return null;
            ItemStack[] offhand = deserialize(row.getString("offhand"));
            return new Stash(
                    deserialize(row.getString("contents")),
                    deserialize(row.getString("armor")),
                    offhand.length > 0 ? offhand[0] : null,
                    row.getInt("level"),
                    row.getFloat("exp"),
                    row.getString("game_mode"));
        }
    }

    public synchronized void clear(UUID player) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM sandbox_stash WHERE player_uuid=?")) {
            delete.setString(1, player.toString());
            delete.executeUpdate();
        }
    }

    /** Todos los jugadores con inventario en consigna. Sirve para reconciliar al arrancar. */
    public synchronized java.util.List<UUID> pending() throws SQLException {
        java.util.List<UUID> result = new java.util.ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT player_uuid FROM sandbox_stash")) {
            while (rows.next()) {
                try {
                    result.add(UUID.fromString(rows.getString("player_uuid")));
                } catch (IllegalArgumentException ignored) {
                    // una fila corrupta no debe impedir devolver el resto de inventarios
                }
            }
        }
        return result;
    }

    static String serialize(ItemStack[] contents) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeInt(contents.length);
            for (ItemStack item : contents) output.writeObject(item);
            output.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo serializar el inventario", error);
        }
    }

    static ItemStack[] deserialize(String data) {
        if (data == null || data.isBlank()) return new ItemStack[0];
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream input = new BukkitObjectInputStream(bytes)) {
            ItemStack[] contents = new ItemStack[input.readInt()];
            for (int index = 0; index < contents.length; index++) contents[index] = (ItemStack) input.readObject();
            return contents;
        } catch (IOException | ClassNotFoundException error) {
            throw new IllegalStateException("No se pudo leer el inventario depositado", error);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}
