package tripticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    public List<String> getAllDepartments() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT name FROM departments ORDER BY name ASC";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) {
            System.err.println("Fetch departments error: " + e.getMessage());
        }
        return list;
    }

    public boolean departmentExists(String name) {
        String sql = "SELECT 1 FROM departments WHERE name = ? COLLATE NOCASE";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Check department error: " + e.getMessage());
            return false;
        }
    }

    public boolean addDepartment(String name) {
        if (name == null || name.isBlank() || departmentExists(name.trim())) return false;
        String sql = "INSERT INTO departments (name) VALUES (?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Add department error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteDepartment(String name) {
        String sql = "DELETE FROM departments WHERE name = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Delete department error: " + e.getMessage());
            return false;
        }
    }
}
