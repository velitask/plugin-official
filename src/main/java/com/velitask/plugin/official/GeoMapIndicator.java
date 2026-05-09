package com.velitask.plugin.official;

import com.velitask.plugin.official.geo.GeoPositionFigure;
import com.velitask.plugin.official.geo.GeoTrackFigure;
import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.data.GeoSensorAtom;
import com.velitask.sdk.data.LandBounds;
import com.velitask.sdk.figures.IFigure;
import com.velitask.sdk.osm.OSMTile;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.FitTrackProperty;
import com.velitask.sdk.properties.GeoSensorProperty;
import com.velitask.sdk.properties.GeoZoomProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.IntegerProperty;
import com.velitask.sdk.properties.MapTilesProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.TransparencyProperty;
import com.velitask.sdk.properties.VisibleProperty;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;
import org.abricos.geo.GeoUtils;
import org.abricos.geo.MapZoom;
import org.abricos.geo.tile.TilePixel;

public class GeoMapIndicator extends Indicator {

    public static final String NAME = "geoMap";

    public static final String KEY = _KEY + "." + NAME;

    private final GeoSensorProperty mGeoSensor = new GeoSensorProperty();

    private final MapTilesProperty mTiles = new MapTilesProperty();

    private final GeoZoomProperty mGeoZoom = new GeoZoomProperty();

    private final TransparencyProperty mMapTransparency = new TransparencyProperty();

    private final VisibleProperty mShowMap = new VisibleProperty();

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
        public void fromJSON(org.json.JSONObject json) {
            super.fromJSON(json);
            mGeoZoom.setEnabled(!get());
        }
    };

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
        return new IProperty[]{mGeoSensor, mTiles, mShowMap, mFitTrack, mGeoZoom, mMapTransparency};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mShowMap, PropertyGroup.APPEARANCE);
        config.set(mFitTrack, PropertyGroup.APPEARANCE);
        config.set(mGeoZoom, PropertyGroup.APPEARANCE);
        config.set(mMapTransparency, PropertyGroup.APPEARANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends IFigure>[] supportedFigures() {
        return (Class<? extends IFigure>[]) new Class<?>[]{
            GeoTrackFigure.class,
            GeoPositionFigure.class
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends IFigure>[] defaultFigures() {
        return (Class<? extends IFigure>[]) new Class<?>[]{
            GeoTrackFigure.class,
            GeoPositionFigure.class
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

        if (ctx.showMap.value) {
            renderMap(ctx);
        }
    }

    private void renderMap(GeoMapContext ctx) {
        Graphics2D g = ctx.graphics;
        int w = ctx.width, h = ctx.height;

        MapZoom zoom = ctx.zoom;
        if (zoom == null) {
            return;
        }
        double centerX = ctx.centerPx;
        double centerY = ctx.centerPy;
        double topLeftX = centerX - w / 2.0;
        double topLeftY = centerY - h / 2.0;

        int xIndexMin = Math.max((int) Math.floor(topLeftX / GeoUtils.TILE_SIZE) - 1, 0);
        int yIndexMin = Math.max((int) Math.floor(topLeftY / GeoUtils.TILE_SIZE) - 1, 0);

        int xIndexMax = xIndexMin
                + (int) Math.max(Math.ceil((double) w / GeoUtils.TILE_SIZE), 1) + 2;
        int yIndexMax = yIndexMin
                + (int) Math.max(Math.ceil((double) h / GeoUtils.TILE_SIZE), 1) + 2;

        double dx = xIndexMin * GeoUtils.TILE_SIZE - topLeftX;
        double dy = yIndexMin * GeoUtils.TILE_SIZE - topLeftY;

        Composite oldComp = g.getComposite();
        float alpha = Math.max(0, Math.min(1, 1f - ctx.mapTransparency.value / 100f));
        if (alpha < 1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        int zoomValue = zoom.getLevel();
        for (int tx = xIndexMin; tx <= xIndexMax; tx++) {
            double x = (tx - xIndexMin) * GeoUtils.TILE_SIZE + dx;
            for (int ty = yIndexMin; ty <= yIndexMax; ty++) {
                OSMTile tile = ctx.player.isPreview
                        ? mTiles.getTile(zoomValue, tx, ty)
                        : mTiles.getTileBlocking(zoomValue, tx, ty, 5_000);
                if (tile == null || tile.getImage() == null) {
                    continue;
                }
                double y = (ty - yIndexMin) * GeoUtils.TILE_SIZE + dy;
                g.drawImage(tile.getImage(), (int) x, (int) y, null);
            }
        }

        g.setComposite(oldComp);
    }

    private static int computeFitZoom(int width, int height, LandBounds b) {
        int zoom = GeoUtils.ZOOM_MIN;
        for (int i = GeoUtils.ZOOM_MIN; i <= GeoUtils.ZOOM_MAX; i++) {
            TilePixel px1 = GeoUtils.getTileX(b.lonMin(), i);
            TilePixel py1 = GeoUtils.getTileY(b.latMin(), i);
            TilePixel px2 = GeoUtils.getTileX(b.lonMax(), i);
            TilePixel py2 = GeoUtils.getTileY(b.latMax(), i);

            double figureWidth = Math.abs(px2.getPixelGlobal() - px1.getPixelGlobal());
            double figureHeight = Math.abs(py2.getPixelGlobal() - py1.getPixelGlobal());

            if (figureWidth > width || figureHeight > height) {
                break;
            }
            zoom = i;
        }
        return zoom;
    }

    public class GeoMapContext extends IndicatorContext {

        public final IntegerProperty.IntegerContext mapTransparency;
        public final VisibleProperty.BooleanContext showMap;
        public final FitTrackProperty.BooleanContext fitTrack;

        public final MapZoom zoom;
        public double centerPx;
        public double centerPy;

        public GeoMapContext(Player player, Canvas canvas) {
            super(player, canvas);
            fitTrack = mFitTrack.createContext();
            mapTransparency = mMapTransparency.createContext();
            showMap = mShowMap.createContext();

            MapZoom zm = null;
            double cPx = 0;
            double cPy = 0;

            if (mGeoSensor.getSensorId() > 0) {
                if (fitTrack.value) {
                    LandBounds b = mGeoSensor.queryLandBounds();
                    if (b != null) {
                        zm = new MapZoom(
                                computeFitZoom(width, height, b)
                        );
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
                        cPx = zm.pixelX(atom.lon);
                        cPy = zm.pixelY(atom.lat);
                    }
                }
            }
            zoom = zm;
            centerPx = cPx;
            centerPy = cPy;
        }
    }

    @Override
    public GeoMapContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new GeoMapContext(player, canvas);
    }

}
