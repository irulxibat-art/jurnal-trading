package com.irul.trading.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupUtil {
    public static void backupDatabase(String backupPath) {
        File source = new File("data/trading.db");
        if (!source.exists()) {
            System.err.println("Database tidak ditemukan.");
            return;
        }
        try (FileInputStream fis = new FileInputStream(source); FileOutputStream fos = new FileOutputStream(backupPath)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            System.out.println("Backup berhasil: " + backupPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void autoBackup() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        new File("backup").mkdirs();
        backupDatabase("backup/trading_" + timestamp + ".db");
    }
}