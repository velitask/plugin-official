package com.velitask.plugin.official.geo;

import com.velitask.plugin.official.GeoMapIndicator;
import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.data.GeoSensorAtom;
import com.velitask.sdk.db.DataCacheRule;
import com.velitask.sdk.db.DataParams;
import com.velitask.sdk.figures.Figure;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.GeoSensorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.LineProperty;
import com.velitask.sdk.properties.PropertyGroup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class GeoTrackFigure extends Figure<GeoFigureContext> {

    public static final String NAME = "track";

    private static final String KEY = GeoMapIndicator.KEY + "." + NAME;

    private final GeoSensorProperty mGeoSensor = new GeoSensorProperty();

    {
        mGeoSensor.query("track")
                .where("zoom = {zoom}")
                .orderBy("measureTime")
                .cache(DataCacheRule.byParams())
                .cacheSize(2)
                .buildList();
    }

    private final LineProperty mLine = new LineProperty();

    {
        mLine.getColor().setDefault(new Color(200, 0, 0));
        mLine.getColor().set(new Color(200, 0, 0));
        mLine.getThickness().setRange(1, 30);
        mLine.getThickness().setDefault(5);
        mLine.getThickness().set(5);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTitle() {
        return localized(KEY + ".title");
    }

    @Override
    public Class<? extends Indicator> parent() {
        return GeoMapIndicator.class;
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{mGeoSensor, mLine};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mLine, PropertyGroup.APPEARANCE);
    }

    @Override
    public void render(GeoFigureContext baseCtx) {
        TrackContext ctx = (TrackContext) baseCtx;
        if (ctx.indicator.zoom == null || ctx.sensorId == 0) {
            return;
        }

        int actualZoom = mGeoSensor.approximateZoom(ctx.zoom.getLevel());
        DataParams params = DataParams.of("sensorId", ctx.sensorId)
                .set("zoom", actualZoom);
        List<GeoSensorAtom> points = mGeoSensor.queryList("track", params);
        if (points == null || points.size() < 2) {
            return;
        }

        Graphics2D g = ctx.graphics;
        g.setColor(ctx.line.color);

        float strokeWidth = (float) (ctx.line.thickness * ctx.scale);
        g.setStroke(buildStroke(strokeWidth, ctx.line.dashPattern));

        int prevX = Integer.MIN_VALUE, prevY = Integer.MIN_VALUE;
        for (GeoSensorAtom p : points) {
            int sx = ctx.toScreenX(p.lon);
            int sy = ctx.toScreenY(p.lat);
            if (prevX != Integer.MIN_VALUE) {
                g.drawLine(prevX, prevY, sx, sy);
            }
            prevX = sx;
            prevY = sy;
        }
    }

    private static BasicStroke buildStroke(float width, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
        String[] parts = pattern.split(",");
        float[] dash = new float[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                dash[i] = Float.parseFloat(parts[i].trim());
                if (dash[i] <= 0) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException ex) {
            return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
        return new BasicStroke(width,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10f, dash, 0f
        );
    }

    public class TrackContext extends GeoFigureContext {

        public final long sensorId;
        public final LineProperty.LineContext line;

        public TrackContext(GeoMapIndicator.GeoMapContext indCtx) {
            super(indCtx);

            sensorId = mGeoSensor.getSensorId();
            line = mLine.createContext();
        }
    }

    @Override
    public GeoFigureContext createContext(IndicatorContext indCtx) {
        TrackContext ctx = new TrackContext((GeoMapIndicator.GeoMapContext) indCtx);
        return ctx;
    }

}
