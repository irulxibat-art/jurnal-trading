package com.irul.trading.util;

import java.awt.*;

/**
 * UITheme — satu sumber kebenaran untuk semua warna dan font aplikasi.
 *
 * Dengan memusatkan palette di sini, kita hanya perlu mengubah satu file
 * jika ingin mengganti tema seluruh aplikasi.
 */
public final class UITheme {

    private UITheme() {} // Utility class, tidak boleh di-instantiate

    // -------------------------------------------------------------------------
    // Background Layers (semakin besar angka = semakin terang)
    // -------------------------------------------------------------------------
    public static final Color BG_DEEPEST  = new Color(13,  14,  18);  // paling gelap
    public static final Color BG_OUTER    = new Color(18,  18,  22);  // body utama
    public static final Color BG_PANEL    = new Color(26,  27,  32);  // panel konten
    public static final Color BG_CARD     = new Color(32,  34,  42);  // card / row alt
    public static final Color BG_HEADER   = new Color(22,  23,  28);  // header & sidebar
    public static final Color BG_INPUT    = new Color(34,  36,  42);  // input field
    public static final Color BG_HOVER    = new Color(42,  45,  56);  // hover state
    public static final Color BG_SELECTED = new Color(29, 158, 117, 40); // row selected

    // -------------------------------------------------------------------------
    // Text
    // -------------------------------------------------------------------------
    public static final Color TEXT_PRIMARY   = new Color(220, 222, 230);
    public static final Color TEXT_SECONDARY = new Color(160, 165, 180);
    public static final Color TEXT_MUTED     = new Color(110, 115, 130);
    public static final Color TEXT_DISABLED  = new Color(75,  80,  95);

    // -------------------------------------------------------------------------
    // Accent — teal hijau (sama dengan ChatPanel)
    // -------------------------------------------------------------------------
    public static final Color ACCENT         = new Color(29,  158, 117);
    public static final Color ACCENT_DIM     = new Color(15,  110, 86);
    public static final Color ACCENT_SUBTLE  = new Color(29,  158, 117, 25);

    // -------------------------------------------------------------------------
    // Status Colors
    // -------------------------------------------------------------------------
    public static final Color SUCCESS        = new Color(39,  174, 96);
    public static final Color SUCCESS_SUBTLE = new Color(39,  174, 96,  25);
    public static final Color DANGER         = new Color(231, 76,  60);
    public static final Color DANGER_SUBTLE  = new Color(231, 76,  60,  25);
    public static final Color WARNING        = new Color(243, 156, 18);
    public static final Color WARNING_SUBTLE = new Color(243, 156, 18, 25);
    public static final Color INFO           = new Color(52,  152, 219);
    public static final Color INFO_SUBTLE    = new Color(52,  152, 219, 25);

    // -------------------------------------------------------------------------
    // Card Accent Colors (untuk border kiri stat cards)
    // -------------------------------------------------------------------------
    public static final Color CARD_TEAL   = new Color(29,  158, 117);
    public static final Color CARD_BLUE   = new Color(52,  152, 219);
    public static final Color CARD_RED    = new Color(231, 76,  60);
    public static final Color CARD_PURPLE = new Color(155, 89,  182);
    public static final Color CARD_ORANGE = new Color(230, 126, 34);

    // -------------------------------------------------------------------------
    // Border
    // -------------------------------------------------------------------------
    public static final Color BORDER         = new Color(52,  55,  65);
    public static final Color BORDER_LIGHT   = new Color(65,  70,  82);

    // -------------------------------------------------------------------------
    // Typography
    // -------------------------------------------------------------------------
    public static final String FONT       = "Segoe UI";
    public static final Font   FONT_H1    = new Font(FONT, Font.BOLD,  22);
    public static final Font   FONT_H2    = new Font(FONT, Font.BOLD,  16);
    public static final Font   FONT_H3    = new Font(FONT, Font.BOLD,  13);
    public static final Font   FONT_BODY  = new Font(FONT, Font.PLAIN, 13);
    public static final Font   FONT_SMALL = new Font(FONT, Font.PLAIN, 11);
    public static final Font   FONT_MONO  = new Font("JetBrains Mono", Font.PLAIN, 12);

    // -------------------------------------------------------------------------
    // Helper — JFreeChart background setup
    // -------------------------------------------------------------------------

