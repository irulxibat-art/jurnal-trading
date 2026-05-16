package com.irul.trading.view;

import com.irul.trading.util.CurrencyUtil;
import com.irul.trading.util.DatabaseHelper;
import com.irul.trading.util.UITheme;

import javax.swing.*;
import java.awt.*;

public class CapitalPanel extends JPanel {

    private JLabel lblBalance, lblEquity, lblMargin, lblFreeMargin;
    private JLabel lblBalanceConv, lblEquityConv;
    private JLabel lblCurrencyBadge;
    private Timer  autoRefreshTimer;

    public CapitalPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_OUTER);
        initUI();
        refreshData();
        autoRefreshTimer = new Timer(10_000, e -> refreshData());
        autoRefreshTimer.start();
    }

    // =========================================================================
    // UI Construction
    // =========================================================================

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

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("Saldo Akun");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(UITheme.TEXT_PRIMARY);
        left.add(title);

        // Badge currency
        lblCurrencyBadge = new JLabel("USD") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.ACCENT_SUBTLE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 99, 99);
                g2.setColor(UITheme.ACCENT);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 99, 99);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblCurrencyBadge.setForeground(UITheme.ACCENT);
        lblCurrencyBadge.setFont(new Font(UITheme.FONT, Font.BOLD, 11));
        lblCurrencyBadge.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        lblCurrencyBadge.setOpaque(false);
        left.add(lblCurrencyBadge);

        bar.add(left, BorderLayout.WEST);

        JButton refreshBtn = UITheme.createOutlineButton("⟳  Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        bar.add(refreshBtn, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.BG_OUTER);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Grid 2x2 kartu
        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);

        // Balance
        JPanel balCard = buildMoneyCard("Balance", UITheme.CARD_TEAL, "💳");
        lblBalance     = (JLabel) balCard.getClientProperty("valueLabel");
        lblBalanceConv = (JLabel) balCard.getClientProperty("convLabel");
        grid.add(balCard);

        // Equity
        JPanel eqCard = buildMoneyCard("Equity", UITheme.CARD_BLUE, "📊");
        lblEquity      = (JLabel) eqCard.getClientProperty("valueLabel");
        lblEquityConv  = (JLabel) eqCard.getClientProperty("convLabel");
        grid.add(eqCard);

        // Margin
        JPanel marCard = buildMoneyCard("Margin Used", UITheme.CARD_ORANGE, "⚡");
        lblMargin      = (JLabel) marCard.getClientProperty("valueLabel");
        grid.add(marCard);

        // Free Margin
        JPanel freeCard = buildMoneyCard("Free Margin", UITheme.CARD_PURPLE, "🆓");
        lblFreeMargin   = (JLabel) freeCard.getClientProperty("valueLabel");
        grid.add(freeCard);

        body.add(grid, BorderLayout.NORTH);

        // Info exchange rate
        JLabel rateInfo = new JLabel(" ");
        rateInfo.setForeground(UITheme.TEXT_MUTED);
        rateInfo.setFont(UITheme.FONT_SMALL);
        rateInfo.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        body.add(rateInfo, BorderLayout.SOUTH);

        return body;
    }

    /**
     * Membuat card untuk satu nilai uang.
     * Label disimpan sebagai ClientProperty agar bisa diakses dari luar.
     */
    private JPanel buildMoneyCard(String title, Color accent, String emoji) {
        JPanel card = UITheme.createStatCard(accent);
        card.setLayout(new BorderLayout(10, 0));

        // Emoji ikon
        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        emojiLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        card.add(emojiLabel, BorderLayout.WEST);

        // Kolom teks
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(new Font(UITheme.FONT, Font.PLAIN, 10));
        titleLbl.setForeground(UITheme.TEXT_MUTED);

        JLabel valueLbl = new JLabel("—");
        valueLbl.setFont(new Font(UITheme.FONT, Font.BOLD, 20));
        valueLbl.setForeground(accent);

        // Nilai konversi sekunder (lebih kecil, abu-abu)
        JLabel convLbl = new JLabel(" ");
        convLbl.setFont(UITheme.FONT_SMALL);
        convLbl.setForeground(UITheme.TEXT_MUTED);

        col.add(titleLbl);
        col.add(Box.createVerticalStrut(4));
        col.add(valueLbl);
        col.add(convLbl);

        card.add(col, BorderLayout.CENTER);

        // Simpan referensi label sebagai property
        card.putClientProperty("valueLabel", valueLbl);
        card.putClientProperty("convLabel",  convLbl);

        return card;
    }

    // =========================================================================
    // Data Refresh
    // =========================================================================

    private void refreshData() {
        String currency = CurrencyUtil.getAccountCurrency();
        lblCurrencyBadge.setText(currency);

        double balance    = DatabaseHelper.getLatestBalance();
        double equity     = DatabaseHelper.getLatestEquity();
        double margin     = DatabaseHelper.getLatestMargin();
        double freeMargin = DatabaseHelper.getLatestFreeMargin();

        // Primary values
        lblBalance.setText(CurrencyUtil.format(balance));
        lblEquity.setText(CurrencyUtil.format(equity));
        lblMargin.setText(CurrencyUtil.format(margin));
        lblFreeMargin.setText(CurrencyUtil.format(freeMargin));

        // Conversion hints (hanya untuk Balance & Equity)
        lblBalanceConv.setText(buildConvText(balance, currency));
        lblEquityConv.setText(buildConvText(equity, currency));

        // Warnai equity: merah jika di bawah balance (floating loss)
        lblEquity.setForeground(equity < balance ? UITheme.DANGER : UITheme.CARD_BLUE);
    }

    /**
     * Membangun teks konversi sekunder.
     * USD akun → tampilkan ekuivalen IDR, dan sebaliknya.
     */
    private String buildConvText(double amount, String currency) {
        try {
            if ("USD".equalsIgnoreCase(currency)) {
                double idr = CurrencyUtil.convert(amount, "USD", "IDR");
                return String.format("≈ Rp %,.0f", idr);
            }
            if ("IDR".equalsIgnoreCase(currency)) {
                double usd = CurrencyUtil.convert(amount, "IDR", "USD");
                return String.format("≈ $%,.2f", usd);
            }
        } catch (Exception ignored) {}
        return " ";
    }

    @Override public void removeNotify() {
        super.removeNotify();
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
    }
}
