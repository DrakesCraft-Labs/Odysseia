package org.metamechanists.odysseia.cheques;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

/** Libro contable SQLite que impide canjear dos copias del mismo cheque. */
public final class ChequeRepository implements AutoCloseable {

    private final Connection connection;

    public ChequeRepository(File database) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS cheque_redemptions(
                      serial TEXT PRIMARY KEY,
                      amount INTEGER NOT NULL,
                      player_uuid TEXT NOT NULL,
                      state TEXT NOT NULL,
                      created_at TEXT NOT NULL,
                      redeemed_at TEXT)""");
        }
    }

    /** Reserva el serial de forma atómica. False significa que ya fue usado o está en proceso. */
    public synchronized boolean reserve(String serial, long amount, UUID player) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT OR IGNORE INTO cheque_redemptions(serial,amount,player_uuid,state,created_at)
                VALUES(?,?,?,'PENDING',?)""")) {
            insert.setString(1, serial);
            insert.setLong(2, amount);
            insert.setString(3, player.toString());
            insert.setString(4, Instant.now().toString());
            return insert.executeUpdate() == 1;
        }
    }

    public synchronized void complete(String serial) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE cheque_redemptions SET state='REDEEMED', redeemed_at=? WHERE serial=? AND state='PENDING'")) {
            update.setString(1, Instant.now().toString());
            update.setString(2, serial);
            if (update.executeUpdate() != 1) throw new SQLException("No se pudo confirmar el serial " + serial);
        }
    }

    public synchronized void release(String serial) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM cheque_redemptions WHERE serial=? AND state='PENDING'")) {
            delete.setString(1, serial);
            delete.executeUpdate();
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}
