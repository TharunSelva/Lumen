-- Mark schema/seed versioning in app_meta
INSERT OR IGNORE INTO app_meta(meta_key, meta_value) VALUES ('schema_version', '1');
INSERT OR IGNORE INTO app_meta(meta_key, meta_value) VALUES ('seed_applied', 'true');

-- Home profile
INSERT OR IGNORE INTO home_profile(home_id, first_name, timezone, postcode_outcode, lat, lon, created_at)
VALUES ('HOME_1', 'Richard', 'Europe/London', 'SO14', 50.9097, -1.4044, datetime('now'));

-- Assets
INSERT OR IGNORE INTO home_energy_assets(home_id, has_ev, has_solar, has_battery, battery_capacity_kwh, min_soc_pct, max_discharge_kw)
VALUES ('HOME_1', 1, 1, 1, 10.5, 20, 5.0);

-- Default devices (for CSV import + device page)
INSERT OR IGNORE INTO device(
  device_id, home_id, display_name, category, icon_key,
  typical_power_w, typical_cycle_kwh, typical_duration_mins,
  power_state, status, current_draw_w,
  green_energy_only, ready_by_time, policy,
  created_at, updated_at
) VALUES
('DEV_EV_1',   'HOME_1', 'EV Charger',       'EV',       'ev',      7000, 3.5, 120, 'OFF', 'IDLE', 0, 0, NULL, 'MIN_COST', datetime('now'), datetime('now')),
('DEV_WM_1',   'HOME_1', 'Washing Machine',  'LAUNDRY',  'washer',  2000, 1.4,  90, 'OFF', 'IDLE', 0, 0, NULL, 'BALANCED', datetime('now'), datetime('now')),
('DEV_HEAT_1', 'HOME_1', 'Heating',          'HEATING',  'heat',    3000, 2.1, 180, 'OFF', 'IDLE', 0, 0, NULL, 'BALANCED', datetime('now'), datetime('now')),
('DEV_BASE_1', 'HOME_1', 'Baseload',         'BASELOAD', 'base',     300, 0.8, 999, 'ON',  'IDLE', 0, 0, NULL, 'BALANCED', datetime('now'), datetime('now'));

-- Recommendation templates (minimum for now..)
INSERT OR IGNORE INTO recommendation_template(template_id, type, title_tpl, body_tpl, why_tpl, action_label, action_type, device_category, rules_json)
VALUES
('TPL_EV_NIGHT', 'TONIGHT', 'Charge your EV between {{start}}–{{end}}', 'Schedule EV charging overnight.', 'Lowest tariff + low carbon intensity.', 'Schedule charging', 'SCHEDULE_EV', 'EV', '{"requires":"has_ev"}'),
('TPL_SOLAR_RUN', 'SOLAR',  'Solar output is strong tomorrow', 'Run laundry during peak solar.', 'High solar generation expected.', 'Shift laundry', 'SHIFT_APPLIANCE', 'LAUNDRY', '{"requires":"has_solar"}');

-- Tips (more than 2 so marquee doesn’t feel dead)
INSERT OR IGNORE INTO quick_tip_catalog(title, body) VALUES
('Fridge Door', 'Closing the fridge door quickly reduces wasted cooling.'),
('Phantom Load', 'Devices left on standby can still draw power.'),
('Kettle', 'Boiling only the water you need can reduce electricity use.'),
('Laundry', 'Running full loads reduces energy per item washed.'),
('Lights', 'Turning off lights when leaving a room saves energy.');