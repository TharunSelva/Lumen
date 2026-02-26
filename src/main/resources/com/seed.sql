-- Seed User Profile
INSERT INTO home_profile (home_id, first_name, timezone, postcode_outcode, last_home_refresh_at)
VALUES ('HOME_1', 'Richard', 'Europe/London', 'SO14', CURRENT_TIMESTAMP);

-- Seed Energy Assets (so the app knows you have Solar/EV for recommendations)
INSERT INTO home_energy_assets (home_id, has_ev, has_solar, has_battery, battery_capacity_kwh, min_soc_pct, max_discharge_kw)
VALUES ('HOME_1', TRUE, TRUE, TRUE, 10.5, 20, 5.0);

-- Seed Default Devices
INSERT INTO device (device_id, home_id, name, category, typical_power_w, is_shiftable, status) VALUES
('DEV_EV_1', 'HOME_1', 'Tesla Model 3', 'EV', 7000.0, TRUE, 'IDLE'),
('DEV_WM_1', 'HOME_1', 'Washing Machine', 'LAUNDRY', 2000.0, TRUE, 'IDLE'),
('DEV_FR_1', 'HOME_1', 'Smart Fridge', 'APPLIANCE', 150.0, FALSE, 'ON');

-- Seed Recommendation Templates
INSERT INTO recommendation_template (template_id, type, title_tpl, body_tpl, why_tpl, action_label) VALUES
('TPL_EV_NIGHT', 'TONIGHT', 'Charge your EV tonight', 'Schedule your EV to charge between 02:00 and 05:00.', 'Lowest tariff + low carbon intensity.', 'Schedule charging'),
('TPL_SOLAR_DAY', 'SOLAR', 'Solar Output is High', 'Run your washing machine now to use free solar energy.', 'High solar generation predicted.', 'Shift dishwasher');

-- Seed Quick Tips for the Marquee
INSERT INTO quick_tip_catalog (title, body) VALUES
('Fridge Temp', 'Closing the fridge door quickly saves up to 10% of its energy.'),
('Phantom Load', 'Turning off devices at the wall can save £40 a year.');