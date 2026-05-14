package com.irul.trading.view;

import com.irul.trading.util.DatabaseHelper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Day;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardPanel extends JPanel {
    private JLabel lblWinRateValue, lblProfitFactorValue, lblMaxDrawdownValue, lblTotalTradesValue;
    private ChartPanel equityChartPanel, donutChartPanel;

    public DashboardPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        initUI();
        refreshStats();
        refreshEquityChart();
    }

    private void initUI() {
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        statsPanel.setOpaque(false);
        statsPanel.add(createStatCard("Win Rate", lblWinRateValue = new JLabel("-"), new Color(0, 150, 136)));
        statsPanel.add(createStatCard("Profit Factor", lblProfitFactorValue = new JLabel("-"), new Color(33, 150, 243)));
        statsPanel.add(createStatCard("Max Drawdown (Rp)", lblMaxDrawdownValue = new JLabel("-"), new Color(255, 87, 34)));
        statsPanel.add(createStatCard("Total Trades", lblTotalTradesValue = new JLabel("-"), new Color(156, 39, 176)));
        add(statsPanel, BorderLayout.NORTH);

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 15, 15));
        chartsPanel.setOpaque(false);
        equityChartPanel = new ChartPanel(null);
        equityChartPanel.setPreferredSize(new Dimension(500, 300));
        equityChartPanel.setBorder(BorderFactory.createTitledBorder("Equity Curve"));
        chartsPanel.add(equityChartPanel);

        donutChartPanel = new ChartPanel(null);
        donutChartPanel.setPreferredSize(new Dimension(300, 300));
        donutChartPanel.setBorder(BorderFactory.createTitledBorder("Win Rate"));
        chartsPanel.add(donutChartPanel);
        add(chartsPanel, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("⟳ Refresh");
        refreshBtn.addActionListener(e -> { refreshStats(); refreshEquityChart(); refreshDonutChart(); });
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(30, 30, 35));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        valueLabel.setForeground(accentColor);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void refreshStats() {
        lblWinRateValue.setText(String.format("%.2f%%", DatabaseHelper.getWinRate()));
        lblProfitFactorValue.setText(String.format("%.2f", DatabaseHelper.getProfitFactor()));
        lblMaxDrawdownValue.setText(String.format("Rp %.2f", DatabaseHelper.getMaxDrawdown()));
        lblTotalTradesValue.setText(String.valueOf(DatabaseHelper.getTotalTrades()));
    }

    private void refreshEquityChart() {
        TimeSeries series = new TimeSeries("Equity");
        String sql = "SELECT tanggal_buka, profit_loss FROM trades WHERE status = 'CLOSED' ORDER BY tanggal_buka";
        double cumulative = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        try (Connection conn = DatabaseHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cumulative += rs.getDouble("profit_loss");
                String tglStr = rs.getString("tanggal_buka");
                if (tglStr != null && tglStr.length() >= 10) {
                    String datePart = tglStr.substring(0, 10);
                    if (datePart.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) {
                        LocalDate date = LocalDate.parse(datePart, formatter);
                        series.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), cumulative);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("", "Date", "Cumulative P/L (Rp)", new TimeSeriesCollection(series), true, true, false);
        chart.setBackgroundPaint(new Color(30, 30, 35));
        chart.getPlot().setBackgroundPaint(new Color(40, 40, 45));
        equityChartPanel.setChart(chart);
    }

    private void refreshDonutChart() {
        double winRate = DatabaseHelper.getWinRate();
        double loseRate = 100 - winRate;
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Win", winRate);
        dataset.setValue("Loss", loseRate);
        JFreeChart chart = ChartFactory.createRingChart("", dataset, true, true, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("Win", new Color(0, 150, 136));
        plot.setSectionPaint("Loss", new Color(244, 67, 54));
        plot.setBackgroundPaint(new Color(30, 30, 35));
        chart.setBackgroundPaint(new Color(30, 30, 35));
        donutChartPanel.setChart(chart);
    }
}