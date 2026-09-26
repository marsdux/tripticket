package tripticket;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.print.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ReportPanel extends JPanel {

    private final MainFrame frame;
    private final TripTicketDAO dao = new TripTicketDAO();

    private JLabel lbTotal, lbCompleted, lbCancelled;
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cbFilter;
    private JButton btnImport;

    // The full ticket objects behind whatever is currently filtered/shown.
    // The visible table only has the "report" columns (no Purpose/Approved
    // By/Remarks), but export needs the complete record, so we keep this
    // alongside the table model instead of reading fields back out of it.
    private List<TripTicket> currentList = new ArrayList<>();

    // Export/Import column headers, in order. Keeping these as one shared
    // list means a file this app exports will always import back cleanly.
    private static final String[] EXPORT_COLS = {
        "Ticket No", "Driver", "Vehicle No", "Vehicle Type", "Plate No", "Department",
        "Destination", "Purpose", "Departure", "Return", "Start Odo", "End Odo", "Distance",
        "Requested By", "Approved By", "Status", "Remarks"
    };

    public ReportPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(MainFrame.MAIN_BG);
        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        build();
    }

    private void build() {
        // Header
        JLabel header = new JLabel("Reports & Summary");
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setForeground(new Color(20, 40, 80));
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel main = new JPanel(new BorderLayout(0, 15));
        main.setBackground(MainFrame.MAIN_BG);

        // Summary cards -- Pending/Approved/On Trip removed, not used in this workflow
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setBackground(MainFrame.MAIN_BG);

        lbTotal     = statLabel("0");
        lbCompleted = statLabel("0");
        lbCancelled = statLabel("0");

        statsRow.add(statCard("Total",     lbTotal,     new Color(52, 120, 210)));
        statsRow.add(statCard("Completed", lbCompleted, new Color(100, 100, 120)));
        statsRow.add(statCard("Cancelled", lbCancelled, new Color(192, 57, 43)));

        main.add(statsRow, BorderLayout.NORTH);

        // Filter + print + export/import
        // Split into two rows on purpose: a single FlowLayout row needs
        // ~940px to fit all six controls, but the app's minimum window size
        // (1000px, minus the 220px sidebar and 60px of padding) only
        // guarantees 720px of content width. Below that, FlowLayout would
        // wrap and the Import button could end up squeezed out of view --
        // this happened. Two dedicated rows always fit, regardless of
        // window size.
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterRow.setBackground(MainFrame.MAIN_BG);
        JLabel lblFilter = new JLabel("Filter by Status:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cbFilter = new JComboBox<>(new String[]{"All","Completed","Cancelled"});
        cbFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbFilter.setPreferredSize(new Dimension(160, 33));
        JButton btnFilter = btn("Apply Filter", new Color(52, 120, 210));
        btnFilter.addActionListener(e -> applyFilter());
        filterRow.add(lblFilter);
        filterRow.add(cbFilter);
        filterRow.add(btnFilter);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actionsRow.setBackground(MainFrame.MAIN_BG);
        JButton btnPrint  = btn("🖨 Print Report", new Color(15, 52, 96));
        JButton btnExcel  = btn("📊 Export to Excel", new Color(39, 174, 96));
        btnImport = btn("📥 Import from Excel", new Color(52, 152, 219));
        btnPrint.addActionListener(e -> printReport());
        btnExcel.addActionListener(e -> exportToExcel());
        btnImport.addActionListener(e -> importFromExcel());
        actionsRow.add(btnPrint);
        actionsRow.add(btnExcel);
        actionsRow.add(btnImport);

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setBackground(MainFrame.MAIN_BG);
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.add(filterRow);
        toolbar.add(actionsRow);

        // Table (on-screen view -- kept intentionally lean; the export has
        // the extra columns that don't fit comfortably on screen)
        String[] cols = {"Ticket No","Driver","Vehicle","Vehicle Type","Plate No","Department",
            "Destination","Departure","Return","Start Odo","End Odo","Distance","Requested By","Status"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable();
        table.getColumn("Status").setCellRenderer(new TicketListPanel.StatusRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 215, 235)));

        JPanel tableSection = new JPanel(new BorderLayout(0, 5));
        tableSection.setBackground(MainFrame.MAIN_BG);
        tableSection.add(toolbar, BorderLayout.NORTH);
        tableSection.add(scroll, BorderLayout.CENTER);

        main.add(tableSection, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    public void refresh() {
        lbTotal.setText(String.valueOf(dao.getAllTickets().size()));
        lbCompleted.setText(String.valueOf(dao.countByStatus("Completed")));
        lbCancelled.setText(String.valueOf(dao.countByStatus("Cancelled")));
        cbFilter.setSelectedIndex(0);
        applyFilter();
    }

    private void applyFilter() {
        String selected = (String) cbFilter.getSelectedItem();
        // getTicketsByStatus does an exact (case-insensitive) match on the
        // status column only -- previously this reused the general keyword
        // search, which could also match a driver name or destination that
        // happened to contain the filter word.
        currentList = "All".equals(selected) ? dao.getAllTickets() : dao.getTicketsByStatus(selected);

        model.setRowCount(0);
        for (TripTicket t : currentList) {
            model.addRow(new Object[]{
                t.getTicketNo(), t.getDriverName(), t.getVehicleNo(),
                t.getVehicleType(), t.getPlateNumber(), t.getDepartment(),
                t.getDestination(), t.getDepartureDate(), t.getReturnDate(),
                t.getStartOdometer(), t.getEndOdometer(),
                String.format("%.2f", t.getDistanceTraveled()),
                t.getRequestedBy(), t.getStatus()
            });
        }
    }

    private void printReport() {
        try {
            String filter = (String) cbFilter.getSelectedItem();
            MessageFormat header = new MessageFormat("TRIP TICKET REPORT — " + filter + " — Page {0,number}");
            MessageFormat footer = new MessageFormat("Generated from Trip Ticket Management System");
            table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(),
                "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Export ─────────────────────────────────────────────────────────────────
    // Exports the full record for every ticket in the current filter (not
    // just the columns visible on screen) so this file can fully restore
    // the data later via Import -- Purpose, Approved By and Remarks are
    // included even though they're not shown in the on-screen table.
    private void exportToExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Report As");
        chooser.setSelectedFile(new File("TripTicketReport.xlsx"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Trip Ticket Report");

            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < EXPORT_COLS.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(EXPORT_COLS[c]);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (TripTicket t : currentList) {
                Row row = sheet.createRow(r++);
                int c = 0;
                setCell(row, c++, t.getTicketNo());
                setCell(row, c++, t.getDriverName());
                setCell(row, c++, t.getVehicleNo());
                setCell(row, c++, t.getVehicleType());
                setCell(row, c++, t.getPlateNumber());
                setCell(row, c++, t.getDepartment());
                setCell(row, c++, t.getDestination());
                setCell(row, c++, t.getPurpose());
                setCell(row, c++, t.getDepartureDate());
                setCell(row, c++, t.getReturnDate());
                setCell(row, c++, t.getStartOdometer());
                setCell(row, c++, t.getEndOdometer());
                setCell(row, c++, t.getDistanceTraveled());
                setCell(row, c++, t.getRequestedBy());
                setCell(row, c++, t.getApprovedBy());
                setCell(row, c++, t.getStatus());
                setCell(row, c,   t.getRemarks());
            }

            for (int c = 0; c < EXPORT_COLS.length; c++) {
                sheet.autoSizeColumn(c);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }

            File savedFile = file;
            int open = JOptionPane.showConfirmDialog(this,
                "Report exported to:\n" + savedFile.getAbsolutePath() +
                "\n\nThis file includes every field, so it can be used later with " +
                "\"Import from Excel\" to restore this data.\n\nOpen it now?",
                "Export Successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (open == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(savedFile);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Export failed: " + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setCell(Row row, int idx, String val) {
        row.createCell(idx).setCellValue(val == null ? "" : val);
    }

    private void setCell(Row row, int idx, double val) {
        row.createCell(idx).setCellValue(val);
    }

    // ─── Import ─────────────────────────────────────────────────────────────────
    // Restores trip tickets from a previously exported (or compatible) .xlsx
    // file. Matches columns by header name (case-insensitive), so column
    // order or a few missing columns won't break it. Tickets whose Ticket
    // No. already exists in the database are skipped, not overwritten, so
    // running an import twice -- or importing a partially overlapping file
    // -- can't clobber existing data.
    private void importFromExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Excel File to Import");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        int confirm = JOptionPane.showConfirmDialog(this,
            "Import trip tickets from:\n" + file.getAbsolutePath() +
            "\n\nTickets whose Ticket No. already exists will be skipped (never overwritten).\n\nContinue?",
            "Confirm Import", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        btnImport.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Runs on a background thread -- importing 100+ rows means 100+
        // sequential DB round-trips, which would otherwise freeze the UI
        // (including the whole app, since Swing does everything on the
        // single Event Dispatch Thread).
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() throws Exception {
                return doImport(file);
            }

            @Override
            protected void done() {
                btnImport.setEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    int[] r = get();
                    StringBuilder msg = new StringBuilder("Import complete.\n\n")
                        .append("Imported: ").append(r[0]).append('\n')
                        .append("Skipped (already exist): ").append(r[1]);
                    if (r[2] > 0) msg.append("\nRows with errors (skipped): ").append(r[2]);
                    JOptionPane.showMessageDialog(ReportPanel.this, msg.toString(),
                        "Import Finished", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ReportPanel.this,
                        "Import failed: " + ex.getMessage(),
                        "Import Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Runs off the EDT -- must not touch any Swing components.
    // Returns {imported, skipped, errors}.
    private int[] doImport(File file) throws Exception {
        List<TripTicket> parsed = new ArrayList<>();
        int parseErrors = 0;

        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("The selected file has no header row.");
            }

            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell c : headerRow) {
                String h = fmt.formatCellValue(c).trim().toLowerCase();
                if (!h.isEmpty()) colIndex.put(h, c.getColumnIndex());
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String ticketNo = getCell(row, colIndex, fmt, "ticket no");
                if (ticketNo == null || ticketNo.isBlank()) continue; // skip blank rows

                try {
                    TripTicket t = new TripTicket();
                    t.setTicketNo(ticketNo);
                    t.setDriverName(orDefault(getCell(row, colIndex, fmt, "driver"), "Unknown"));
                    String vehicleNo = getCell(row, colIndex, fmt, "vehicle no");
                    String plate = getCell(row, colIndex, fmt, "plate no");
                    t.setVehicleNo(orDefault(vehicleNo != null ? vehicleNo : plate, ""));
                    t.setVehicleType(orDefault(getCell(row, colIndex, fmt, "vehicle type"), ""));
                    t.setPlateNumber(orDefault(plate, ""));
                    t.setDepartment(orDefault(getCell(row, colIndex, fmt, "department"), ""));
                    t.setDestination(orDefault(getCell(row, colIndex, fmt, "destination"), "N/A"));
                    t.setPurpose(orDefault(getCell(row, colIndex, fmt, "purpose"), "Imported record"));

                    String departure = getCell(row, colIndex, fmt, "departure");
                    String ret = getCell(row, colIndex, fmt, "return");
                    t.setDepartureDate(orDefault(departure, orDefault(ret, "")));
                    t.setReturnDate(orDefault(ret, orDefault(departure, "")));

                    t.setStartOdometer(parseDoubleSafe(getCell(row, colIndex, fmt, "start odo")));
                    t.setEndOdometer(parseDoubleSafe(getCell(row, colIndex, fmt, "end odo")));
                    t.setRequestedBy(orDefault(getCell(row, colIndex, fmt, "requested by"), "Unknown"));
                    t.setApprovedBy(orDefault(getCell(row, colIndex, fmt, "approved by"), ""));
                    t.setStatus(orDefault(getCell(row, colIndex, fmt, "status"), ""));
                    t.setRemarks(orDefault(getCell(row, colIndex, fmt, "remarks"), ""));

                    parsed.add(t);
                } catch (Exception rowEx) {
                    parseErrors++;
                }
            }
        }

        // One transaction for the whole batch -- fast, and atomic against
        // any mid-batch failure -- instead of a connection+commit per row.
        int[] bulk = dao.bulkImport(parsed);
        return new int[]{bulk[0], bulk[1], bulk[2] + parseErrors};
    }

    private String getCell(Row row, Map<String, Integer> colIndex, DataFormatter fmt, String key) {
        Integer idx = colIndex.get(key);
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        String v = fmt.formatCellValue(c).trim();
        return v.isEmpty() ? null : v;
    }

    private String orDefault(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }

    private double parseDoubleSafe(String s) {
        if (s == null) return 0.0;
        try {
            return Double.parseDouble(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void styleTable() {
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setSelectionBackground(new Color(180, 210, 255));
        table.setGridColor(new Color(220, 228, 240));
        table.setShowGrid(true);
        JTableHeader th = table.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(new Color(15, 52, 96));
        th.setForeground(Color.BLACK);
        th.setPreferredSize(new Dimension(0, 38));
    }

    private JPanel statCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(color);
        card.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        valueLabel.setForeground(Color.BLACK);
        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(Color.BLACK);
        card.add(valueLabel);
        card.add(lbl);
        return card;
    }

    private JLabel statLabel(String val) {
        JLabel lbl = new JLabel(val, SwingConstants.CENTER);
        return lbl;
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(145, 33));
        return b;
    }
}
