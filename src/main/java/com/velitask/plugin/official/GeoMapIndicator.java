package com.velitask.plugin.official;

import com.velitask.plugin.official.geo.GeoPlaceBoundaryFigure;
import com.velitask.plugin.official.geo.GeoPlaceLabelsFigure;
import com.velitask.plugin.official.geo.GeoPositionFigure;
import com.velitask.plugin.official.geo.GeoTrackFigure;
import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.data.GeoSensorAtom;
import com.velitask.sdk.data.LandBounds;
import com.velitask.sdk.db.DataCacheRule;
import com.velitask.sdk.db.DataParams;
import com.velitask.sdk.figures.IFigure;
import com.velitask.sdk.osm.OSMTile;
import com.velitask.sdk.properties.ArrayProperty;
import com.velitask.sdk.properties.BorderProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DisplayHint;
import com.velitask.sdk.properties.EnumArrayProperty;
import com.velitask.sdk.properties.FitTrackProperty;
import com.velitask.sdk.properties.GeoSensorProperty;
import com.velitask.sdk.properties.GeoZoomProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.IntegerProperty;
import com.velitask.sdk.properties.MapTilesProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.TransparencyProperty;
import com.velitask.sdk.properties.VisibleProperty;
import com.velitask.units.angle.TrackBearing;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;
import org.abricos.geo.GeoUtils;
import org.abricos.geo.MapZoom;
import org.abricos.geo.tile.TilePixel;
import org.json.JSONObject;

public class GeoMapIndicator extends Indicator {

    public static final String NAME = "geoMap";

    public static final String KEY = _KEY + "." + NAME;

    public enum RotationMode {
        NORTH_UP, HEADING_UP, MANUAL
    }

    public enum VehicleAnchor {
        CENTER, BOTTOM_THIRD
    }

    private final GeoSensorProperty mGeoSensor = new GeoSensorProperty();

    {
        mGeoSensor.query("track")
                .where("zoom = {zoom}")
                .orderBy("timeRaw")
                .cache(DataCacheRule.byParams())
                .cacheSize(2)
                .buildList();
    }

    private final MapTilesProperty mTiles = new MapTilesProperty();

    private final GeoZoomProperty mGeoZoom = new GeoZoomProperty();

    private final TransparencyProperty mMapTransparency = new TransparencyProperty();

    private final VisibleProperty mShowMap = new VisibleProperty();

    private final BorderProperty mBorder = new BorderProperty();

