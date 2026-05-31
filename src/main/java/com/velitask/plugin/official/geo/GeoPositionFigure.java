package com.velitask.plugin.official.geo;

import com.velitask.plugin.official.GeoMapIndicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.data.GeoSensorAtom;
import com.velitask.sdk.figures.Figure;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DisplayHint;
import com.velitask.sdk.properties.GeoSensorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.SizeProperty;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class GeoPositionFigure extends Figure<GeoFigureContext> {

    public static final String NAME = "position";

    private static final String KEY = GeoMapIndicator.KEY + "." + NAME;

    private final GeoSensorProperty mGeoSensor = new GeoSensorProperty();

    private static final String FILL_COLOR = "fillColor";
    private final ColorProperty mFillColor = new ColorProperty(
            new Color(0, 0, 200),
            FILL_COLOR,
            localized(KEY + "." + FILL_COLOR + ".title")
    );

    private static final String BORDER_COLOR = "borderColor";
    private final ColorProperty mBorderColor = new ColorProperty(
            Color.WHITE,
            BORDER_COLOR,
            localized(KEY + "." + BORDER_COLOR + ".title")
    );

    private final SizeProperty mSize = new SizeProperty() {
        {
            setRange(4, 100);
        }

        @Override
        public Integer getDefault() {
            return 30;
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
        return new IProperty[]{mGeoSensor, mFillColor, mBorderColor, mSize};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mFillColor, PropertyGroup.APPEARANCE);
        config.set(mBorderColor, PropertyGroup.APPEARANCE);
        config.set(mSize, PropertyGroup.APPEARANCE, DisplayHint.SPINNER);
    }

    @Override
    public void render(GeoFigureContext baseCtx) {
        PositionContext ctx = (PositionContext) baseCtx;
        if (ctx.zoom == null || ctx.sensorId == 0) {
            return;
        }

        long rawTime = mGeoSensor.convertToRawTime(ctx.indicator.player.time);
        if (ctx.indicator.player.isPreview) {
            rawTime = mGeoSensor.clampToSensorRange(rawTime);
        }
        GeoSensorAtom atom = mGeoSensor.queryAtom(rawTime);
        if (atom == null) {
            return;
        }

        Point2D.Double pt = ctx.toScreen(atom.lat, atom.lon);
        int cx = (int) pt.x;
        int cy = (int) pt.y;

        int size = Math.max(1, (int) Math.round(ctx.size * ctx.scale));
        int x = cx - size / 2;
        int y = cy - size / 2;

        Graphics2D g = ctx.graphics;
        g.setColor(ctx.fill);
        g.fillOval(x, y, size, size);

        g.setColor(ctx.border);
        g.setStroke(new BasicStroke((float) Math.max(1, 2 * ctx.scale)));
        g.drawOval(x, y, size, size);
    }

    @Override
    public GeoFigureContext createContext(IndicatorContext indCtx) {
        PositionContext ctx = new PositionContext((GeoMapIndicator.GeoMapContext) indCtx);
        return ctx;
    }

    public class PositionContext extends GeoFigureContext {

        public final long sensorId;
        public final Color fill;
        public final Color border;
        public final int size;

        public PositionContext(GeoMapIndicator.GeoMapContext indCtx) {
            super(indCtx);

            sensorId = mGeoSensor.getSensorId();
            fill = mFillColor.createContext().value;
            border = mBorderColor.createContext().value;
            size = mSize.createContext().value;
        }
    }

}
