package com.irul.trading;

import com.formdev.flatlaf.FlatDarkLaf;
import com.irul.trading.view.*;
import com.irul.trading.util.DatabaseHelper;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private JPanel contentPanel;
    private CardLayout cardLayout;

    public MainFrame() {
        setTitle("Trading Journal Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(18, 18, 18));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel title = new JLabel("📊 TRADING JOURNAL");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        sidebar.add(title);

        String[][] menuItems = {
            {"📈", "Dashboard"},
            {"📋", "Journal"},
            {"📉", "Analytics"},
            {"💰", "Saldo"},
            {"⚙️", "Settings"},
            {"🤖", "AI Assistant"}
        };
        for (String[] item : menuItems) {
            JButton btn = new JButton(item[0] + " " + item[1]);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(200, 40));
            btn.setBackground(new Color(18, 18, 18));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            btn.addActionListener(e -> cardLayout.show(contentPanel, item[1]));
            sidebar.add(btn);
        }
        sidebar.add(Box.createVerticalGlue());

        // Content
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(new DashboardPanel(), "Dashboard");
        contentPanel.add(new JournalPanel(), "Journal");
        contentPanel.add(new AnalyticsPanel(), "Analytics");
        contentPanel.add(new CapitalPanel(), "Saldo");
        contentPanel.add(new SettingsPanel(), "Settings");
        contentPanel.add(new ChatPanel(), "AI Assistant");

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 1. Buat semua tabel database TERLEBIH DAHULU
        DatabaseHelper.createTables();

        // 2. Jalankan HTTP server di thread terpisah
        new Thread(() -> {
            try {
                com.irul.trading.util.TradeHttpServer.main(new String[]{});
            } catch (Exception e) {
                System.err.println("HTTP Server gagal: " + e.getMessage());
            }
        }).start();

        // 3. Jalankan GUI aplikasi utama
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}