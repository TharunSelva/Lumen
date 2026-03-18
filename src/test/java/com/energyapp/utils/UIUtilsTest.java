package com.energyapp.utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}