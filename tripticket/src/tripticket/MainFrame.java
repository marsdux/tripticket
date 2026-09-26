package tripticket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Panels
    private DashboardPanel dashboardPanel;
    private TicketFormPanel ticketFormPanel;
    private TicketListPanel ticketListPanel;
    private ReportPanel reportPanel;

    // Colors
    static final Color SIDEBAR_BG   = new Color(15, 52, 96);
    static final Color SIDEBAR_HOVER = new Color(21, 76, 138);
    static final Color ACCENT        = new Color(232, 93, 4);
    static final Color TEXT_LIGHT    = Color.WHITE;
    static final Color MAIN_BG       = new Color(245, 247, 250);

    public MainFrame() {
        DatabaseHelper.initializeDatabase();
        setTitle("Trip Ticket Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);

        showPanel("DASHBOARD");
        setVisible(true);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));

        // Logo area
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        logoPanel.setBackground(SIDEBAR_BG);
        logoPanel.setMaximumSize(new Dimension(220, 100));
        JLabel icon = new JLabel("🚌");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        JPanel logoText = new JPanel();
        logoText.setBackground(SIDEBAR_BG);
        logoText.setLayout(new BoxLayout(logoText, BoxLayout.Y_AXIS));
        JLabel title1 = new JLabel("TRIP TICKET");
        title1.setForeground(Color.WHITE);
        title1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel title2 = new JLabel("MANAGEMENT");
        title2.setForeground(new Color(150, 180, 220));
        title2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        logoText.add(title1);
        logoText.add(title2);
        logoPanel.add(icon);
        logoPanel.add(logoText);
        sidebar.add(logoPanel);

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 80, 130));
        sep.setMaximumSize(new Dimension(200, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(10));

        // Nav buttons
        String[][] navItems = {
            {"🏠", " Dashboard",    "DASHBOARD"},
            {"➕", " New Ticket",   "FORM"},
            {"📋", " All Tickets",  "LIST"},
            {"📊", " Reports",      "REPORT"},
        };

        for (String[] item : navItems) {
            sidebar.add(buildNavButton(item[0], item[1], item[2]));
        }

        sidebar.add(Box.createVerticalGlue());

        JLabel version = new JLabel("  v1.0.0 — 2025");
        version.setForeground(new Color(100, 140, 190));
        version.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(version);
        sidebar.add(Box.createVerticalStrut(15));

        return sidebar;
    }

    private JButton buildNavButton(String emoji, String label, String panelName) {
        JButton btn = new JButton(emoji + label);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btn.setForeground(TEXT_LIGHT);
        btn.setBackground(SIDEBAR_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 50));
        btn.setPreferredSize(new Dimension(220, 50));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(SIDEBAR_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(SIDEBAR_BG); }
        });
        btn.addActionListener(e -> showPanel(panelName));
        return btn;
    }

    private JPanel buildContent() {
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(MAIN_BG);

        dashboardPanel = new DashboardPanel(this);
        ticketFormPanel = new TicketFormPanel(this);
        ticketListPanel = new TicketListPanel(this);
        reportPanel     = new ReportPanel(this);

        contentPanel.add(dashboardPanel, "DASHBOARD");
        contentPanel.add(ticketFormPanel, "FORM");
        contentPanel.add(ticketListPanel, "LIST");
        contentPanel.add(reportPanel,     "REPORT");

        return contentPanel;
    }

    public void showPanel(String name) {
        cardLayout.show(contentPanel, name);
        if ("LIST".equals(name))      ticketListPanel.loadData();
        if ("DASHBOARD".equals(name)) dashboardPanel.refresh();
        if ("REPORT".equals(name))    reportPanel.refresh();
    }

    public void openEditForm(TripTicket ticket) {
        ticketFormPanel.loadTicket(ticket);
        showPanel("FORM");
    }

    public void openNewForm() {
        ticketFormPanel.clearForm();
        showPanel("FORM");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
