package com.irul.trading.view;

import com.irul.trading.util.DatabaseHelper;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;

public class CapitalPanel extends JPanel {
    private JLabel lblBalance, lblEquity;
    private NumberFormat rupiahFormat;

    public CapitalPanel() {
        rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        initUI();
        refreshData();
        new Timer(10000, e -> refreshData()).start();
    }

    private void initUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Account Balance:"), gbc);
        gbc.gridx = 1;
        lblBalance = new JLabel("-");
        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(lblBalance, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Equity:"), gbc);
        gbc.gridx = 1;
        lblEquity = new JLabel("-");
        lblEquity.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(lblEquity, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        add(refreshBtn, gbc);
    }

    private void refreshData() {
        double balance = DatabaseHelper.getLatestBalance();
        double equity = DatabaseHelper.getLatestEquity();
        lblBalance.setText(rupiahFormat.format(balance));
        lblEquity.setText(rupiahFormat.format(equity));
    }
}