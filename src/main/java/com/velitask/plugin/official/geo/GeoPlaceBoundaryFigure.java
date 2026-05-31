package com.velitask.plugin.official.geo;

import com.velitask.plugin.official.GeoMapIndicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.figures.Figure;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DisplayHint;
import com.velitask.sdk.properties.EnumArrayProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.LineProperty;
import com.velitask.sdk.properties.MapPlacesProperty;
import com.velitask.sdk.properties.MapVectorProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.SizeProperty;
import com.velitask.sdk.properties.TransparencyProperty;
import com.velitask.sdk.properties.VisibleProperty;
import com.velitask.sdk.vector.OSMDataTypes;
import com.velitask.sdk.vector.OSMFeature;
import com.velitask.sdk.vector.OSMGeometryType;
import com.velitask.sdk.vector.OSMVectorTile;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.abricos.geo.GeoUtils;

public class GeoPlaceBoundaryFigure extends Figure<GeoFigureContext> {

    public static final String NAME = "placeBoundary";

    private static final String KEY = GeoMapIndicator.KEY + "." + NAME;

    private static final int MAX_DATA_ZOOM = 12;

    private static final int NEAREST_MAX_RADIUS = 6;
    private static final int NEAREST_SAFETY_RINGS = 1;

    private static final Set<String> ALL_PLACE_TYPES = Set.of(
            OSMDataTypes.CITY, OSMDataTypes.TOWN, OSMDataTypes.VILLAGE, OSMDataTypes.HAMLET);

    private static final int CIRCLE_POINTS = 24;

    public enum DisplayMode {
        ACTIVE, NEAREST, ALL
    }

    private final EnumArrayProperty<DisplayMode> mMode
            = new EnumArrayProperty<>(DisplayMode.class) {
        @Override
        public String getName() {
            return "mode";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".mode.title");
        }

