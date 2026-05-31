package com.velitask.plugin.official;

import com.velitask.plugin.official.slope.SlopeDetector;
import com.velitask.plugin.official.slope.SlopeGroup;
import com.velitask.plugin.official.slope.SlopeGroupAtom;
import com.velitask.sdk.IFileSource;
import com.velitask.sdk.IIndicator;
import com.velitask.sdk.IPlagin;
import com.velitask.sdk.SensorType;
import com.velitask.sdk.data.ISensor;
import com.velitask.sdk.data.SlopeSensorAtom;
import com.velitask.sdk.db.PluginDatabase;
import java.util.ArrayList;
import java.util.List;

public class VelitaskPlagin implements IPlagin {

    @Override
    public String getUID() {
        return "com.velitask.plagin.official";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public int getDbVersion() {
        return 1;
    }

    @Override
    public String[] defineAdditionLocales() {
        return new String[]{"ru"};
    }

    @Override
    public IIndicator[] defineIndicators() {
        return new IIndicator[]{
            new VideoIndicator(),
            new ImageIndicator(),
            new SpeedometerIndicator(),
            new DistanceTextIndicator(),
            new SlopeChartIndicator(),
            new SlopeTextIndicator(),
            new TemperatureTextIndicator(),
            new TimeTextIndicator(),
            new TextIndicator(),
            new LatLonTextIndicator(),
            new GeoMapIndicator(),
            new CompassIndicator(),
            new RectangleIndicator(),
            new EllipseIndicator(),
            new LineIndicator()
        };
    }

    @Override
    public void onSourceImported(IFileSource source, PluginDatabase db) {
        if (source == null || db == null) {
            return;
        }
        ISensor slopeSensor = source.getSensorList().getBySensorType(SensorType.SLOPE);
        if (slopeSensor == null) {
            return;
        }

        db.execute(
                "DELETE FROM ${table:slope_atom_groups}"
                + " WHERE group_id IN ("
                + "   SELECT id FROM ${table:slope_groups} WHERE sensor_id = ?"
                + " )",
                slopeSensor.getId()
        );
        db.execute(
                "DELETE FROM ${table:slope_groups} WHERE sensor_id = ?",
                slopeSensor.getId()
        );

        List<SlopeSensorAtom> atoms = db.queryList(
                "SELECT data_id AS id, measureIndex AS \"index\","
                + "       timeRaw, duration,"
                + "       distance, distanceDelta, distanceSlope,"
                + "       elevation, elevationDelta,"
                + "       eleUp, eleUpDelta, eleDown, eleDownDelta,"
                + "       slopeType, slopePercent"
                + " FROM sensors_slope_data"
                + " WHERE sensor_id = ?"
                + " ORDER BY timeRaw",
                SlopeSensorAtom.class,
                slopeSensor.getId()
        );
        if (atoms == null || atoms.isEmpty()) {
            return;
        }

        SlopeDetector detector = new SlopeDetector();
        for (SlopeSensorAtom a : atoms) {
            detector.add(a);
        }

        List<SlopeGroup> groups = detector.getGroups();
        int measureIndex = 0;
        List<Object[]> atomGroupRows = new ArrayList<>();
        for (SlopeGroup group : groups) {
            SlopeGroupAtom row = group.buildAtom(slopeSensor.getId(), measureIndex++);
            db.execute(
                    "INSERT INTO ${table:slope_groups}"
                    + " (sensor_id, measureIndex, timeRaw, duration,"
                    + "  distance, distanceDelta, elevation, elevationDelta,"
                    + "  eleUp, eleDown, slopeType, slopePercent)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    row.sensor_id, row.measureIndex, row.timeRaw, row.duration,
                    row.distance, row.distanceDelta, row.elevation, row.elevationDelta,
                    row.eleUp, row.eleDown, row.slopeType, row.slopePercent
            );
            Long groupId = db.queryValue(
                    "SELECT id FROM ${table:slope_groups}"
                    + " WHERE sensor_id = ? AND measureIndex = ?",
                    Long.class,
                    row.sensor_id, row.measureIndex
            );
            if (groupId == null) {
                continue;
            }
            group.setId(groupId);
            for (SlopeSensorAtom a : group.getList()) {
                atomGroupRows.add(new Object[]{a.id, groupId});
            }
        }

        if (!atomGroupRows.isEmpty()) {
            db.batchInsert(
                    "INSERT INTO ${table:slope_atom_groups} (atom_id, group_id) VALUES (?, ?)",
                    atomGroupRows
            );
        }
    }

}
