package com.irul.trading.view;

import com.irul.trading.util.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SettingsPanel extends JPanel {
    private JComboBox<String> themeCombo;

    public SettingsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        initUI();
    }

    private void initUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Theme:"), gbc);
        gbc.gridx = 1;
        themeCombo = new JComboBox<>(new String[]{"Dark", "Light", "IntelliJ"});
        themeCombo.addActionListener(e -> ThemeUtil.applyTheme((String) themeCombo.getSelectedItem()));
        add(themeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JButton exportBtn = new JButton("Export to Excel");
        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("trades_backup.xlsx"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                ExportUtil.exportToExcel(fc.getSelectedFile().getAbsolutePath());
            }
        });
        add(exportBtn, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        JButton backupBtn = new JButton("Backup DB");
        backupBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("backup/trading.db"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                BackupUtil.backupDatabase(fc.getSelectedFile().getAbsolutePath());
            }
        });
        add(backupBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JButton notifyBtn = new JButton("Test Notification");
        notifyBtn.addActionListener(e -> NotifikasiUtil.showNotification("Test", "Notifikasi berfungsi!"));
        add(notifyBtn, gbc);
    }
}