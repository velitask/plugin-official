package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.data.DistanceSensorAtom;
import com.velitask.sdk.data.SlopeSensorAtom;
import com.velitask.sdk.db.SensorDataManager;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DistanceSensorProperty;
import com.velitask.sdk.properties.FontProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.SlopeAtomProperty;
import com.velitask.sdk.properties.TextAlignProperty;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class SlopeSpeedIndicator extends Indicator {

    public static final String NAME = "slopeSpeed";

    private static final String KEY = _KEY + "." + NAME;

    private final SlopeAtomProperty mSlope = new SlopeAtomProperty();

    private final DistanceSensorProperty mDistance = new DistanceSensorProperty();

    {
        mSlope.addManager(
                new SensorDataManager.Builder<>("slope", mSlope.getAtomClass())
                        .table(SlopeAtomProperty.TABLE_NAME)
                        .where("group_id > 0")
                        .byTime()
                        .build()
        );
    }

    private final FontProperty mFont = new FontProperty() {
        @Override
        public String getName() {
            return "Font";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".font.title");
        }
    };

    private static final String SLOPE_COLOR_UP = "slopeColorUp";
    private final ColorProperty mSlopeColorUp = new ColorProperty(
            new Color(220, 80, 80),
            SLOPE_COLOR_UP,
            localized(KEY + "." + SLOPE_COLOR_UP + ".title")
    );

    private static final String SLOPE_COLOR_DOWN = "slopeColorDown";
    private final ColorProperty mSlopeColorDown = new ColorProperty(
            new Color(80, 220, 80),
            SLOPE_COLOR_DOWN,
            localized(KEY + "." + SLOPE_COLOR_DOWN + ".title")
    );

    private static final String SLOPE_COLOR_PLAIN = "slopeColorPlain";
    private final ColorProperty mSlopeColorPlain = new ColorProperty(
            Color.GRAY,
            SLOPE_COLOR_PLAIN,
            localized(KEY + "." + SLOPE_COLOR_PLAIN + ".title")
    );

    private static final String SPEED_COLOR = "speedColor";
    private final ColorProperty mSpeedColor = new ColorProperty(
            Color.WHITE,
            SPEED_COLOR,
            localized(KEY + "." + SPEED_COLOR + ".title")
    );

    private final TextAlignProperty mTextAlign = new TextAlignProperty() {
        @Override
        public String getName() {
            return "TextAlign";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".textAlign.title");
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
        maket.setLayerMargin(null, 100d, 100d, null);
        maket.setLayerSize(400d, 200d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mSlope,
            mDistance,
            mFont,
            mSlopeColorUp,
            mSlopeColorDown,
            mSlopeColorPlain,
            mSpeedColor,
            mTextAlign
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mFont, PropertyGroup.FONT);
        config.set(mSlopeColorUp, PropertyGroup.APPEARANCE);
        config.set(mSlopeColorDown, PropertyGroup.APPEARANCE);
        config.set(mSlopeColorPlain, PropertyGroup.APPEARANCE);
        config.set(mSpeedColor, PropertyGroup.APPEARANCE);
        config.set(mTextAlign, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("neutral", localized(KEY + ".skin.neutral"))
            .set(mSlopeColorUp.skin(Color.WHITE))
            .set(mSlopeColorDown.skin(Color.WHITE))
            .set(mSlopeColorPlain.skin(Color.WHITE))
            .set(mSpeedColor.skin(Color.WHITE))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        SlopeSpeedContext ctx = (SlopeSpeedContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        long slopeRawTime = mSlope.convertToRawTime(ctx.player.time);
        long distRawTime = mDistance.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            slopeRawTime = mSlope.clampToSensorRange(slopeRawTime);
            distRawTime = mDistance.clampToSensorRange(distRawTime);
        }
        SlopeSensorAtom slope = mSlope.queryAtom("slope", slopeRawTime);
        DistanceSensorAtom dist = mDistance.queryAtom(distRawTime);

        if (slope == null && dist == null) {
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = ctx.font.toFont(ctx.scale);
        g.setFont(font);

        int halfHeight = ctx.height / 2;

        if (slope != null) {
            String arrow = switch (slope.slopeType) {
                case SlopeSensorAtom.TYPE_DOWN ->
                    "▼";
                case SlopeSensorAtom.TYPE_UP ->
                    "▲";
                default ->
                    "—";
            };

            float percent = slope.getElevationPercent();
            String text = String.format("%s %.1f%%", arrow, percent);

            Color color = switch (slope.slopeType) {
                case SlopeSensorAtom.TYPE_UP ->
                    ctx.slopeColorUp.value;
                case SlopeSensorAtom.TYPE_DOWN ->
                    ctx.slopeColorDown.value;
                default ->
                    ctx.slopeColorPlain.value;
            };
            g.setColor(color);

            ctx.textAlign.drawText(g, text, ctx.width, halfHeight);
        }

        if (dist != null) {
            double speedKmH = dist.speed / 1_000_000.0;
            String text = String.format(localized(KEY + ".speedText.format"), speedKmH);

            g.setColor(ctx.speedColor.value);
            g.translate(0, halfHeight);
            ctx.textAlign.drawText(g, text, ctx.width, halfHeight);
            g.translate(0, -halfHeight);
        }
    }

    public class SlopeSpeedContext extends IndicatorContext {

        public final FontProperty.FontContext font;
        public final ColorProperty.ColorContext slopeColorUp;
        public final ColorProperty.ColorContext slopeColorDown;
        public final ColorProperty.ColorContext slopeColorPlain;
        public final ColorProperty.ColorContext speedColor;
        public final TextAlignProperty.TextAlignContext textAlign;

        public SlopeSpeedContext(Player player, Canvas canvas) {
            super(player, canvas);
            font = mFont.createContext();
            slopeColorUp = mSlopeColorUp.createContext();
            slopeColorDown = mSlopeColorDown.createContext();
            slopeColorPlain = mSlopeColorPlain.createContext();
            speedColor = mSpeedColor.createContext();
            textAlign = mTextAlign.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new SlopeSpeedContext(player, canvas);
    }
}
