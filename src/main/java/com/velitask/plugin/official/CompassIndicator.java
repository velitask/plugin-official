package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.data.GeoSensorAtom;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.GeoSensorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.SvgProperty;
import com.velitask.units.coordinates.Coordinates;
import java.awt.Color;
import java.awt.Graphics2D;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class CompassIndicator extends Indicator {

    public static final String NAME = "compass";

    private static final String KEY = _KEY + "." + NAME;

    private final GeoSensorProperty mGeoSensor = new GeoSensorProperty();

    private static final String BODY_COLOR = "bodyColor";
    private final ColorProperty mBodyColor = new ColorProperty(
            new Color(0x22, 0x33, 0x44),
            BODY_COLOR,
            localized(KEY + "." + BODY_COLOR + ".title")
    );

    private static final String RING_COLOR = "ringColor";
    private final ColorProperty mRingColor = new ColorProperty(
            Color.WHITE,
            RING_COLOR,
            localized(KEY + "." + RING_COLOR + ".title")
    );

    private static final String NEEDLE_NORTH_COLOR = "needleNorthColor";
    private final ColorProperty mNeedleNorthColor = new ColorProperty(
            new Color(0xcc, 0x33, 0x33),
            NEEDLE_NORTH_COLOR,
            localized(KEY + "." + NEEDLE_NORTH_COLOR + ".title")
    );

    private static final String NEEDLE_SOUTH_COLOR = "needleSouthColor";
    private final ColorProperty mNeedleSouthColor = new ColorProperty(
            new Color(0xdd, 0xdd, 0xdd),
            NEEDLE_SOUTH_COLOR,
            localized(KEY + "." + NEEDLE_SOUTH_COLOR + ".title")
    );

    private final SvgProperty mCompassSvg = new SvgProperty("CompassSvg", "svg/compass.svg")
            .bind(".body", "fill", mBodyColor)
            .bind(".ring", "stroke", mRingColor)
            .bind(".tick", "stroke", mRingColor)
            .bind(".needle-n", "fill", mNeedleNorthColor)
            .bind(".needle-s", "fill", mNeedleSouthColor);

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
        maket.setHorizontal(HorizontalAlign.RIGHT);
        maket.setLayerMargin(null, 50d, 50d, null);
        maket.setLayerSize(200d, 200d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{mGeoSensor, mBodyColor, mRingColor, mNeedleNorthColor, mNeedleSouthColor};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mBodyColor, PropertyGroup.APPEARANCE);
        config.set(mRingColor, PropertyGroup.APPEARANCE);
        config.set(mNeedleNorthColor, PropertyGroup.APPEARANCE);
        config.set(mNeedleSouthColor, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("dark", localized(KEY + ".skin.dark"))
            .set(mBodyColor.skin(0x1a, 0x1a, 0x1a))
            .set(mRingColor.skin(0xcc, 0xcc, 0xcc))
            .set(mNeedleNorthColor.skin(0xff, 0x44, 0x44))
            .set(mNeedleSouthColor.skin(0xaa, 0xaa, 0xaa))
            .build(),
            IndicatorSkin.builder("brass", localized(KEY + ".skin.brass"))
            .set(mBodyColor.skin(0x8b, 0x6f, 0x3a))
            .set(mRingColor.skin(0xd4, 0xa7, 0x5f))
            .set(mNeedleNorthColor.skin(0x6b, 0x2b, 0x1c))
            .set(mNeedleSouthColor.skin(0xe8, 0xd9, 0xb0))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        CompassContext ctx = (CompassContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        double bearing = 0;
        long rawTime = mGeoSensor.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mGeoSensor.clampToSensorRange(rawTime);
        }
        GeoSensorAtom atom = mGeoSensor.queryAtom(rawTime);
        if (atom != null) {
            bearing = Coordinates.bearing(atom.latDelta, atom.lonDelta, atom.lat);
        }

        int size = Math.min(ctx.width, ctx.height);
        int x = (ctx.width - size) / 2;
        int y = (ctx.height - size) / 2;

        ctx.compassSvg.render(g, x, y, size, size, bearing);
    }

    public class CompassContext extends IndicatorContext {

        public final SvgProperty.SvgContext compassSvg;

        public CompassContext(Player player, Canvas canvas) {
            super(player, canvas);

            compassSvg = mCompassSvg.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new CompassContext(player, canvas);
    }

}
