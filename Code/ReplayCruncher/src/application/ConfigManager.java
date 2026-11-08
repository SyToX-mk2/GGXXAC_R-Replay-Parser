package application;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;



public class ConfigManager {
	
    private static final String CONFIG_FILE = "config.cfg";
    private static Properties properties = new Properties(); 

    public static void loadConfig() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
        }
    }

    public static void saveConfig() {
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            for (String key : properties.stringPropertyNames()) {
            	osw.write(key + "=" + properties.getProperty(key).replace("\\", "\\\\"));
                osw.write(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static void set(String key, String value) {
        if (value == null || value.isEmpty()) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}