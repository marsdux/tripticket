package tripticket;

import java.sql.*;
import java.io.File;
import java.nio.file.Files;

public class DatabaseHelper {

    // Previously this was the relative path "triptickets.db", which resolves
    // against whatever directory happens to be the current working directory
    // at the moment the app is launched. That differs between running from
    // NetBeans, double-clicking the jar, or a desktop shortcut -- each one
    // could silently create (or read) a completely different, empty
    // database file. Using a fixed location under the user's home directory
    // makes the database location consistent no matter how the app starts.
    private static final String DB_DIR  = System.getProperty("user.home") + File.separator + ".tripticket";
    private static final String DB_FILE = DB_DIR + File.separator + "triptickets.db";
    private static final String DB_URL  = "jdbc:sqlite:" + DB_FILE;

    static {
        migrateLegacyDatabaseLocation();
    }

    // One-time safety net: if an old database exists at the legacy relative
    // path (from a previous run of this app) and nothing exists yet at the
    // new fixed location, copy it over so no data is lost by this fix.
    private static void migrateLegacyDatabaseLocation() {
        try {
            File dir = new File(DB_DIR);
            if (!dir.exists()) dir.mkdirs();

            File newDb = new File(DB_FILE);
            if (!newDb.exists()) {
                File legacyDb = new File("triptickets.db");
                if (legacyDb.exists() && legacyDb.isFile()) {
                    Files.copy(legacyDb.toPath(), newDb.toPath());
                    System.out.println("Migrated existing database to: " + DB_FILE);
                }
            }
        } catch (Exception e) {
            System.err.println("Database migration check failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // SQLite locks the whole file for writes. Without this, two
        // connections open at the same moment (e.g. the Excel import running
        // on a background thread while the UI queries the DB) can throw
        // "database is locked" instead of just waiting briefly.
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }

    public static void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS trip_tickets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_no TEXT NOT NULL UNIQUE,
                driver_name TEXT NOT NULL,
                vehicle_no TEXT NOT NULL,
                vehicle_type TEXT,
                plate_number TEXT,
                department TEXT,
                destination TEXT NOT NULL,
                purpose TEXT NOT NULL,
                departure_date TEXT NOT NULL,
                return_date TEXT NOT NULL,
                start_odometer REAL DEFAULT 0,
                end_odometer REAL DEFAULT 0,
                requested_by TEXT NOT NULL,
                approved_by TEXT,
                status TEXT DEFAULT 'Pending',
                remarks TEXT,
                created_at TEXT DEFAULT (datetime('now'))
            )
        """;

        String deptSql = """
            CREATE TABLE IF NOT EXISTS departments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            )
        """;

        // Generic lookup table backing the +/- managed dropdowns (Driver Name,
        // Vehicle Type, Plate Number, Status). "category" separates the lists.
        String lookupSql = """
            CREATE TABLE IF NOT EXISTS lookup_options (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                value TEXT NOT NULL,
                UNIQUE(category, value)
            )
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(deptSql);
            stmt.execute(lookupSql);
            System.out.println("Database initialized.");
        } catch (SQLException e) {
            System.err.println("DB init error: " + e.getMessage());
            e.printStackTrace();
        }

        // If the database already existed before these columns were introduced,
        // add them now. SQLite has no "ADD COLUMN IF NOT EXISTS", so we just try
        // and silently ignore the error if the column is already there.
        migrateColumn("vehicle_type", "TEXT");
        migrateColumn("plate_number", "TEXT");
        migrateColumn("department", "TEXT");
        migrateColumn("start_odometer", "REAL DEFAULT 0");
        migrateColumn("end_odometer", "REAL DEFAULT 0");

        seedDefaultDepartments();
        seedDefaultStatuses();
        removeDeprecatedStatuses();
    }

    private static void migrateColumn(String column, String type) {
        String alter = "ALTER TABLE trip_tickets ADD COLUMN " + column + " " + type;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(alter);
            System.out.println("Migrated column: " + column);
        } catch (SQLException e) {
            // Column already exists (or another benign issue) - safe to ignore
        }
    }

    private static void seedDefaultDepartments() {
        String[] defaults = {
            "Administration", "Operations", "Finance", "Human Resources", "IT Department"
        };
        String checkSql = "SELECT COUNT(*) FROM departments";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO departments (name) VALUES (?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (String d : defaults) {
                        ps.setString(1, d);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Seed departments error: " + e.getMessage());
        }
    }

    // Pre-populates the "status" lookup list so the Status dropdown isn't
    // empty on first run. Users can still add/remove values afterward.
    // Pending/Approved/On Trip are intentionally left out -- this workflow
    // only uses Completed and Cancelled.
    private static void seedDefaultStatuses() {
        String[] defaults = {"Completed", "Cancelled"};
        String checkSql = "SELECT COUNT(*) FROM lookup_options WHERE category='status'";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO lookup_options (category, value) VALUES ('status', ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (String s : defaults) {
                        ps.setString(1, s);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Seed statuses error: " + e.getMessage());
        }
    }

    // Removes "Pending" / "Approved" / "On Trip" (any casing) from the Status
    // dropdown list on every startup. This does NOT touch existing
    // trip_tickets rows that already have that status -- it only trims the
    // pick-list so new tickets can't select them anymore.
    private static void removeDeprecatedStatuses() {
        String sql = "DELETE FROM lookup_options WHERE category='status' AND LOWER(value) IN ('pending','approved','on trip')";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Cleanup deprecated statuses error: " + e.getMessage());
        }
    }
}
