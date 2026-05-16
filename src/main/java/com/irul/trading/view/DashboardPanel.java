package com.irul.trading.view;

import com.irul.trading.util.CurrencyUtil;
import com.irul.trading.util.DatabaseHelper;
import com.irul.trading.util.UITheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardPanel extends JPanel {

    private JLabel lblWinRate, lblProfitFactor, lblMaxDrawdown, lblTotalTrades;
    private ChartPanel equityChartPanel, donutChartPanel;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_OUTER);
        initUI();
        refresh();
    }

    // =========================================================================
    // UI Construction
    // =========================================================================

    private void initUI() {
        // --- Top bar ---
        add(buildTopBar(),    BorderLayout.NORTH);
        // --- Body: stat cards + charts ---
        add(buildBody(),      BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.BG_HEADER);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        JLabel title = new JLabel("Dashboard");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(UITheme.TEXT_PRIMARY);
        bar.add(title, BorderLayout.WEST);

        JButton refreshBtn = UITheme.createOutlineButton("⟳  Refresh");
        refreshBtn.addActionListener(e -> refresh());
        bar.add(refreshBtn, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(UITheme.BG_OUTER);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        body.add(buildStatCards(), BorderLayout.NORTH);
        body.add(buildCharts(),    BorderLayout.CENTER);

        return body;
    }

    // -------------------------------------------------------------------------
    // Stat Cards
    // -------------------------------------------------------------------------

    private JPanel buildStatCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);

        grid.add(buildCard("Win Rate",      lblWinRate      = makeValueLabel(), UITheme.CARD_TEAL,   "📈"));
        grid.add(buildCard("Profit Factor", lblProfitFactor = makeValueLabel(), UITheme.CARD_BLUE,   "💹"));
        grid.add(buildCard("Max Drawdown",  lblMaxDrawdown  = makeValueLabel(), UITheme.CARD_RED,    "📉"));
        grid.add(buildCard("Total Trades",  lblTotalTrades  = makeValueLabel(), UITheme.CARD_PURPLE, "🔢"));

        return grid;
    }

    private JPanel buildCard(String title, JLabel valueLabel, Color accent, String emoji) {
        JPanel card = UITheme.createStatCard(accent);

        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(new Font(UITheme.FONT, Font.PLAIN, 10));
        titleLabel.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setFont(new Font(UITheme.FONT, Font.BOLD, 22));
        valueLabel.setForeground(accent);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        textCol.add(titleLabel);
        textCol.add(Box.createVerticalStrut(6));
        textCol.add(valueLabel);

        card.add(emojiLabel, BorderLayout.WEST);
        card.add(textCol,    BorderLayout.CENTER);

        return card;
    }

    private JLabel makeValueLabel() {
        JLabel l = new JLabel("—");
        l.setFont(UITheme.FONT_H2);
        return l;
    }

    // -------------------------------------------------------------------------
    // Charts
    // -------------------------------------------------------------------------

    private JPanel buildCharts() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);

        // Equity curve
        equityChartPanel = new ChartPanel(null);
        equityChartPanel.setBackground(UITheme.BG_CARD);
        equityChartPanel.setPreferredSize(new Dimension(500, 300));
        row.add(wrapChart(equityChartPanel, "Equity Curve"));

        // Donut win rate
        donutChartPanel = new ChartPanel(null);
        donutChartPanel.setBackground(UITheme.BG_CARD);
        donutChartPanel.setPreferredSize(new Dimension(280, 300));
        row.add(wrapChart(donutChartPanel, "Win / Loss"));

        return row;
    }

    private JPanel wrapChart(ChartPanel cp, String title) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(UITheme.BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel titleLabel = new JLabel("  " + title);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);
        titleLabel.setFont(new Font(UITheme.FONT, Font.BOLD, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));

        cp.setOpaque(false);
        cp.setBackground(UITheme.BG_CARD);

        wrapper.add(titleLabel, BorderLayout.NORTH);
        wrapper.add(cp,         BorderLayout.CENTER);
        return wrapper;
    }

    // =========================================================================
    // Data Refresh
    // =========================================================================

    private void refresh() {
        refreshStats();
        refreshEquityChart();
        refreshDonutChart();
    }

    private void refreshStats() {
        double winRate  = DatabaseHelper.getWinRate();
        double pf       = DatabaseHelper.getProfitFactor();
        double maxDD    = DatabaseHelper.getMaxDrawdown();
        int    total    = DatabaseHelper.getTotalTrades();

        lblWinRate.setText(String.format("%.1f%%", winRate));
        lblProfitFactor.setText(String.format("%.2f", pf));
        // Max drawdown: tampilkan dalam currency akun
        lblMaxDrawdown.setText(CurrencyUtil.format(maxDD));
        lblTotalTrades.setText(String.valueOf(total));

        // Warnai profit factor: merah jika < 1, hijau jika >= 1
        lblProfitFactor.setForeground(pf >= 1.0 ? UITheme.SUCCESS : UITheme.DANGER);
        lblWinRate.setForeground(winRate >= 50 ? UITheme.CARD_TEAL : UITheme.WARNING);
    }

    private void refreshEquityChart() {
        TimeSeries series = new TimeSeries("Equity");
        String sql = "SELECT tanggal_buka, profit_loss FROM trades "
                   + "WHERE status = 'CLOSED' ORDER BY tanggal_buka";
        double cumulative = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cumulative += rs.getDouble("profit_loss");
                String tgl = rs.getString("tanggal_buka");
                if (tgl != null && tgl.length() >= 10) {
                    String part = tgl.substring(0, 10);
                    if (part.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) {
                        LocalDate d = LocalDate.parse(part, fmt);
                        series.addOrUpdate(
                            new Day(d.getDayOfMonth(), d.getMonthValue(), d.getYear()),
                            cumulative);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "", "Date", "Cumulative P/L", new TimeSeriesCollection(series), false, true, false);

        // Style renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, UITheme.ACCENT);
        renderer.setSeriesStroke(0, new BasicStroke(2f));
        ((org.jfree.chart.plot.XYPlot) chart.getPlot()).setRenderer(renderer);

        // Fill under line
        org.jfree.chart.renderer.xy.XYAreaRenderer areaRenderer = new org.jfree.chart.renderer.xy.XYAreaRenderer();
        areaRenderer.setSeriesPaint(0, new Color(29, 158, 117, 40));

        UITheme.applyChartTheme(chart);
        equityChartPanel.setChart(chart);
    }

    private void refreshDonutChart() {
        double win  = DatabaseHelper.getWinRate();
        double loss = 100 - win;

        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Win",  win  > 0 ? win  : 0.01);
        dataset.setValue("Loss", loss > 0 ? loss : 0.01);

        JFreeChart chart = ChartFactory.createRingChart("", dataset, true, true, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("Win",  UITheme.ACCENT);
        plot.setSectionPaint("Loss", UITheme.DANGER);
        plot.setSectionOutlinesVisible(false);
        plot.setLabelGenerator(null);
        plot.setSectionDepth(0.35);

        UITheme.applyChartTheme(chart);
        donutChartPanel.setChart(chart);
    }
}
