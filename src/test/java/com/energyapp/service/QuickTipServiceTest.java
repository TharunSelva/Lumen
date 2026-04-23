package com.energyapp.service;

import com.energyapp.db.QuickTipRepository;
import com.energyapp.model.QuickTip;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class QuickTipServiceTest {

    @Test
    void repositoryBackedServiceLoadsTips() {
        QuickTipService service = new QuickTipService(new QuickTipRepository(), new Random(42));
        assertFalse(service.getAllTips().isEmpty());
    }

    @Test
    void continuousMarqueeIncludesEntireTipList() {
        QuickTipRepository repository = new QuickTipRepository() {
            @Override
            public List<QuickTip> fetchAllTips() {
                return List.of(
                        new QuickTip(1, "Tip1", "Body1"),
                        new QuickTip(2, "Tip2", "Body2"),
                        new QuickTip(3, "Tip3", "Body3"),
                        new QuickTip(4, "Tip4", "Body4"),
                        new QuickTip(5, "Tip5", "Body5"),
                        new QuickTip(6, "Tip6", "Body6"),
                        new QuickTip(7, "Tip7", "Body7"),
                        new QuickTip(8, "Tip8", "Body8"),
                        new QuickTip(9, "Tip9", "Body9"),
                        new QuickTip(10, "Tip10", "Body10"),
                        new QuickTip(11, "Tip11", "Body11")
                );
            }
        };

        QuickTipService service = new QuickTipService(repository, new Random(1));
        String text = service.getContinuousMarqueeText();
        assertEquals(11, text.split("   \\|   ").length);
    }
}
