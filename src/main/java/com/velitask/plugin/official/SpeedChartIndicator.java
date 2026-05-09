package com.velitask.plugin.official;

import com.velitask.plugin.official.charts.MillimetersChart;
import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.data.DistanceSensorAtom;
import com.velitask.sdk.db.DataCacheRule;
import com.velitask.sdk.db.DataCacheStorage;
import com.velitask.sdk.db.DataParams;
import com.velitask.sdk.db.SensorData;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DisplayHint;
import com.velitask.sdk.properties.DistanceSensorProperty;
import com.velitask.sdk.properties.GeoZoomProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.IntegerProperty;
import com.velitask.sdk.properties.LineProperty;
import com.velitask.sdk.properties.PropertyGroup;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.List;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;
import org.abricos.geo.GeoUtils;

public class SpeedChartIndicator extends Indicator {

    public static final String NAME = "speedChart";

    private static final String KEY = _KEY + "." + NAME;

    private final DistanceSensorProperty mDistance = new DistanceSensorProperty();

    {
        mDistance.query("speedRange")
                .where("(distance + distanceDelta) >= {minDistance} "
                        + "AND distance < {maxDistance}")
                .orderBy("timeRaw")
                .cache(DataCacheRule.none())
                .cacheSize(2)
                .buildList();

        mDistance.aggregate("avgSpeed")
                .select("AVG(speed) AS avgSpeed")
                .where("speed > 0")
                .cache(ONCE_CACHE_RULE)
                .cacheSize(1)
                .build();
    }

    private final GeoZoomProperty mGeoZoom = new GeoZoomProperty();

