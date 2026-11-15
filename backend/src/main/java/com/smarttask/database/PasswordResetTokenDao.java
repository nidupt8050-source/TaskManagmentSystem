package com.smarttask.database;

import com.smarttask.model.PasswordResetToken;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class PasswordResetTokenDao {

    private final DataSource dataSource;

    public PasswordResetTokenDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<PasswordResetToken> findByHash(String tokenHash) {
        String sql = "SELECT token_hash, user_id, expires_at FROM password_reset_tokens WHERE token_hash = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find reset token.", ex);
        }
    }

    public Optional<PasswordResetToken> findByUserId(Long userId) {
        String sql = "SELECT token_hash, user_id, expires_at FROM password_reset_tokens WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find reset token by user.", ex);
        }
    }

    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM password_reset_tokens WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete reset tokens.", ex);
        }
    }

    public void deleteByHash(String tokenHash) {
        String sql = "DELETE FROM password_reset_tokens WHERE token_hash = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete reset token.", ex);
        }
    }

    public void save(PasswordResetToken token) {
        deleteByUserId(token.getUserId());
        String sql = "INSERT INTO password_reset_tokens (token_hash, user_id, expires_at) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token.getTokenHash());
            statement.setLong(2, token.getUserId());
            statement.setTimestamp(3, Timestamp.from(token.getExpiresAt()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save reset token.", ex);
        }
    }

    private PasswordResetToken map(ResultSet rs) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setTokenHash(rs.getString("token_hash"));
        token.setUserId(rs.getLong("user_id"));
        Timestamp expires = rs.getTimestamp("expires_at");
        token.setExpiresAt(expires == null ? null : expires.toInstant());
        return token;
    }
}
