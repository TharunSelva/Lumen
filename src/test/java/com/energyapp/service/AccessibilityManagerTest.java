package com.energyapp.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AccessibilityManagerTest {

    @BeforeEach
    void setUp() {
        // Reset to defaults before each test
        AccessibilityManager.setCompactView(false);
        AccessibilityManager.setShowAnimations(true);
        AccessibilityManager.setLargeText(false);
        AccessibilityManager.setFontSize(14.0);
        AccessibilityManager.setCustomFontBold(false);
        AccessibilityManager.setCustomFontFamily("System");
        AccessibilityManager.setScreenReaderEnabled(false);
    }

    @Test
    void testCompactViewProperty() {
        AccessibilityManager.setCompactView(true);
        assertTrue(AccessibilityManager.isCompactView().get(), "Compact view should be enabled");
    }

    @Test
    void testShowAnimationsProperty() {
        AccessibilityManager.setShowAnimations(false);
        assertFalse(AccessibilityManager.animationsShowing().get(), "Animations should be disabled");
    }

    @Test
    void testLargeTextProperty() {
        AccessibilityManager.setLargeText(true);
        assertTrue(AccessibilityManager.isLargeText().get(), "Large text should be enabled");
    }

    @Test
    void testCustomFontProperties() {
        AccessibilityManager.setFontSize(18.5);
        AccessibilityManager.setCustomFontBold(true);
        AccessibilityManager.setCustomFontFamily("Arial");

        assertEquals(18.5, AccessibilityManager.customFontSizeProperty().get());
        assertTrue(AccessibilityManager.customFontBoldProperty().get());
        assertEquals("Arial", AccessibilityManager.customFontFamilyProperty().get());
    }

    @Test
    void testScreenReaderProperty() {
        AccessibilityManager.setScreenReaderEnabled(true);
        assertTrue(AccessibilityManager.screenReaderProperty().get(), "Screen reader should be enabled");
    }
}