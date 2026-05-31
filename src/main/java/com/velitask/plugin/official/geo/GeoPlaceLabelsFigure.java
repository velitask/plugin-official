package com.velitask.plugin.official.geo;

import com.velitask.plugin.official.GeoMapIndicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.figures.Figure;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DisplayHint;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.MapPlacesProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.SizeProperty;
import com.velitask.sdk.properties.VisibleProperty;
import com.velitask.sdk.vector.OSMDataTypes;
import com.velitask.sdk.vector.OSMFeature;
import com.velitask.sdk.vector.OSMVectorTile;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.abricos.geo.GeoUtils;

public class GeoPlaceLabelsFigure extends Figure<GeoFigureContext> {

    public static final String NAME = "placeLabels";

    private static final String KEY = GeoMapIndicator.KEY + "." + NAME;

    private static final int MAX_DATA_ZOOM = 12;

    private static final double DEPTH_STRENGTH = 0.88;
    private static final double MIN_DEPTH_SCALE = 0.4;
    private static final double MAX_DEPTH_SCALE = 1.6;

    private static final Font BASE_FONT = new Font("SansSerif", Font.BOLD, 12);

    private final MapPlacesProperty mPlaces = new MapPlacesProperty();

    private static final String TEXT_COLOR = "textColor";
    private final ColorProperty mTextColor = new ColorProperty(
            new Color(30, 30, 30), TEXT_COLOR, localized(KEY + "." + TEXT_COLOR + ".title"));

    private static final String HALO_COLOR = "haloColor";
    private final ColorProperty mHaloColor = new ColorProperty(
            Color.WHITE, HALO_COLOR, localized(KEY + "." + HALO_COLOR + ".title"));

    private final SizeProperty mFontSize = new SizeProperty() {
        {
            setRange(8, 72);
        }

        @Override
        public Integer getDefault() {
            return 16;
        }
    };

