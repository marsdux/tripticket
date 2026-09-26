package tripticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic lookup-list DAO. Backs every dropdown that needs a user-managed
 * "+/-" list (Driver Name, Vehicle Type, Plate Number, Status), the same way
 * DepartmentDAO backs the Department dropdown. The "category" column keeps
 * each list separate inside the single lookup_options table.
 */
public class OptionDAO {

    public List<String> getValues(String category) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT value FROM lookup_options WHERE category = ? ORDER BY value ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("value"));
        } catch (SQLException e) {
            System.err.println("Fetch options error (" + category + "): " + e.getMessage());
        }
        return list;
    }

    public boolean valueExists(String category, String value) {
        String sql = "SELECT 1 FROM lookup_options WHERE category = ? AND value = ? COLLATE NOCASE";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, value);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Check option error (" + category + "): " + e.getMessage());
            return false;
        }
    }

    public boolean addValue(String category, String value) {
        if (value == null || value.isBlank() || valueExists(category, value.trim())) return false;
        String sql = "INSERT INTO lookup_options (category, value) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, value.trim());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Add option error (" + category + "): " + e.getMessage());
            return false;
        }
    }

    public boolean deleteValue(String category, String value) {
        String sql = "DELETE FROM lookup_options WHERE category = ? AND value = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, value);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Delete option error (" + category + "): " + e.getMessage());
            return false;
        }
    }
}
