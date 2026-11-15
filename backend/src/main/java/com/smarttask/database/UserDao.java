package com.smarttask.database;

import com.smarttask.model.User;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserDao {

    private final DataSource dataSource;

    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to check email.", ex);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT user_id, name, email, password, created_at FROM users WHERE email = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapUser(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find user by email.", ex);
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT user_id, name, email, password, created_at FROM users WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapUser(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find user by id.", ex);
        }
    }

    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        }
        return update(user);
    }

    private User insert(User user) {
        String sql = "INSERT INTO users (name, email, password, created_at) VALUES (?, ?, ?, ?)";
        LocalDateTime createdAt = user.getCreatedAt() == null ? LocalDateTime.now() : user.getCreatedAt();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setTimestamp(4, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            user.setCreatedAt(createdAt);
            return user;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to insert user.", ex);
        }
    }

    private User update(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, password = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setLong(4, user.getId());
            statement.executeUpdate();
            return user;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update user.", ex);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        Timestamp created = rs.getTimestamp("created_at");
        user.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return user;
    }
}
