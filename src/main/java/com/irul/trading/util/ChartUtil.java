package com.irul.trading.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Day;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ChartUtil {
    public static JFreeChart createEquityCurve(String asset) {
        TimeSeries series = new TimeSeries("Equity");
        String sql = "SELECT tanggal_buka, profit_loss FROM trades WHERE status = 'CLOSED'";
        if (!"ALL".equals(asset)) sql += " AND aset = '" + asset + "'";
        sql += " ORDER BY tanggal_buka";
        try (Connection conn = DatabaseHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            double cumulative = 0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            while (rs.next()) {
                cumulative += rs.getDouble("profit_loss");
                String tgl = rs.getString("tanggal_buka");
                if (tgl != null && tgl.length() >= 10) {
                    LocalDate date = LocalDate.parse(tgl.substring(0, 10), formatter);
                    series.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), cumulative);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return ChartFactory.createTimeSeriesChart("Equity Curve", "Date", "Cumulative P/L", new TimeSeriesCollection(series), true, true, false);
    }
}