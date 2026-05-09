package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.IndicatorSkinTransfer;
import com.velitask.sdk.data.DistanceSensorAtom;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DistanceSensorProperty;
import com.velitask.sdk.properties.FontColorProperty;
import com.velitask.sdk.properties.FontProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.TextAlignProperty;
import com.velitask.sdk.properties.TextTemplateProperty;
import com.velitask.sdk.properties.measurement.MeasurementProperty;
import com.velitask.units.UnitKind;
import com.velitask.units.format.UnitValue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class DistanceTextIndicator extends Indicator {

    public static final String NAME = "distanceText";

    private static final String KEY = _KEY + "." + NAME;

    private final DistanceSensorProperty mDistanceSensor = new DistanceSensorProperty();

    private final TextAlignProperty mTextAlign = new TextAlignProperty();

    private final FontColorProperty mValueText
            = new FontColorProperty("valueText", localized(KEY + ".valueFont.title"), 48);

    private final FontProperty mUnitFont
            = new FontProperty("unitFont", localized(KEY + ".unitFont.title"), 28);

    private final MeasurementProperty mUnits = new MeasurementProperty(UnitKind.DISTANCE);

    private final DistanceTextTemplateProperty mTemplate = new DistanceTextTemplateProperty();

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
        maket.setLayerSize(750d, 100d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mDistanceSensor,
            mValueText, mUnitFont,
            mTextAlign,
            mUnits,
            mTemplate
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mUnitFont, PropertyGroup.FONT);
        config.set(mUnits, PropertyGroup.APPEARANCE);
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        DistanceTextContext ctx = (DistanceTextContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        long rawTime = mDistanceSensor.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mDistanceSensor.clampToSensorRange(rawTime);
        }
        DistanceSensorAtom atom = mDistanceSensor.queryAtom(rawTime);
        if (atom == null) {
            return;
        }

        DistanceSensorAtom lastAtom = mDistanceSensor.queryLast();
        long distanceFullMm = lastAtom != null ? lastAtom.getNextDistance() : 0;

        UnitValue curr = ctx.units.distanceParts(atom.distance);
        UnitValue full = ctx.units.distanceParts(distanceFullMm);

        String text = ctx.template.makeText(
                curr.value(), curr.unit(),
                full.value(), full.unit()
        );

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ctx.valueText.color.value);

        Font font = ctx.valueText.toFont(ctx.scale);
        g.setFont(font);

        ctx.textAlign.drawText(g, text, ctx.width, ctx.height);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("minimal", localized(KEY + ".skin.minimal"))
            .set(mTemplate.skinMinimal())
            .build()
        };
    }

    static class DistanceTextTemplateProperty extends TextTemplateProperty {

        private static final String KEY_PROPERTY = KEY + "." + NAME;
        private static final String KEY_PROPERTY_TEMPLATE = KEY_PROPERTY + ".template";
        private static final String KEY_PROPERTY_VAR = KEY_PROPERTY + ".var";

        @Override
        protected String defineTemplate() {
            return localized(KEY_PROPERTY_TEMPLATE + ".default");
        }

        @Override
        protected Var[] defineVars() {
            return new Var[]{
                new Var("currNum", "%.2f", localized(KEY_PROPERTY_VAR + ".currNum")),
                new Var("currUnit", "%s", localized(KEY_PROPERTY_VAR + ".currUnit")),
                new Var("fullNum", "%.0f", localized(KEY_PROPERTY_VAR + ".fullNum")),
                new Var("fullUnit", "%s", localized(KEY_PROPERTY_VAR + ".fullUnit"))
            };
        }

        public IndicatorSkinTransfer skinMinimal() {
            return skin(localized(KEY_PROPERTY_TEMPLATE + ".minimal"));
        }
    }

    public class DistanceTextContext extends IndicatorContext {

        public final FontColorProperty.FontColorContext valueText;
        public final FontProperty.FontContext unitFont;
        public final TextAlignProperty.TextAlignContext textAlign;
        public final TextTemplateProperty.TextTemplateContext template;
        public final MeasurementProperty.Context units;

        public DistanceTextContext(Player player, Canvas canvas) {
            super(player, canvas);
            valueText = mValueText.createContext();
            unitFont = mUnitFont.createContext();
            textAlign = mTextAlign.createContext();
            template = mTemplate.createContext();
            units = mUnits.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new DistanceTextContext(player, canvas);
    }

}
