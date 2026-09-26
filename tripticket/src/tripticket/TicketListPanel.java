package tripticket;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.text.MessageFormat;
import java.util.List;

public class TicketListPanel extends JPanel {

    private final MainFrame frame;
    private final TripTicketDAO dao = new TripTicketDAO();

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private JComboBox<String> cbScope;
    private JLabel countLabel;
    private List<TripTicket> currentList;
    private Timer searchDebounce;

    private static final String[] COLS = {
        "ID", "Ticket No", "Driver", "Vehicle", "Vehicle Type", "Plate No", "Department",
        "Destination", "Departure", "Return", "Start Odo", "End Odo", "Distance",
        "Requested By", "Status"
    };

    // Column indices that hold numeric values -- kept as real Double/Integer
    // objects in the table model (not pre-formatted strings) so clicking a
    // column header sorts numerically instead of alphabetically.
    private static final int COL_ID = 0, COL_START_ODO = 10, COL_END_ODO = 11, COL_DISTANCE = 12;

    // Search scope options shown in the dropdown -- "All Fields" triggers a
    // search across every column; the rest narrow to exactly one column so
    // a single ticket can be pinned down without unrelated matches.
    private static final String[] SEARCH_SCOPES = {
        "All Fields", "Ticket No", "Driver", "Vehicle No", "Vehicle Type", "Plate No",
        "Department", "Destination", "Purpose", "Departure Date", "Return Date",
        "Start Odometer", "End Odometer", "Requested By", "Approved By", "Status", "Remarks"
    };