    private final VisibleProperty mShowMarker = new VisibleProperty() {
        {
            setDefault(false);
            set(false);
        }

        @Override
        public String getName() {
            return "showMarker";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".showMarker.title");
        }
    };

    private final VisibleProperty mCityShow = showToggle("cityShow", true);
    private final SizeProperty mCityZoom = zoomSpinner("cityZoom", 8);
    private final SizeProperty mCityDelta = deltaSpinner("cityDelta", 100);

    private final VisibleProperty mTownShow = showToggle("townShow", true);
    private final SizeProperty mTownZoom = zoomSpinner("townZoom", 10);
    private final SizeProperty mTownDelta = deltaSpinner("townDelta", 85);

    private final VisibleProperty mVillageShow = showToggle("villageShow", true);
    private final SizeProperty mVillageZoom = zoomSpinner("villageZoom", 12);
    private final SizeProperty mVillageDelta = deltaSpinner("villageDelta", 72);

    private final VisibleProperty mHamletShow = showToggle("hamletShow", true);
    private final SizeProperty mHamletZoom = zoomSpinner("hamletZoom", 14);
    private final SizeProperty mHamletDelta = deltaSpinner("hamletDelta", 62);

    private VisibleProperty showToggle(String name, boolean def) {
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

    private SizeProperty deltaSpinner(String name, int def) {
        return new SizeProperty() {
            {
                setRange(10, 300);
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
            mTextColor, mHaloColor, mFontSize, mShowMarker,
            mCityShow, mCityZoom, mCityDelta,
            mTownShow, mTownZoom, mTownDelta,
            mVillageShow, mVillageZoom, mVillageDelta,
            mHamletShow, mHamletZoom, mHamletDelta
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mTextColor, PropertyGroup.APPEARANCE);
        config.set(mHaloColor, PropertyGroup.APPEARANCE);
        config.set(mFontSize, PropertyGroup.APPEARANCE, DisplayHint.SPINNER);
        config.set(mShowMarker, PropertyGroup.APPEARANCE);
        for (SizeProperty s : new SizeProperty[]{
            mCityZoom, mCityDelta, mTownZoom, mTownDelta,
            mVillageZoom, mVillageDelta, mHamletZoom, mHamletDelta}) {
            config.set(s, PropertyGroup.APPEARANCE, DisplayHint.SPINNER);
        }
        for (VisibleProperty v : new VisibleProperty[]{
            mCityShow, mTownShow, mVillageShow, mHamletShow}) {
            config.set(v, PropertyGroup.APPEARANCE);
        }
    }

    @Override
    public void render(GeoFigureContext baseCtx) {
        LabelsContext ctx = (LabelsContext) baseCtx;
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
        if (required.isEmpty()) {
            return;
        }

        double cx = ctx.centerPx;
        double cy = ctx.centerPy;
        double extra = Math.max(0.05, ctx.indicator.extraScale);
        double radiusPx = Math.hypot(ctx.width, ctx.height) / extra + GeoUtils.TILE_SIZE;
        int maxIndex = (1 << z) - 1;
        int xMin = Math.max((int) Math.floor((cx - radiusPx) / GeoUtils.TILE_SIZE) - 1, 0);
        int yMin = Math.max((int) Math.floor((cy - radiusPx) / GeoUtils.TILE_SIZE) - 1, 0);
        int xMax = Math.min((int) Math.floor((cx + radiusPx) / GeoUtils.TILE_SIZE) + 1, maxIndex);
        int yMax = Math.min((int) Math.floor((cy + radiusPx) / GeoUtils.TILE_SIZE) + 1, maxIndex);

        Graphics2D g = ctx.graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int markerR = Math.max(2, (int) Math.round(3 * ctx.scale));
        int gap = markerR + Math.max(2, (int) Math.round(3 * ctx.scale));
        int haloO = Math.max(1, (int) Math.round(1.5 * ctx.scale));

        int dataZoom = Math.min(z, MAX_DATA_ZOOM);
        int shift = z - dataZoom;

        Set<Long> requested = new HashSet<>();
        Set<String> seen = new HashSet<>();
        List<OSMFeature> places = new ArrayList<>();
        for (int tx = xMin; tx <= xMax; tx++) {
            for (int ty = yMin; ty <= yMax; ty++) {
                int px = tx >> shift;
                int py = ty >> shift;
                long pkey = (((long) px) << 32) | (py & 0xffffffffL);
                if (!requested.add(pkey)) {
                    continue;
                }
                OSMVectorTile tile = ctx.indicator.player.isPreview
                        ? mPlaces.getPlaces(dataZoom, px, py, required)
                        : mPlaces.getPlacesBlocking(dataZoom, px, py, required, 5_000);
                List<OSMFeature> feats = tile.getFeatures();
                if (feats == null) {
                    continue;
                }
                for (OSMFeature f : feats) {
                    if (f.getId() != null && !seen.add(f.getId())) {
                        continue;
                    }
                    if (f.getName() == null || f.getName().isBlank()) {
                        continue;
                    }
                    places.add(f);
                }
            }
        }
        places.sort(Comparator.comparingInt(f -> rank(f.getSubtype())));

        List<Rectangle> placed = new ArrayList<>();
        for (OSMFeature f : places) {
            int deltaPct = deltaFor(ctx, f.getSubtype());
            double fs = Math.max(6, ctx.fontSize * ctx.scale * deltaPct / 100.0);
            g.setFont(BASE_FONT.deriveFont((float) fs));
            FontMetrics fm = g.getFontMetrics();

            Point2D.Double p = ctx.toScreen(f.lat(), f.lon());
            int px = (int) p.x;
            int py = (int) p.y;
            if (px < -200 || py < -200 || px > ctx.width + 200 || py > ctx.height + 200) {
                continue;
            }
            String name = f.getName();
            double ds = depthScale(ctx, py);
            int w = fm.stringWidth(name);
            int sw = (int) Math.round(w * ds);
            int sAsc = (int) Math.round(fm.getAscent() * ds);
            int sH = (int) Math.round(fm.getHeight() * ds);
            int labelY = ctx.showMarker ? py - gap : py + sAsc / 2;
            Rectangle box = new Rectangle(px - sw / 2, labelY - sAsc, sw, sH);
            boolean overlap = false;
            for (Rectangle r : placed) {
                if (r.intersects(box)) {
                    overlap = true;
                    break;
                }
            }
            if (overlap) {
                continue;
            }
            placed.add(box);

            if (ctx.showMarker) {
                g.setColor(ctx.textColor);
                g.fillOval(px - markerR, py - markerR, markerR * 2, markerR * 2);
                g.setColor(ctx.haloColor);
                g.drawOval(px - markerR, py - markerR, markerR * 2, markerR * 2);
            }
            Graphics2D gp = (Graphics2D) g.create();
            try {
                gp.translate(px, labelY);
                gp.scale(ds, ds);
                drawHaloText(gp, name, -w / 2, 0, ctx.haloColor, ctx.textColor, haloO);
            } finally {
                gp.dispose();
            }
        }
    }

    private static double depthScale(LabelsContext ctx, int py) {
        double tilt = ctx.indicator.tiltRad;
        if (tilt <= 0.01) {
            return 1.0;
        }
        double anchorY = ctx.height / 2.0 + ctx.indicator.anchorOffsetY;
        double rel = (anchorY - py) / Math.max(1.0, ctx.height);
        double factor = rel * Math.sin(tilt) * DEPTH_STRENGTH;
        double s = 1.0 / (1.0 + factor);
        return Math.max(MIN_DEPTH_SCALE, Math.min(MAX_DEPTH_SCALE, s));
    }

    private static int deltaFor(LabelsContext ctx, String subtype) {
        if (subtype == null) {
            return 100;
        }
        switch (subtype) {
            case "city":
                return ctx.cityDelta;
            case "town":
                return ctx.townDelta;
            case "village":
                return ctx.villageDelta;
            case "hamlet":
                return ctx.hamletDelta;
            default:
                return 100;
        }
    }

    private static int rank(String subtype) {
        if (subtype == null) {
            return 4;
        }
        switch (subtype) {
            case "city":
                return 0;
            case "town":
                return 1;
            case "village":
                return 2;
            case "hamlet":
                return 3;
            default:
                return 4;
        }
    }

    private static void drawHaloText(Graphics2D g, String s, int x, int y,
            Color halo, Color text, int o) {
        g.setColor(halo);
        for (int dx = -o; dx <= o; dx++) {
            for (int dy = -o; dy <= o; dy++) {
                if (dx != 0 || dy != 0) {
                    g.drawString(s, x + dx, y + dy);
                }
            }
        }
        g.setColor(text);
        g.drawString(s, x, y);
    }

    @Override
    public GeoFigureContext createContext(IndicatorContext indCtx) {
        return new LabelsContext((GeoMapIndicator.GeoMapContext) indCtx);
    }

    public class LabelsContext extends GeoFigureContext {

        public final Color textColor;
        public final Color haloColor;
        public final int fontSize;
        public final boolean showMarker;

        public final boolean cityShow;
        public final int cityZoom;
        public final int cityDelta;
        public final boolean townShow;
        public final int townZoom;
        public final int townDelta;
        public final boolean villageShow;
        public final int villageZoom;
        public final int villageDelta;
        public final boolean hamletShow;
        public final int hamletZoom;
        public final int hamletDelta;

        public LabelsContext(GeoMapIndicator.GeoMapContext indCtx) {
            super(indCtx);
            textColor = mTextColor.createContext().value;
            haloColor = mHaloColor.createContext().value;
            fontSize = mFontSize.createContext().value;
            showMarker = mShowMarker.createContext().value;

            cityShow = mCityShow.createContext().value;
            cityZoom = mCityZoom.createContext().value;
            cityDelta = mCityDelta.createContext().value;
            townShow = mTownShow.createContext().value;
            townZoom = mTownZoom.createContext().value;
            townDelta = mTownDelta.createContext().value;
            villageShow = mVillageShow.createContext().value;
            villageZoom = mVillageZoom.createContext().value;
            villageDelta = mVillageDelta.createContext().value;
            hamletShow = mHamletShow.createContext().value;
            hamletZoom = mHamletZoom.createContext().value;
            hamletDelta = mHamletDelta.createContext().value;
        }
    }
}
