package com.irul.trading.view;

import com.irul.trading.controller.TradeController;
import com.irul.trading.model.Trade;
import com.irul.trading.util.DatabaseHelper;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.sql.*;

public class JournalPanel extends JPanel {
    private JTable openTable, closedTable;
    private DefaultTableModel openModel, closedModel;
    private TableRowSorter<DefaultTableModel> openSorter, closedSorter;
    private TradeController tradeController;
    private Timer refreshTimer;
    private JTextField searchField;
    private JComboBox<String> assetFilter;

    public JournalPanel() {
        tradeController = new TradeController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        initUI();
        loadData();
        refreshTimer = new Timer(5000, e -> loadData());
        refreshTimer.start();
    }

    private void initUI() {
        // Panel filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("Search:"));
        searchField = new JTextField(15);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Asset:"));
        assetFilter = new JComboBox<>();
        assetFilter.addItem("All");
        assetFilter.addItem("XAUUSDm");
        assetFilter.addItem("BTCUSDm");
        assetFilter.addActionListener(e -> loadData());
        filterPanel.add(assetFilter);
        add(filterPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Open Positions", createOpenTradesPanel());
        tabbedPane.addTab("Closed Positions", createClosedTradesPanel());
        tabbedPane.addTab("New Trade", createNewTradePanel());
        add(tabbedPane, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("⟳ Refresh");
        refreshBtn.addActionListener(e -> loadData());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(refreshBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JScrollPane createOpenTradesPanel() {
        openModel = new DefaultTableModel(new String[]{"ID", "Date", "Asset", "Type", "Entry", "Lot", "SL", "TP", "Action"}, 0);
        openTable = new JTable(openModel);
        openTable.setRowHeight(28);
        openTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        openTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        openSorter = new TableRowSorter<>(openModel);
        openTable.setRowSorter(openSorter);
        return new JScrollPane(openTable);
    }

    private JScrollPane createClosedTradesPanel() {
        closedModel = new DefaultTableModel(new String[]{"ID", "Open Date", "Close Date", "Asset", "Type", "Entry", "Exit", "Lot", "P&L"}, 0);
        closedTable = new JTable(closedModel);
        closedTable.setRowHeight(28);
        closedTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closedTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        closedSorter = new TableRowSorter<>(closedModel);
        closedTable.setRowSorter(closedSorter);
        return new JScrollPane(closedTable);
    }

    private JPanel createNewTradePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField assetField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"BUY", "SELL"});
        JTextField entryField = new JTextField();
        JTextField lotField = new JTextField();
        JTextField slField = new JTextField();
        JTextField tpField = new JTextField();
        JTextArea reasonArea = new JTextArea(3, 20);
        JButton openBtn = new JButton("Open Trade");

        openBtn.addActionListener(e -> {
            try {
                Trade trade = new Trade();
                trade.setOpenDate(LocalDate.now());
                trade.setAsset(assetField.getText());
                trade.setType((String) typeCombo.getSelectedItem());
                trade.setEntryPrice(Double.parseDouble(entryField.getText()));
                trade.setLotSize(Double.parseDouble(lotField.getText()));
                trade.setStopLoss(slField.getText().isEmpty() ? 0 : Double.parseDouble(slField.getText()));
                trade.setTakeProfit(tpField.getText().isEmpty() ? 0 : Double.parseDouble(tpField.getText()));
                trade.setOpenReason(reasonArea.getText());
                tradeController.openTrade(trade);
                loadData();
                JOptionPane.showMessageDialog(this, "Trade opened!");
                assetField.setText("");
                entryField.setText("");
                lotField.setText("");
                slField.setText("");
                tpField.setText("");
                reasonArea.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number format");
            }
        });

        panel.add(new JLabel("Asset:")); panel.add(assetField);
        panel.add(new JLabel("Type:")); panel.add(typeCombo);
        panel.add(new JLabel("Entry Price:")); panel.add(entryField);
        panel.add(new JLabel("Lot Size:")); panel.add(lotField);
        panel.add(new JLabel("Stop Loss:")); panel.add(slField);
        panel.add(new JLabel("Take Profit:")); panel.add(tpField);
        panel.add(new JLabel("Reason:")); panel.add(new JScrollPane(reasonArea));
        panel.add(openBtn);
        return panel;
    }

    private void filter() {
        String text = searchField.getText().trim();
        RowFilter<DefaultTableModel, Object> filter = null;
        if (!text.isEmpty()) {
            filter = RowFilter.regexFilter("(?i)" + text);
        }
        openSorter.setRowFilter(filter);
        closedSorter.setRowFilter(filter);
    }

    private void loadData() {
        String selectedAsset = (String) assetFilter.getSelectedItem();
        String assetCondition = "";
        if (!"All".equals(selectedAsset)) {
            assetCondition = " AND aset = '" + selectedAsset + "'";
        }

        // Open trades
        openModel.setRowCount(0);
        String sqlOpen = "SELECT id, tanggal_buka, aset, tipe, entry_price, lot_size, stop_loss, take_profit FROM trades WHERE status = 'OPEN'" + assetCondition;
        try (Connection conn = DatabaseHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlOpen)) {
            while (rs.next()) {
                openModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("tanggal_buka"), rs.getString("aset"),
                    rs.getString("tipe"), rs.getDouble("entry_price"), rs.getDouble("lot_size"),
                    rs.getDouble("stop_loss"), rs.getDouble("take_profit"), "Close"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Closed trades dengan formatting P&L ke Rupiah
        closedModel.setRowCount(0);
        String sqlClosed = "SELECT id, tanggal_buka, tanggal_tutup, aset, tipe, entry_price, exit_price, lot_size, profit_loss FROM trades WHERE status = 'CLOSED'" + assetCondition + " ORDER BY id DESC";
        try (Connection conn = DatabaseHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlClosed)) {
            while (rs.next()) {
                double pl = rs.getDouble("profit_loss");
                String plFormatted = String.format("Rp %,.2f", pl);
                closedModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("tanggal_buka"), rs.getString("tanggal_tutup"),
                    rs.getString("aset"), rs.getString("tipe"), rs.getDouble("entry_price"),
                    rs.getDouble("exit_price"), rs.getDouble("lot_size"), plFormatted
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        filter();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (refreshTimer != null) refreshTimer.stop();
    }
}