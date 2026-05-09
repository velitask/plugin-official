package com.velitask.plugin.official;

import com.velitask.plugin.official.charts.MillimetersChart;
import com.velitask.plugin.official.slope.SlopeGroupAtom;
import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.data.SlopeSensorAtom;
import com.velitask.sdk.db.DataCacheRule;
import com.velitask.sdk.db.DataParams;
import com.velitask.sdk.db.PluginDatabase;
import com.velitask.sdk.db.SensorDataManager;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DisplayHint;
import com.velitask.sdk.properties.GeoZoomProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.IntegerProperty;
import com.velitask.sdk.properties.LineProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.SlopeAtomProperty;
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

public class SlopeChartIndicator extends Indicator {

    public static final String NAME = "slopeChart";

    private static final String KEY = _KEY + "." + NAME;

    private static final int SLOPE_GROUP_COLOR_ALPHA = 30;

    private static final Color FILL_DOWN = setAlpha(Color.GREEN, SLOPE_GROUP_COLOR_ALPHA);
    private static final Color FILL_UP = setAlpha(Color.RED, SLOPE_GROUP_COLOR_ALPHA);
    private static final Color FILL_PLAIN = setAlpha(Color.WHITE, SLOPE_GROUP_COLOR_ALPHA + 10);

    private static final int MIN_PX_BETWEEN_POINTS = 5;

    private final SlopeAtomProperty mSlope = new SlopeAtomProperty();

    {
        mSlope.addManager(
                new SensorDataManager.Builder<>("slope", mSlope.getAtomClass())
                        .table(SlopeAtomProperty.TABLE_NAME)
                        .byTime()
                        .build()
        );

        mSlope.query("slopeRange")
                .where("(distance + distanceDelta) >= {minDistance} "
                        + "AND distance < {maxDistance}")
                .orderBy("timeRaw")
                .cache(DataCacheRule.none())
                .cacheSize(2)
                .buildList();
    }

    private final GeoZoomProperty mGeoZoom = new GeoZoomProperty();

