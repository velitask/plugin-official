-- ====================================================================
-- plugin.official DB schema (v1).
-- Slope groups are computed by the plugin in onSourceImported after a
-- source is imported (see SlopeDetector). The link to core slope points
-- is via slope_atom_groups.atom_id -> sensors_slope_data.data_id.
-- ====================================================================

CREATE TABLE IF NOT EXISTS ${table:slope_groups} (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sensor_id INTEGER NOT NULL,

    measureIndex INTEGER NOT NULL,
    timeRaw INTEGER NOT NULL,
    duration INTEGER NOT NULL,

    distance INTEGER NOT NULL,
    distanceDelta INTEGER NOT NULL,

    elevation INTEGER NOT NULL,
    elevationDelta INTEGER NOT NULL,

    eleUp INTEGER NOT NULL,
    eleDown INTEGER NOT NULL,

    slopeType INTEGER NOT NULL,
    slopePercent INTEGER NOT NULL
);

CREATE INDEX idx_slope_groups_sensor ON ${table:slope_groups} (sensor_id);
CREATE INDEX idx_slope_groups_distance ON ${table:slope_groups} (distance);

CREATE TABLE IF NOT EXISTS ${table:slope_atom_groups} (
    atom_id INTEGER PRIMARY KEY,
    group_id INTEGER NOT NULL
);

CREATE INDEX idx_slope_atom_groups_group ON ${table:slope_atom_groups} (group_id);
