package com.smarttask.database;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Aim;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class AimDao {

    private final DataSource dataSource;

    public AimDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DynamicArray<Aim> findByUserId(Long userId) {
        String sql = "SELECT aim_id, user_id, title, description, deadline, created_at FROM aims WHERE user_id = ? ORDER BY created_at DESC";
        DynamicArray<Aim> aims = new DynamicArray<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    aims.add(map(rs));
                }
            }
            return aims;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load aims.", ex);
        }
    }

    public Optional<Aim> findByIdAndUserId(Long aimId, Long userId) {
        String sql = "SELECT aim_id, user_id, title, description, deadline, created_at FROM aims WHERE aim_id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, aimId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find aim.", ex);
        }
    }

    public Aim save(Aim aim) {
        if (aim.getId() == null) {
            return insert(aim);
        }
        return update(aim);
    }

    public void delete(Long aimId, Long userId) {
        String sql = "DELETE FROM aims WHERE aim_id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, aimId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete aim.", ex);
        }
    }

    private Aim insert(Aim aim) {
        String sql = "INSERT INTO aims (user_id, title, description, deadline, created_at) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime createdAt = aim.getCreatedAt() == null ? LocalDateTime.now() : aim.getCreatedAt();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, aim.getUserId());
            statement.setString(2, aim.getTitle());
            statement.setString(3, aim.getDescription());
            if (aim.getDeadline() == null) {
                statement.setNull(4, Types.DATE);
            } else {
                statement.setDate(4, Date.valueOf(aim.getDeadline()));
            }
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    aim.setId(keys.getLong(1));
                }
            }
            aim.setCreatedAt(createdAt);
            return aim;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to insert aim.", ex);
        }
    }

    private Aim update(Aim aim) {
        String sql = "UPDATE aims SET title = ?, description = ?, deadline = ? WHERE aim_id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, aim.getTitle());
            statement.setString(2, aim.getDescription());
            if (aim.getDeadline() == null) {
                statement.setNull(3, Types.DATE);
            } else {
                statement.setDate(3, Date.valueOf(aim.getDeadline()));
            }
            statement.setLong(4, aim.getId());
            statement.setLong(5, aim.getUserId());
            statement.executeUpdate();
            return aim;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update aim.", ex);
        }
    }

    private Aim map(ResultSet rs) throws SQLException {
        Aim aim = new Aim();
        aim.setId(rs.getLong("aim_id"));
        aim.setUserId(rs.getLong("user_id"));
        aim.setTitle(rs.getString("title"));
        aim.setDescription(rs.getString("description"));
        Date deadline = rs.getDate("deadline");
        aim.setDeadline(deadline == null ? null : deadline.toLocalDate());
        Timestamp created = rs.getTimestamp("created_at");
        aim.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return aim;
    }
}
