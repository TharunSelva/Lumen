package com.energyapp.utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UIUtilsTest {

    @Test
    public void testBindOnOffIndicator() {
        Circle testCircle = new Circle();
        SimpleBooleanProperty testProperty = new SimpleBooleanProperty(false);

        UIUtils.bindOnOffIndicator(testCircle, testProperty);
        assertEquals(Color.RED, testCircle.getFill());

        testProperty.set(true);
        assertEquals(Color.GREEN, testCircle.getFill());

        testProperty.set(false);
        assertEquals(Color.RED, testCircle.getFill());
    }

    @Test
    public void testBindOnOffIndicator_InitiallyOn() {
        Circle testCircle = new Circle();
        SimpleBooleanProperty testProperty = new SimpleBooleanProperty(true);

        UIUtils.bindOnOffIndicator(testCircle, testProperty);
        assertEquals(Color.GREEN, testCircle.getFill());
    }

    @Test
    public void testBindOnOffIndicator_InitiallyOff() {
        Circle testCircle = new Circle();
        SimpleBooleanProperty testProperty = new SimpleBooleanProperty(false);

        UIUtils.bindOnOffIndicator(testCircle, testProperty);
        assertEquals(Color.RED, testCircle.getFill());
    }

    @Test
    public void testBindOnOffIndicator_MultipleToggles() {
        Circle testCircle = new Circle();
        SimpleBooleanProperty testProperty = new SimpleBooleanProperty(false);

        UIUtils.bindOnOffIndicator(testCircle, testProperty);

        // Multiple toggles
        for (int i = 0; i < 5; i++) {
            testProperty.set(true);
            assertEquals(Color.GREEN, testCircle.getFill());
            testProperty.set(false);
            assertEquals(Color.RED, testCircle.getFill());
        }
    }

    @Test
    public void testBindOnOffIndicator_IndependentCircles() {
        Circle circle1 = new Circle();
        Circle circle2 = new Circle();
        SimpleBooleanProperty property1 = new SimpleBooleanProperty(true);
        SimpleBooleanProperty property2 = new SimpleBooleanProperty(false);

        UIUtils.bindOnOffIndicator(circle1, property1);
        UIUtils.bindOnOffIndicator(circle2, property2);

        assertEquals(Color.GREEN, circle1.getFill());
        assertEquals(Color.RED, circle2.getFill());

        // Change one, the other should not be affected
        property1.set(false);
        assertEquals(Color.RED, circle1.getFill());
        assertEquals(Color.RED, circle2.getFill());

        property2.set(true);
        assertEquals(Color.RED, circle1.getFill());
        assertEquals(Color.GREEN, circle2.getFill());
    }

    @Test
    public void testBindOnOffIndicator_SameStateTwice() {
        Circle testCircle = new Circle();
        SimpleBooleanProperty testProperty = new SimpleBooleanProperty(true);

        UIUtils.bindOnOffIndicator(testCircle, testProperty);
        assertEquals(Color.GREEN, testCircle.getFill());

        // Set to same state
        testProperty.set(true);
        assertEquals(Color.GREEN, testCircle.getFill());

        testProperty.set(false);
        assertEquals(Color.RED, testCircle.getFill());

        // Set to same state again
        testProperty.set(false);
        assertEquals(Color.RED, testCircle.getFill());
    }

    @Test
    public void testBindOnOffIndicator_BindingIsPersistent() {
        Circle testCircle = new Circle();
        SimpleBooleanProperty testProperty = new SimpleBooleanProperty(false);

        UIUtils.bindOnOffIndicator(testCircle, testProperty);

        // Verify binding persists through multiple state changes
        testProperty.set(true);
        assertEquals(Color.GREEN, testCircle.getFill());

        testProperty.set(false);
        assertEquals(Color.RED, testCircle.getFill());

        testProperty.set(true);
        assertEquals(Color.GREEN, testCircle.getFill());
    }

    @Test
    public void testBindOnOffIndicator_CircleStaysUpdated() {
        Circle testCircle = new Circle();
        SimpleBooleanProperty testProperty = new SimpleBooleanProperty(false);

        UIUtils.bindOnOffIndicator(testCircle, testProperty);

        assertEquals(Color.RED, testCircle.getFill());

        // Simulate rapid changes
        testProperty.set(true);
        testProperty.set(false);
        testProperty.set(true);

        assertEquals(Color.GREEN, testCircle.getFill());
    }

}