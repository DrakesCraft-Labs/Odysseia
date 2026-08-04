package org.metamechanists.odysseia.vaults;

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
 * Almacen SQLite de las bovedas por modalidad. La clave primaria incluye la modalidad, asi que
 * cada modalidad tiene su propio juego de bovedas para el mismo jugador y no hay forma de mover
 * items de una a otra a traves de /pv.
 *
 * Los items se guardan con la serializacion de Bukkit en Base64, que sobrevive a los cambios de
 * version del servidor mejor que un volcado NBT crudo.
 */
public final class ModalityVaultRepository implements AutoCloseable {

    private final Connection connection;

    public ModalityVaultRepository(File database) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS modality_vaults(
                      player_uuid TEXT NOT NULL,
                      modality TEXT NOT NULL,
                      vault_number INTEGER NOT NULL,
                      items_data TEXT,
                      updated_at TEXT NOT NULL,
                      PRIMARY KEY(player_uuid, modality, vault_number))""");
        }
    }

    /** Contenido de una boveda, o un arreglo vacio si nunca se ha usado. */
    public synchronized ItemStack[] load(UUID player, String modality, int vault, int size) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT items_data FROM modality_vaults WHERE player_uuid=? AND modality=? AND vault_number=?")) {
            query.setString(1, player.toString());
            query.setString(2, modality);
            query.setInt(3, vault);
            ResultSet row = query.executeQuery();
            if (!row.next()) return new ItemStack[size];
            String data = row.getString("items_data");
            if (data == null || data.isBlank()) return new ItemStack[size];
            ItemStack[] stored = deserialize(data);
            // Si el tamano configurado cambio, conservamos lo que quepa en vez de perderlo.
            ItemStack[] result = new ItemStack[size];
            System.arraycopy(stored, 0, result, 0, Math.min(stored.length, size));
            return result;
        }
    }

    public synchronized void save(UUID player, String modality, int vault, ItemStack[] contents) throws SQLException {
        try (PreparedStatement upsert = connection.prepareStatement("""
                INSERT INTO modality_vaults(player_uuid,modality,vault_number,items_data,updated_at)
                VALUES(?,?,?,?,?)
                ON CONFLICT(player_uuid,modality,vault_number)
                DO UPDATE SET items_data=excluded.items_data, updated_at=excluded.updated_at""")) {
            upsert.setString(1, player.toString());
            upsert.setString(2, modality);
            upsert.setInt(3, vault);
            upsert.setString(4, serialize(contents));
            upsert.setString(5, Instant.now().toString());
            upsert.executeUpdate();
        }
    }

    /** Numeros de boveda que el jugador ya usa en esa modalidad. */
    public synchronized java.util.List<Integer> used(UUID player, String modality) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT vault_number FROM modality_vaults WHERE player_uuid=? AND modality=? ORDER BY vault_number")) {
            query.setString(1, player.toString());
            query.setString(2, modality);
            ResultSet rows = query.executeQuery();
            java.util.List<Integer> result = new java.util.ArrayList<>();
            while (rows.next()) result.add(rows.getInt("vault_number"));
            return result;
        }
    }

    static String serialize(ItemStack[] contents) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeInt(contents.length);
            for (ItemStack item : contents) output.writeObject(item);
            output.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo serializar la boveda", error);
        }
    }

    static ItemStack[] deserialize(String data) {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream input = new BukkitObjectInputStream(bytes)) {
            ItemStack[] contents = new ItemStack[input.readInt()];
            for (int index = 0; index < contents.length; index++) contents[index] = (ItemStack) input.readObject();
            return contents;
        } catch (IOException | ClassNotFoundException error) {
            throw new IllegalStateException("No se pudo leer la boveda", error);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}
