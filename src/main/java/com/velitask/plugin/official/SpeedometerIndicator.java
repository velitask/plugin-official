package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.IndicatorSkinTransfer;
import com.velitask.sdk.data.DistanceSensorAtom;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DistanceSensorProperty;
import com.velitask.sdk.properties.EnumArrayProperty;
import com.velitask.sdk.properties.FontColorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.SvgProperty;
import com.velitask.sdk.properties.TextTemplateProperty;
import com.velitask.sdk.properties.TransparentColorProperty;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class SpeedometerIndicator extends Indicator {

    public static final String NAME = "speedometer";

    private static final String KEY = _KEY + "." + NAME;
    private static final String SVG_PATH = "svg/speedometer/";

    private final DistanceSensorProperty mDistance = new DistanceSensorProperty();

    public static enum SpeedScale {
        SCALE_40(40),
        SCALE_80(80),
        SCALE_160(160),
        SCALE_320(320);

        public final int value;

        SpeedScale(int value) {
            this.value = value;
        }
    }

    private static final String SPEED_SCALE = "speedScale";
    private final EnumArrayProperty<SpeedScale> mSpeedScale
            = new EnumArrayProperty<>(SpeedScale.class) {
        {
            set(SpeedScale.SCALE_160);
        }

        @Override
        public String getName() {
            return SPEED_SCALE;
        }

        @Override
        public String getTitle() {
            return localized(KEY + "." + SPEED_SCALE + ".title");
        }

        @Override
        protected String[] defineTitles() {
            SpeedScale[] vs = SpeedScale.values();
            String[] t = new String[vs.length];
            for (int i = 0; i < vs.length; i++) {
                t[i] = String.valueOf(vs[i].value);
            }
            return t;
        }
    };

    private final FontColorProperty mText = new FontColorProperty(120);

    private static final String BACKGROUND = "background";
    private final TransparentColorProperty mBackground = new TransparentColorProperty(
            BACKGROUND,
            localized(KEY + "." + BACKGROUND + ".title"),
            new Color(204, 204, 204), 80
    );

    private static final String SCALE_COLOR = "scaleColor";
    private final ColorProperty mScaleColor = new ColorProperty(
            Color.WHITE,
            SCALE_COLOR,
            localized(KEY + "." + SCALE_COLOR + ".title")
    );

    private static final String ARROW_FILL_COLOR = "arrowFillColor";
    private final ColorProperty mArrowFillColor = new ColorProperty(
            new Color(0xcc, 0x58, 0x54),
            ARROW_FILL_COLOR,
            localized(KEY + "." + ARROW_FILL_COLOR + ".title")
    );

    private static final String ARROW_STROKE_COLOR = "arrowStrokeColor";
    private final ColorProperty mArrowStrokeColor = new ColorProperty(
            Color.WHITE,
            ARROW_STROKE_COLOR,
            localized(KEY + "." + ARROW_STROKE_COLOR + ".title")
    );

    private static final String TAIL_COLOR = "tailColor";
    private final ColorProperty mTailColor = new ColorProperty(
            new Color(0x67, 0x88, 0xa6, 0xb2),
            TAIL_COLOR,
            localized(KEY + "." + TAIL_COLOR + ".title")
    );

    private final SpeedometerClassicTextTemplateProperty mTemplate = new SpeedometerClassicTextTemplateProperty();

    private TextTemplateProperty getTemplate() {
        return mTemplate;
    }

    private final SvgProperty mBgSvg = new SvgProperty("bgSvg", SVG_PATH + "background.svg")
            .bind(".ring", "fill", mBackground.getColor());

    private final SvgProperty mDelimiterSvg = new SvgProperty("delimiterSvg", SVG_PATH + "delimiter.svg")
            .bind(".tick", "fill", mScaleColor);

    private final SvgProperty[] mScaleSvgs = new SvgProperty[]{
        new SvgProperty("scale40", SVG_PATH + "numeric_40.svg")
        .bind(".number", "fill", mScaleColor),
        new SvgProperty("scale80", SVG_PATH + "numeric_80.svg")
        .bind(".number", "fill", mScaleColor),
        new SvgProperty("scale160", SVG_PATH + "numeric_160.svg")
        .bind(".number", "fill", mScaleColor),
        new SvgProperty("scale320", SVG_PATH + "numeric_320.svg")
        .bind(".number", "fill", mScaleColor)
    };

    private final SvgProperty mArrowSvg = new SvgProperty("arrowSvg", SVG_PATH + "arrow.svg")
            .bind(".arrow-body", "fill", mArrowFillColor)
            .bind(".arrow-body", "stroke", mArrowStrokeColor);

    private final SvgProperty mTailSvg = new SvgProperty("tailSvg", SVG_PATH + "arrow_tail.svg")
            .bind(".tail", "stroke", mTailColor);

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
        maket.setLayerMargin(null, null, 50d, 50d);
        maket.setLayerSize(400d, 400d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mDistance, mSpeedScale,
            mText, getTemplate(),
            mScaleColor,
            mArrowFillColor, mArrowStrokeColor, mTailColor,
            mBackground
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mSpeedScale, PropertyGroup.APPEARANCE);
        config.set(mScaleColor, PropertyGroup.APPEARANCE);
        config.set(mArrowFillColor, PropertyGroup.APPEARANCE);
        config.set(mArrowStrokeColor, PropertyGroup.APPEARANCE);
        config.set(mTailColor, PropertyGroup.APPEARANCE);
        config.set(mBackground, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("night", localized(KEY + ".skin.night"))
            .set(mText.getColor().skin(0xaa, 0xaa, 0xaa))
            .set(mBackground.getColor().skin(0x11, 0x11, 0x11))
            .set(mBackground.getTransparency().skin(33))
            .set(mScaleColor.skin(0x88, 0x88, 0x88))
            .set(mArrowFillColor.skin(0x55, 0x2b, 0x2a))
            .set(mArrowStrokeColor.skin(0xaa, 0xaa, 0xaa))
            .set(mTailColor.skin(0x33, 0x44, 0x55, 0xaa))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        SpeedometerClassicContext ctx = (SpeedometerClassicContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        int size = Math.min(ctx.width, ctx.height);
        int x = (ctx.width - size) / 2;
        int y = (ctx.height - size) / 2;

        ctx.bgSvg.render(g, x, y, size, size, ctx.background.alpha);

        double speedKmh = 0;
        long rawTime = mDistance.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mDistance.clampToSensorRange(rawTime);
        }
        DistanceSensorAtom atom = mDistance.queryAtom(rawTime);
        if (atom != null) {
            speedKmh = atom.speed / 1000.0;
        }
        int scale = Math.max(1, ctx.speedScale.value);
        double angleDeg = Math.min(speedKmh, scale) * 360.0 / scale;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderTailArc(g, ctx.tailSvg, x, y, size, angleDeg);

        ctx.delimiterSvg.render(g, x, y, size, size);

        ctx.scaleSvg.render(g, x, y, size, size);

        ctx.arrowSvg.render(g, x, y, size, size, angleDeg);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = ctx.text.toFont(ctx.scale);
        g.setFont(font);
        g.setColor(ctx.text.color.value);
        String text = ctx.template.makeText(speedKmh);
        FontMetrics fm = g.getFontMetrics(font);
        Rectangle2D r = fm.getStringBounds(text, g);
        int tx = (ctx.width - (int) r.getWidth()) / 2;
        int ty = (ctx.height - (int) r.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, tx, ty);
    }

    private static void renderTailArc(Graphics2D g, SvgProperty.SvgContext tailSvg,
            int x, int y, int size, double angleDeg) {
        if (angleDeg <= 0) {
            return;
        }
        if (angleDeg >= 360) {
            tailSvg.render(g, x, y, size, size);
            return;
        }
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float r = size;
        Arc2D pie = new Arc2D.Float(
                cx - r, cy - r,
                2 * r, 2 * r,
                270f, (float) -angleDeg,
                Arc2D.PIE
        );
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.clip(pie);
            tailSvg.render(g2, x, y, size, size);
        } finally {
            g2.dispose();
        }
    }

    static class SpeedometerClassicTextTemplateProperty extends TextTemplateProperty {

        private static final String KEY_PROPERTY = KEY + "." + NAME;
        private static final String KEY_PROPERTY_TEMPLATE = KEY_PROPERTY + ".template";
        private static final String KEY_PROPERTY_VAR = KEY_PROPERTY + ".var";
        private static final String KEY_PROPERTY_VAR_CURR = KEY_PROPERTY_VAR + ".curr";

        @Override
        protected String defineTemplate() {
            return localized(KEY_PROPERTY_TEMPLATE + ".default");
        }

        @Override
        protected Var[] defineVars() {
            return new Var[]{
                new Var("curr", "%.0f", localized(KEY_PROPERTY_VAR_CURR))
            };
        }

        public IndicatorSkinTransfer skinFloat() {
            return skin(localized(KEY_PROPERTY_TEMPLATE + ".float"),
                    new Var[]{new Var("curr", "%.1f", localized(KEY_PROPERTY_VAR_CURR))});
        }
    }

    public class SpeedometerClassicContext extends IndicatorContext {

        public final SpeedScale speedScale;
        public final FontColorProperty.FontColorContext text;
        public final TextTemplateProperty.TextTemplateContext template;

        public final TransparentColorProperty.TransparentColorContext background;

        public final SvgProperty.SvgContext bgSvg;
        public final SvgProperty.SvgContext delimiterSvg;
        public final SvgProperty.SvgContext scaleSvg;
        public final SvgProperty.SvgContext arrowSvg;
        public final SvgProperty.SvgContext tailSvg;

        public SpeedometerClassicContext(Player player, Canvas canvas) {
            super(player, canvas);
            speedScale = mSpeedScale.get();
            text = mText.createContext();
            template = getTemplate().createContext();
            background = mBackground.createContext();

            bgSvg = mBgSvg.createContext();
            delimiterSvg = mDelimiterSvg.createContext();
            scaleSvg = scaleSvgFor(speedScale).createContext();
            arrowSvg = mArrowSvg.createContext();
            tailSvg = mTailSvg.createContext();
        }

        private SvgProperty scaleSvgFor(SpeedScale scale) {
            return mScaleSvgs[scale.ordinal()];
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new SpeedometerClassicContext(player, canvas);
    }
}
