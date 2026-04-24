package com.energyapp.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;

public final class ThemeController {

    private static final String APP_STYLESHEET = "/com/css/app.css";
    private static final String APP_ROOT_CLASS = "app-root";
    private static final String DARK_CLASS = "dark";
    private static final String HIGH_CONTRAST_CLASS = "high-contrast";

    private static final BooleanProperty darkModeEnabled = new SimpleBooleanProperty(false);
    private static final BooleanProperty highContrastEnabled = new SimpleBooleanProperty(false);

    ThemeController() {
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
        darkModeEnabled.set(enabled);
        applyCurrentTheme(scene);
    }

    public static boolean isDarkModeEnabled() {
        return darkModeEnabled.get();
    }

    public static BooleanProperty darkModeProperty() {
        return darkModeEnabled;
    }

    public static void setHighContrast(Scene scene, boolean enabled) {
        highContrastEnabled.set(enabled);
        applyCurrentTheme(scene);
    }

    public static boolean isHighContrastEnabled() {
        return highContrastEnabled.get();
    }

    public static BooleanProperty highContrastProperty() {
        return highContrastEnabled;
    }

    private static void applyCurrentTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        var styleClasses = scene.getRoot().getStyleClass();

        if (darkModeEnabled.get()) {
            if (!styleClasses.contains(DARK_CLASS)) {
                styleClasses.add(DARK_CLASS);
            }
        } else {
            styleClasses.remove(DARK_CLASS);
        }

        if (highContrastEnabled.get()) {
            if (!styleClasses.contains(HIGH_CONTRAST_CLASS)) {
                styleClasses.add(HIGH_CONTRAST_CLASS);
            }
        } else {
            styleClasses.remove(HIGH_CONTRAST_CLASS);
        }
    }
}
