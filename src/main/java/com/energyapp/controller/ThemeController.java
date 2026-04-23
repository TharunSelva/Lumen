package com.energyapp.controller;

import javafx.scene.Scene;

public final class ThemeController {

    private static final String APP_STYLESHEET = "/com/css/app.css";
    private static final String APP_ROOT_CLASS = "app-root";
    private static final String DARK_CLASS = "dark";
    private static final String HIGH_CONTRAST_CLASS = "high-contrast";

    private static boolean darkModeEnabled = false;
    private static boolean highContrastEnabled = false;

    private ThemeController() {
    }

    public static void applyTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        if (!scene.getRoot().getStyleClass().contains(APP_ROOT_CLASS)) {
            scene.getRoot().getStyleClass().add(APP_ROOT_CLASS);
        }

        var cssUrl = ThemeController.class.getResource(APP_STYLESHEET);
        if (cssUrl != null) {
            String stylesheetUrl = cssUrl.toExternalForm();
            if (!scene.getStylesheets().contains(stylesheetUrl)) {
                scene.getStylesheets().add(stylesheetUrl);
            }
        }

        applyCurrentTheme(scene);
    }

    public static void setDarkMode(Scene scene, boolean enabled) {
        darkModeEnabled = enabled;
        applyCurrentTheme(scene);
    }

    public static boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public static void setHighContrast(Scene scene, boolean enabled) {
        highContrastEnabled = enabled;
        applyCurrentTheme(scene);
    }

    public static boolean isHighContrastEnabled() {
        return highContrastEnabled;
    }

    private static void applyCurrentTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        var styleClasses = scene.getRoot().getStyleClass();

        if (darkModeEnabled) {
            if (!styleClasses.contains(DARK_CLASS)) {
                styleClasses.add(DARK_CLASS);
            }
        } else {
            styleClasses.remove(DARK_CLASS);
        }

        if (highContrastEnabled) {
            if (!styleClasses.contains(HIGH_CONTRAST_CLASS)) {
                styleClasses.add(HIGH_CONTRAST_CLASS);
            }
        } else {
            styleClasses.remove(HIGH_CONTRAST_CLASS);
        }
    }
}