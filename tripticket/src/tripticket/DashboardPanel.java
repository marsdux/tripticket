package tripticket;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final MainFrame frame;
    private final TripTicketDAO dao = new TripTicketDAO();

    private JLabel totalLabel, completedLabel, cancelledLabel;

    // Kept as fields so refresh() can actually repopulate them -- previously
    // this table was only ever filled once, at panel construction, so it
    // never showed newly added/edited tickets.
    private javax.swing.table.DefaultTableModel recentModel;

    public DashboardPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(MainFrame.MAIN_BG);
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        build();
    }

    private void build() {
        // Header
        JLabel header = new JLabel("Dashboard");
        header.setFont(new Font("Segoe UI", Font.BOLD, 26));
        header.setForeground(new Color(20, 40, 80));
        JLabel sub = new JLabel("Trip Ticket Overview");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(Color.GRAY);

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(MainFrame.MAIN_BG);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.add(header);
        headerPanel.add(sub);
        add(headerPanel, BorderLayout.NORTH);

        // Cards row -- Pending/Approved/On Trip removed; only the statuses
        // actually used in this workflow are shown.
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setBackground(MainFrame.MAIN_BG);

        totalLabel     = new JLabel("0", SwingConstants.CENTER);
        completedLabel = new JLabel("0", SwingConstants.CENTER);
        cancelledLabel = new JLabel("0", SwingConstants.CENTER);

        cardsPanel.add(makeCard("Total Tickets", totalLabel,     new Color(52, 120, 210)));
        cardsPanel.add(makeCard("Completed",     completedLabel, new Color(142, 68, 173)));
        cardsPanel.add(makeCard("Cancelled",     cancelledLabel, new Color(192, 57, 43)));

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setBackground(MainFrame.MAIN_BG);
        center.add(cardsPanel, BorderLayout.NORTH);
        center.add(buildRecentTable(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // Quick action bar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setBackground(MainFrame.MAIN_BG);
        JButton newBtn = styledButton("+ New Trip Ticket", MainFrame.ACCENT, Color.WHITE);
        JButton listBtn = styledButton("View All Tickets", new Color(52, 120, 210), Color.WHITE);
        newBtn.addActionListener(e -> frame.openNewForm());
        listBtn.addActionListener(e -> frame.showPanel("LIST"));
        actions.add(newBtn);
        actions.add(listBtn);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel makeCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(color);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        valueLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleLabel.setForeground(new Color(220, 235, 255));

        card.add(valueLabel);
        card.add(titleLabel);
        return card;
    }

    private JScrollPane buildRecentTable() {
        String[] cols = {"Ticket #", "Driver", "Destination", "Departure", "Status"};
        recentModel = new javax.swing.table.DefaultTableModel(new Object[][]{}, cols) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(recentModel);
        styleTable(table);
        fillRecentTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 230)),
            "Recent Trip Tickets",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13)
        ));
        return scroll;
    }

    // Re-reads the latest 5 tickets from the DB. Called both at construction
    // and from refresh(), so the table no longer goes stale.
    private void fillRecentTable() {
        recentModel.setRowCount(0);
        List<TripTicket> list = dao.getAllTickets(); // already ordered by id DESC (newest first)
        int count = Math.min(5, list.size());
        for (int i = 0; i < count; i++) {
            TripTicket t = list.get(i);
            recentModel.addRow(new Object[]{
                t.getTicketNo(), t.getDriverName(), t.getDestination(),
                t.getDepartureDate(), t.getStatus()
            });
        }
    }

    private void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(52, 120, 210));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(180, 210, 255));
        table.setGridColor(new Color(220, 228, 240));
        table.setShowGrid(true);
    }

    private JButton styledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 40));
        return btn;
    }

    public void refresh() {
        List<TripTicket> all = dao.getAllTickets();
        totalLabel.setText(String.valueOf(all.size()));
        completedLabel.setText(String.valueOf(dao.countByStatus("Completed")));
        cancelledLabel.setText(String.valueOf(dao.countByStatus("Cancelled")));
        fillRecentTable();
        revalidate();
        repaint();
    }
}
