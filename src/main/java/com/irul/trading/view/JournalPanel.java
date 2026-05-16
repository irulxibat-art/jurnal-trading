package com.irul.trading.view;

import com.irul.trading.controller.TradeController;
import com.irul.trading.model.Trade;
import com.irul.trading.util.CurrencyUtil;
import com.irul.trading.util.DatabaseHelper;
import com.irul.trading.util.UITheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class JournalPanel extends JPanel {

    private JTable            openTable, closedTable;
    private DefaultTableModel openModel, closedModel;
    private TableRowSorter<DefaultTableModel> openSorter, closedSorter;
    private TradeController   tradeController;
    private Timer             refreshTimer;
    private JTextField        searchField;
    private JComboBox<String> assetFilter;

    public JournalPanel() {
        tradeController = new TradeController();
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_OUTER);
        initUI();
        loadData();
        refreshTimer = new Timer(5000, e -> loadData());
        refreshTimer.start();
    }

    // =========================================================================
    // UI Construction
    // =========================================================================

    private void initUI() {
        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildTabs(),    BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(UITheme.BG_HEADER);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));

        JLabel title = new JLabel("Journal");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(UITheme.TEXT_PRIMARY);
        bar.add(title, BorderLayout.WEST);

        // Filter row
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setOpaque(false);

        // Search field
        searchField = buildSearchField();
        filters.add(new JLabel("Cari:") {{ setForeground(UITheme.TEXT_MUTED); setFont(UITheme.FONT_BODY); }});
        filters.add(searchField);

        // Asset filter
        filters.add(new JLabel("Aset:") {{ setForeground(UITheme.TEXT_MUTED); setFont(UITheme.FONT_BODY); }});
        assetFilter = new JComboBox<>(new String[]{ "All", "XAUUSDm", "BTCUSDm" });
        styleCombo(assetFilter);
        assetFilter.addActionListener(e -> loadData());
        filters.add(assetFilter);

        // Refresh button
        JButton refreshBtn = UITheme.createOutlineButton("⟳  Refresh");
        refreshBtn.addActionListener(e -> loadData());
        filters.add(refreshBtn);

        bar.add(filters, BorderLayout.EAST);
        return bar;
    }

    private JTextField buildSearchField() {
        JTextField f = new JTextField(14) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(UITheme.BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setBackground(UITheme.BG_INPUT);
        f.setForeground(UITheme.TEXT_PRIMARY);
        f.setCaretColor(UITheme.ACCENT);
        f.setFont(UITheme.FONT_BODY);
        f.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        f.setOpaque(false);
        f.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filter(); }
            @Override public void removeUpdate(DocumentEvent e)  { filter(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });
        return f;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(UITheme.BG_INPUT);
        combo.setForeground(UITheme.TEXT_PRIMARY);
        combo.setFont(UITheme.FONT_BODY);
        combo.setPreferredSize(new Dimension(110, 32));
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UITheme.BG_PANEL);
        tabs.setForeground(UITheme.TEXT_SECONDARY);
        tabs.setFont(UITheme.FONT_BODY);

        tabs.addTab("🟢  Open Positions",   buildOpenPanel());
        tabs.addTab("✅  Closed Positions",  buildClosedPanel());
        tabs.addTab("➕  New Trade",          buildNewTradePanel());

        return tabs;
    }

    // -------------------------------------------------------------------------
    // Tab: Open Positions
    // -------------------------------------------------------------------------

    private JPanel buildOpenPanel() {
        openModel = new DefaultTableModel(
            new String[]{ "#", "Tanggal Buka", "Aset", "Tipe", "Entry", "Lot", "SL", "TP" }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        openTable = new JTable(openModel);
        UITheme.styleTable(openTable);
        openSorter = new TableRowSorter<>(openModel);
        openTable.setRowSorter(openSorter);

        // Warnai kolom Tipe
        openTable.getColumnModel().getColumn(3).setCellRenderer(typeCellRenderer());

        JScrollPane sp = new JScrollPane(openTable);
        UITheme.styleScrollPane(sp);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // -------------------------------------------------------------------------
    // Tab: Closed Positions
    // -------------------------------------------------------------------------

    private JPanel buildClosedPanel() {
        closedModel = new DefaultTableModel(
            new String[]{ "#", "Buka", "Tutup", "Aset", "Tipe", "Entry", "Exit", "Lot", "P&L" }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        closedTable = new JTable(closedModel);
        UITheme.styleTable(closedTable);
        closedSorter = new TableRowSorter<>(closedModel);
        closedTable.setRowSorter(closedSorter);

        // Renderer: Tipe + P&L berwarna
        closedTable.getColumnModel().getColumn(4).setCellRenderer(typeCellRenderer());
        closedTable.getColumnModel().getColumn(8).setCellRenderer(plCellRenderer());

        JScrollPane sp = new JScrollPane(closedTable);
        UITheme.styleScrollPane(sp);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // -------------------------------------------------------------------------
    // Tab: New Trade
    // -------------------------------------------------------------------------

    private JPanel buildNewTradePanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UITheme.BG_PANEL);
        outer.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Form card
        JPanel card = new JPanel(new GridBagLayout()) {
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
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 8, 7, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Form fields
        JTextField   assetField  = buildFormField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{ "BUY", "SELL" });
        styleCombo(typeCombo);
        JTextField   entryField  = buildFormField();
        JTextField   lotField    = buildFormField();
        JTextField   slField     = buildFormField();
        JTextField   tpField     = buildFormField();
        JTextArea    reasonArea  = new JTextArea(3, 20);
        styleTextArea(reasonArea);

        Object[][] rows = {
            { "Aset",        assetField  },
            { "Tipe",        typeCombo   },
            { "Entry Price", entryField  },
            { "Lot Size",    lotField    },
            { "Stop Loss",   slField     },
            { "Take Profit", tpField     },
            { "Alasan",      new JScrollPane(reasonArea) },
        };

        for (int i = 0; i < rows.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            JLabel lbl = new JLabel((String) rows[i][0]);
            lbl.setForeground(UITheme.TEXT_SECONDARY);
            lbl.setFont(UITheme.FONT_BODY);
            card.add(lbl, gbc);

            gbc.gridx = 1; gbc.weightx = 1;
            card.add((Component) rows[i][1], gbc);
        }

        // Submit button
        gbc.gridx = 1; gbc.gridy = rows.length; gbc.weightx = 0;
        JButton openBtn = UITheme.createPrimaryButton("  Buka Trade  ");
        openBtn.addActionListener(e -> {
            try {
                Trade t = new Trade();
                t.setOpenDate(LocalDate.now());
                t.setAsset(assetField.getText().trim());
                t.setType((String) typeCombo.getSelectedItem());
                t.setEntryPrice(Double.parseDouble(entryField.getText()));
                t.setLotSize(Double.parseDouble(lotField.getText()));
                t.setStopLoss(slField.getText().isEmpty() ? 0 : Double.parseDouble(slField.getText()));
                t.setTakeProfit(tpField.getText().isEmpty() ? 0 : Double.parseDouble(tpField.getText()));
                t.setOpenReason(reasonArea.getText());
                tradeController.openTrade(t);
                loadData();
                JOptionPane.showMessageDialog(this, "Trade berhasil dibuka!", "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);
                assetField.setText(""); entryField.setText(""); lotField.setText("");
                slField.setText(""); tpField.setText(""); reasonArea.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Format angka tidak valid!", "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        card.add(openBtn, gbc);

        outer.add(card, BorderLayout.NORTH);
        return outer;
    }

    // =========================================================================
    // Cell Renderers
    // =========================================================================

    private javax.swing.table.TableCellRenderer typeCellRenderer() {
        return new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setFont(new Font(UITheme.FONT, Font.BOLD, 12));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                String v = value != null ? value.toString() : "";
                if ("BUY".equalsIgnoreCase(v)) {
                    setForeground(UITheme.SUCCESS);
                } else if ("SELL".equalsIgnoreCase(v)) {
                    setForeground(UITheme.DANGER);
                } else {
                    setForeground(UITheme.TEXT_PRIMARY);
                }
                if (!isSelected) setBackground(row % 2 == 0 ? UITheme.BG_PANEL : UITheme.BG_CARD);
                return this;
            }
        };
    }

    private javax.swing.table.TableCellRenderer plCellRenderer() {
        return new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setFont(new Font(UITheme.FONT, Font.BOLD, 12));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!isSelected) setBackground(row % 2 == 0 ? UITheme.BG_PANEL : UITheme.BG_CARD);
                // value sudah berupa string formatted dari loadData()
                String v = value != null ? value.toString() : "0";
                boolean positive = !v.startsWith("-");
                setForeground(positive ? UITheme.SUCCESS : UITheme.DANGER);
                return this;
            }
        };
    }

    // =========================================================================
    // Data Load
    // =========================================================================

    private void loadData() {
        String assetCond = "";
        String sel = (String) assetFilter.getSelectedItem();
        if (!"All".equals(sel)) {
            assetCond = " AND aset = '" + sel + "'";
        }

        // Open trades
        openModel.setRowCount(0);
        String sqlOpen = "SELECT id, tanggal_buka, aset, tipe, entry_price, lot_size, stop_loss, take_profit "
                       + "FROM trades WHERE status = 'OPEN'" + assetCond;
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlOpen)) {
            while (rs.next()) {
                openModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("tanggal_buka"),
                    rs.getString("aset"),
                    rs.getString("tipe"),
                    rs.getDouble("entry_price"),
                    rs.getDouble("lot_size"),
                    rs.getDouble("stop_loss"),
                    rs.getDouble("take_profit")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Closed trades — P&L diformat dengan CurrencyUtil
        closedModel.setRowCount(0);
        String sqlClosed = "SELECT id, tanggal_buka, tanggal_tutup, aset, tipe, entry_price, "
                         + "exit_price, lot_size, profit_loss "
                         + "FROM trades WHERE status = 'CLOSED'" + assetCond + " ORDER BY id DESC";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlClosed)) {
            while (rs.next()) {
                double pl = rs.getDouble("profit_loss");
                closedModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("tanggal_buka"),
                    rs.getString("tanggal_tutup"),
                    rs.getString("aset"),
                    rs.getString("tipe"),
                    rs.getDouble("entry_price"),
                    rs.getDouble("exit_price"),
                    rs.getDouble("lot_size"),
                    CurrencyUtil.format(pl)   // ← otomatis pakai currency akun
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        filter();
    }

    private void filter() {
        String text = searchField.getText().trim();
        RowFilter<DefaultTableModel, Object> f = null;
        if (!text.isEmpty()) {
            try { f = RowFilter.regexFilter("(?i)" + text); }
            catch (Exception ignored) {}
        }
        openSorter.setRowFilter(f);
        closedSorter.setRowFilter(f);
    }

    @Override public void removeNotify() {
        super.removeNotify();
        if (refreshTimer != null) refreshTimer.stop();
    }

    // =========================================================================
    // Form Helpers
    // =========================================================================

    private JTextField buildFormField() {
        JTextField f = new JTextField();
        f.setBackground(UITheme.BG_INPUT);
        f.setForeground(UITheme.TEXT_PRIMARY);
        f.setCaretColor(UITheme.ACCENT);
        f.setFont(UITheme.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        f.setPreferredSize(new Dimension(220, 34));
        return f;
    }

    private void styleTextArea(JTextArea area) {
        area.setBackground(UITheme.BG_INPUT);
        area.setForeground(UITheme.TEXT_PRIMARY);
        area.setCaretColor(UITheme.ACCENT);
        area.setFont(UITheme.FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }
}
