package com.velitask.plugin.official.geo;

import com.velitask.plugin.official.GeoMapIndicator;
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
import com.velitask.sdk.properties.VisibleProperty;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;

public class GeoTrackFigure extends Figure<GeoFigureContext> {

    public static final String NAME = "track";

    private static final String KEY = GeoMapIndicator.KEY + "." + NAME;

    private final GeoSensorProperty mGeoSensor = new GeoSensorProperty();

    {
        mGeoSensor.query("track")
                .where("zoom = {zoom}")
                .orderBy("timeRaw")
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

    private final VisibleProperty mPassedOnly = new VisibleProperty() {
        {
            setDefault(false);
            set(false);
        }

        @Override
        public String getName() {
            return "passedOnly";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".passedOnly.title");
        }
    };

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTitle() {
        return localized(KEY + ".title");
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{mGeoSensor, mLine, mPassedOnly};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mLine, PropertyGroup.APPEARANCE);
        config.set(mPassedOnly, PropertyGroup.APPEARANCE);
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

        int upTo = points.size();
        Point2D.Double tail = null;
        if (ctx.passedOnly.value) {
            long rawTime = mGeoSensor.convertToRawTime(ctx.indicator.player.time);
            int idx = lowerBoundByTimeRaw(points, rawTime);
            upTo = Math.min(idx + 1, points.size());
            GeoSensorAtom curr = points.get(Math.min(idx, points.size() - 1));
            tail = ctx.toScreen(curr.calcLat(rawTime), curr.calcLon(rawTime));
        }

        int prevX = Integer.MIN_VALUE, prevY = Integer.MIN_VALUE;
        for (int i = 0; i < upTo; i++) {
            GeoSensorAtom p = points.get(i);
            Point2D.Double pt = ctx.toScreen(p.lat, p.lon);
            int sx = (int) pt.x;
            int sy = (int) pt.y;
            if (prevX != Integer.MIN_VALUE) {
                g.drawLine(prevX, prevY, sx, sy);
            }
            prevX = sx;
            prevY = sy;
        }
        if (tail != null && prevX != Integer.MIN_VALUE) {
            g.drawLine(prevX, prevY, (int) tail.x, (int) tail.y);
        }
    }

    private static int lowerBoundByTimeRaw(List<GeoSensorAtom> points, long t) {
        int lo = 0;
        int hi = points.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (points.get(mid).timeRaw <= t) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return Math.max(0, lo - 1);
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
        public final VisibleProperty.BooleanContext passedOnly;

        public TrackContext(GeoMapIndicator.GeoMapContext indCtx) {
            super(indCtx);

            sensorId = mGeoSensor.getSensorId();
            line = mLine.createContext();
            passedOnly = mPassedOnly.createContext();
        }
    }

    @Override
    public GeoFigureContext createContext(IndicatorContext indCtx) {
        TrackContext ctx = new TrackContext((GeoMapIndicator.GeoMapContext) indCtx);
        return ctx;
    }

}
