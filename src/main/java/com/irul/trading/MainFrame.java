package com.irul.trading;

import com.formdev.flatlaf.FlatDarkLaf;
import com.irul.trading.util.DatabaseHelper;
import com.irul.trading.util.UITheme;
import com.irul.trading.view.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainFrame extends JFrame {

    private JPanel    contentPanel;
    private CardLayout cardLayout;

    // Simpan semua sidebar button agar bisa toggle active state
    private JButton[] sidebarButtons;

    public MainFrame() {
        setTitle("Trading Journal Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1340, 820);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_OUTER);
        initUI();
    }

    private void initUI() {
        add(buildSidebar(),  BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
    }

    // =========================================================================
    // Sidebar
    // =========================================================================

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Border kanan tipis
                g.setColor(UITheme.BORDER);
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.BG_HEADER);
        sidebar.setPreferredSize(new Dimension(220, 0));

        // Logo / Brand area
        sidebar.add(buildBrandPanel());
        sidebar.add(Box.createVerticalStrut(8));

        // Menu items
        String[][] menuItems = {
            { "📈",  "Dashboard",    "Dashboard"    },
            { "📋",  "Journal",      "Journal"      },
            { "📉",  "Analytics",    "Analytics"    },
            { "💰",  "Saldo",        "Saldo"        },
            { "🤖",  "AI Assistant", "AI Assistant" },
            { "⚙️", "Settings",     "Settings"     },
        };

        sidebarButtons = new JButton[menuItems.length];

        for (int i = 0; i < menuItems.length; i++) {
            String icon   = menuItems[i][0];
            String label  = menuItems[i][1];
            String cardKey = menuItems[i][2];

            JButton btn = buildSidebarItem(icon, label, cardKey, i);
            sidebarButtons[i] = btn;

            JPanel btnWrapper = new JPanel(new BorderLayout());
            btnWrapper.setOpaque(false);
            btnWrapper.setMaximumSize(new Dimension(220, 44));
            btnWrapper.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
            btnWrapper.add(btn, BorderLayout.CENTER);
            sidebar.add(btnWrapper);
        }

        // Settings selalu di bawah
        sidebar.add(Box.createVerticalGlue());

        // Footer: versi kecil
        JLabel version = new JLabel("v1.0.0");
        version.setForeground(UITheme.TEXT_DISABLED);
        version.setFont(UITheme.FONT_SMALL);
        version.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 0));
        sidebar.add(version);

        // Set Dashboard sebagai active default
        setActiveButton(0);

        return sidebar;
    }

    private JPanel buildBrandPanel() {
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.setMaximumSize(new Dimension(220, 70));
        brand.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        // Icon lingkaran
        JLabel icon = new JLabel("TJ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setForeground(Color.WHITE);
        icon.setFont(new Font(UITheme.FONT, Font.BOLD, 12));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(36, 36));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel title = new JLabel("Trading Journal");
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setFont(new Font(UITheme.FONT, Font.BOLD, 13));

        JLabel sub = new JLabel("Pro Edition");
        sub.setForeground(UITheme.ACCENT);
        sub.setFont(UITheme.FONT_SMALL);

        text.add(title);
        text.add(sub);

        brand.add(icon, BorderLayout.WEST);
        brand.add(text, BorderLayout.CENTER);
        return brand;
    }

    private JButton buildSidebarItem(String icon, String label, String cardKey, int index) {
        JButton btn = new JButton(icon + "  " + label) {
            private boolean active = false;

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(UITheme.ACCENT_SUBTLE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    // Bar kiri aktif
                    g2.setColor(UITheme.ACCENT);
                    g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
                    setForeground(UITheme.ACCENT);
                } else if (getModel().isRollover()) {
                    g2.setColor(UITheme.BG_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    setForeground(UITheme.TEXT_PRIMARY);
                } else {
                    setForeground(UITheme.TEXT_SECONDARY);
                }
                g2.dispose();
                super.paintComponent(g);
            }

            public void setActive(boolean a) {
                this.active = a;
                setForeground(a ? UITheme.ACCENT : UITheme.TEXT_SECONDARY);
                repaint();
            }
            public boolean isActive() { return active; }
        };

        btn.setFont(new Font(UITheme.FONT, Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        btn.addActionListener(e -> {
            cardLayout.show(contentPanel, cardKey);
            setActiveButton(index);
        });

        return btn;
    }

    private void setActiveButton(int activeIndex) {
        for (int i = 0; i < sidebarButtons.length; i++) {
            JButton btn = sidebarButtons[i];
            // Akses method setActive via reflection workaround — gunakan cast
            if (btn instanceof JButton) {
                try {
                    btn.getClass().getMethod("setActive", boolean.class)
                       .invoke(btn, i == activeIndex);
                } catch (Exception ignored) {}
            }
        }
    }

    // =========================================================================
    // Content Area
    // =========================================================================

    private JPanel buildContent() {
        cardLayout    = new CardLayout();
        contentPanel  = new JPanel(cardLayout);
        contentPanel.setBackground(UITheme.BG_OUTER);

        contentPanel.add(new DashboardPanel(),  "Dashboard");
        contentPanel.add(new JournalPanel(),    "Journal");
        contentPanel.add(new AnalyticsPanel(),  "Analytics");
        contentPanel.add(new CapitalPanel(),    "Saldo");
        contentPanel.add(new ChatPanel(),       "AI Assistant");
        contentPanel.add(new SettingsPanel(),   "Settings");

        return contentPanel;
    }

    // =========================================================================
    // Entry Point
    // =========================================================================

    public static void main(String[] args) {
        DatabaseHelper.createTables();

        // HTTP server di thread terpisah
        new Thread(() -> {
            try {
                com.irul.trading.util.TradeHttpServer.main(new String[]{});
            } catch (Exception e) {
                System.err.println("HTTP Server gagal: " + e.getMessage());
            }
        }, "http-server").start();

        // Terapkan FlatDarkLaf sebagai base theme
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
