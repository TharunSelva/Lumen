-- 1. Home Profile (User details and settings)
CREATE TABLE IF NOT EXISTS home_profile (
                                            home_id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(50),
    timezone VARCHAR(50),
    postcode_outcode VARCHAR(20),
    last_home_refresh_at TIMESTAMP
    );

-- 2. Energy Interval (Replaces your CSV for usage/cost/CO2)
CREATE TABLE IF NOT EXISTS energy_interval (
                                               interval_id IDENTITY PRIMARY KEY, -- Auto-incrementing ID in H2
                                               home_id VARCHAR(50),
    ts_start TIMESTAMP,
    ts_end TIMESTAMP,
    total_load_kwh DOUBLE,
    tariff_p_per_kwh DOUBLE,
    carbon_gco2_per_kwh DOUBLE,
    solar_used_kwh DOUBLE,
    battery_used_kwh DOUBLE,
    grid_import_kwh DOUBLE,
    data_source VARCHAR(20), -- Tracks if this is 'SEED', 'SIMULATED', or 'API'
    FOREIGN KEY (home_id) REFERENCES home_profile(home_id)
    );

-- 3. Home Card Cache (Snapshot for the Dashboard eco-impact)
CREATE TABLE IF NOT EXISTS home_card_cache (
                                               home_id VARCHAR(50) PRIMARY KEY,
    ts TIMESTAMP,
    uk_avg_gco2_kwh DOUBLE,
    clean_share_pct DOUBLE,
    fossil_share_pct DOUBLE,
    low_carbon_days_achieved INT,
    low_carbon_days_window INT,
    computed_from VARCHAR(20) -- e.g., 'API' or 'SIM'
    );

-- 4. Forecast Cache (For 24h mini forecasts and charts)
CREATE TABLE IF NOT EXISTS forecast_interval_cache (
                                                       forecast_id IDENTITY PRIMARY KEY,
                                                       home_id VARCHAR(50),
    ts_start TIMESTAMP,
    ts_end TIMESTAMP,
    tariff_p_per_kwh DOUBLE,
    carbon_gco2_per_kwh DOUBLE,
    solar_shortwave_wm2 DOUBLE,
    expected_load_kwh DOUBLE,
    battery_soc_pct_pred DOUBLE,
    battery_action VARCHAR(20),
    retrieved_at TIMESTAMP,
    source VARCHAR(50)
    );

-- 5. Recommendations (For the UI tips)
CREATE TABLE IF NOT EXISTS recommendation_instance (
                                                       instance_id IDENTITY PRIMARY KEY,
                                                       home_id VARCHAR(50),
    title VARCHAR(255),
    summary TEXT,
    priority INT,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    created_at TIMESTAMP
    );