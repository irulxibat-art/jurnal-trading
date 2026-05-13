package com.irul.trading.model;

import java.time.LocalDate;

public class Trade {
    private int id;
    private LocalDate openDate;
    private String asset;
    private String type; // "BUY" atau "SELL"
    private double entryPrice;
    private double lotSize;
    private double stopLoss;
    private double takeProfit;
    private String openReason;
    private LocalDate closeDate;
    private double exitPrice;
    private double profitLoss;
    private TradeStatus status;

    public Trade() {}

    // --- Getter dan Setter (lengkap) ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }

    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getEntryPrice() { return entryPrice; }
    public void setEntryPrice(double entryPrice) { this.entryPrice = entryPrice; }

    public double getLotSize() { return lotSize; }
    public void setLotSize(double lotSize) { this.lotSize = lotSize; }

    public double getStopLoss() { return stopLoss; }
    public void setStopLoss(double stopLoss) { this.stopLoss = stopLoss; }

    public double getTakeProfit() { return takeProfit; }
    public void setTakeProfit(double takeProfit) { this.takeProfit = takeProfit; }

    public String getOpenReason() { return openReason; }
    public void setOpenReason(String openReason) { this.openReason = openReason; }

    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }

    public double getExitPrice() { return exitPrice; }
    public void setExitPrice(double exitPrice) { this.exitPrice = exitPrice; }

    public double getProfitLoss() { return profitLoss; }
    public void setProfitLoss(double profitLoss) { this.profitLoss = profitLoss; }

    public TradeStatus getStatus() { return status; }
    public void setStatus(TradeStatus status) { this.status = status; }
}