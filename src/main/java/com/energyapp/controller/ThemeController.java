package com.energyapp.controller;

import javafx.scene.Scene;

/**
 * Applies the shared application stylesheet and prepares the root for future themes.
 */
public final class ThemeController {

    private static final String APP_STYLESHEET = "/com/css/app.css";
    private static final String APP_ROOT_CLASS = "app-root";
    private static final String DARK_CLASS = "dark";

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
    }

    public static void setDarkMode(Scene scene, boolean enabled) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        var styleClasses = scene.getRoot().getStyleClass();
        if (enabled) {
            if (!styleClasses.contains(DARK_CLASS)) {
                styleClasses.add(DARK_CLASS);
            }
        } else {
            styleClasses.remove(DARK_CLASS);
        }
    }
}
