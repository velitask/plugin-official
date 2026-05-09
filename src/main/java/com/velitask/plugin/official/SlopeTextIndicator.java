package com.velitask.plugin.official;

import com.velitask.plugin.official.slope.SlopeGroupAtom;
import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.IndicatorSkinTransfer;
import com.velitask.sdk.data.SlopeSensorAtom;
import com.velitask.sdk.db.PluginDatabase;
import com.velitask.sdk.db.SensorDataManager;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.FontColorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.SlopeAtomProperty;
import com.velitask.sdk.properties.TextAlignProperty;
import com.velitask.sdk.properties.TextTemplateProperty;
import com.velitask.sdk.properties.measurement.MeasurementProperty;
import com.velitask.units.UnitKind;
import com.velitask.units.angle.Angle;
import com.velitask.units.format.UnitValue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class SlopeTextIndicator extends Indicator {

    public static final String NAME = "slopeText";

    private static final String KEY = _KEY + "." + NAME;

    private final SlopeAtomProperty mSlope = new SlopeAtomProperty();

    {
        mSlope.addManager(
                new SensorDataManager.Builder<>("slope", mSlope.getAtomClass())
                        .table(SlopeAtomProperty.TABLE_NAME)
                        .byTime()
                        .build()
        );
    }

    private final FontColorProperty mText = new FontColorProperty();

    private final TextAlignProperty mTextAlign = new TextAlignProperty();

    private final MeasurementProperty mUnits = new MeasurementProperty(
            UnitKind.DISTANCE, UnitKind.ALTITUDE
    );

    private final SlopeTextTemplateProperty mTemplate = new SlopeTextTemplateProperty();

    private TextTemplateProperty getTemplate() {
        return mTemplate;
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
        maket.setVertical(VerticalAlign.TOP);
        maket.setHorizontal(HorizontalAlign.LEFT);
        maket.setLayerMargin(null, 300d, 100d, null);
        maket.setLayerSize(600d, 300d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{mSlope, mText, mTextAlign, mUnits, getTemplate()};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mTextAlign, PropertyGroup.APPEARANCE);
        config.set(mUnits, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("short", localized(KEY + ".skin.short"))
            .set(mTemplate.skinShort())
            .build(),
            IndicatorSkin.builder("detailed", localized(KEY + ".skin.detailed"))
            .set(mTemplate.skinDetailed())
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        SlopeTextContext ctx = (SlopeTextContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        long rawTime = mSlope.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mSlope.clampToSensorRange(rawTime);
        }
        SlopeSensorAtom atom = mSlope.queryAtom("slope", rawTime);
        if (atom == null) {
            return;
        }

        SlopeGroupAtom group = queryGroupForAtom(ctx.db, mSlope.getSensorId(), atom);
        if (group == null) {
            return;
        }

        long distanceSlope = atom.distance - group.distance;
        long elevationSlope = atom.elevation - group.elevation;

        UnitValue currV = ctx.units.distanceParts(distanceSlope);
        UnitValue lengthV = ctx.units.distanceParts(group.distanceDelta);
        UnitValue heightV = ctx.units.altitudeParts(group.elevationDelta);
        UnitValue eleV = ctx.units.altitudeParts(atom.elevation);
        UnitValue eleCurrV = ctx.units.altitudeParts(Math.abs(elevationSlope));
        UnitValue upV = ctx.units.altitudeParts(atom.eleUp);
        UnitValue downV = ctx.units.altitudeParts(Math.abs(atom.eleDown));

        String type = slopeTypeToString(group.slopeType);
        String typec = slopeTypeCToString(group.slopeType);
        double pc = Angle.slopePercentToPercent(group.slopePercent);

        String text = ctx.template.makeText(
                type, typec, pc,
                currV.value(), currV.unit(),
                lengthV.value(), lengthV.unit(),
                heightV.value(), heightV.unit(),
                eleV.value(), eleV.unit(),
                eleCurrV.value(),
                upV.value(),
                downV.value()
        );

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = ctx.text.toFont(ctx.scale);
        g.setFont(font);
        g.setColor(ctx.text.color.value);

        ctx.textAlign.drawText(g, text, ctx.width, ctx.height);
    }

    private static SlopeGroupAtom queryGroupForAtom(PluginDatabase db, long sensorId, SlopeSensorAtom atom) {
        if (db == null) {
            return null;
        }
        return db.query(
                "SELECT * FROM ${table:slope_groups}"
                + " WHERE sensor_id = ?"
                + " AND distance <= ?"
                + " AND ? < (distance + distanceDelta)"
                + " LIMIT 1",
                SlopeGroupAtom.class,
                sensorId, atom.distance, atom.distance
        );
    }

    private String slopeTypeToString(int type) {
        return switch (type) {
            case SlopeSensorAtom.TYPE_UP ->
                localized(KEY + ".type.up");
            case SlopeSensorAtom.TYPE_DOWN ->
                localized(KEY + ".type.down");
            default ->
                localized(KEY + ".type.plain");
        };
    }

    private String slopeTypeCToString(int type) {
        return switch (type) {
            case SlopeSensorAtom.TYPE_UP ->
                "↑";
            case SlopeSensorAtom.TYPE_DOWN ->
                "↓";
            default ->
                "";
        };
    }

    static class SlopeTextTemplateProperty extends TextTemplateProperty {

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
                new Var("type", "%s", localized(KEY_PROPERTY_VAR + ".type")),
                new Var("typec", "%s", localized(KEY_PROPERTY_VAR + ".typec")),
                new Var("pc", "%.1f", localized(KEY_PROPERTY_VAR + ".pc")),
                new Var("curr", "%.3f", localized(KEY_PROPERTY_VAR + ".curr")),
                new Var("currUnit", "%s", localized(KEY_PROPERTY_VAR + ".currUnit")),
                new Var("length", "%.2f", localized(KEY_PROPERTY_VAR + ".length")),
                new Var("lengthUnit", "%s", localized(KEY_PROPERTY_VAR + ".lengthUnit")),
                new Var("height", "%.0f", localized(KEY_PROPERTY_VAR + ".height")),
                new Var("heightUnit", "%s", localized(KEY_PROPERTY_VAR + ".heightUnit")),
                new Var("ele", "%.0f", localized(KEY_PROPERTY_VAR + ".ele")),
                new Var("eleUnit", "%s", localized(KEY_PROPERTY_VAR + ".eleUnit")),
                new Var("eleCurr", "%.1f", localized(KEY_PROPERTY_VAR + ".eleCurr")),
                new Var("up", "%.0f", localized(KEY_PROPERTY_VAR + ".up")),
                new Var("down", "%.0f", localized(KEY_PROPERTY_VAR + ".down"))
            };
        }

        public IndicatorSkinTransfer skinShort() {
            return skin(localized(KEY_PROPERTY_TEMPLATE + ".short"));
        }

        public IndicatorSkinTransfer skinDetailed() {
            return skin(localized(KEY_PROPERTY_TEMPLATE + ".detailed"));
        }
    }

    public class SlopeTextContext extends IndicatorContext {

        public final FontColorProperty.FontColorContext text;
        public final TextAlignProperty.TextAlignContext textAlign;
        public final TextTemplateProperty.TextTemplateContext template;
        public final MeasurementProperty.Context units;

        public SlopeTextContext(Player player, Canvas canvas) {
            super(player, canvas);
            text = mText.createContext();
            textAlign = mTextAlign.createContext();
            template = getTemplate().createContext();
            units = mUnits.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new SlopeTextContext(player, canvas);
    }
}
