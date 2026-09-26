package tripticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TripTicketDAO {

    // Holds the last error message so the UI can show the REAL reason for failure
    private String lastError;

    public String getLastError() { return lastError; }

    // INSERT
    public boolean addTicket(TripTicket t) {
        lastError = null;
        String sql = """
            INSERT INTO trip_tickets
            (ticket_no, driver_name, vehicle_no, vehicle_type, plate_number, department,
             destination, purpose, departure_date, return_date, start_odometer, end_odometer,
             requested_by, approved_by, status, remarks)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTicketNo());
            ps.setString(2, t.getDriverName());
            ps.setString(3, t.getVehicleNo());
            ps.setString(4, t.getVehicleType());
            ps.setString(5, t.getPlateNumber());
            ps.setString(6, t.getDepartment());
            ps.setString(7, t.getDestination());
            ps.setString(8, t.getPurpose());
            ps.setString(9, t.getDepartureDate());
            ps.setString(10, t.getReturnDate());
            ps.setDouble(11, t.getStartOdometer());
            ps.setDouble(12, t.getEndOdometer());
            ps.setString(13, t.getRequestedBy());
            ps.setString(14, t.getApprovedBy());
            ps.setString(15, t.getStatus());
            ps.setString(16, t.getRemarks());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            lastError = e.getMessage();
            System.err.println("Insert error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // UPDATE
    public boolean updateTicket(TripTicket t) {
        lastError = null;
        String sql = """
            UPDATE trip_tickets SET
            driver_name=?, vehicle_no=?, vehicle_type=?, plate_number=?, department=?,
            destination=?, purpose=?, departure_date=?, return_date=?,
            start_odometer=?, end_odometer=?, requested_by=?, approved_by=?,
            status=?, remarks=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getDriverName());
            ps.setString(2, t.getVehicleNo());
            ps.setString(3, t.getVehicleType());
            ps.setString(4, t.getPlateNumber());
            ps.setString(5, t.getDepartment());
            ps.setString(6, t.getDestination());
            ps.setString(7, t.getPurpose());
            ps.setString(8, t.getDepartureDate());
            ps.setString(9, t.getReturnDate());
            ps.setDouble(10, t.getStartOdometer());
            ps.setDouble(11, t.getEndOdometer());
            ps.setString(12, t.getRequestedBy());
            ps.setString(13, t.getApprovedBy());
            ps.setString(14, t.getStatus());
            ps.setString(15, t.getRemarks());
            ps.setInt(16, t.getId());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            lastError = e.getMessage();
            System.err.println("Update error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // DELETE
    public boolean deleteTicket(int id) {
        lastError = null;
        String sql = "DELETE FROM trip_tickets WHERE id=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            lastError = e.getMessage();
            System.err.println("Delete error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // CHECK IF TICKET NO ALREADY EXISTS (used for friendly validation before insert)
    public boolean ticketNoExists(String ticketNo) {
        String sql = "SELECT 1 FROM trip_tickets WHERE ticket_no=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketNo);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Check exists error: " + e.getMessage());
            return false;
        }
    }

    // GET ALL
    public List<TripTicket> getAllTickets() {
        List<TripTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM trip_tickets ORDER BY id DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Fetch error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // SEARCH (general keyword search across the fields a user is likely to
    // look a ticket up by)
    // Searches every meaningful column, not just a handful -- so a keyword
    // typed into the general search box can match on any field (vehicle,
    // dates, odometer readings, purpose, remarks, etc.), not only the
    // original 7 columns. Numeric columns are cast to TEXT so LIKE works
    // on them too (e.g. typing part of an odometer reading).
    private static final String[] SEARCHABLE_COLUMNS = {
        "ticket_no", "driver_name", "vehicle_no", "vehicle_type", "plate_number",
        "department", "destination", "purpose", "departure_date", "return_date",
        "CAST(start_odometer AS TEXT)", "CAST(end_odometer AS TEXT)",
        "requested_by", "approved_by", "status", "remarks"
    };

    public List<TripTicket> searchTickets(String keyword) {
        List<TripTicket> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM trip_tickets WHERE ");
        for (int i = 0; i < SEARCHABLE_COLUMNS.length; i++) {
            if (i > 0) sql.append(" OR ");
            sql.append(SEARCHABLE_COLUMNS[i]).append(" LIKE ?");
        }
        sql.append(" ORDER BY id DESC");
        String kw = "%" + keyword + "%";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 1; i <= SEARCHABLE_COLUMNS.length; i++) ps.setString(i, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // Narrows the search to one specific field, so a ticket can be pinned
    // down precisely (e.g. searching Ticket No only, instead of a keyword
    // also matching an unrelated driver name or remark). "field" must be
    // one of the labels in TicketListPanel's scope dropdown -- mapped to a
    // real column here through a fixed whitelist so nothing outside
    // SEARCHABLE_COLUMNS can ever reach the query.
    public List<TripTicket> searchTicketsByField(String field, String keyword) {
        String column = switch (field) {
            case "Ticket No"       -> "ticket_no";
            case "Driver"          -> "driver_name";
            case "Vehicle No"      -> "vehicle_no";
            case "Vehicle Type"    -> "vehicle_type";
            case "Plate No"        -> "plate_number";
            case "Department"     -> "department";
            case "Destination"     -> "destination";
            case "Purpose"          -> "purpose";
            case "Departure Date"  -> "departure_date";
            case "Return Date"     -> "return_date";
            case "Start Odometer"  -> "CAST(start_odometer AS TEXT)";
            case "End Odometer"    -> "CAST(end_odometer AS TEXT)";
            case "Requested By"    -> "requested_by";
            case "Approved By"     -> "approved_by";
            case "Status"          -> "status";
            case "Remarks"         -> "remarks";
            default -> null;
        };
        if (column == null) return searchTickets(keyword);

        List<TripTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM trip_tickets WHERE " + column + " LIKE ? ORDER BY id DESC";
        String kw = "%" + keyword + "%";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // FILTER BY EXACT STATUS (case-insensitive). Report & Summary's status
    // filter used to reuse the general keyword search above, which meant
    // filtering by "Completed" could also match a driver name or destination
    // that happened to contain the word "Completed" -- a real correctness
    // loophole. This filters the status column only.
    public List<TripTicket> getTicketsByStatus(String status) {
        List<TripTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM trip_tickets WHERE status = ? COLLATE NOCASE ORDER BY id DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Filter by status error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // GET BY ID
    public TripTicket getTicketById(int id) {
        String sql = "SELECT * FROM trip_tickets WHERE id=?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Get by ID error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // GET BY TICKET NO. (used by Excel import to tell new rows from ones
    // that were already restored)
    public TripTicket getTicketByTicketNo(String ticketNo) {
        String sql = "SELECT * FROM trip_tickets WHERE ticket_no = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Get by ticket no error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // COUNT BY STATUS (for report/dashboard)
    // COLLATE NOCASE so "Cancelled", "CANCELLED", "cancelled", etc. all count
    // together -- the Status dropdown lets users type free-form values via
    // "+", so casing can vary between entries.
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM trip_tickets WHERE status = ? COLLATE NOCASE";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Count error: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // BULK IMPORT (used by Excel Import). Runs the whole batch on one
    // connection with autocommit off, instead of the one-connection-per-row
    // approach a naive loop of addTicket() would do -- much faster for a
    // large restore, and atomic: if something goes wrong partway through,
    // nothing already committed is left half-applied.
    // Per-row insert failures are swallowed and counted, NOT thrown, so one
    // bad row doesn't lose the rest of a large import.
    // Returns {inserted, skippedDuplicates, rowErrors}.
    public int[] bulkImport(List<TripTicket> tickets) {
        int inserted = 0, skipped = 0, errors = 0;

        String checkSql = "SELECT 1 FROM trip_tickets WHERE ticket_no = ?";
        String insertSql = """
            INSERT INTO trip_tickets
            (ticket_no, driver_name, vehicle_no, vehicle_type, plate_number, department,
             destination, purpose, departure_date, return_date, start_odometer, end_odometer,
             requested_by, approved_by, status, remarks)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql);
                 PreparedStatement insertPs = conn.prepareStatement(insertSql)) {

                for (TripTicket t : tickets) {
                    checkPs.setString(1, t.getTicketNo());
                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (rs.next()) { skipped++; continue; }
                    }
                    try {
                        insertPs.setString(1, t.getTicketNo());
                        insertPs.setString(2, t.getDriverName());
                        insertPs.setString(3, t.getVehicleNo());
                        insertPs.setString(4, t.getVehicleType());
                        insertPs.setString(5, t.getPlateNumber());
                        insertPs.setString(6, t.getDepartment());
                        insertPs.setString(7, t.getDestination());
                        insertPs.setString(8, t.getPurpose());
                        insertPs.setString(9, t.getDepartureDate());
                        insertPs.setString(10, t.getReturnDate());
                        insertPs.setDouble(11, t.getStartOdometer());
                        insertPs.setDouble(12, t.getEndOdometer());
                        insertPs.setString(13, t.getRequestedBy());
                        insertPs.setString(14, t.getApprovedBy());
                        insertPs.setString(15, t.getStatus());
                        insertPs.setString(16, t.getRemarks());
                        insertPs.executeUpdate();
                        inserted++;
                    } catch (SQLException rowEx) {
                        // Bad row (e.g. a stray duplicate ticket_no that slipped
                        // past the check above) -- skip it, keep the batch going.
                        errors++;
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("Bulk import error: " + e.getMessage());
            e.printStackTrace();
        }

        return new int[]{inserted, skipped, errors};
    }

    private TripTicket mapRow(ResultSet rs) throws SQLException {
        TripTicket t = new TripTicket();
        t.setId(rs.getInt("id"));
        t.setTicketNo(rs.getString("ticket_no"));
        t.setDriverName(rs.getString("driver_name"));
        t.setVehicleNo(rs.getString("vehicle_no"));
        t.setVehicleType(rs.getString("vehicle_type"));
        t.setPlateNumber(rs.getString("plate_number"));
        t.setDepartment(rs.getString("department"));
        t.setDestination(rs.getString("destination"));
        t.setPurpose(rs.getString("purpose"));
        t.setDepartureDate(rs.getString("departure_date"));
        t.setReturnDate(rs.getString("return_date"));
        t.setStartOdometer(rs.getDouble("start_odometer"));
        t.setEndOdometer(rs.getDouble("end_odometer"));
        t.setRequestedBy(rs.getString("requested_by"));
        t.setApprovedBy(rs.getString("approved_by"));
        t.setStatus(rs.getString("status"));
        t.setRemarks(rs.getString("remarks"));
        t.setCreatedAt(rs.getString("created_at"));
        return t;
    }
}
