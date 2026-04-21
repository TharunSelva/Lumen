PRAGMA foreign_keys = ON;

-- =========================
-- Meta / config
-- =========================
CREATE TABLE IF NOT EXISTS app_meta (
    meta_key   TEXT PRIMARY KEY,
    meta_value TEXT NOT NULL
);

-- =========================
-- Home / profile
-- =========================
CREATE TABLE IF NOT EXISTS home_profile (
    home_id          TEXT PRIMARY KEY,
    first_name       TEXT NOT NULL,
    timezone         TEXT NOT NULL,
    postcode_outcode TEXT,
    lat              REAL,
    lon              REAL,
    weekly_budget    INTEGER,
    created_at       TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS home_energy_assets (
    home_id              TEXT PRIMARY KEY,
    has_ev               INTEGER NOT NULL DEFAULT 0,
    has_solar            INTEGER NOT NULL DEFAULT 0,
    has_battery          INTEGER NOT NULL DEFAULT 0,
    battery_capacity_kwh REAL,
    min_soc_pct          INTEGER,
    max_discharge_kw     REAL,
    FOREIGN KEY (home_id) REFERENCES home_profile(home_id)
);

-- =========================
-- Devices (Option A)
-- =========================
CREATE TABLE IF NOT EXISTS device (
    device_id                 TEXT PRIMARY KEY,
    home_id                   TEXT NOT NULL,
    display_name              TEXT NOT NULL,
    category                  TEXT NOT NULL,
    icon_key                  TEXT,
    typical_power_w           REAL,
    typical_cycle_kwh         REAL,
    typical_duration_mins     INTEGER,

    power_state               TEXT,     -- ON/OFF
    status                    TEXT,     -- IDLE/RUNNING/WAITING_FOR_SOLAR/WAITING_FOR_CHEAP/SCHEDULED
    current_draw_w            REAL,

    green_energy_only         INTEGER NOT NULL DEFAULT 0,
    ready_by_time             TEXT,     -- "07:00"
    policy                    TEXT,     -- MIN_COST/MIN_CO2/MAX_SOLAR/BALANCED

    next_run_window_start     TEXT,
    next_run_window_end       TEXT,
    next_run_est_cost_gbp     REAL,
    next_run_est_co2_kg       REAL,
    next_run_source_solar_pct REAL,
    next_run_source_grid_pct  REAL,

    created_at                TEXT NOT NULL,
    updated_at                TEXT NOT NULL,

    FOREIGN KEY (home_id) REFERENCES home_profile(home_id)
);

-- =========================
-- Energy intervals (replaces CSV as the data source)
-- =========================
CREATE TABLE IF NOT EXISTS energy_interval (
    home_id           TEXT NOT NULL,
    ts_start          TEXT NOT NULL, -- "yyyy-MM-dd HH:mm:ss"
    time_label        TEXT,
    price_p_per_kwh   REAL,
    carbon_gco2_kwh   REAL,
    total_load_kwh    REAL,
    solar_gen_kwh     REAL,
    battery_level_pct INTEGER,
    grid_import_kwh   REAL,
    data_source       TEXT NOT NULL, -- SEED/SIM/API

    PRIMARY KEY (home_id, ts_start),
    FOREIGN KEY (home_id) REFERENCES home_profile(home_id)
);

CREATE TABLE IF NOT EXISTS device_usage_interval (
    home_id   TEXT NOT NULL,
    ts_start  TEXT NOT NULL,
    device_id TEXT NOT NULL,
    kwh       REAL NOT NULL,

    PRIMARY KEY (home_id, ts_start, device_id),
    FOREIGN KEY (home_id) REFERENCES home_profile(home_id),
    FOREIGN KEY (device_id) REFERENCES device(device_id)
);

-- =========================
-- Forecast caches (combined)
-- =========================
CREATE TABLE IF NOT EXISTS forecast_interval_cache (
    home_id              TEXT NOT NULL,
    ts_start             TEXT NOT NULL,
    ts_end               TEXT,
    tariff_p_per_kwh     REAL,
    carbon_gco2_kwh      REAL,
    solar_shortwave_wm2  REAL,
    expected_load_kwh    REAL,
    battery_soc_pct_pred REAL,
    battery_action       TEXT,
    retrieved_at         TEXT NOT NULL,
    source_json          TEXT,

    PRIMARY KEY (home_id, ts_start),
    FOREIGN KEY (home_id) REFERENCES home_profile(home_id)
);

CREATE TABLE IF NOT EXISTS forecast_insight (
    insight_id               INTEGER PRIMARY KEY AUTOINCREMENT,
    home_id                  TEXT NOT NULL,
    type                     TEXT NOT NULL, -- CHEAPEST_WINDOW/PEAK_PRICE/STRONG_SOLAR/PEAK_CO2/BATTERY_DISCHARGE
    title                    TEXT NOT NULL,
    summary                  TEXT,
    detail_body              TEXT,
    window_start             TEXT,
    window_end               TEXT,
    recommended_actions_json TEXT,
    potential_savings_gbp    REAL,
    potential_co2_saving_kg  REAL,
    created_at               TEXT NOT NULL,
    FOREIGN KEY (home_id) REFERENCES home_profile(home_id)
);

-- =========================
-- Generation mix (normalized cache)
-- =========================
CREATE TABLE IF NOT EXISTS grid_generation_mix_interval (
    scope        TEXT NOT NULL,        -- 'national' (or 'regional' later)
    region_id    INTEGER,              -- NULL for national
    ts_start     TEXT NOT NULL,         -- "yyyy-MM-dd HH:mm:ss"
    ts_end       TEXT,
    fuel         TEXT NOT NULL,         -- e.g. 'wind', 'gas'
    perc         REAL NOT NULL,         -- percent
    retrieved_at TEXT NOT NULL,
    PRIMARY KEY (scope, region_id, ts_start, fuel)
);

CREATE INDEX IF NOT EXISTS idx_genmix_time
ON grid_generation_mix_interval(scope, region_id, ts_start);

-- =========================
-- Recommendations + tips
-- =========================
CREATE TABLE IF NOT EXISTS recommendation_template (
    template_id     TEXT PRIMARY KEY,
    type            TEXT NOT NULL, -- PRIORITY/TONIGHT/SOLAR/QUICK
    title_tpl       TEXT NOT NULL,
    body_tpl        TEXT,
    why_tpl         TEXT,
    action_label    TEXT,
    action_type     TEXT,
    device_category TEXT,
    rules_json      TEXT
);

CREATE TABLE IF NOT EXISTS recommendation_instance (
    instance_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    home_id                TEXT NOT NULL,
    template_id            TEXT,
    type                   TEXT,
    title                  TEXT NOT NULL,
    summary                TEXT,
    why_text               TEXT,
    priority               INTEGER DEFAULT 0,
    valid_from             TEXT,
    valid_to               TEXT,
    recommended_start      TEXT,
    recommended_end        TEXT,
    impact_cost_saving_gbp REAL,
    impact_co2_saving_kg   REAL,
    status                 TEXT NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/DISMISSED/ACCEPTED/EXPIRED
    created_at             TEXT NOT NULL,

    FOREIGN KEY (home_id) REFERENCES home_profile(home_id),
    FOREIGN KEY (template_id) REFERENCES recommendation_template(template_id)
    );

CREATE TABLE IF NOT EXISTS quick_tip_catalog (
    tip_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT NOT NULL,
    body        TEXT NOT NULL,
    source_note TEXT
);

CREATE TABLE IF NOT EXISTS tip_rotation_state (
    home_id         TEXT NOT NULL,
    screen          TEXT NOT NULL, -- "home" / "recommendations"
    last_tip_id     INTEGER,
    last_changed_at TEXT NOT NULL,
    PRIMARY KEY (home_id, screen),
    FOREIGN KEY (home_id) REFERENCES home_profile(home_id)
);

-- =========================
-- Indexes (important for charts)
-- =========================
CREATE INDEX IF NOT EXISTS idx_energy_interval_ts ON energy_interval(home_id, ts_start);
CREATE INDEX IF NOT EXISTS idx_dev_usage_ts ON device_usage_interval(home_id, ts_start);
CREATE INDEX IF NOT EXISTS idx_forecast_ts ON forecast_interval_cache(home_id, ts_start);
