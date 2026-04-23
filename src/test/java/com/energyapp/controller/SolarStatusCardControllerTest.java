package com.energyapp.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolarStatusCardControllerTest {

    @Test
    void solarKwFromHalfHourKwh_convertsEnergyToAveragePower() {
        assertEquals(1.6, SolarStatusCardController.solarKwFromHalfHourKwh(0.8), 0.0001);
    }

    @Test
    void capacityProgress_clampsToSaneRange() {
        assertEquals(0.0, SolarStatusCardController.capacityProgress(1.0, 0.0), 0.0001);
        assertEquals(0.5, SolarStatusCardController.capacityProgress(2.0, 4.0), 0.0001);
        assertEquals(1.0, SolarStatusCardController.capacityProgress(8.0, 4.0), 0.0001);
    }

    @Test
    void statusStyleClassFor_handlesNotGeneratingExplicitly() {
        assertEquals("status-pill-idle",
                SolarStatusCardController.statusStyleClassFor("Status: Not Generating"));
        assertEquals("status-pill-optimal",
                SolarStatusCardController.statusStyleClassFor("Status: Optimal"));
        assertEquals("status-pill-underperforming",
                SolarStatusCardController.statusStyleClassFor("Status: Underperforming"));
    }
}
