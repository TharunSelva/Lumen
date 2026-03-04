package com.energyapp.utils;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public final class UIUtils {

    private UIUtils() {}

    public static void bindOnOffIndicator(Circle circle, BooleanProperty isOnProperty) {
        circle.fillProperty().bind(
                Bindings.when(isOnProperty)
                        .then(Color.GREEN)
                        .otherwise(Color.RED)
        );
    }
}
