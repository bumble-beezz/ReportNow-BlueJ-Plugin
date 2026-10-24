package com.group22.reportnow.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class StudentIdManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".reportnow";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";
    private static final String KEY_STUDENT_ID = "student_id";
    private static final String DEFAULT_ID = "UNKNOWN_STUDENT";

    private final Properties props = new Properties();

    public StudentIdManager() {
        load();
    }

    private void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("[ReportNow] Could not load config: " + e.getMessage());
            }
        }
    }

    private void save() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) dir.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "ReportNow Extension Configuration");
        } catch (IOException e) {
            System.err.println("[ReportNow] Could not save config: " + e.getMessage());
        }
    }

    public String getStudentId() {
        String id = props.getProperty(KEY_STUDENT_ID);
        if (id == null || id.isBlank()) {
            id = promptForStudentId();
            if (id != null && !id.isBlank()) {
                props.setProperty(KEY_STUDENT_ID, id.trim());
                save();
                return id.trim();
            }
            return DEFAULT_ID;
        }
        return id;
    }

    private String promptForStudentId() {
        URL imgURL = getClass().getResource("/reportnow_icon.png");
        ImageIcon icon = null;
        if (imgURL != null) {
            ImageIcon originalIcon = new ImageIcon(imgURL);
            java.awt.Image scaledImage = originalIcon.getImage().getScaledInstance(64, 64, java.awt.Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaledImage);
        }

        return (String) JOptionPane.showInputDialog(
                null,
                "Welcome to ReportNow!\nPlease enter your Student ID for this session:",
                "ReportNow Setup",
                JOptionPane.PLAIN_MESSAGE,
                icon,
                null,
                null
        );
    }
}