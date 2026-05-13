package com.irul.trading.controller;

import com.irul.trading.model.Trade;
import com.irul.trading.model.TradeStatus;
import com.irul.trading.util.DatabaseHelper;
import java.sql.*;
import java.time.LocalDate;

public class TradeController {

    public void openTrade(Trade trade) {
        String sql = "INSERT INTO trades (tanggal_buka, aset, tipe, entry_price, lot_size, stop_loss, take_profit, alasan_buka, status) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trade.getOpenDate().toString());
            pstmt.setString(2, trade.getAsset());
            pstmt.setString(3, trade.getType());
            pstmt.setDouble(4, trade.getEntryPrice());
            pstmt.setDouble(5, trade.getLotSize());
            pstmt.setDouble(6, trade.getStopLoss());
            pstmt.setDouble(7, trade.getTakeProfit());
            pstmt.setString(8, trade.getOpenReason());
            pstmt.setString(9, TradeStatus.OPEN.name());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void closeTrade(int tradeId, double exitPrice) {
        String selectSql = "SELECT entry_price, lot_size, tipe FROM trades WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, tradeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double entry = rs.getDouble("entry_price");
                double lot = rs.getDouble("lot_size");
                String type = rs.getString("tipe");
                double pl = (type.equalsIgnoreCase("BUY")) ? (exitPrice - entry) * lot : (entry - exitPrice) * lot;
                String updateSql = "UPDATE trades SET tanggal_tutup = ?, exit_price = ?, profit_loss = ?, status = 'CLOSED' WHERE id = ?";
                try (PreparedStatement pstmt2 = conn.prepareStatement(updateSql)) {
                    pstmt2.setString(1, LocalDate.now().toString());
                    pstmt2.setDouble(2, exitPrice);
                    pstmt2.setDouble(3, pl);
                    pstmt2.setString(4, TradeStatus.CLOSED.name());
                    pstmt2.setInt(5, tradeId);
                    pstmt2.executeUpdate();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}