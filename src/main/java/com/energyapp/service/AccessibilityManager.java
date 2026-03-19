package com.energyapp.service;

import javafx.beans.property.*;

public final class AccessibilityManager {

    private static BooleanProperty compactView = new SimpleBooleanProperty(false);
    private static BooleanProperty showAnimations = new SimpleBooleanProperty(true);
    private static BooleanProperty largeText = new SimpleBooleanProperty(false);
    public static DoubleProperty customFontSize = new SimpleDoubleProperty(14.0);
    private static BooleanProperty customFontBold = new SimpleBooleanProperty(false);
    private static StringProperty customFontFamily = new SimpleStringProperty("System");
    private static BooleanProperty screenReaderEnabled = new SimpleBooleanProperty(false);

    public static void setCompactView(boolean cv) {
        compactView.set(cv);
    }

    public static void setShowAnimations(boolean sa) {
        showAnimations.set(sa);
    }

    public static void setLargeText(boolean lt) {
        largeText.set(lt);
    }

    public static BooleanProperty isCompactView() {
        return compactView;
    }

    public static BooleanProperty animationsShowing() {
        return showAnimations;
    }

    public static BooleanProperty isLargeText() {
        return largeText;
    }

    public static DoubleProperty customFontSizeProperty() {
        return customFontSize;
    }

    public static void setFontSize(double fontSize) {
        customFontSize.set(fontSize);
    }

    public static BooleanProperty customFontBoldProperty() {
        return customFontBold;
    }

    public static void setCustomFontBold(boolean isBold) {
        customFontBoldProperty().set(isBold);
    }

    public static StringProperty customFontFamilyProperty() {
        return customFontFamily;
    }

    public static void setCustomFontFamily(String font) {
        customFontFamily.set(font);
    }

    public static BooleanProperty screenReaderProperty() {
        return screenReaderEnabled;
    }

    public static void setScreenReaderEnabled(boolean isEnabled) {
        screenReaderEnabled.set(isEnabled);
    }


}
