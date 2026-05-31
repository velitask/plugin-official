package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.IndicatorSkinTransfer;
import com.velitask.sdk.data.TemperatureSensorAtom;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.FontColorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.TemperatureAtomProperty;
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

public class TemperatureTextIndicator extends Indicator {

    public static final String NAME = "temperatureText";

    private static final String KEY = _KEY + "." + NAME;

    private final TemperatureAtomProperty mTemperature = new TemperatureAtomProperty();

    private final FontColorProperty mText = new FontColorProperty();

    private final TextAlignProperty mTextAlign = new TextAlignProperty();

    private final MeasurementProperty mUnits = new MeasurementProperty(UnitKind.TEMPERATURE);

    private final TemperatureTextTemplateProperty mTemplate = new TemperatureTextTemplateProperty();

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
        maket.setLayerSize(360d, 120d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mTemperature,
            mText,
            mTextAlign,
            mUnits,
            mTemplate
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mTextAlign, PropertyGroup.APPEARANCE);
        config.set(mUnits, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("compact", localized(KEY + ".skin.compact"))
            .set(mTemplate.skinCompact())
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        TemperatureTextContext ctx = (TemperatureTextContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        long rawTime = mTemperature.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mTemperature.clampToSensorRange(rawTime);
        }
        TemperatureSensorAtom atom = mTemperature.queryAtom(rawTime);
        if (atom == null) {
            return;
        }

        double celsius = atom.calcTemperature(rawTime);
        UnitValue curr = ctx.units.temperatureParts(celsius);

        String text = ctx.template.makeText(
                curr.value(), curr.unit(), celsius
        );

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = ctx.text.toFont(ctx.scale);
        g.setFont(font);
        g.setColor(ctx.text.color.value);

        ctx.textAlign.drawText(g, text, ctx.width, ctx.height);
    }

    static class TemperatureTextTemplateProperty extends TextTemplateProperty {

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
                new Var("currNum", "%.1f", localized(KEY_PROPERTY_VAR + ".currNum")),
                new Var("currUnit", "%s", localized(KEY_PROPERTY_VAR + ".currUnit")),
                new Var("celsius", "%.1f", localized(KEY_PROPERTY_VAR + ".celsius"))
            };
        }

        public IndicatorSkinTransfer skinCompact() {
            return skin(localized(KEY_PROPERTY_TEMPLATE + ".compact"));
        }
    }

    public class TemperatureTextContext extends IndicatorContext {

        public final FontColorProperty.FontColorContext text;
        public final TextAlignProperty.TextAlignContext textAlign;
        public final MeasurementProperty.Context units;
        public final TextTemplateProperty.TextTemplateContext template;

        public TemperatureTextContext(Player player, Canvas canvas) {
            super(player, canvas);
            text = mText.createContext();
            textAlign = mTextAlign.createContext();
            units = mUnits.createContext();
            template = mTemplate.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new TemperatureTextContext(player, canvas);
    }
}