        @Override
        protected String[] defineTitles() {
            return new String[]{
                localized(KEY + ".mode.active"),
                localized(KEY + ".mode.nearest"),
                localized(KEY + ".mode.all")
            };
        }
    };

    private final MapVectorProperty mVector = new MapVectorProperty();
    private final MapPlacesProperty mPlaces = new MapPlacesProperty();

    private final LineProperty mLine = new LineProperty();

    {
        mLine.getColor().setDefault(new Color(180, 80, 0));
        mLine.getColor().set(new Color(180, 80, 0));
        mLine.getThickness().setRange(1, 20);
        mLine.getThickness().setDefault(2);
        mLine.getThickness().set(2);
    }

    private static final String FILL_COLOR = "fillColor";
    private final ColorProperty mFillColor = new ColorProperty(
            new Color(255, 160, 60, 60), FILL_COLOR, localized(KEY + "." + FILL_COLOR + ".title"));

    private final VisibleProperty mShowFill = toggle("showFill", false);

    private final VisibleProperty mShowHighlight = toggle("showHighlight", true);

    private static final String HIGHLIGHT_COLOR = "highlightColor";
    private final ColorProperty mHighlightColor = new ColorProperty(
            new Color(255, 210, 70), HIGHLIGHT_COLOR, localized(KEY + "." + HIGHLIGHT_COLOR + ".title"));

    private final TransparencyProperty mHighlightTransparency = new TransparencyProperty() {
        @Override
        public Integer getDefault() {
            return 55;
        }
    };

    private final VisibleProperty mCityShow = toggle("cityShow", true);
    private final SizeProperty mCityZoom = zoomSpinner("cityZoom", 8);

    private final VisibleProperty mTownShow = toggle("townShow", true);
    private final SizeProperty mTownZoom = zoomSpinner("townZoom", 10);

    private final VisibleProperty mVillageShow = toggle("villageShow", true);
    private final SizeProperty mVillageZoom = zoomSpinner("villageZoom", 12);

    private final VisibleProperty mHamletShow = toggle("hamletShow", true);
    private final SizeProperty mHamletZoom = zoomSpinner("hamletZoom", 14);

    private VisibleProperty toggle(String name, boolean def) {
        return new VisibleProperty() {
            {
                setDefault(def);
                set(def);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getTitle() {
                return localized(KEY + "." + name + ".title");
            }
        };
    }

    private SizeProperty zoomSpinner(String name, int def) {
        return new SizeProperty() {
            {
                setRange(1, 19);
            }

            @Override
            public Integer getDefault() {
                return def;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getTitle() {
                return localized(KEY + "." + name + ".title");
            }
        };
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
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mMode,
            mLine, mFillColor, mShowFill,
            mShowHighlight, mHighlightColor, mHighlightTransparency,
            mCityShow, mCityZoom,
            mTownShow, mTownZoom,
            mVillageShow, mVillageZoom,
            mHamletShow, mHamletZoom
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mMode, PropertyGroup.APPEARANCE);
        config.set(mLine, PropertyGroup.APPEARANCE);
        config.set(mFillColor, PropertyGroup.APPEARANCE);
        config.set(mShowFill, PropertyGroup.APPEARANCE);
        config.set(mShowHighlight, PropertyGroup.APPEARANCE);
        config.set(mHighlightColor, PropertyGroup.APPEARANCE);
        config.set(mHighlightTransparency, PropertyGroup.APPEARANCE);
        for (SizeProperty s : new SizeProperty[]{mCityZoom, mTownZoom, mVillageZoom, mHamletZoom}) {
            config.set(s, PropertyGroup.APPEARANCE, DisplayHint.SPINNER);
        }
        for (VisibleProperty v : new VisibleProperty[]{mCityShow, mTownShow, mVillageShow, mHamletShow}) {
            config.set(v, PropertyGroup.APPEARANCE);
        }
    }

    @Override
    public void render(GeoFigureContext baseCtx) {
        BoundaryContext ctx = (BoundaryContext) baseCtx;
        if (ctx.zoom == null) {
            return;
        }
        int z = ctx.zoom.getLevel();

        Set<String> required = new LinkedHashSet<>();
        if (ctx.cityShow && z >= ctx.cityZoom) {
            required.add(OSMDataTypes.CITY);
        }
        if (ctx.townShow && z >= ctx.townZoom) {
            required.add(OSMDataTypes.TOWN);
        }
        if (ctx.villageShow && z >= ctx.villageZoom) {
            required.add(OSMDataTypes.VILLAGE);
        }
        if (ctx.hamletShow && z >= ctx.hamletZoom) {
            required.add(OSMDataTypes.HAMLET);
        }

        boolean active = ctx.displayMode == DisplayMode.ACTIVE;
        boolean nearest = ctx.displayMode == DisplayMode.NEAREST;
        boolean hasPos = !Double.isNaN(ctx.indicator.posLat) && !Double.isNaN(ctx.indicator.posLon);
        boolean needCurrent = hasPos && (active || ctx.showHighlight);
        if (active || nearest) {
            if (!hasPos) {
                return;
            }
        } else if (required.isEmpty() && !ctx.showHighlight) {
            return;
        }

        int dataZoom = Math.min(z, MAX_DATA_ZOOM);
        int shift = z - dataZoom;
        int maxData = (1 << dataZoom) - 1;
        boolean preview = ctx.indicator.player.isPreview;

        Set<String> seenPoly = new HashSet<>();
        Set<String> seenNode = new HashSet<>();
        List<OSMFeature> polygons = new ArrayList<>();
        List<OSMFeature> nodes = new ArrayList<>();
        Set<String> nodeSubtypes = active
                ? ALL_PLACE_TYPES
                : (nearest ? (required.isEmpty() ? ALL_PLACE_TYPES : required) : null);

        if (active) {
            int px = clamp(((int) (ctx.zoom.pixelX(ctx.indicator.posLon) / GeoUtils.TILE_SIZE)) >> shift, maxData);
            int py = clamp(((int) (ctx.zoom.pixelY(ctx.indicator.posLat) / GeoUtils.TILE_SIZE)) >> shift, maxData);
            collectTile(px, py, dataZoom, preview, seenPoly, polygons, nodeSubtypes, seenNode, nodes);
        } else if (nearest) {
            int ptx = ((int) (ctx.zoom.pixelX(ctx.indicator.posLon) / GeoUtils.TILE_SIZE)) >> shift;
            int pty = ((int) (ctx.zoom.pixelY(ctx.indicator.posLat) / GeoUtils.TILE_SIZE)) >> shift;
            Set<Long> requested = new HashSet<>();
            int found = -1;
            for (int rad = 0; rad <= NEAREST_MAX_RADIUS; rad++) {
                boolean pending = collectRing(ptx, pty, rad, dataZoom, maxData, preview,
                        requested, seenPoly, polygons, nodeSubtypes, seenNode, nodes);
                boolean any = !polygons.isEmpty() || !nodes.isEmpty();
                if (any && found < 0) {
                    found = rad;
                }
                if (found >= 0 && rad >= found + NEAREST_SAFETY_RINGS) {
                    break;
                }
                if (found < 0 && preview && pending) {
                    break;
                }
            }
        } else {
            double cx = ctx.centerPx;
            double cy = ctx.centerPy;
            double extra = Math.max(0.05, ctx.indicator.extraScale);
            double radiusPx = Math.hypot(ctx.width, ctx.height) / extra + GeoUtils.TILE_SIZE;
            int maxIndex = (1 << z) - 1;
            int xMin = Math.max((int) Math.floor((cx - radiusPx) / GeoUtils.TILE_SIZE) - 1, 0);
            int yMin = Math.max((int) Math.floor((cy - radiusPx) / GeoUtils.TILE_SIZE) - 1, 0);
            int xMax = Math.min((int) Math.floor((cx + radiusPx) / GeoUtils.TILE_SIZE) + 1, maxIndex);
            int yMax = Math.min((int) Math.floor((cy + radiusPx) / GeoUtils.TILE_SIZE) + 1, maxIndex);
            Set<Long> requested = new HashSet<>();
            for (int tx = xMin; tx <= xMax; tx++) {
                for (int ty = yMin; ty <= yMax; ty++) {
                    int px = tx >> shift;
                    int py = ty >> shift;
                    long pkey = (((long) px) << 32) | (py & 0xffffffffL);
                    if (!requested.add(pkey)) {
                        continue;
                    }
                    collectTile(px, py, dataZoom, preview, seenPoly, polygons, null, seenNode, nodes);
                }
            }
        }

        if (active || nearest) {
            int areaCount = polygons.size();
            for (OSMFeature node : nodes) {
                boolean inArea = false;
                for (int i = 0; i < areaCount; i++) {
                    if (contains(polygons.get(i).rings(), node.lon(), node.lat())) {
                        inArea = true;
                        break;
                    }
                }
                if (!inArea) {
                    polygons.add(circleFeature(node));
                }
            }
        }
        if (polygons.isEmpty()) {
            return;
        }

        Graphics2D g = ctx.graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float lw = (float) Math.max(1.0, ctx.line.thickness * ctx.scale);
        BasicStroke baseStroke = new BasicStroke(lw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke highlightStroke = new BasicStroke(lw * 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        Color baseFill = ctx.fillColor;
        int hlAlpha = (int) Math.round(255.0 * (100 - ctx.highlightTransparency) / 100.0);
        Color highlightFill = new Color(ctx.highlightColor.getRed(), ctx.highlightColor.getGreen(),
                ctx.highlightColor.getBlue(), Math.max(0, Math.min(255, hlAlpha)));

        List<Path2D.Double> normal = new ArrayList<>();
        OSMFeature special = null;
        double bestDist = Double.MAX_VALUE;
        double cosLat = Math.cos(Math.toRadians(ctx.indicator.posLat));
        for (OSMFeature poly : polygons) {
            double[][][] rings = poly.rings();
            if (rings == null || rings.length == 0) {
                continue;
            }
            if (poly.getName() == null || poly.getName().isBlank()) {
                continue;
            }
            if (nearest) {
                String type = poly.getSubtype();
                if (!required.isEmpty() && (type == null || !required.contains(type))) {
                    continue;
                }
                double d = polyDistance(rings, ctx.indicator.posLon, ctx.indicator.posLat, cosLat);
                if (d < bestDist) {
                    bestDist = d;
                    special = poly;
                }
                continue;
            }
            if (special == null && needCurrent
                    && contains(rings, ctx.indicator.posLon, ctx.indicator.posLat)) {
                special = poly;
                continue;
            }
            if (active) {
                continue;
            }
            String type = poly.getSubtype();
            if (type == null || !required.contains(type)) {
                continue;
            }
            Path2D.Double path = buildPath(ctx, rings);
            if (path != null) {
                normal.add(path);
            }
        }
        Path2D.Double current = special != null ? buildPath(ctx, special.rings()) : null;

        for (Path2D.Double path : normal) {
            if (ctx.showFill) {
                g.setColor(baseFill);
                g.fill(path);
            }
            g.setColor(ctx.line.color);
            g.setStroke(baseStroke);
            g.draw(path);
        }
        if (current != null) {
            if (ctx.showHighlight) {
                g.setColor(highlightFill);
                g.fill(current);
            } else if (ctx.showFill) {
                g.setColor(baseFill);
                g.fill(current);
            }
            g.setColor(ctx.line.color);
            g.setStroke(ctx.showHighlight ? highlightStroke : baseStroke);
            g.draw(current);
        }
    }

    private boolean collectRing(int ptx, int pty, int rad, int dataZoom, int maxData, boolean preview,
            Set<Long> requested, Set<String> seenPoly, List<OSMFeature> polygons,
            Set<String> nodeSubtypes, Set<String> seenNode, List<OSMFeature> nodes) {
        boolean pending = false;
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dy = -rad; dy <= rad; dy++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) != rad) {
                    continue;
                }
                int px = clamp(ptx + dx, maxData);
                int py = clamp(pty + dy, maxData);
                long pkey = (((long) px) << 32) | (py & 0xffffffffL);
                if (!requested.add(pkey)) {
                    continue;
                }
                if (collectTile(px, py, dataZoom, preview, seenPoly, polygons,
                        nodeSubtypes, seenNode, nodes)) {
                    pending = true;
                }
            }
        }
        return pending;
    }

    private boolean collectTile(int px, int py, int dataZoom, boolean preview,
            Set<String> seenPoly, List<OSMFeature> polygons,
            Set<String> nodeSubtypes, Set<String> seenNode, List<OSMFeature> nodes) {
        OSMVectorTile bt = preview
                ? mVector.getFeatures(OSMDataTypes.PLACE_BOUNDARY, dataZoom, px, py)
                : mVector.getFeaturesBlocking(OSMDataTypes.PLACE_BOUNDARY, dataZoom, px, py, 5_000);
        boolean pending = !bt.isLoaded();
        if (bt.getFeatures() != null) {
            for (OSMFeature f : bt.getFeatures()) {
                if (f.getId() != null && !seenPoly.add(f.getId())) {
                    continue;
                }
                polygons.add(f);
            }
        }
        if (nodeSubtypes != null && !nodeSubtypes.isEmpty()) {
            OSMVectorTile pt = preview
                    ? mPlaces.getPlaces(dataZoom, px, py, nodeSubtypes)
                    : mPlaces.getPlacesBlocking(dataZoom, px, py, nodeSubtypes, 5_000);
            if (pt.getFeatures() != null) {
                for (OSMFeature f : pt.getFeatures()) {
                    if (f.getId() != null && !seenNode.add(f.getId())) {
                        continue;
                    }
                    nodes.add(f);
                }
            }
        }
        return pending;
    }

    private static OSMFeature circleFeature(OSMFeature node) {
        double lon = node.lon();
        double lat = node.lat();
        double rMeters = radiusMeters(node.getSubtype());
        double latR = rMeters / 111_320.0;
        double lonR = rMeters / (111_320.0 * Math.cos(Math.toRadians(lat)));
        double[][] ring = new double[CIRCLE_POINTS + 1][];
        for (int i = 0; i <= CIRCLE_POINTS; i++) {
            double a = 2.0 * Math.PI * i / CIRCLE_POINTS;
            ring[i] = new double[]{lon + lonR * Math.cos(a), lat + latR * Math.sin(a)};
        }
        return new OSMFeature(node.getId(), node.getName(), node.getSubtype(),
                OSMGeometryType.POLYGON, new double[][][]{ring});
    }

    private static double radiusMeters(String subtype) {
        if (subtype == null) {
            return 300.0;
        }
        switch (subtype) {
            case "city":
                return 2000.0;
            case "town":
                return 1000.0;
            case "village":
                return 500.0;
            case "hamlet":
                return 250.0;
            default:
                return 300.0;
        }
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(v, max));
    }

    private static double polyDistance(double[][][] rings, double lon, double lat, double cosLat) {
        if (contains(rings, lon, lat)) {
            return 0.0;
        }
        double min = Double.MAX_VALUE;
        for (double[][] ring : rings) {
            if (ring == null || ring.length < 2) {
                continue;
            }
            for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
                double d = segDistance(lon, lat, ring[j][0], ring[j][1], ring[i][0], ring[i][1], cosLat);
                if (d < min) {
                    min = d;
                }
            }
        }
        return min;
    }

    private static double segDistance(double plon, double plat,
            double alon, double alat, double blon, double blat, double cosLat) {
        double px = plon * cosLat;
        double ax = alon * cosLat;
        double bx = blon * cosLat;
        double dx = bx - ax;
        double dy = blat - alat;
        double len2 = dx * dx + dy * dy;
        double t = len2 > 0 ? ((px - ax) * dx + (plat - alat) * dy) / len2 : 0.0;
        t = Math.max(0.0, Math.min(1.0, t));
        double cxp = ax + t * dx;
        double cyp = alat + t * dy;
        double ex = px - cxp;
        double ey = plat - cyp;
        return Math.sqrt(ex * ex + ey * ey);
    }

    private static boolean contains(double[][][] rings, double lon, double lat) {
        boolean inside = false;
        for (double[][] ring : rings) {
            if (ring == null || ring.length < 3) {
                continue;
            }
            int n = ring.length;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = ring[i][0];
                double yi = ring[i][1];
                double xj = ring[j][0];
                double yj = ring[j][1];
                boolean intersect = ((yi > lat) != (yj > lat))
                        && (lon < (xj - xi) * (lat - yi) / (yj - yi) + xi);
                if (intersect) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static Path2D.Double buildPath(BoundaryContext ctx, double[][][] rings) {
        Path2D.Double path = new Path2D.Double();
        for (double[][] ring : rings) {
            if (ring == null || ring.length < 2) {
                continue;
            }
            for (int i = 0; i < ring.length; i++) {
                Point2D.Double p = ctx.toScreen(ring[i][1], ring[i][0]);
                if (i == 0) {
                    path.moveTo(p.x, p.y);
                } else {
                    path.lineTo(p.x, p.y);
                }
            }
            path.closePath();
        }
        return path;
    }

    @Override
    public GeoFigureContext createContext(IndicatorContext indCtx) {
        return new BoundaryContext((GeoMapIndicator.GeoMapContext) indCtx);
    }

    public class BoundaryContext extends GeoFigureContext {

        public final DisplayMode displayMode;
        public final LineProperty.LineContext line;
        public final Color fillColor;
        public final boolean showFill;
        public final boolean showHighlight;
        public final Color highlightColor;
        public final int highlightTransparency;

        public final boolean cityShow;
        public final int cityZoom;
        public final boolean townShow;
        public final int townZoom;
        public final boolean villageShow;
        public final int villageZoom;
        public final boolean hamletShow;
        public final int hamletZoom;

        public BoundaryContext(GeoMapIndicator.GeoMapContext indCtx) {
            super(indCtx);
            displayMode = mMode.createContext().value;
            line = mLine.createContext();
            fillColor = mFillColor.createContext().value;
            showFill = mShowFill.createContext().value;
            showHighlight = mShowHighlight.createContext().value;
            highlightColor = mHighlightColor.createContext().value;
            highlightTransparency = mHighlightTransparency.createContext().value;

            cityShow = mCityShow.createContext().value;
            cityZoom = mCityZoom.createContext().value;
            townShow = mTownShow.createContext().value;
            townZoom = mTownZoom.createContext().value;
            villageShow = mVillageShow.createContext().value;
            villageZoom = mVillageZoom.createContext().value;
            hamletShow = mHamletShow.createContext().value;
            hamletZoom = mHamletZoom.createContext().value;
        }
    }
}
