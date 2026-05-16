package com.irul.trading.util;

import com.irul.trading.model.Trade;

public class RiskAnalyzer {
    public static double calculateRiskReward(Trade trade, double stopLoss, double takeProfit) {
        double risk = Math.abs(trade.getEntryPrice() - stopLoss) * trade.getLotSize();
        double reward = Math.abs(takeProfit - trade.getEntryPrice()) * trade.getLotSize();
        if (risk == 0) return 0;
        return reward / risk;
    }
}