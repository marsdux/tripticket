package tripticket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class TicketFormPanel extends JPanel {

    private final MainFrame frame;
    private final TripTicketDAO dao = new TripTicketDAO();
    private final DepartmentDAO deptDao = new DepartmentDAO();
    private final OptionDAO optionDao = new OptionDAO();

    // Fields
    private JTextField tfTicketNo, tfDestination, tfDepartureDate, tfReturnDate,
                       tfStartOdometer, tfEndOdometer,
                       tfRequestedBy;
    private JTextArea  taPurpose, taRemarks;

    // +/- managed dropdowns
    private JComboBox<String> cbDriverName, cbVehicleType, cbPlateNumber, cbStatus;
    private JComboBox<String> cbDepartment;

    private JLabel lblTitle;

    private int editingId = -1;

    public TicketFormPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(MainFrame.MAIN_BG);
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        build();
    }

    private void build() {
        // Header
        lblTitle = new JLabel("New Trip Ticket");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(new Color(20, 40, 80));
        JLabel sub = new JLabel("Fill in the details below");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(Color.GRAY);

        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setBackground(MainFrame.MAIN_BG);
        headerPanel.add(lblTitle);
        headerPanel.add(sub);
        add(headerPanel, BorderLayout.NORTH);

        // Form in scrollpane
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 235)),
            BorderFactory.createEmptyBorder(25, 30, 25, 30)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        tfTicketNo      = field("");
        tfDestination   = field("");
        tfDepartureDate = field(LocalDate.now().toString());
        tfReturnDate    = field(LocalDate.now().plusDays(1).toString());
        tfStartOdometer = field("0");
        tfEndOdometer   = field("0");
        tfRequestedBy   = field("");
        taPurpose       = area();
        taRemarks       = area();

        // Enter in Remarks saves the ticket and clears the form for the next
        // entry (Shift+Enter still inserts a newline if a multi-line remark
        // is needed).
        taRemarks.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke("ENTER"), "saveAndNew");
        taRemarks.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke("shift ENTER"), "insert-break");
        taRemarks.getActionMap().put("saveAndNew", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { saveTicket(false); }
        });

        // Department dropdown -- now uses the same +/- managed pattern as
        // Driver Name, Vehicle Type, Plate Number, and Status.
        cbDepartment = new JComboBox<>();
        JPanel deptPanel = buildDepartmentField();

        // Driver Name, Vehicle Type, Plate Number, Status: +/- managed dropdowns
        cbDriverName  = new JComboBox<>();
        cbVehicleType = new JComboBox<>();
        cbPlateNumber = new JComboBox<>();
        cbStatus      = new JComboBox<>();

        JPanel driverPanel      = buildOptionField("driver", cbDriverName);
        JPanel vehicleTypePanel = buildOptionField("vehicle_type", cbVehicleType);
        JPanel platePanel       = buildOptionField("plate_number", cbPlateNumber);
        JPanel statusPanel      = buildOptionField("status", cbStatus);

        int row = 0;
        addRow(form, gc, row++, "TRIPTICKET.*",     tfTicketNo,       "Driver Name.*",    driverPanel);
        addRow(form, gc, row++, "Vehicle Type.*",   vehicleTypePanel, "Plate Number.*",   platePanel);
        addRow(form, gc, row++, "Department.*",     deptPanel,        "Status.*",         statusPanel);
        addRow(form, gc, row++, "Departure Date.*", tfDepartureDate,  "Return Date.*",    tfReturnDate);
        addRow(form, gc, row++, "Start Odometer.*", tfStartOdometer,  "End Odometer.*",   tfEndOdometer);
        addRow(form, gc, row++, "Requested By.*",   tfRequestedBy,    null,               null);
        addRow(form, gc, row++, "Destination.*",    tfDestination,    null,               null);

        // Purpose
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        form.add(label("Purpose*"), gc);
        gc.gridx = 1; gc.gridy = row; gc.gridwidth = 3;
        form.add(new JScrollPane(taPurpose), gc);
        gc.gridwidth = 1; row++;

        // Remarks
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        form.add(label("Remarks"), gc);
        gc.gridx = 1; gc.gridy = row; gc.gridwidth = 3;
        form.add(new JScrollPane(taRemarks), gc);
        gc.gridwidth = 1; row++;

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel center = new JPanel(new BorderLayout(0, 15));
        center.setBackground(MainFrame.MAIN_BG);
        center.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        center.add(scroll, BorderLayout.CENTER);
        center.add(buildButtonBar(), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    // ─── Department dropdown (now +/- managed, same as the other lookups) ───────
    private JPanel buildDepartmentField() {
        styleCombo(cbDepartment);
        refreshDepartments(null);

        JButton btnAdd = smallBtn("+", new Color(52, 120, 210));
        JButton btnDel = smallBtn("−", new Color(192, 57, 43));
        btnAdd.addActionListener(e -> addNewDepartment());
        btnDel.addActionListener(e -> deleteDepartment());

        JPanel btns = new JPanel(new GridLayout(1, 2, 4, 0));
        btns.setBackground(Color.WHITE);
        btns.add(btnAdd);
        btns.add(btnDel);

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBackground(Color.WHITE);
        panel.add(cbDepartment, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.EAST);
        return panel;
    }

    private void refreshDepartments(String selectValue) {
        List<String> depts = deptDao.getAllDepartments();
        cbDepartment.removeAllItems();
        for (String d : depts) cbDepartment.addItem(d);
        if (selectValue != null && depts.contains(selectValue)) {
            cbDepartment.setSelectedItem(selectValue);
        } else if (cbDepartment.getItemCount() > 0) {
            cbDepartment.setSelectedIndex(0);
        }
    }

    private void addNewDepartment() {
        String name = JOptionPane.showInputDialog(this,
            "Enter new department name:", "Add Department", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return; // cancelled
        name = name.trim();
        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this, "Department name cannot be blank.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (deptDao.departmentExists(name)) {
            JOptionPane.showMessageDialog(this, "That department already exists.",
                "Duplicate Department", JOptionPane.WARNING_MESSAGE);
            refreshDepartments(name);
            return;
        }
        if (deptDao.addDepartment(name)) {
            refreshDepartments(name);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add department.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteDepartment() {
        String selected = (String) cbDepartment.getSelectedItem();
        if (selected == null || selected.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a department to delete first.",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete \"" + selected + "\" from the Department list?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            deptDao.deleteDepartment(selected);
            refreshDepartments(null);
        }
    }

    // ─── Generic +/- managed dropdowns (Driver Name, Vehicle Type, Plate Number, Status) ─
    private JPanel buildOptionField(String category, JComboBox<String> combo) {
        styleCombo(combo);
        refreshOptions(category, combo, null);

        JButton btnAdd = smallBtn("+", new Color(52, 120, 210));
        JButton btnDel = smallBtn("−", new Color(192, 57, 43));
        btnAdd.addActionListener(e -> addOption(category, combo));
        btnDel.addActionListener(e -> deleteOption(category, combo));

        JPanel btns = new JPanel(new GridLayout(1, 2, 4, 0));
        btns.setBackground(Color.WHITE);
        btns.add(btnAdd);
        btns.add(btnDel);

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBackground(Color.WHITE);
        panel.add(combo, BorderLayout.CENTER);
        panel.add(btns, BorderLayout.EAST);
        return panel;
    }

    private void refreshOptions(String category, JComboBox<String> combo, String selectValue) {
        List<String> vals = optionDao.getValues(category);
        combo.removeAllItems();
        for (String v : vals) combo.addItem(v);
        if (selectValue != null && !selectValue.isBlank()) {
            // Preserve a historical value even if it isn't (yet) saved in the list,
            // e.g. data entered before this dropdown existed.
            if (!vals.contains(selectValue)) combo.addItem(selectValue);
            combo.setSelectedItem(selectValue);
        } else if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private void addOption(String category, JComboBox<String> combo) {
        String label = optionLabel(category);
        String name = JOptionPane.showInputDialog(this,
            "Enter new " + label + ":", "Add " + label, JOptionPane.PLAIN_MESSAGE);
        if (name == null) return; // cancelled
        name = name.trim();
        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this, label + " cannot be blank.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (optionDao.valueExists(category, name)) {
            JOptionPane.showMessageDialog(this, "That " + label.toLowerCase() + " already exists.",
                "Duplicate Entry", JOptionPane.WARNING_MESSAGE);
            refreshOptions(category, combo, name);
            return;
        }
        if (optionDao.addValue(category, name)) {
            refreshOptions(category, combo, name);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add " + label.toLowerCase() + ".",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteOption(String category, JComboBox<String> combo) {
        String selected = (String) combo.getSelectedItem();
        if (selected == null || selected.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a value to delete first.",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String label = optionLabel(category);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete \"" + selected + "\" from the " + label + " list?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            optionDao.deleteValue(category, selected);
            refreshOptions(category, combo, null);
        }
    }

    private String optionLabel(String category) {
        return switch (category) {
            case "driver" -> "Driver Name";
            case "vehicle_type" -> "Vehicle Type";
            case "plate_number" -> "Plate Number";
            case "status" -> "Status";
            default -> "Value";
        };
    }

    private void addRow(JPanel form, GridBagConstraints gc,
                        int row, String lbl1, Component c1, String lbl2, Component c2) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        form.add(label(lbl1), gc);
        gc.gridx = 1; gc.weightx = 0.4;
        form.add(c1, gc);

        if (lbl2 != null) {
            gc.gridx = 2; gc.weightx = 0;
            form.add(label(lbl2), gc);
            gc.gridx = 3; gc.weightx = 0.4;
            form.add(c2, gc);
        }
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setBackground(MainFrame.MAIN_BG);

        JButton btnCancel = btn("Cancel", new Color(150, 160, 175), Color.WHITE);
        JButton btnSave   = btn("Save Ticket", MainFrame.ACCENT, Color.WHITE);
        JButton btnClear  = btn("Clear Form", new Color(100, 120, 150), Color.WHITE);

        btnSave.addActionListener(e -> saveTicket(true));
        btnClear.addActionListener(e -> clearForm());
        btnCancel.addActionListener(e -> frame.showPanel("LIST"));

        bar.add(btnClear);
        bar.add(btnCancel);
        bar.add(btnSave);
        return bar;
    }

    /**
     * @param returnToList true (Save Ticket button) goes back to the ticket
     *                      list on success; false (Enter-in-Remarks shortcut)
     *                      clears the form and stays here, ready for the next
     *                      new ticket.
     */
    private void saveTicket(boolean returnToList) {
        // Validation
        if (tfTicketNo.getText().isBlank()
            || cbDriverName.getSelectedItem() == null
            || cbVehicleType.getSelectedItem() == null
            || cbPlateNumber.getSelectedItem() == null
            || cbStatus.getSelectedItem() == null
            || tfDestination.getText().isBlank()
            || tfDepartureDate.getText().isBlank() || tfReturnDate.getText().isBlank()
            || tfRequestedBy.getText().isBlank() || taPurpose.getText().isBlank()
            || cbDepartment.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                "Please fill all required fields (marked with *).\nIf Status has no options, add one with its \"+\" button first.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Odometer validation
        double startOdo, endOdo;
        try {
            startOdo = Double.parseDouble(tfStartOdometer.getText().trim());
            endOdo = Double.parseDouble(tfEndOdometer.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Starting and Ending Odometer must be numeric values.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (endOdo < startOdo) {
            JOptionPane.showMessageDialog(this,
                "Ending Odometer cannot be less than Starting Odometer.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ticketNo = tfTicketNo.getText().trim();

        // Friendly duplicate check before insert (only relevant for NEW tickets)
        if (editingId <= 0 && dao.ticketNoExists(ticketNo)) {
            JOptionPane.showMessageDialog(this,
                "Ticket No. \"" + ticketNo + "\" already exists. Please use a different ticket number.",
                "Duplicate Ticket No.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String plateNumber = (String) cbPlateNumber.getSelectedItem();

        TripTicket t = new TripTicket();
        t.setTicketNo(ticketNo);
        t.setDriverName((String) cbDriverName.getSelectedItem());
        // "Vehicle No." was removed from the UI; keep it in sync with Plate
        // Number so the database's NOT NULL column and existing list/report
        // "Vehicle" columns keep working.
        t.setVehicleNo(plateNumber);
        t.setVehicleType((String) cbVehicleType.getSelectedItem());
        t.setPlateNumber(plateNumber);
        t.setDepartment((String) cbDepartment.getSelectedItem());
        t.setDestination(tfDestination.getText().trim());
        t.setPurpose(taPurpose.getText().trim());
        t.setDepartureDate(tfDepartureDate.getText().trim());
        t.setReturnDate(tfReturnDate.getText().trim());
        t.setStartOdometer(startOdo);
        t.setEndOdometer(endOdo);
        t.setRequestedBy(tfRequestedBy.getText().trim());
        // "Approved By" was removed from the UI.
        t.setApprovedBy("");
        t.setStatus((String) cbStatus.getSelectedItem());
        t.setRemarks(taRemarks.getText().trim());

        boolean ok;
        if (editingId > 0) {
            t.setId(editingId);
            ok = dao.updateTicket(t);
        } else {
            ok = dao.addTicket(t);
        }

        if (ok) {
            clearForm();
            if (returnToList) {
                JOptionPane.showMessageDialog(this,
                    "Ticket saved successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                frame.showPanel("LIST");
            }
            // else: stay on the form, already cleared and ready for the next entry
        } else {
            // Show the REAL underlying error instead of always guessing "duplicate"
            String detail = dao.getLastError();
            String msg = (detail != null)
                ? "Failed to save ticket.\nReason: " + detail
                : "Failed to save ticket. Please check your entries and try again.";
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadTicket(TripTicket t) {
        editingId = t.getId();
        lblTitle.setText("Edit Trip Ticket");
        tfTicketNo.setText(t.getTicketNo());
        tfTicketNo.setEditable(false); // ticket no. locked once created
        refreshOptions("driver", cbDriverName, t.getDriverName());
        refreshOptions("vehicle_type", cbVehicleType, t.getVehicleType());
        refreshOptions("plate_number", cbPlateNumber, t.getPlateNumber());
        refreshDepartments(t.getDepartment());
        tfDestination.setText(t.getDestination());
        taPurpose.setText(t.getPurpose());
        tfDepartureDate.setText(t.getDepartureDate());
        tfReturnDate.setText(t.getReturnDate());
        tfStartOdometer.setText(String.valueOf(t.getStartOdometer()));
        tfEndOdometer.setText(String.valueOf(t.getEndOdometer()));
        tfRequestedBy.setText(t.getRequestedBy());
        refreshOptions("status", cbStatus, t.getStatus());
        taRemarks.setText(t.getRemarks() != null ? t.getRemarks() : "");
    }

    public void clearForm() {
        editingId = -1;
        lblTitle.setText("New Trip Ticket");
        tfTicketNo.setEditable(true);
        tfTicketNo.setText("");
        refreshOptions("driver", cbDriverName, null);
        refreshOptions("vehicle_type", cbVehicleType, null);
        refreshOptions("plate_number", cbPlateNumber, null);
        tfDestination.setText("");
        refreshDepartments(null);
        taPurpose.setText(""); tfDepartureDate.setText(LocalDate.now().toString());
        tfReturnDate.setText(LocalDate.now().plusDays(1).toString());
        tfStartOdometer.setText("0"); tfEndOdometer.setText("0");
        tfRequestedBy.setText(""); taRemarks.setText("");
        refreshOptions("status", cbStatus, null);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────
    private JTextField field(String val) {
        JTextField tf = new JTextField(val, 18);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setPreferredSize(new Dimension(200, 32));
        return tf;
    }

    private JTextArea area() {
        JTextArea ta = new JTextArea(3, 18);
        ta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);

        // By default a JTextArea swallows Tab as a literal tab character
        // instead of moving focus. Re-enable normal Tab/Shift+Tab traversal
        // so pressing Tab in Purpose moves on to Remarks (and beyond).
        ta.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
            Collections.singleton(KeyStroke.getKeyStroke("TAB")));
        ta.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
            Collections.singleton(KeyStroke.getKeyStroke("shift TAB")));
        return ta;
    }

    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(60, 80, 110));
        return lbl;
    }

    private JButton btn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(150, 40));
        return b;
    }

    private JButton smallBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(28, 32));
        return b;
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(200, 32));
    }
}
