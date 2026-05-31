package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.data.GeoSensorAtom;
import com.velitask.sdk.properties.ArrayProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.EnumArrayProperty;
import com.velitask.sdk.properties.FontColorProperty;
import com.velitask.sdk.properties.GeoSensorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.IntegerProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.TextAlignProperty;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class LatLonTextIndicator extends Indicator {

    public static final String NAME = "latLonText";

    private static final String KEY = _KEY + "." + NAME;

    public enum DisplayType {
        DECIMAL,
        DECIMAL_NSEW,
        DMS
    }

    private final GeoSensorProperty mGeo = new GeoSensorProperty();

    private final FontColorProperty mFont = new FontColorProperty();

    private final TextAlignProperty mTextAlign = new TextAlignProperty();

    private final EnumArrayProperty<DisplayType> mDisplayType = new EnumArrayProperty<>(DisplayType.class) {
        @Override
        public String getName() {
            return "displayType";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".displayType.title");
        }

        @Override
        protected String[] defineTitles() {
            return new String[]{
                localized(KEY + ".displayType.decimal"),
                localized(KEY + ".displayType.decimalNsew"),
                localized(KEY + ".displayType.dms")
            };
        }
    };

    private final IntegerProperty mPrecision = new IntegerProperty() {
        {
            setRange(0, 12);
        }

        @Override
        public String getName() {
            return "precision";
        }

        @Override
        public Integer getDefault() {
            return 7;
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".precision.title");
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
        maket.setVertical(VerticalAlign.BOTTOM);
        maket.setHorizontal(HorizontalAlign.CENTER);
        maket.setLayerMargin(null, null, 50d, null);
        maket.setLayerSize(800d, 80d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mGeo,
            mFont,
            mTextAlign,
            mDisplayType,
            mPrecision
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mTextAlign, PropertyGroup.APPEARANCE);
        config.set(mDisplayType, PropertyGroup.APPEARANCE);
        config.set(mPrecision, PropertyGroup.APPEARANCE);
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        LatLonTextContext ctx = (LatLonTextContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        long rawTime = mGeo.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mGeo.clampToSensorRange(rawTime);
        }
        GeoSensorAtom atom = mGeo.queryAtom(rawTime);
        if (atom == null) {
            return;
        }

        double lat = atom.calcLat(rawTime);
        double lon = atom.calcLon(rawTime);

        String text = format(lat, lon, ctx.displayType.value, ctx.precision.value);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = ctx.font.toFont(ctx.scale);
        g.setFont(font);
        g.setColor(ctx.font.color.value);

        ctx.textAlign.drawText(g, text, ctx.width, ctx.height);
    }

    private static String format(double lat, double lon, DisplayType type, int precision) {
        if (type == null) {
            type = DisplayType.DECIMAL;
        }
        return switch (type) {
            case DECIMAL_NSEW ->
                formatDecimalNSEW(lat, lon, precision);
            case DMS ->
                formatDMS(lat, lon);
            default ->
                formatDecimal(lat, lon, precision);
        };
    }

    private static String formatDecimal(double lat, double lon, int precision) {
        int p = Math.max(0, precision);
        String fmt = "%." + p + "f, %." + p + "f";
        return String.format(Locale.US, fmt, lat, lon);
    }

    private static String formatDecimalNSEW(double lat, double lon, int precision) {
        int p = Math.max(0, precision);
        String fmt = "%." + p + "f°%s, %." + p + "f°%s";
        return String.format(Locale.US, fmt,
                Math.abs(lat), lat >= 0 ? "N" : "S",
                Math.abs(lon), lon >= 0 ? "E" : "W");
    }

    private static String formatDMS(double lat, double lon) {
        return toDMS(lat, "N", "S") + ", " + toDMS(lon, "E", "W");
    }

    private static String toDMS(double v, String pos, String neg) {
        double abs = Math.abs(v);
        int deg = (int) abs;
        double minF = (abs - deg) * 60.0;
        int min = (int) minF;
        double sec = (minF - min) * 60.0;
        return String.format(Locale.US, "%d°%02d'%05.2f\"%s",
                deg, min, sec, v >= 0 ? pos : neg);
    }

    public class LatLonTextContext extends IndicatorContext {

        public final FontColorProperty.FontColorContext font;
        public final TextAlignProperty.TextAlignContext textAlign;
        public final ArrayProperty<DisplayType>.ArrayContext displayType;
        public final IntegerProperty.IntegerContext precision;

        public LatLonTextContext(Player player, Canvas canvas) {
            super(player, canvas);
            font = mFont.createContext();
            textAlign = mTextAlign.createContext();
            displayType = mDisplayType.createContext();
            precision = mPrecision.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new LatLonTextContext(player, canvas);
    }
}
