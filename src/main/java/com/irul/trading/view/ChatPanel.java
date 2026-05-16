package com.irul.trading.view;

import com.irul.trading.util.AIClient;
import com.irul.trading.util.AIVisionClient;
import com.irul.trading.util.DebugLogger;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

/**
 * ChatPanel — UI percakapan antara user dan asisten AI "Nara".
 *
 * Arsitektur utama:
 *  - Container pesan menggunakan JPanel + BoxLayout(Y_AXIS), bukan JList.
 *    Ini memungkinkan setiap bubble tumbuh secara natural mengikuti panjang teks.
 *  - Lebar bubble dibatasi maksimal 68% lebar panel via override getPreferredSize().
 *  - Input menggunakan JTextArea (bukan JTextField) agar bisa multi-baris
 *    dan auto-resize hingga batas maksimal tinggi tertentu.
 *  - Typing indicator animasi 3 titik via javax.swing.Timer.
 */
public class ChatPanel extends JPanel {

    // -------------------------------------------------------------------------
    // Color Palette — dark theme sesuai mockup
    // -------------------------------------------------------------------------
    private static final Color C_BG_OUTER   = new Color(18,  18,  22);
    private static final Color C_BG_PANEL   = new Color(26,  27,  32);
    private static final Color C_BG_HEADER  = new Color(22,  23,  28);
    private static final Color C_BG_INPUT   = new Color(34,  36,  42);
    private static final Color C_ACCENT     = new Color(29,  158, 117);
    private static final Color C_ACCENT_DIM = new Color(15,  110, 86);
    private static final Color C_USER_BG    = new Color(29,  158, 117);
    private static final Color C_USER_TEXT  = new Color(225, 245, 238);
    private static final Color C_AI_BG      = new Color(42,  46,  55);
    private static final Color C_AI_TEXT    = new Color(220, 222, 230);
    private static final Color C_MUTED      = new Color(110, 115, 130);
    private static final Color C_BORDER     = new Color(52,  55,  65);
    private static final Color C_DOT_ACTIVE = new Color(150, 155, 170);
    private static final Color C_DOT_IDLE   = new Color(65,  70,  82);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private JPanel         chatBody;
    private JScrollPane    scrollPane;
    private JTextArea      inputArea;
    private JButton        sendBtn;
    private JButton        uploadBtn;
    private AIClient       textClient;
    private AIVisionClient visionClient;
    private File           currentImageFile;

    // Typing indicator
    private JPanel   typingRow;
    private Timer    typingTimer;
    private int      typingPhase = 0;
    private JLabel[] typingDots  = new JLabel[3];

    // =========================================================================
    // Constructor
    // =========================================================================

    public ChatPanel() {
        textClient   = new AIClient();
        visionClient = new AIVisionClient();
        setLayout(new BorderLayout());
        setBackground(C_BG_OUTER);
        initUI();
        addWelcomeMessage();
    }

    // =========================================================================
    // UI Construction
    // =========================================================================

    private void initUI() {
        add(buildHeader(),    BorderLayout.NORTH);
        add(buildChatArea(),  BorderLayout.CENTER);
        add(buildInputArea(), BorderLayout.SOUTH);
    }