    private final IntegerProperty mEleChartScale = new IntegerProperty() {
        {
            setRange(1, 50);
            setSkinnable(false);
        }

        @Override
        public String getName() {
            return "EleChartScale";
        }

        @Override
        public Integer getDefault() {
            return 10;
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".eleChartScale.title");
        }
    };

    private final LineProperty mLine = new LineProperty();

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
        maket.setVertical(VerticalAlign.TOP);
        maket.setHorizontal(HorizontalAlign.LEFT);
        maket.setLayerMargin(null, 300d, 100d, null);
        maket.setLayerSize(600d, 300d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mSlope,
            mGeoZoom, mEleChartScale,
            mLine
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mGeoZoom, PropertyGroup.APPEARANCE);
        config.set(mEleChartScale, PropertyGroup.APPEARANCE, DisplayHint.SLIDER);
        config.set(mLine, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("bold", localized(KEY + ".skin.bold"))
            .set(mLine.getColor().skin(Color.WHITE))
            .set(mLine.getThickness().skin(10))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        SlopeChartContext ctx = (SlopeChartContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        long rawTime = mSlope.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mSlope.clampToSensorRange(rawTime);
        }
        SlopeSensorAtom currAtom = mSlope.queryAtom("slope", rawTime);
        if (currAtom == null) {
            return;
        }

        MillimetersChart chart = new MillimetersChart(
                ctx.width, ctx.height,
                ctx.geoZoom.value, GeoUtils.LAT_EQUATOR,
                currAtom.distance, currAtom.elevation,
                1, ctx.eleChartScale.value
        );

        long margin = chart.getMmPerPixel() * 50;
        long minDist = chart.getMinMmX() - margin;
        long maxDist = chart.getMaxMmX() + margin;

        DataParams rangeParams = DataParams
                .of("sensorId", mSlope.getSensorId())
                .set("minDistance", minDist)
                .set("maxDistance", maxDist);

        List<SlopeSensorAtom> atoms = mSlope.queryList("slopeRange", rangeParams);
        List<SlopeGroupAtom> groups = queryGroups(ctx.db, mSlope.getSensorId(), minDist, maxDist);

        if (atoms == null || atoms.isEmpty()) {
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (groups != null && !groups.isEmpty()) {
            renderGroupFills(g, chart, groups, atoms);
        }

        renderLine(g, chart, atoms, ctx);

        renderMarker(g, chart, currAtom, ctx);
    }

    private static List<SlopeGroupAtom> queryGroups(PluginDatabase db,
            long sensorId, long minDistance, long maxDistance) {
        if (db == null) {
            return List.of();
        }
        return db.queryList(
                "SELECT * FROM ${table:slope_groups}"
                + " WHERE sensor_id = ?"
                + " AND (distance + distanceDelta) >= ?"
                + " AND distance < ?"
                + " ORDER BY distance",
                SlopeGroupAtom.class,
                sensorId, minDistance, maxDistance
        );
    }

    private void renderGroupFills(Graphics2D g, MillimetersChart chart,
            List<SlopeGroupAtom> groups,
            List<SlopeSensorAtom> atoms) {
        for (SlopeGroupAtom groupAtom : groups) {
            long groupEndDistance = groupAtom.distance + groupAtom.distanceDelta;
            if (groupEndDistance < chart.getMinMmX()) {
                continue;
            }
            if (groupAtom.distance > chart.getMaxMmX()) {
                break;
            }

            Polygon polygon = new Polygon();

            int x1 = chart.convertMmToPxX(groupAtom.distance);

            polygon.addPoint(x1, chart.getHeight());
            polygon.addPoint(x1, chart.convertMmToPxY(groupAtom.elevation));

            SlopeSensorAtom item = null;
            for (SlopeSensorAtom point : atoms) {
                if (point.distance < groupAtom.distance || point.distance >= groupEndDistance) {
                    continue;
                }
                int x2 = chart.convertMmToPxX(point.getNextDistance());
                if (x2 == x1) {
                    continue;
                }
                x1 = x2;
                item = point;
                polygon.addPoint(x2, chart.convertMmToPxY(point.getNextElevation()));
            }
            if (item == null) {
                continue;
            }
            polygon.addPoint(x1, chart.getHeight());

            Color fill;
            if (groupAtom.slopeType < 0) {
                fill = FILL_DOWN;
            } else if (groupAtom.slopeType > 0) {
                fill = FILL_UP;
            } else {
                fill = FILL_PLAIN;
            }
            g.setColor(fill);
            g.fillPolygon(polygon);
        }
    }

    private void renderLine(Graphics2D g, MillimetersChart chart,
            List<SlopeSensorAtom> atoms, SlopeChartContext ctx) {
        float depth = (float) (ctx.line.thickness * ctx.scale);
        g.setStroke(new BasicStroke(depth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        g.setColor(ctx.line.color);

        for (int i = 0; i < atoms.size(); i++) {
            SlopeSensorAtom item = atoms.get(i);

            int x1 = chart.convertMmToPxX(item.distance);
            int y1 = chart.convertMmToPxY(item.elevation);

            int x2, y2;
            boolean hasItem2 = false;
            i++;
            for (; i < atoms.size(); i++) {
                SlopeSensorAtom item2 = atoms.get(i);
                x2 = chart.convertMmToPxX(item2.getNextDistance());
                y2 = chart.convertMmToPxY(item2.getNextElevation());
                if ((x2 - x1) > MIN_PX_BETWEEN_POINTS) {
                    hasItem2 = true;
                    g.drawLine(x1, y1, x2, y2);
                    break;
                }
            }
            if (!hasItem2) {
                x2 = chart.convertMmToPxX(item.getNextDistance());
                y2 = chart.convertMmToPxY(item.getNextElevation());
                g.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void renderMarker(Graphics2D g, MillimetersChart chart,
            SlopeSensorAtom atom, SlopeChartContext ctx) {
        float depth = (float) (ctx.line.thickness * ctx.scale);
        int rOut = (int) (depth * 6);
        int rIn = (int) (depth * 4);

        int x = chart.convertMmToPxX(atom.distance);
        int y = chart.convertMmToPxY(atom.elevation);

        g.setColor(ctx.line.color);
        g.fillOval(x - rOut / 2, y - rOut / 2, rOut, rOut);

        g.setComposite(AlphaComposite.Clear);
        g.fillOval(x - rIn / 2, y - rIn / 2, rIn, rIn);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static Color setAlpha(Color c, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public class SlopeChartContext extends IndicatorContext {

        public final IntegerProperty.IntegerContext geoZoom;
        public final IntegerProperty.IntegerContext eleChartScale;
        public final LineProperty.LineContext line;

        public SlopeChartContext(Player player, Canvas canvas) {
            super(player, canvas);
            geoZoom = mGeoZoom.createContext();
            eleChartScale = mEleChartScale.createContext();
            line = mLine.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new SlopeChartContext(player, canvas);
    }
}
