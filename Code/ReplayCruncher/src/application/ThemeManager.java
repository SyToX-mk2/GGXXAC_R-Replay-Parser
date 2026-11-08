package application;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;

public class ThemeManager {
    private static final String THEME_KEY = "theme";
    private static String currentTheme = "light";
    private static final List<WeakReference<Scene>> registeredScenes = new ArrayList<>();

    public static void init(Scene primaryScene) {
        ConfigManager.loadConfig();
        currentTheme = ConfigManager.get(THEME_KEY, "dark");
        registerScene(primaryScene);
        applyThemeToAll();
    }

    public static void registerScene(Scene scene) {
        if (scene == null) return;
        cleanupDeadReferences();
        for (WeakReference<Scene> ref : registeredScenes) {
            if (scene.equals(ref.get())) return;
        }
        registeredScenes.add(new WeakReference<>(scene));
        applyTheme(scene);
    }

    public static void setTheme(String theme) {
        if (theme.equals(currentTheme)) return;
        currentTheme = theme;
        ConfigManager.set(THEME_KEY, theme);
        ConfigManager.saveConfig();
        applyThemeToAll();
    }

    public static String getTheme() {
        return currentTheme;
    }

    public static boolean isDark() {
        return "dark".equals(currentTheme);
    }

    private static void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = isDark() ? "dark-theme.css" : "light-theme.css";
        scene.getStylesheets().add(ThemeManager.class.getResource(css).toExternalForm());
    }

    private static void applyThemeToAll() {
        cleanupDeadReferences();
        for (WeakReference<Scene> ref : registeredScenes) {
            Scene scene = ref.get();
            if (scene != null) {
                applyTheme(scene);
            }
        }
    }

    private static void cleanupDeadReferences() {
        registeredScenes.removeIf(ref -> ref.get() == null);
    }
}