    /** Header: avatar lingkaran, nama Nara, status online */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(C_BG_HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Kiri: avatar + info teks
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel avatar = new JLabel("N") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setForeground(C_USER_TEXT);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setOpaque(false);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel("Nara");
        nameLabel.setForeground(new Color(220, 222, 230));
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel subLabel = new JLabel("AI Trading Assistant");
        subLabel.setForeground(C_MUTED);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        info.add(nameLabel);
        info.add(subLabel);

        left.add(avatar);
        left.add(info);
        header.add(left, BorderLayout.WEST);

        // Kanan: status dot hijau
        JLabel statusDot = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ACCENT);
                g2.fillOval(0, 4, 9, 9);
                g2.dispose();
            }
        };
        statusDot.setPreferredSize(new Dimension(9, 17));
        statusDot.setToolTipText("Online");
        header.add(statusDot, BorderLayout.EAST);

        return header;
    }

    /** Area scrollable tempat semua bubble pesan ditampilkan */
    private JScrollPane buildChatArea() {
        chatBody = new JPanel();
        chatBody.setLayout(new BoxLayout(chatBody, BoxLayout.Y_AXIS));
        chatBody.setBackground(C_BG_PANEL);
        chatBody.setBorder(BorderFactory.createEmptyBorder(14, 12, 8, 12));

        scrollPane = new JScrollPane(chatBody);
        scrollPane.setBackground(C_BG_PANEL);
        scrollPane.getViewport().setBackground(C_BG_PANEL);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));

        // Revalidate bubble width saat window di-resize
        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                chatBody.revalidate();
                chatBody.repaint();
            }
        });

        return scrollPane;
    }

    /** Panel input bawah: tombol upload + textarea + tombol kirim */
    private JPanel buildInputArea() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(C_BG_HEADER);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
            BorderFactory.createEmptyBorder(10, 12, 14, 12)
        ));

        JPanel inputBar = new JPanel(new BorderLayout(8, 0));
        inputBar.setBackground(C_BG_INPUT);
        inputBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 8)
        ));

        // Tombol upload (kiri)
        uploadBtn = buildIconButton("Upload Chart");
        uploadBtn.addActionListener(e -> pickImage());
        inputBar.add(uploadBtn, BorderLayout.WEST);

        // Textarea auto-resize (tengah)
        inputArea = new JTextArea(1, 1);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(C_BG_INPUT);
        inputArea.setForeground(new Color(220, 222, 230));
        inputArea.setCaretColor(C_ACCENT);
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputArea.setBorder(null);
        inputArea.setOpaque(false);

        applyPlaceholder(inputArea, "Tanya Nara tentang trading...");

        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { adjustInputHeight(); }
            @Override public void removeUpdate(DocumentEvent e)  { adjustInputHeight(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });

        // Enter = kirim, Shift+Enter = baris baru
        inputArea.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });

        inputBar.add(inputArea, BorderLayout.CENTER);

        // Tombol kirim (kanan)
        sendBtn = buildSendButton();
        sendBtn.addActionListener(e -> sendMessage());
        inputBar.add(sendBtn, BorderLayout.EAST);

        outer.add(inputBar, BorderLayout.CENTER);
        return outer;
    }

    // =========================================================================
    // Helper Builder
    // =========================================================================

    private JButton buildIconButton(String tooltip) {
        JButton btn = new JButton("\uD83D\uDCF7") {
            @Override protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(60, 65, 78));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btn.setForeground(C_MUTED);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 8));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton buildSendButton() {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled()
                    ? (getModel().isRollover() ? C_ACCENT_DIM : C_ACCENT)
                    : new Color(50, 55, 65));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Ikon panah kirim
                g2.setColor(isEnabled() ? Color.WHITE : C_MUTED);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawLine(cx - 5, cy + 4, cx + 1, cy - 4);
                g2.drawLine(cx + 1, cy - 4, cx + 7, cy + 4);
                g2.drawLine(cx + 1, cy - 4, cx + 1, cy + 6);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setBorder(null);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setToolTipText("Kirim (Enter)");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void applyPlaceholder(JTextArea area, String placeholder) {
        area.setText(placeholder);
        area.setForeground(C_MUTED);
        area.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (isPlaceholderActive()) {
                    area.setText("");
                    area.setForeground(new Color(220, 222, 230));
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (area.getText().trim().isEmpty()) {
                    area.setForeground(C_MUTED);
                    area.setText(placeholder);
                }
            }
        });
    }

    private boolean isPlaceholderActive() {
        return inputArea.getForeground().equals(C_MUTED);
    }

    /** Sesuaikan tinggi textarea: min 1 baris, maks 5 baris */
    private void adjustInputHeight() {
        if (isPlaceholderActive()) return;
        int lineH    = inputArea.getFontMetrics(inputArea.getFont()).getHeight();
        int lines    = Math.min(inputArea.getLineCount(), 5);
        int newH     = lines * lineH + 6;
        Dimension d  = inputArea.getPreferredSize();
        if (d.height != newH) {
            inputArea.setPreferredSize(new Dimension(d.width, newH));
            revalidate();
        }
    }

    // =========================================================================
    // Pesan — Pengelolaan Bubble
    // =========================================================================

    private void addWelcomeMessage() {
        addDateDivider("Hari ini");
        appendMessage(
            "Halo! Saya Nara, asisten trading Anda.\n\n"
            + "Anda bisa tanya tentang:\n"
            + "• Harga & analisis XAUUSDm / BTCUSDm\n"
            + "• Tren pasar dan skenario bullish/bearish\n"
            + "• Evaluasi performa akun Anda\n"
            + "• Saran manajemen risiko\n"
            + "• Upload chart untuk analisis visual\n\n"
            + "Pastikan EA MT5 sudah berjalan agar data pasar tersedia.",
            false
        );
    }

    private void addDateDivider(String label) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 10, 0));

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setForeground(C_MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        row.add(makeSeparator(), BorderLayout.WEST);
        row.add(lbl,            BorderLayout.CENTER);
        row.add(makeSeparator(), BorderLayout.EAST);

        chatBody.add(row);
    }

    private JSeparator makeSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(C_BORDER);
        sep.setBackground(C_BG_PANEL);
        sep.setPreferredSize(new Dimension(80, 1));
        return sep;
    }

    private void appendMessage(String text, boolean isUser) {
        MessageRow row = new MessageRow(text, isUser, scrollPane);
        chatBody.add(row);
        chatBody.add(Box.createVerticalStrut(6));
        chatBody.revalidate();
        chatBody.repaint();
        scrollToBottom();
    }

    private void replaceLastBubble(String text) {
        stopTypingIndicator();
        appendMessage(text, false);
    }

    // =========================================================================
    // Typing Indicator
    // =========================================================================

    private void showTypingIndicator() {
        typingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        typingRow.setOpaque(false);
        typingRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        // Avatar kecil Nara
        JLabel av = new JLabel("N") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        av.setForeground(C_USER_TEXT);
        av.setFont(new Font("Segoe UI", Font.BOLD, 10));
        av.setHorizontalAlignment(SwingConstants.CENTER);
        av.setPreferredSize(new Dimension(26, 26));
        av.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        typingRow.add(av);

        // Bubble dots
        JPanel bubble = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_AI_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        for (int i = 0; i < 3; i++) {
            typingDots[i] = new JLabel("●");
            typingDots[i].setFont(new Font("Segoe UI", Font.PLAIN, 9));
            typingDots[i].setForeground(C_DOT_IDLE);
            bubble.add(typingDots[i]);
        }
        typingRow.add(bubble);

        chatBody.add(typingRow);
        chatBody.add(Box.createVerticalStrut(6));
        chatBody.revalidate();
        scrollToBottom();

        typingPhase = 0;
        typingTimer = new Timer(380, e -> {
            for (int i = 0; i < 3; i++) {
                typingDots[i].setForeground(i == typingPhase % 3 ? C_DOT_ACTIVE : C_DOT_IDLE);
            }
            typingPhase++;
            chatBody.repaint();
        });
        typingTimer.start();
    }

    private void stopTypingIndicator() {
        if (typingTimer != null) { typingTimer.stop(); typingTimer = null; }
        if (typingRow != null) {
            Component[] comps = chatBody.getComponents();
            for (int i = 0; i < comps.length; i++) {
                if (comps[i] == typingRow) {
                    chatBody.remove(i);
                    if (i < chatBody.getComponentCount()) chatBody.remove(i);
                    break;
                }
            }
            typingRow = null;
            chatBody.revalidate();
            chatBody.repaint();
        }
    }

    // =========================================================================
    // Kirim Pesan
    // =========================================================================

    private void sendMessage() {
        String text = isPlaceholderActive() ? "" : inputArea.getText().trim();
        if (text.isEmpty() && currentImageFile == null) return;

        DebugLogger.log("post-fix", "H8", "ChatPanel.java", "sendMessage entered",
            new JSONObject().put("question", text).put("hasImage", currentImageFile != null));

        setInputEnabled(false);

        // Reset input field
        inputArea.setText("");
        inputArea.setForeground(C_MUTED);
        adjustInputHeight();

        if (currentImageFile != null) {
            handleImageMessage(text);
        } else {
            handleTextMessage(text);
        }
    }

    private void handleTextMessage(String question) {
        appendMessage(question, true);
        showTypingIndicator();

        boolean needsAnalysis = isAnalysisIntent(question.toLowerCase());

        DebugLogger.log("post-fix", "H9", "ChatPanel.java", "Routing decision",
            new JSONObject().put("needsAnalysis", needsAnalysis));

        AIClient.Callback cb = new AIClient.Callback() {
            @Override public void onSuccess(String result) {
                SwingUtilities.invokeLater(() -> {
                    replaceLastBubble(result);
                    setInputEnabled(true);
                });
            }
            @Override public void onFailure(String error) {
                SwingUtilities.invokeLater(() -> {
                    replaceLastBubble(buildErrorMessage(error));
                    setInputEnabled(true);
                });
            }
        };

        if (needsAnalysis) {
            textClient.askWithFunctionCalling(question, cb);
        } else {
            textClient.askWithContext(question, cb);
        }
    }

    private void handleImageMessage(String question) {
        String display = question.isEmpty() ? "[Analisis chart ini]" : question;
        appendMessage(display + "\n📎 " + currentImageFile.getName(), true);
        showTypingIndicator();

        File img = currentImageFile;
        currentImageFile = null;

        try {
            String b64 = AIVisionClient.encodeImageToBase64(img);
            visionClient.askWithImage(display, b64, new AIVisionClient.Callback() {
                @Override public void onSuccess(String result) {
                    SwingUtilities.invokeLater(() -> {
                        replaceLastBubble(result);
                        setInputEnabled(true);
                    });
                }
                @Override public void onFailure(String error) {
                    SwingUtilities.invokeLater(() -> {
                        replaceLastBubble(buildErrorMessage(error));
                        setInputEnabled(true);
                    });
                }
            });
        } catch (Exception e) {
            stopTypingIndicator();
            appendMessage("Gagal memuat gambar: " + e.getMessage(), false);
            setInputEnabled(true);
        }
    }

    private void pickImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image files", "jpg", "jpeg", "png", "gif", "bmp"));
        fc.setDialogTitle("Pilih chart untuk dianalisis");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentImageFile = fc.getSelectedFile();
            appendMessage("Chart siap dianalisis: " + currentImageFile.getName(), true);
        }
    }

    // =========================================================================
    // Routing Intent
    // =========================================================================

    private boolean isAnalysisIntent(String msg) {
        String[] keywords = {
            "harga", "price", "xauusd", "gold", "emas", "btc", "bitcoin", "market", "pasar",
            "analisis", "analisa", "analysis", "prediksi", "predict", "forecast",
            "tren", "trend", "arah", "naik", "turun",
            "open", "close", "entry", "exit", "beli", "jual", "buy", "sell", "posisi",
            "rekomendasi", "saran", "advice", "sinyal", "signal", "setup",
            "bullish", "bearish", "sideways", "support", "resistance"
        };
        for (String kw : keywords) {
            if (msg.contains(kw)) return true;
        }
        return false;
    }

    // =========================================================================
    // UI Utilities
    // =========================================================================

    private void setInputEnabled(boolean enabled) {
        inputArea.setEnabled(enabled);
        sendBtn.setEnabled(enabled);
        uploadBtn.setEnabled(enabled);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private String buildErrorMessage(String raw) {
        if (raw != null && raw.contains("429")) {
            return "Terlalu banyak permintaan. Tunggu beberapa detik lalu coba lagi.";
        }
        if (raw != null && (raw.contains("Koneksi") || raw.contains("connect"))) {
            return "Tidak dapat terhubung ke server AI.\nPeriksa koneksi internet Anda.\n\nDetail: " + raw;
        }
        return "Terjadi kesalahan saat memproses permintaan.\n\nDetail: " + raw;
    }

    // =========================================================================
    // Inner Class — MessageRow
    // =========================================================================

    /**
     * Satu baris pesan dalam chat: opsional avatar (AI) + bubble teks.
     *
     * Setiap MessageRow memiliki satu BubblePanel yang lebar maksimalnya
     * dibatasi 68% dari viewport scrollPane.
     */
    private static class MessageRow extends JPanel {

        private final boolean    isUser;
        private final JTextArea  textArea;
        private final JScrollPane parentScroll;

        MessageRow(String text, boolean isUser, JScrollPane parentScroll) {
            this.isUser       = isUser;
            this.parentScroll = parentScroll;

            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

            textArea = new JTextArea(text);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setFocusable(false);
            textArea.setOpaque(false);
            textArea.setBorder(null);
            textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            textArea.setForeground(isUser ? C_USER_TEXT : C_AI_TEXT);

            BubblePanel bubble = new BubblePanel(isUser, textArea, parentScroll);
            bubble.setLayout(new BorderLayout());
            bubble.add(textArea, BorderLayout.CENTER);

            // Avatar hanya untuk pesan AI (kiri)
            if (!isUser) {
                JLabel avatar = buildAvatar();
                JPanel avWrap = new JPanel(new BorderLayout());
                avWrap.setOpaque(false);
                avWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
                avWrap.add(avatar, BorderLayout.SOUTH);
                add(avWrap, BorderLayout.WEST);
            }

            // Posisi bubble: user = kanan, AI = kiri
            JPanel align = new JPanel(new FlowLayout(
                isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
            align.setOpaque(false);
            align.add(bubble);
            add(align, BorderLayout.CENTER);
        }

        private JLabel buildAvatar() {
            JLabel av = new JLabel("N") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(C_ACCENT);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            av.setForeground(C_USER_TEXT);
            av.setFont(new Font("Segoe UI", Font.BOLD, 10));
            av.setHorizontalAlignment(SwingConstants.CENTER);
            av.setPreferredSize(new Dimension(26, 26));
            av.setOpaque(false);
            return av;
        }
    }

    // =========================================================================
    // Inner Class — BubblePanel
    // =========================================================================

    /**
     * Panel bubble dengan rounded-rect dan lebar maksimal 68% viewport.
     *
     * Cara kerja getPreferredSize() — ini adalah inti dari layout chat di Swing:
     *
     * Masalah: JTextArea tidak tahu seberapa lebar ia akan dirender,
     *          sehingga ia melaporkan preferredSize berdasarkan satu baris panjang
     *          tanpa wrapping — bubble jadi lebar tak terbatas.
     *
     * Solusi: Sebelum query preferredSize, kita set lebar textArea ke innerWidth
     *         (lebar maksimal yang kita inginkan dikurangi padding). Dengan begitu
     *         textArea menghitung ulang berapa banyak baris yang dibutuhkan untuk
     *         wrap di lebar tersebut, dan melaporkan tinggi yang benar.
     */
    private static class BubblePanel extends JPanel {

        private final boolean    isUser;
        private final JTextArea  textArea;
        private final JScrollPane parentScroll;

        private static final int PAD_H  = 14;
        private static final int PAD_V  = 10;
        private static final int RADIUS = 16;

        BubblePanel(boolean isUser, JTextArea textArea, JScrollPane parentScroll) {
            this.isUser       = isUser;
            this.textArea     = textArea;
            this.parentScroll = parentScroll;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(PAD_V, PAD_H, PAD_V, PAD_H));
        }

        @Override
        public Dimension getPreferredSize() {
            int viewWidth = parentScroll.getViewport().getWidth();
            if (viewWidth <= 0) viewWidth = 520;

            // Offset untuk avatar AI + margin chatBody
            int avatarSpace = isUser ? 0 : 34;
            int bodyMargin  = 24;
            int available   = viewWidth - avatarSpace - bodyMargin;

            int maxBubble  = (int)(available * 0.68);
            int innerWidth = maxBubble - PAD_H * 2;
            if (innerWidth < 80) innerWidth = 80;

            // Kunci: set lebar textArea terlebih dahulu agar preferredSize akurat
            textArea.setSize(innerWidth, Short.MAX_VALUE);
            Dimension textPref = textArea.getPreferredSize();

            int w = Math.min(textPref.width + PAD_H * 2, maxBubble);
            int h = textPref.height + PAD_V * 2;
            return new Dimension(w, h);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight(), r = RADIUS;
            g2.setColor(isUser ? C_USER_BG : C_AI_BG);

            // Gambar bubble penuh dulu
            g2.fillRoundRect(0, 0, w, h, r, r);

            // Tumpulkan satu sudut bawah sebagai "ekor" bubble
            // User: pojok kanan-bawah, AI: pojok kiri-bawah
            if (isUser) {
                g2.fillRect(w - r, h - r, r, r);
            } else {
                g2.fillRect(0, h - r, r, r);
            }

            // Border tipis untuk bubble AI
            if (!isUser) {
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}