    public TicketListPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(MainFrame.MAIN_BG);
        setLayout(new BorderLayout(0, 15));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        build();
    }

    private void build() {
        // ---- Title row: header on the left, live result count on the right ----
        JLabel header = new JLabel("All Trip Tickets");
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setForeground(new Color(20, 40, 80));

        countLabel = new JLabel("");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        countLabel.setForeground(new Color(100, 110, 130));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(MainFrame.MAIN_BG);
        titleRow.add(header, BorderLayout.WEST);
        titleRow.add(countLabel, BorderLayout.EAST);

        // ---- Search row: field + scope + search/clear, on its own line ----
        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(240, 32));
        searchField.putClientProperty("JTextField.placeholderText", "Type to search...");

        cbScope = new JComboBox<>(SEARCH_SCOPES);
        cbScope.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbScope.setPreferredSize(new Dimension(150, 32));
        cbScope.setToolTipText("Narrow the search to one field, or search all fields");

        JButton btnSearch = flatBtn("🔍 Search", new Color(52, 120, 210));
        JButton btnClear  = flatBtn("✕ Clear", new Color(140, 148, 160));

        btnSearch.addActionListener(e -> doSearch());
        btnClear.addActionListener(e -> clearSearch());
        cbScope.addActionListener(e -> doSearch());

        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { doSearch(); return; }
                // Live-filter as the user types, so even a single matching
                // ticket surfaces immediately without needing Enter/Search.
                if (searchDebounce != null) searchDebounce.stop();
                searchDebounce = new Timer(250, ev -> doSearch());
                searchDebounce.setRepeats(false);
                searchDebounce.start();
            }
        });

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchRow.setBackground(MainFrame.MAIN_BG);
        searchRow.add(lblSearch);
        searchRow.add(searchField);
        searchRow.add(cbScope);
        searchRow.add(btnSearch);
        searchRow.add(btnClear);

        // ---- Actions row: record actions, kept on their own line so they
        // never compete with the search controls for space and never get
        // clipped/hidden at the app's minimum window width. ----
        JButton btnNew    = flatBtn("➕ New", MainFrame.ACCENT);
        JButton btnEdit   = flatBtn("✏️ Edit", new Color(39, 174, 96));
        JButton btnDelete = flatBtn("🗑 Delete", new Color(192, 57, 43));
        JButton btnPrint  = flatBtn("🖨 Print", new Color(100, 100, 120));
        JButton btnRefresh= flatBtn("↺ Refresh", new Color(80, 100, 130));

        btnNew.addActionListener(e -> frame.openNewForm());
        btnEdit.addActionListener(e -> openEdit());
        btnDelete.addActionListener(e -> deleteSelected());
        btnPrint.addActionListener(e -> printTable());
        btnRefresh.addActionListener(e -> clearSearch());

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        actionsRow.setBackground(MainFrame.MAIN_BG);
        actionsRow.add(btnNew);
        actionsRow.add(btnEdit);
        actionsRow.add(btnDelete);
        actionsRow.add(new JSeparator(SwingConstants.VERTICAL));
        actionsRow.add(btnPrint);
        actionsRow.add(btnRefresh);

        // Stack search row and actions row vertically (instead of cramming
        // search + all 5 buttons into one FlowLayout row) so nothing runs
        // out of horizontal space and silently disappears at min window size.
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setBackground(MainFrame.MAIN_BG);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.add(searchRow);
        toolbar.add(actionsRow);

        JPanel topSection = new JPanel(new BorderLayout(0, 8));
        topSection.setBackground(MainFrame.MAIN_BG);
        topSection.add(titleRow, BorderLayout.NORTH);
        topSection.add(toolbar, BorderLayout.CENTER);
        add(topSection, BorderLayout.NORTH);

        // Table
        model = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }

            // Real column types -- lets the row sorter compare Start Odo,
            // End Odo and Distance numerically instead of as text, so e.g.
            // "9" doesn't sort after "10".
            public Class<?> getColumnClass(int col) {
                if (col == COL_ID) return Integer.class;
                if (col == COL_START_ODO || col == COL_END_ODO || col == COL_DISTANCE) return Double.class;
                return String.class;
            }
        };
        table = new JTable(model);
        styleTable();

        // Click a column header to sort by it (ascending, then descending,
        // then back to insertion order) -- the quickest way to retrieve a
        // single ticket, e.g. sort by Ticket No or Departure Date to find it.
        table.setAutoCreateRowSorter(true);

        // Double-click to edit
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEdit();
            }
        });

        // Status column color renderer
        table.getColumn("Status").setCellRenderer(new StatusRenderer());

        // 2-decimal renderer for the numeric columns
        DecimalRenderer decRenderer = new DecimalRenderer();
        table.getColumnModel().getColumn(COL_START_ODO).setCellRenderer(decRenderer);
        table.getColumnModel().getColumn(COL_END_ODO).setCellRenderer(decRenderer);
        table.getColumnModel().getColumn(COL_DISTANCE).setCellRenderer(decRenderer);

        // Hide ID column
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 215, 235)));
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    public void loadData() {
        currentList = dao.getAllTickets();
        fillTable(currentList);
    }

    private void clearSearch() {
        searchField.setText("");
        cbScope.setSelectedIndex(0);
        loadData();
    }

    private void doSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { loadData(); return; }
        String scope = (String) cbScope.getSelectedItem();
        currentList = ("All Fields".equals(scope))
            ? dao.searchTickets(kw)
            : dao.searchTicketsByField(scope, kw);
        fillTable(currentList);
    }

    private void fillTable(List<TripTicket> list) {
        model.setRowCount(0);
        for (TripTicket t : list) {
            model.addRow(new Object[]{
                t.getId(), t.getTicketNo(), t.getDriverName(), t.getVehicleNo(),
                t.getVehicleType(), t.getPlateNumber(), t.getDepartment(),
                t.getDestination(), t.getDepartureDate(), t.getReturnDate(),
                t.getStartOdometer(), t.getEndOdometer(), t.getDistanceTraveled(),
                t.getRequestedBy(), t.getStatus()
            });
        }
        countLabel.setText(list.size() + (list.size() == 1 ? " ticket" : " tickets"));
    }

    private void openEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) model.getValueAt(modelRow, 0);
        TripTicket t = dao.getTicketById(id);
        if (t != null) frame.openEditForm(t);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete ticket " + model.getValueAt(modelRow, 1) + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int id = (int) model.getValueAt(modelRow, 0);
            if (dao.deleteTicket(id)) {
                doSearch();
                if (searchField.getText().trim().isEmpty()) loadData();
                JOptionPane.showMessageDialog(this, "Deleted successfully.", "Done", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void printTable() {
        try {
            MessageFormat header = new MessageFormat("Trip Ticket Report — Page {0,number}");
            MessageFormat footer = new MessageFormat("Printed from Trip Ticket Management System");
            table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(),
                "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void styleTable() {
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(180, 210, 255));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(220, 228, 240));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(8, 0));

        JTableHeader th = table.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(new Color(15, 52, 96));
        th.setForeground(Color.WHITE);
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(0, 40));
    }

    // Flat, fixed-height button used across both toolbar rows. Renamed from
    // the old iconBtn but kept deliberately simple/compact -- no fixed
    // absolute width beyond a sensible minimum, so the row's FlowLayout can
    // size buttons to their label rather than reserving oversized slots.
    private JButton flatBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(6, 12, 6, 12));
        return b;
    }

    // Status color renderer
    static class StatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
            lbl.setOpaque(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            String s = val == null ? "" : val.toString();
            switch (s) {
                case "Approved"  -> { lbl.setBackground(new Color(39, 174, 96));  lbl.setForeground(Color.WHITE); }
                case "Pending"   -> { lbl.setBackground(new Color(230, 160, 20)); lbl.setForeground(Color.WHITE); }
                case "On Trip"   -> { lbl.setBackground(new Color(52, 120, 210)); lbl.setForeground(Color.WHITE); }
                case "Completed" -> { lbl.setBackground(new Color(100, 100, 120));lbl.setForeground(Color.WHITE); }
                case "Cancelled" -> { lbl.setBackground(new Color(192, 57, 43));  lbl.setForeground(Color.WHITE); }
                default          -> { lbl.setBackground(Color.WHITE); lbl.setForeground(Color.BLACK); }
            }
            if (sel) { lbl.setBackground(new Color(70, 130, 200)); lbl.setForeground(Color.WHITE); }
            return lbl;
        }
    }

    // Formats Double values to 2 decimal places for display while leaving
    // the underlying model value numeric, so the column sorts correctly.
    static class DecimalRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            String text = (val instanceof Number n) ? String.format("%.2f", n.doubleValue()) : "";
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, text, sel, focus, row, col);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            return lbl;
        }
    }
}
