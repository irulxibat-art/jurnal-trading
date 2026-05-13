package com.irul.trading.view;

import com.irul.trading.util.ChartUtil;
import org.jfree.chart.ChartPanel;
import javax.swing.*;
import java.awt.*;

public class AnalyticsPanel extends JPanel {
    private JComboBox<String> assetCombo;
    private ChartPanel chartPanel;

    public AnalyticsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        initUI();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("Select Asset:"));
        assetCombo = new JComboBox<>();
        assetCombo.addItem("ALL");
        assetCombo.addItem("XAUUSDm");
        assetCombo.addItem("BTCUSDm");
        JButton refreshBtn = new JButton("Show Equity Curve");
        refreshBtn.addActionListener(e -> updateChart());
        topPanel.add(assetCombo);
        topPanel.add(refreshBtn);
        add(topPanel, BorderLayout.NORTH);

        chartPanel = new ChartPanel(null);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        chartPanel.setBackground(new Color(30, 30, 35));
        chartPanel.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        add(chartPanel, BorderLayout.CENTER);
    }

    private void updateChart() {
        String asset = (String) assetCombo.getSelectedItem();
        if (asset != null) {
            chartPanel.setChart(ChartUtil.createEquityCurve(asset));
        }
    }
}