package com.energyapp.controller;

import com.energyapp.model.Recommendation;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecommendationCardControllerTest {

    @BeforeAll
    static void initJfx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
    }

    @Test
    void cardClickRunsNavigationHandler() throws Exception {
        RecommendationCardController controller = new RecommendationCardController();
        VBox root = new VBox();
        Label title = new Label();
        Label body = new Label();
        Label timestamp = new Label();

        inject(controller, "recommendationCardRoot", root);
        inject(controller, "recTitleLabel", title);
        inject(controller, "recExplanationLabel", body);
        inject(controller, "recTimestampLabel", timestamp);

        AtomicBoolean called = new AtomicBoolean(false);
        controller.setOnNavigate(() -> called.set(true));
        controller.initialize();
        controller.setRecommendation(new Recommendation(
                "id", "Title", "Body", record -> true, LocalDateTime.of(2026, 3, 31, 12, 30), true
        ));

        root.getOnMouseClicked().handle(null);

        assertTrue(called.get());
        assertEquals("Title", title.getText());
        assertTrue(timestamp.getText().contains("12:30"));
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