    private final IntegerProperty mSpeedScale = new IntegerProperty() {
        {
            setRange(1, 20);
            setSkinnable(false);
        }

        @Override
        public String getName() {
            return "SpeedScale";
        }

        @Override
        public Integer getDefault() {
            return 1;
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".speedScale.title");
        }
    };

    private final LineProperty mLine = new LineProperty();

    private static final String FILL_COLOR_LOW = "fillColorLow";
    private final ColorProperty mFillColorLow = new ColorProperty(
            new Color(0, 200, 0, 60),
            FILL_COLOR_LOW,
            localized(KEY + "." + FILL_COLOR_LOW + ".title")
    );

    private static final String FILL_COLOR_HIGH = "fillColorHigh";
    private final ColorProperty mFillColorHigh = new ColorProperty(
            new Color(200, 0, 0, 60),
            FILL_COLOR_HIGH,
            localized(KEY + "." + FILL_COLOR_HIGH + ".title")
    );

    private static final String AVG_COLOR = "avgColor";
    private final ColorProperty mAvgColor = new ColorProperty(
            new Color(255, 255, 0, 180),
            AVG_COLOR,
            localized(KEY + "." + AVG_COLOR + ".title")
    );

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTitle() {
        return localized(KEY + ".title");
    }

    @Override
    public String getDescription() {
        return localized(KEY + ".description");
    }

    @Override
    public String getTags() {
        return localized(KEY + ".tags");
    }

    @Override
    public void defineMaket(Maket maket) {
        maket.setVertical(VerticalAlign.BOTTOM);
        maket.setHorizontal(HorizontalAlign.LEFT);
        maket.setLayerMargin(100D, null, null, 100d);
        maket.setLayerSize(600d, 300d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mDistance,
            mGeoZoom, mSpeedScale,
            mLine,
            mFillColorLow, mFillColorHigh, mAvgColor
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mGeoZoom, PropertyGroup.APPEARANCE);
        config.set(mSpeedScale, PropertyGroup.APPEARANCE, DisplayHint.SLIDER);
        config.set(mLine, PropertyGroup.APPEARANCE);
        config.set(mFillColorLow, PropertyGroup.APPEARANCE);
        config.set(mFillColorHigh, PropertyGroup.APPEARANCE);
        config.set(mAvgColor, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("neon", localized(KEY + ".skin.neon"))
            .set(mLine.getColor().skin(0, 255, 100))
            .set(mLine.getThickness().skin(4))
            .set(mFillColorLow.skin(0, 100, 0, 40))
            .set(mFillColorHigh.skin(0, 255, 0, 80))
            .set(mAvgColor.skin(0, 200, 200, 150))
            .build()
        };
    }

    private static final DataCacheRule ONCE_CACHE_RULE = new DataCacheRule() {
        @Override
        public boolean isCached(DataParams params, DataCacheStorage cache) {
            return cache.has("avg");
        }

        @Override
        public Object getFromCache(DataParams params, DataCacheStorage cache) {
            return cache.get("avg");
        }

        @Override
        public void store(DataParams params, Object data, DataCacheStorage cache) {
            cache.put("avg", data);
        }
    };

    @Override
    public void render(IndicatorContext indicatorContext) {
        SpeedChartContext ctx = (SpeedChartContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        long rawTime = mDistance.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mDistance.clampToSensorRange(rawTime);
        }
        DistanceSensorAtom currAtom = mDistance.queryAtom(rawTime);
        if (currAtom == null) {
            return;
        }

        MillimetersChart chart = new MillimetersChart(
                ctx.width, ctx.height,
                ctx.geoZoom.value, GeoUtils.LAT_EQUATOR,
                currAtom.distance, 0,
                1, 1
        );

        long margin = chart.getMmPerPixel() * 50;
        long minDist = chart.getMinMmX() - margin;
        long maxDist = chart.getMaxMmX() + margin;

        DataParams rangeParams = DataParams
                .of("sensorId", mDistance.getSensorId())
                .set("minDistance", minDist)
                .set("maxDistance", maxDist);

        List<DistanceSensorAtom> atoms = mDistance.queryList("speedRange", rangeParams);
        if (atoms == null || atoms.isEmpty()) {
            return;
        }

        int maxSpeed = 0;
        for (DistanceSensorAtom atom : atoms) {
            int s = Math.max(atom.speed, atom.speed + atom.speedDelta);
            if (s > maxSpeed) {
                maxSpeed = s;
            }
        }
        if (maxSpeed == 0) {
            return;
        }

        double yScale = (double) ctx.height * ctx.speedScale.value / (maxSpeed * 1.1);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        renderFill(g, chart, atoms, maxSpeed, yScale, ctx);
        renderLine(g, chart, atoms, yScale, ctx);
        renderAvgLine(g, chart, yScale, ctx);
        renderMarker(g, chart, currAtom, yScale, ctx);
    }

    private int speedToPixelY(int speed, double yScale, int height) {
        return height - (int) (speed * yScale);
    }

    private void renderFill(Graphics2D g, MillimetersChart chart,
            List<DistanceSensorAtom> atoms, int maxSpeed,
            double yScale, SpeedChartContext ctx) {
        int height = ctx.height;
        Color cLow = ctx.fillColorLow.value;
        Color cHigh = ctx.fillColorHigh.value;

        for (int i = 0; i < atoms.size(); i++) {
            DistanceSensorAtom atom = atoms.get(i);
            int x1 = chart.convertMmToPxX(atom.distance);
            int y1 = speedToPixelY(atom.speed, yScale, height);

            int x2, y2;
            int speedSum = atom.speed;
            int speedCount = 1;

            boolean found = false;
            for (int j = i + 1; j < atoms.size(); j++) {
                DistanceSensorAtom next = atoms.get(j);
                x2 = chart.convertMmToPxX(next.getNextDistance());
                if ((x2 - x1) >= MIN_PX_BETWEEN_POINTS) {
                    y2 = speedToPixelY(next.getNextSpeed(), yScale, height);
                    speedSum += next.getNextSpeed();
                    speedCount++;
                    i = j;
                    found = true;

                    double ratio = (double) speedSum / speedCount / maxSpeed;
                    ratio = Math.max(0, Math.min(1, ratio));

                    Polygon polygon = new Polygon();
                    polygon.addPoint(x1, height);
                    polygon.addPoint(x1, y1);
                    polygon.addPoint(x2, y2);
                    polygon.addPoint(x2, height);

                    g.setColor(lerpColor(cLow, cHigh, ratio));
                    g.fillPolygon(polygon);
                    break;
                }
                speedSum += next.speed;
                speedCount++;
            }
            if (!found) {
                x2 = chart.convertMmToPxX(atom.getNextDistance());
                y2 = speedToPixelY(atom.getNextSpeed(), yScale, height);
                if (x2 - x1 < 1) {
                    continue;
                }

                double ratio = (double) (atom.speed + atom.getNextSpeed()) / 2.0 / maxSpeed;
                ratio = Math.max(0, Math.min(1, ratio));

                Polygon polygon = new Polygon();
                polygon.addPoint(x1, height);
                polygon.addPoint(x1, y1);
                polygon.addPoint(x2, y2);
                polygon.addPoint(x2, height);

                g.setColor(lerpColor(cLow, cHigh, ratio));
                g.fillPolygon(polygon);
            }
        }
    }

    private static final int MIN_PX_BETWEEN_POINTS = 2;

    private void renderLine(Graphics2D g, MillimetersChart chart,
            List<DistanceSensorAtom> atoms,
            double yScale, SpeedChartContext ctx) {
        int height = ctx.height;
        float depth = (float) (ctx.line.thickness * ctx.scale);
        g.setStroke(new BasicStroke(depth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(ctx.line.color);

        int x1, y1, x2, y2;
        for (int i = 0; i < atoms.size(); i++) {
            DistanceSensorAtom atom = atoms.get(i);

            x1 = chart.convertMmToPxX(atom.distance);
            y1 = speedToPixelY(atom.speed, yScale, height);

            boolean hasNext = false;
            i++;
            for (; i < atoms.size(); i++) {
                DistanceSensorAtom next = atoms.get(i);
                x2 = chart.convertMmToPxX(next.getNextDistance());
                y2 = speedToPixelY(next.getNextSpeed(), yScale, height);
                if ((x2 - x1) >= MIN_PX_BETWEEN_POINTS) {
                    hasNext = true;
                    g.drawLine(x1, y1, x2, y2);
                    break;
                }
            }
            if (!hasNext) {
                x2 = chart.convertMmToPxX(atom.getNextDistance());
                y2 = speedToPixelY(atom.getNextSpeed(), yScale, height);
                g.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void renderAvgLine(Graphics2D g, MillimetersChart chart,
            double yScale, SpeedChartContext ctx) {
        DataParams avgParams = DataParams.of("sensorId", mDistance.getSensorId());
        SensorData avgData = mDistance.queryItem("avgSpeed", avgParams);
        if (avgData == null || !avgData.has("avgSpeed")) {
            return;
        }

        Object avgValue = avgData.get("avgSpeed");
        if (avgValue == null) {
            return;
        }
        int avgSpeed = ((Number) avgValue).intValue();
        if (avgSpeed <= 0) {
            return;
        }

        int y = speedToPixelY(avgSpeed, yScale, ctx.height);

        float[] dash = {10f, 8f};
        g.setStroke(new BasicStroke(
                2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND,
                10f, dash, 0f
        ));
        g.setColor(ctx.avgColor.value);
        g.drawLine(0, y, ctx.width, y);
    }

    private void renderMarker(Graphics2D g, MillimetersChart chart,
            DistanceSensorAtom currAtom,
            double yScale, SpeedChartContext ctx) {
        float depth = (float) (ctx.line.thickness * ctx.scale);
        int rOut = (int) (depth * 6);
        int rIn = (int) (depth * 4);

        int x = ctx.width / 2;
        int y = speedToPixelY(currAtom.speed, yScale, ctx.height);

        g.setColor(ctx.line.color);
        g.fillOval(x - rOut / 2, y - rOut / 2, rOut, rOut);

        g.setComposite(AlphaComposite.Clear);
        g.fillOval(x - rIn / 2, y - rIn / 2, rIn, rIn);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static Color lerpColor(Color a, Color b, double t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    public class SpeedChartContext extends IndicatorContext {

        public final IntegerProperty.IntegerContext geoZoom;
        public final IntegerProperty.IntegerContext speedScale;
        public final LineProperty.LineContext line;
        public final ColorProperty.ColorContext fillColorLow;
        public final ColorProperty.ColorContext fillColorHigh;
        public final ColorProperty.ColorContext avgColor;

        public SpeedChartContext(Player player, Canvas canvas) {
            super(player, canvas);
            geoZoom = mGeoZoom.createContext();
            speedScale = mSpeedScale.createContext();
            line = mLine.createContext();
            fillColorLow = mFillColorLow.createContext();
            fillColorHigh = mFillColorHigh.createContext();
            avgColor = mAvgColor.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new SpeedChartContext(player, canvas);
    }
}
