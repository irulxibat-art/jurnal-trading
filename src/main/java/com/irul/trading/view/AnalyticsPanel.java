// ============================================================
// FILE 1: AnalyticsPanel.java
// ============================================================
package com.irul.trading.view;

import com.irul.trading.util.ChartUtil;
import com.irul.trading.util.UITheme;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import javax.swing.*;
import java.awt.*;

public class AnalyticsPanel extends JPanel {

    private JComboBox<String> assetCombo;
    private ChartPanel        chartPanel;

    public AnalyticsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_OUTER);
        initUI();
    }

    private void initUI() {
        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildBody(),    BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.BG_HEADER);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));

        JLabel title = new JLabel("Analytics");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(UITheme.TEXT_PRIMARY);
        bar.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        right.add(makeLabel("Aset:"));
        assetCombo = new JComboBox<>(new String[]{ "ALL", "XAUUSDm", "BTCUSDm" });
        styleCombo(assetCombo);
        right.add(assetCombo);

        JButton showBtn = UITheme.createPrimaryButton("Tampilkan");
        showBtn.addActionListener(e -> updateChart());
        right.add(showBtn);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.BG_OUTER);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel card = new JPanel(new BorderLayout()) {
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
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        chartPanel = new ChartPanel(null);
        chartPanel.setBackground(UITheme.BG_CARD);
        chartPanel.setPreferredSize(new Dimension(800, 420));
        chartPanel.setOpaque(false);
        card.add(chartPanel, BorderLayout.CENTER);

        body.add(card, BorderLayout.CENTER);
        return body;
    }

    private void updateChart() {
        String asset = (String) assetCombo.getSelectedItem();
        if (asset != null) {
            JFreeChart chart = ChartUtil.createEquityCurve(asset);
            UITheme.applyChartTheme(chart);
            chartPanel.setChart(chart);
        }
    }

    private void styleCombo(JComboBox<String> c) {
        c.setBackground(UITheme.BG_INPUT);
        c.setForeground(UITheme.TEXT_PRIMARY);
        c.setFont(UITheme.FONT_BODY);
        c.setPreferredSize(new Dimension(120, 32));
    }

    private JLabel makeLabel(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(UITheme.TEXT_MUTED);
        l.setFont(UITheme.FONT_BODY);
        return l;
    }
}
