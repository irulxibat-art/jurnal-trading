package com.irul.trading.view;

import com.irul.trading.util.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SettingsPanel extends JPanel {

    private JComboBox<String> themeCombo;

    public SettingsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_OUTER);
        initUI();
    }

    private void initUI() {
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.BG_HEADER);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));
        JLabel title = new JLabel("Settings");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(UITheme.TEXT_PRIMARY);
        bar.add(title, BorderLayout.WEST);
        return bar;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.BG_OUTER);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        col.add(buildSection("Tampilan",    buildAppearanceCard()));
        col.add(Box.createVerticalStrut(16));
        col.add(buildSection("Data",        buildDataCard()));
        col.add(Box.createVerticalStrut(16));
        col.add(buildSection("Notifikasi",  buildNotifCard()));

        body.add(col, BorderLayout.NORTH);
        return body;
    }

    // =========================================================================
    // Section builder
    // =========================================================================

    private JPanel buildSection(String title, JPanel content) {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UITheme.FONT, Font.BOLD, 12));
        titleLbl.setForeground(UITheme.TEXT_MUTED);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        section.add(titleLbl,  BorderLayout.NORTH);
        section.add(content,   BorderLayout.CENTER);

        return section;
    }

    // =========================================================================
    // Cards
    // =========================================================================

    private JPanel buildAppearanceCard() {
        JPanel card = makeCard();

        addRow(card, "Tema Aplikasi",
            themeCombo = makeCombo(new String[]{ "Dark", "Light", "IntelliJ" }));
        themeCombo.addActionListener(e ->
            ThemeUtil.applyTheme((String) themeCombo.getSelectedItem()));

        return card;
    }

    private JPanel buildDataCard() {
        JPanel card = makeCard();

        JButton exportBtn = UITheme.createOutlineButton("Export ke Excel (.xlsx)");
        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("trades_backup.xlsx"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                ExportUtil.exportToExcel(fc.getSelectedFile().getAbsolutePath());
            }
        });

        JButton backupBtn = UITheme.createOutlineButton("Backup Database (.db)");
        backupBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("backup/trading.db"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                BackupUtil.backupDatabase(fc.getSelectedFile().getAbsolutePath());
            }
        });

        addRow(card, "Export Data",   exportBtn);
        addRow(card, "Backup DB",     backupBtn);

        return card;
    }

    private JPanel buildNotifCard() {
        JPanel card = makeCard();

        JButton testBtn = UITheme.createOutlineButton("Tes Notifikasi");
        testBtn.addActionListener(e ->
            NotifikasiUtil.showNotification("Tes", "Notifikasi berfungsi dengan baik!"));

        addRow(card, "System Tray", testBtn);
        return card;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UITheme.BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        return card;
    }

    private void addRow(JPanel card, String label, Component control) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lbl.setFont(UITheme.FONT_BODY);
        row.add(lbl, BorderLayout.WEST);
        row.add(control, BorderLayout.EAST);

        card.add(row);
    }

    private JComboBox<String> makeCombo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setBackground(UITheme.BG_INPUT);
        c.setForeground(UITheme.TEXT_PRIMARY);
        c.setFont(UITheme.FONT_BODY);
        c.setPreferredSize(new Dimension(150, 32));
        return c;
    }
}
