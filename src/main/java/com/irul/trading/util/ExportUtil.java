package com.irul.trading.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportUtil {

    public static void exportToExcel(String filePath) {
        String sql = "SELECT * FROM trades ORDER BY id";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safePath = filePath.endsWith(".xlsx") ? filePath : filePath.replace(".csv", "_" + timestamp + ".xlsx");

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(safePath)) {

            Sheet sheet = workbook.createSheet("Trading Journal");
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Header
            Row headerRow = sheet.createRow(0);
            for (int i = 1; i <= columnCount; i++) {
                Cell cell = headerRow.createCell(i - 1);
                cell.setCellValue(metaData.getColumnName(i));
            }

            // Data
            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 1; i <= columnCount; i++) {
                    Cell cell = row.createCell(i - 1);
                    Object value = rs.getObject(i);
                    cell.setCellValue(value != null ? value.toString() : "");
                }
            }

            // Auto-size column + padding
            for (int i = 0; i < columnCount; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 512);
            }

            workbook.write(fileOut);
            JOptionPane.showMessageDialog(null,
                    "Export Excel berhasil ke:\n" + safePath,
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Export gagal: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}