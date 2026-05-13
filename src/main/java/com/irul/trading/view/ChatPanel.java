package com.irul.trading.view;

import com.irul.trading.util.AIClient;
import com.irul.trading.util.AIVisionClient;
import com.irul.trading.util.DebugLogger;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import org.json.JSONObject;

public class ChatPanel extends JPanel {
    private JTextField inputField;
    private JButton sendBtn, uploadBtn;
    private AIClient textClient;
    private AIVisionClient visionClient;
    private DefaultListModel<ChatMessage> listModel;
    private JList<ChatMessage> messageList;
    private File currentImageFile;

    public ChatPanel() {
        textClient = new AIClient();
        visionClient = new AIVisionClient();
        setLayout(new BorderLayout());
        setBackground(new Color(18, 18, 18));
        initUI();
        // Tambahkan pesan sambutan
        listModel.addElement(new ChatMessage("Halo! Saya Nara, asisten trading Anda. Ada yang bisa dibantu?", false));
    }

    private void initUI() {
        listModel = new DefaultListModel<>();
        messageList = new JList<>(listModel);
        messageList.setCellRenderer(new ChatMessageRenderer());
        messageList.setBackground(new Color(18, 18, 18));
        messageList.setSelectionBackground(null);
        JScrollPane scroll = new JScrollPane(messageList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(18, 18, 18));
        add(scroll, BorderLayout.CENTER);

        // Panel input
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBackground(new Color(30, 30, 30));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(new Color(45, 45, 45));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        inputField.addActionListener(e -> sendMessage());
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        sendBtn = new JButton("Kirim");
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendBtn.setBackground(new Color(0, 120, 180));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        sendBtn.addActionListener(e -> sendMessage());
        buttonPanel.add(sendBtn);

        uploadBtn = new JButton("📷");
        uploadBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        uploadBtn.setBackground(new Color(60, 60, 60));
        uploadBtn.setForeground(Color.WHITE);
        uploadBtn.setFocusPainted(false);
        uploadBtn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        uploadBtn.addActionListener(e -> uploadImage());
        buttonPanel.add(uploadBtn);

        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void uploadImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif", "bmp"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentImageFile = fc.getSelectedFile();
            listModel.addElement(new ChatMessage("📎 Gambar siap: " + currentImageFile.getName(), true));
            scrollToBottom();
        }
    }

    private void sendMessage() {
        String question = inputField.getText().trim();
        if (question.isEmpty() && currentImageFile == null) return;
        // #region agent log
        DebugLogger.log("pre-fix", "H8", "ChatPanel.java:96", "sendMessage entered",
                new JSONObject().put("question", question).put("hasImage", currentImageFile != null));
        // #endregion

        System.out.println("DEBUG: sendMessage called, question='" + question + "'");

        // Hapus jika hanya ingin uji UI (komentar baris di bawah)
        // testDummy(question);
        // return;

        if (currentImageFile == null) {
            // Pesan teks
            listModel.addElement(new ChatMessage(question, true));
            inputField.setText("");
            listModel.addElement(new ChatMessage("...", false));
            scrollToBottom();

            String lower = question.toLowerCase();
            System.out.println("DEBUG: Calling AI with " + (lower.contains("harga emas") ? "FunctionCalling" : "Context"));
            // #region agent log
            DebugLogger.log("pre-fix", "H9", "ChatPanel.java:112", "chat branch chosen",
                    new JSONObject().put("useFunctionCalling",
                            lower.contains("harga emas") || lower.contains("xauusd")
                                    || lower.contains("prediksi") || lower.contains("open") || lower.contains("close")));
            // #endregion

            if (lower.contains("harga emas") || lower.contains("xauusd") || 
                lower.contains("prediksi") || lower.contains("open") || lower.contains("close")) {
                textClient.askWithFunctionCalling(question, new AIClient.Callback() {
                    @Override public void onSuccess(String result) {
                        System.out.println("DEBUG: AI success, result length=" + result.length());
                        SwingUtilities.invokeLater(() -> replaceLastMessage(result));
                    }
                    @Override public void onFailure(String error) {
                        System.err.println("DEBUG: AI failure: " + error);
                        SwingUtilities.invokeLater(() -> replaceLastMessage("Error: " + error));
                    }
                });
            } else {
                textClient.askWithContext(question, new AIClient.Callback() {
                    @Override public void onSuccess(String result) {
                        System.out.println("DEBUG: AI success, result length=" + result.length());
                        SwingUtilities.invokeLater(() -> replaceLastMessage(result));
                    }
                    @Override public void onFailure(String error) {
                        System.err.println("DEBUG: AI failure: " + error);
                        SwingUtilities.invokeLater(() -> replaceLastMessage("Error: " + error));
                    }
                });
            }
        } else {
            // Ada gambar
            listModel.addElement(new ChatMessage(question + " [gambar: " + currentImageFile.getName() + "]", true));
            inputField.setText("");
            listModel.addElement(new ChatMessage("...", false));
            scrollToBottom();
            try {
                String base64 = AIVisionClient.encodeImageToBase64(currentImageFile);
                visionClient.askWithImage(question, base64, new AIVisionClient.Callback() {
                    @Override public void onSuccess(String result) {
                        SwingUtilities.invokeLater(() -> replaceLastMessage(result));
                    }
                    @Override public void onFailure(String error) {
                        SwingUtilities.invokeLater(() -> replaceLastMessage("Error: " + error));
                    }
                });
            } catch (Exception e) {
                replaceLastMessage("Gagal memuat gambar: " + e.getMessage());
            }
            currentImageFile = null;
        }
    }

    // Untuk uji UI tanpa AI
    private void testDummy(String question) {
        listModel.addElement(new ChatMessage(question, true));
        inputField.setText("");
        listModel.addElement(new ChatMessage("(dummy response, AI tidak dipanggil)", false));
        scrollToBottom();
    }

    private void replaceLastMessage(String text) {
        int last = listModel.getSize() - 1;
        if (last >= 0 && !listModel.get(last).isUser) {
            listModel.remove(last);
        }
        listModel.addElement(new ChatMessage(text, false));
        scrollToBottom();
        System.out.println("DEBUG: replaceLastMessage added '" + text + "'");
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            int last = listModel.getSize() - 1;
            if (last >= 0) messageList.ensureIndexIsVisible(last);
        });
    }

    // Model, renderer, dan border (sama seperti sebelumnya, dengan perbaikan RenderingHints)
    static class ChatMessage {
        String text;
        boolean isUser;
        ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    static class ChatMessageRenderer extends JPanel implements ListCellRenderer<ChatMessage> {
        private JTextArea textArea;
        private Color userBubble = new Color(37, 211, 102);
        private Color aiBubble = new Color(58, 65, 73);
        private Color userText = Color.WHITE;
        private Color aiText = Color.WHITE;

        public ChatMessageRenderer() {
            setLayout(new BorderLayout());
            textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            textArea.setOpaque(true);
            add(textArea, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ChatMessage> list,
                ChatMessage msg, int index, boolean isSelected, boolean cellHasFocus) {
            textArea.setText(msg.text);
            textArea.setForeground(msg.isUser ? userText : aiText);
            textArea.setBackground(msg.isUser ? userBubble : aiBubble);
            int arc = 18;
            textArea.setBorder(new RoundedBorder(arc, msg.isUser ? userBubble : aiBubble));
            removeAll();
            setLayout(new BorderLayout());
            if (msg.isUser) add(textArea, BorderLayout.EAST);
            else add(textArea, BorderLayout.WEST);
            setBackground(new Color(18, 18, 18));
            return this;
        }
    }

    static class RoundedBorder implements Border {
        private int radius;
        private Color color;
        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width-1, height-1, radius, radius);
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 12, 8, 12);
        }
        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}