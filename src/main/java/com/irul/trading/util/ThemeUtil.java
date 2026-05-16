package com.irul.trading.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;

public class ThemeUtil {
    public static void applyTheme(String themeName) {
        try {
            switch (themeName) {
                case "Light": UIManager.setLookAndFeel(new FlatLightLaf()); break;
                case "Dark": UIManager.setLookAndFeel(new FlatDarkLaf()); break;
                case "IntelliJ": UIManager.setLookAndFeel(new FlatIntelliJLaf()); break;
                default: UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}