    /**
     * Menerapkan warna dark theme ke JFreeChart.
     * Dipanggil setelah chart dibuat agar semua elemen ikut tema.
     *
     * @param chart Chart yang akan di-style
     */
    public static void applyChartTheme(org.jfree.chart.JFreeChart chart) {
        chart.setBackgroundPaint(BG_PANEL);
        chart.getPlot().setBackgroundPaint(BG_CARD);
        chart.getPlot().setOutlinePaint(BORDER);

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(BG_PANEL);
            chart.getLegend().setItemPaint(TEXT_SECONDARY);
        }

        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(TEXT_PRIMARY);
        }

        // Axis styling untuk XY charts
        if (chart.getPlot() instanceof org.jfree.chart.plot.XYPlot) {
            org.jfree.chart.plot.XYPlot plot = (org.jfree.chart.plot.XYPlot) chart.getPlot();
            styleAxis(plot.getDomainAxis());
            styleAxis(plot.getRangeAxis());
            plot.setDomainGridlinePaint(BORDER);
            plot.setRangeGridlinePaint(BORDER);
            plot.setDomainCrosshairPaint(ACCENT);
            plot.setRangeCrosshairPaint(ACCENT);
        }

        // Axis styling untuk CategoryPlot
        if (chart.getPlot() instanceof org.jfree.chart.plot.CategoryPlot) {
            org.jfree.chart.plot.CategoryPlot plot = (org.jfree.chart.plot.CategoryPlot) chart.getPlot();
            styleAxis(plot.getDomainAxis());
            styleAxis(plot.getRangeAxis());
            plot.setDomainGridlinePaint(BORDER);
            plot.setRangeGridlinePaint(BORDER);
        }

        // Pie plot
        if (chart.getPlot() instanceof org.jfree.chart.plot.PiePlot) {
            org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) chart.getPlot();
            plot.setLabelPaint(TEXT_SECONDARY);
            plot.setLabelBackgroundPaint(BG_CARD);
            plot.setLabelOutlinePaint(BORDER);
            plot.setLabelShadowPaint(null);
        }
    }

    private static void styleAxis(org.jfree.chart.axis.Axis axis) {
        if (axis == null) return;
        axis.setLabelPaint(TEXT_SECONDARY);
        axis.setTickLabelPaint(TEXT_MUTED);
        axis.setAxisLinePaint(BORDER);
        axis.setTickMarkPaint(BORDER);
    }

    // -------------------------------------------------------------------------
    // Helper — Buat JPanel card dengan border kiri berwarna
    // -------------------------------------------------------------------------

    /**
     * Membuat panel card dengan border kiri berwarna, background gelap, dan padding.
     *
     * @param accentColor Warna border kiri (identitas kategori card)
     * @return JPanel siap pakai
     */
    public static javax.swing.JPanel createStatCard(Color accentColor) {
        javax.swing.JPanel card = new javax.swing.JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 18, 14, 16));
        return card;
    }

    /**
     * Membuat JButton bergaya sidebar: transparan, teks di kiri, dengan hover teal.
     *
     * @param text  Label tombol
     * @return JButton yang sudah di-style
     */
    public static javax.swing.JButton createSidebarButton(String text) {
        javax.swing.JButton btn = new javax.swing.JButton(text) {
            private boolean active = false;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(ACCENT_SUBTLE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(ACCENT);
                    g2.fillRoundRect(0, 4, 3, getHeight() - 8, 3, 3);
                } else if (getModel().isRollover()) {
                    g2.setColor(BG_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
            public void setActive(boolean a) { this.active = a; repaint(); }
            public boolean isActive()        { return active; }
        };
        btn.setForeground(TEXT_SECONDARY);
        btn.setFont(FONT_BODY);
        btn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btn.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 14, 10, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Membuat JButton aksi utama (filled teal).
     */
    public static javax.swing.JButton createPrimaryButton(String text) {
        javax.swing.JButton btn = new javax.swing.JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DIM : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font(FONT, Font.BOLD, 12));
        btn.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Membuat JButton outline (border teal, background transparan).
     */
    public static javax.swing.JButton createOutlineButton(String text) {
        javax.swing.JButton btn = new javax.swing.JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(ACCENT_SUBTLE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(ACCENT);
        btn.setFont(new Font(FONT, Font.PLAIN, 12));
        btn.setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 16, 7, 16));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Menerapkan dark style ke JTable.
     */
    public static void styleTable(javax.swing.JTable table) {
        table.setBackground(BG_PANEL);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(new Color(29, 158, 117, 60));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER);
        table.setRowHeight(32);
        table.setFont(FONT_BODY);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        // Header
        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setBackground(BG_HEADER);
        header.setForeground(TEXT_SECONDARY);
        header.setFont(new Font(FONT, Font.BOLD, 12));
        header.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);

        // Alternate row renderer
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    javax.swing.JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setFont(FONT_BODY);
                setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    setBackground(new Color(29, 158, 117, 60));
                    setForeground(TEXT_PRIMARY);
                } else {
                    setBackground(row % 2 == 0 ? BG_PANEL : BG_CARD);
                    setForeground(TEXT_PRIMARY);
                }
                return this;
            }
        });
    }

    /**
     * Menerapkan dark style ke JScrollPane.
     */
    public static void styleScrollPane(javax.swing.JScrollPane sp) {
        sp.setBackground(BG_PANEL);
        sp.getViewport().setBackground(BG_PANEL);
        sp.setBorder(javax.swing.BorderFactory.createLineBorder(BORDER, 1));
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        sp.getVerticalScrollBar().setBackground(BG_PANEL);
    }
}
