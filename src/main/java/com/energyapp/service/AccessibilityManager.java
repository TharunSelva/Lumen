package com.energyapp.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class AccessibilityManager {

    private static BooleanProperty compactView = new SimpleBooleanProperty(false);
    private static BooleanProperty showAnimations = new SimpleBooleanProperty(true);
    private static BooleanProperty largeText = new SimpleBooleanProperty(false);

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
}