    private final IntegerProperty mTilt = new IntegerProperty() {
        {
            setRange(0, 80);
        }

        @Override
        public String getName() {
            return "tilt";
        }

        @Override
        public Integer getDefault() {
            return 50;
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".tilt.title");
        }
    };

    private final VisibleProperty mView3D = new VisibleProperty() {
        {
            setDefault(false);
            set(false);
        }

        @Override
        public String getName() {
            return "view3D";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".view3D.title");
        }

        @Override
        public IProperty[] dependents() {
            return new IProperty[]{mTilt};
        }

        @Override
        protected void onChanged(Object oldVal, Object newVal) {
            mTilt.setEnabled(Boolean.TRUE.equals(newVal));
        }

        @Override
        public void fromJSON(JSONObject json) {
            super.fromJSON(json);
            mTilt.setEnabled(get());
        }
    };

    private final IntegerProperty mManualBearing = new IntegerProperty() {
        {
            setRange(0, 359);
        }

        @Override
        public String getName() {
            return "manualBearing";
        }

        @Override
        public Integer getDefault() {
            return 0;
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".manualBearing.title");
        }
    };

    private final IntegerProperty mMinSpeedKmh = new IntegerProperty() {
        {
            setRange(0, 50);
        }

        @Override
        public String getName() {
            return "minSpeedKmh";
        }

        @Override
        public Integer getDefault() {
            return 2;
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".minSpeedKmh.title");
        }
    };

    private final EnumArrayProperty<VehicleAnchor> mVehicleAnchor
            = new EnumArrayProperty<>(VehicleAnchor.class) {
        @Override
        public String getName() {
            return "vehicleAnchor";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".vehicleAnchor.title");
        }

        @Override
        protected String[] defineTitles() {
            return new String[]{
                localized(KEY + ".vehicleAnchor.center"),
                localized(KEY + ".vehicleAnchor.bottomThird")
            };
        }
    };

    private final EnumArrayProperty<RotationMode> mRotation
            = new EnumArrayProperty<>(RotationMode.class) {
        @Override
        public String getName() {
            return "rotation";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".rotation.title");
        }

        @Override
        protected String[] defineTitles() {
            return new String[]{
                localized(KEY + ".rotation.northUp"),
                localized(KEY + ".rotation.headingUp"),
                localized(KEY + ".rotation.manual")
            };
        }

        @Override
        public IProperty[] dependents() {
            return new IProperty[]{mManualBearing, mMinSpeedKmh};
        }

        @Override
        protected void onChanged(Object oldVal, Object newVal) {
            applyRotationDependents();
        }

        @Override
        public void fromJSON(JSONObject json) {
            super.fromJSON(json);
            applyRotationDependents();
        }
    };

    private final FitTrackProperty mFitTrack = new FitTrackProperty() {
        @Override
        public IProperty[] dependents() {
            return new IProperty[]{mGeoZoom};
        }

        @Override
        protected void onChanged(Object oldVal, Object newVal) {
            mGeoZoom.setEnabled(!Boolean.TRUE.equals(newVal));
        }

        @Override
        public void fromJSON(JSONObject json) {
            super.fromJSON(json);
            mGeoZoom.setEnabled(!get());
        }
    };

    private double mLastValidBearingDeg = 0;

    private void applyRotationDependents() {
        RotationMode m = mRotation.get();
        mManualBearing.setEnabled(m == RotationMode.MANUAL);
        mMinSpeedKmh.setEnabled(m == RotationMode.HEADING_UP);
    }

    public GeoSensorProperty getGeo() {
        return mGeoSensor;
    }

    public FitTrackProperty getFitTrack() {
        return mFitTrack;
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
        maket.setLayerMargin(50d, null, null, 50d);
        maket.setLayerSize(700d, 400d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mGeoSensor, mTiles, mShowMap, mFitTrack, mGeoZoom, mMapTransparency, mBorder,
            mView3D, mTilt, mRotation, mManualBearing, mMinSpeedKmh, mVehicleAnchor
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mShowMap, PropertyGroup.APPEARANCE);
        config.set(mFitTrack, PropertyGroup.APPEARANCE);
        config.set(mGeoZoom, PropertyGroup.APPEARANCE);
        config.set(mMapTransparency, PropertyGroup.APPEARANCE);
        config.set(mBorder, PropertyGroup.APPEARANCE);
        config.set(mView3D, PropertyGroup.APPEARANCE);
        config.set(mTilt, PropertyGroup.APPEARANCE, DisplayHint.SLIDER);
        config.set(mRotation, PropertyGroup.APPEARANCE);
        config.set(mManualBearing, PropertyGroup.APPEARANCE, DisplayHint.SLIDER);
        config.set(mMinSpeedKmh, PropertyGroup.APPEARANCE);
        config.set(mVehicleAnchor, PropertyGroup.APPEARANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends IFigure>[] supportedFigures() {
        return (Class<? extends IFigure>[]) new Class<?>[]{
            GeoPlaceBoundaryFigure.class,
            GeoTrackFigure.class,
            GeoPositionFigure.class,
            GeoPlaceLabelsFigure.class
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends IFigure>[] defaultFigures() {
        return (Class<? extends IFigure>[]) new Class<?>[]{
            GeoPlaceBoundaryFigure.class,
            GeoTrackFigure.class,
            GeoPositionFigure.class,
            GeoPlaceLabelsFigure.class
        };
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("faded", localized(KEY + ".skin.faded"))
            .set(mShowMap.skin(true))
            .set(mMapTransparency.skin(60))
            .build(),
            IndicatorSkin.builder("hidden", localized(KEY + ".skin.hidden"))
            .set(mShowMap.skin(false))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        GeoMapContext ctx = (GeoMapContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (!ctx.showMap.value) {
            return;
        }

        renderMap(ctx);
    }

    private void renderMap(GeoMapContext ctx) {
        Graphics2D g = ctx.graphics;
        int w = ctx.width, h = ctx.height;

        MapZoom zoom = ctx.zoom;
        if (zoom == null) {
            return;
        }

        Shape oldClip = g.getClip();
        int radius = (int) (ctx.border.radius * ctx.scale);
        if (radius > 0) {
            g.setClip(new RoundRectangle2D.Double(0, 0, w, h, radius * 2.0, radius * 2.0));
        }

        AffineTransform oldTransform = g.getTransform();
        g.transform(ctx.mapTransform);

        double invExtra = 1.0 / Math.max(0.0001, ctx.extraScale);
        double halfW = w / 2.0 * invExtra;
        double halfH;
        if (ctx.tiltRad > 0) {
            double cosTilt = Math.cos(ctx.tiltRad);
            halfH = (h / 2.0 + ctx.anchorOffsetY) / Math.max(0.05, cosTilt) * invExtra;
        } else {
            halfH = h / 2.0 * invExtra;
        }

        double centerX = ctx.centerPx;
        double centerY = ctx.centerPy;

        int xIndexMin, yIndexMin, xIndexMax, yIndexMax;
        if (ctx.bearingRad == 0 && ctx.tiltRad == 0) {
            xIndexMin = Math.max(
                    (int) Math.floor((centerX - halfW) / GeoUtils.TILE_SIZE) - 1, 0);
            yIndexMin = Math.max(
                    (int) Math.floor((centerY - halfH) / GeoUtils.TILE_SIZE) - 1, 0);
            xIndexMax = (int) Math.floor((centerX + halfW) / GeoUtils.TILE_SIZE) + 1;
            yIndexMax = (int) Math.floor((centerY + halfH) / GeoUtils.TILE_SIZE) + 1;
        } else {
            double radiusPx = Math.hypot(halfW, halfH) + GeoUtils.TILE_SIZE;
            xIndexMin = Math.max(
                    (int) Math.floor((centerX - radiusPx) / GeoUtils.TILE_SIZE) - 1, 0);
            yIndexMin = Math.max(
                    (int) Math.floor((centerY - radiusPx) / GeoUtils.TILE_SIZE) - 1, 0);
            xIndexMax = (int) Math.floor((centerX + radiusPx) / GeoUtils.TILE_SIZE) + 1;
            yIndexMax = (int) Math.floor((centerY + radiusPx) / GeoUtils.TILE_SIZE) + 1;
        }

        double horizonCullY = -halfH * 1.5;

        Composite oldComp = g.getComposite();
        float alpha = Math.max(0, Math.min(1, 1f - ctx.mapTransparency.value / 100f));
        if (alpha < 1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        int zoomValue = zoom.getLevel();
        for (int tx = xIndexMin; tx <= xIndexMax; tx++) {
            for (int ty = yIndexMin; ty <= yIndexMax; ty++) {
                double localY = ty * GeoUtils.TILE_SIZE - centerY;
                if (localY + GeoUtils.TILE_SIZE < horizonCullY) {
                    continue;
                }
                OSMTile tile = ctx.player.isPreview
                        ? mTiles.getTile(zoomValue, tx, ty)
                        : mTiles.getTileBlocking(zoomValue, tx, ty, 5_000);
                if (tile == null || tile.getImage() == null) {
                    continue;
                }
                g.drawImage(tile.getImage(),
                        tx * GeoUtils.TILE_SIZE, ty * GeoUtils.TILE_SIZE, null);
            }
        }

        g.setComposite(oldComp);
        g.setTransform(oldTransform);

        if (radius > 0) {
            g.setClip(oldClip);
        }

        drawBorder(g, ctx, w, h, radius);
    }

    private static void drawBorder(Graphics2D g, GeoMapContext ctx, int w, int h, int radius) {
        int bw = (int) (ctx.border.thickness * ctx.scale);
        if (bw <= 0) {
            return;
        }
        g.setColor(ctx.border.color);
        g.setStroke(new BasicStroke(bw));
        int bx = bw / 2, by = bw / 2;
        int bw2 = Math.max(1, w - bw);
        int bh2 = Math.max(1, h - bw);
        if (radius > 0) {
            g.drawRoundRect(bx, by, bw2, bh2, radius * 2, radius * 2);
        } else {
            g.drawRect(bx, by, bw2, bh2);
        }
    }

    private record FitZoom(int intZoom, double extraScale) {}

    private static final double FIT_THRESHOLD = Math.sqrt(2.0);

    private static FitZoom computeFitZoom(int baseWidth, int baseHeight, LandBounds b) {
        int padding = Math.max(4, Math.min(40,
                (int) Math.round(Math.min(baseWidth, baseHeight) * 0.05)));
        int innerW = Math.max(1, baseWidth - 2 * padding);
        int innerH = Math.max(1, baseHeight - 2 * padding);

        int intZoom = GeoUtils.ZOOM_MIN;
        double fitW = 0, fitH = 0;
        for (int i = GeoUtils.ZOOM_MIN; i <= GeoUtils.ZOOM_MAX; i++) {
            TilePixel px1 = GeoUtils.getTileX(b.lonMin(), i);
            TilePixel py1 = GeoUtils.getTileY(b.latMin(), i);
            TilePixel px2 = GeoUtils.getTileX(b.lonMax(), i);
            TilePixel py2 = GeoUtils.getTileY(b.latMax(), i);

            double figureWidth = Math.abs(px2.getPixelGlobal() - px1.getPixelGlobal());
            double figureHeight = Math.abs(py2.getPixelGlobal() - py1.getPixelGlobal());

            if (figureWidth > innerW * FIT_THRESHOLD
                    || figureHeight > innerH * FIT_THRESHOLD) {
                if (i == GeoUtils.ZOOM_MIN) {
                    double extra = Math.min(innerW / figureWidth, innerH / figureHeight);
                    return new FitZoom(GeoUtils.ZOOM_MIN, extra);
                }
                break;
            }
            intZoom = i;
            fitW = figureWidth;
            fitH = figureHeight;
        }

        if (fitW <= 0 || fitH <= 0) {
            return new FitZoom(intZoom, 1.0);
        }
        double extra = Math.min(innerW / fitW, innerH / fitH);
        return new FitZoom(intZoom, extra);
    }

    private static final int BEARING_WINDOW = 16;

    private double computeHeadingBearing(MapZoom zm, long rawTime) {
        int actualZoom = mGeoSensor.approximateZoom(zm.getLevel());
        DataParams params = DataParams.of("sensorId", mGeoSensor.getSensorId())
                .set("zoom", actualZoom);
        List<GeoSensorAtom> points = mGeoSensor.queryList("track", params);
        if (points == null || points.size() < 2) {
            return Double.NaN;
        }
        int from = lowerBoundByTimeRaw(points, rawTime);
        if (from >= points.size() - 1) {
            from = points.size() - 2;
        }
        int to = Math.min(from + BEARING_WINDOW, points.size());
        if (to - from < 2) {
            return Double.NaN;
        }
        double[] lats = new double[to - from];
        double[] lons = new double[to - from];
        for (int i = from; i < to; i++) {
            GeoSensorAtom p = points.get(i);
            lats[i - from] = p.lat;
            lons[i - from] = p.lon;
        }
        return TrackBearing.fromPoints(lats, lons);
    }

    private static int lowerBoundByTimeRaw(List<GeoSensorAtom> points, long t) {
        int lo = 0;
        int hi = points.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (points.get(mid).timeRaw < t) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return Math.max(0, lo - 1);
    }

    private static double estimateSpeedKmh(GeoSensorAtom atom) {
        if (atom.duration <= 0) {
            return 0.0;
        }
        double latRad = Math.toRadians(atom.lat);
        double dyM = atom.latDelta * 111_320.0;
        double dxM = atom.lonDelta * 111_320.0 * Math.cos(latRad);
        double distM = Math.hypot(dxM, dyM);
        double seconds = atom.duration / 1000.0;
        return distM / seconds * 3.6;
    }

    public class GeoMapContext extends IndicatorContext {

        public final IntegerProperty.IntegerContext mapTransparency;
        public final BorderProperty.BorderContext border;
        public final VisibleProperty.BooleanContext showMap;
        public final FitTrackProperty.BooleanContext fitTrack;
        public final VisibleProperty.BooleanContext view3D;
        public final IntegerProperty.IntegerContext tilt;
        public final IntegerProperty.IntegerContext manualBearing;
        public final IntegerProperty.IntegerContext minSpeedKmh;
        public final ArrayProperty<RotationMode>.ArrayContext rotation;
        public final ArrayProperty<VehicleAnchor>.ArrayContext vehicleAnchor;

        public final MapZoom zoom;
        public final double centerPx;
        public final double centerPy;
        public final double bearingDeg;
        public final double bearingRad;
        public final double tiltRad;
        public final double anchorOffsetY;
        public final double extraScale;
        public final double posLat;
        public final double posLon;
        public final AffineTransform mapTransform;

        public GeoMapContext(Player player, Canvas canvas) {
            super(player, canvas);
            fitTrack = mFitTrack.createContext();
            mapTransparency = mMapTransparency.createContext();
            border = mBorder.createContext();
            showMap = mShowMap.createContext();
            view3D = mView3D.createContext();
            tilt = mTilt.createContext();
            manualBearing = mManualBearing.createContext();
            minSpeedKmh = mMinSpeedKmh.createContext();
            rotation = mRotation.createContext();
            vehicleAnchor = mVehicleAnchor.createContext();

            MapZoom zm = null;
            double cPx = 0;
            double cPy = 0;
            double br = 0;
            double extra = 1.0;

            if (mGeoSensor.getSensorId() > 0) {
                if (fitTrack.value) {
                    LandBounds b = mGeoSensor.queryLandBounds();
                    if (b != null) {
                        FitZoom fz = computeFitZoom(width, height, b);
                        zm = new MapZoom(fz.intZoom());
                        extra = fz.extraScale();
                        double centerLat = (b.latMin() + b.latMax()) / 2.0;
                        double centerLon = (b.lonMin() + b.lonMax()) / 2.0;
                        cPx = zm.pixelX(centerLon);
                        cPy = zm.pixelY(centerLat);
                    }
                } else {
                    long rawTime = mGeoSensor.convertToRawTime(player.time);
                    if (player.isPreview) {
                        rawTime = mGeoSensor.clampToSensorRange(rawTime);
                    }
                    GeoSensorAtom atom = mGeoSensor.queryAtom(rawTime);
                    if (atom != null) {
                        zm = new MapZoom(mGeoZoom.createContext().value);
                        double lat = atom.calcLat(rawTime);
                        double lon = atom.calcLon(rawTime);
                        cPx = zm.pixelX(lon);
                        cPy = zm.pixelY(lat);

                        if (rotation.value == RotationMode.HEADING_UP) {
                            double speedKmh = estimateSpeedKmh(atom);
                            if (speedKmh >= minSpeedKmh.value) {
                                double deg = computeHeadingBearing(zm, rawTime);
                                if (!Double.isNaN(deg)) {
                                    mLastValidBearingDeg = deg;
                                }
                            }
                            br = mLastValidBearingDeg;
                        }
                    }
                }
            }
            double pLat = Double.NaN;
            double pLon = Double.NaN;
            if (mGeoSensor.getSensorId() > 0) {
                long posRawTime = mGeoSensor.convertToRawTime(player.time);
                if (player.isPreview) {
                    posRawTime = mGeoSensor.clampToSensorRange(posRawTime);
                }
                GeoSensorAtom posAtom = mGeoSensor.queryAtom(posRawTime);
                if (posAtom != null) {
                    pLat = posAtom.calcLat(posRawTime);
                    pLon = posAtom.calcLon(posRawTime);
                }
            }
            posLat = pLat;
            posLon = pLon;

            zoom = zm;
            centerPx = cPx;
            centerPy = cPy;
            bearingDeg = br;

            boolean fit = fitTrack.value;
            boolean is3D = !fit && view3D.value;
            double bRad;
            if (fit) {
                bRad = 0;
            } else {
                bRad = switch (rotation.value) {
                    case NORTH_UP ->
                        0.0;
                    case MANUAL ->
                        Math.toRadians(manualBearing.value);
                    case HEADING_UP ->
                        Math.toRadians(br);
                };
            }
            bearingRad = bRad;
            tiltRad = is3D ? Math.toRadians(tilt.value) : 0.0;
            anchorOffsetY = (!fit && vehicleAnchor.value == VehicleAnchor.BOTTOM_THIRD)
                    ? height / 6.0 : 0.0;
            extraScale = extra;

            AffineTransform t = new AffineTransform();
            t.translate(width / 2.0, height / 2.0 + anchorOffsetY);
            if (extraScale != 1.0) {
                t.scale(extraScale, extraScale);
            }
            if (tiltRad > 0) {
                t.scale(1.0, Math.cos(tiltRad));
            }
            if (bearingRad != 0) {
                t.rotate(-bearingRad);
            }
            t.translate(-centerPx, -centerPy);
            mapTransform = t;
        }
    }

    @Override
    public GeoMapContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new GeoMapContext(player, canvas);
    }

}
