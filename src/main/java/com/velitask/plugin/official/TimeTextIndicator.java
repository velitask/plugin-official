package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.IndicatorSkinTransfer;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.FontColorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.StringProperty;
import com.velitask.sdk.properties.TextAlignProperty;
import com.velitask.sdk.properties.TextTemplateProperty;
import com.velitask.sdk.properties.TimeZoneProperty;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class TimeTextIndicator extends Indicator {

    public static final String NAME = "timeText";

    private static final String KEY = _KEY + "." + NAME;

    private final FontColorProperty mFont = new FontColorProperty();

    private final TextAlignProperty mTextAlign = new TextAlignProperty();

    private final TimeZoneProperty mTimeZone = new TimeZoneProperty();

    private final StringProperty mTimeFormat = new StringProperty(
            localized(KEY + ".timeFormat.default"),
            "timeFormat",
            localized(KEY + ".timeFormat.title")
    );

    private final StringProperty mDurationFormat = new StringProperty(
            localized(KEY + ".durationFormat.default"),
            "durationFormat",
            localized(KEY + ".durationFormat.title")
    );

    private final TimeTextTemplateProperty mTemplate = new TimeTextTemplateProperty();

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
        return new IProperty[]{
            mFont, mTextAlign,
            mTimeZone, mTimeFormat, mDurationFormat,
            getTemplate()
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mTextAlign, PropertyGroup.APPEARANCE);
        config.set(mTimeZone, PropertyGroup.APPEARANCE);
        config.set(mTimeFormat, PropertyGroup.APPEARANCE);
        config.set(mDurationFormat, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("compact", localized(KEY + ".skin.compact"))
            .set(mTemplate.skinCompact())
            .build(),
            IndicatorSkin.builder("withZone", localized(KEY + ".skin.withZone"))
            .set(mTemplate.skinWithZone())
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        TimeTextContext ctx = (TimeTextContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        long time = ctx.player.time;
        long passed = time - ctx.player.period.start;
        long left = ctx.player.period.end - time;

        ZoneId zone = ctx.timeZone.zoneId;
        DateTimeFormatter timeFmt = parseFormatter(ctx.timeFormat.value, "yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter durFmt = parseFormatter(ctx.durationFormat.value, "HH:mm:ss");

        String currStr = timeFmt.format(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(time), zone)
        );
        String tzidStr = zone.getDisplayName(TextStyle.FULL, Locale.getDefault());
        String passedStr = formatDuration(passed, durFmt);
        String leftStr = formatDuration(left, durFmt);

        String text = ctx.template.makeText(currStr, tzidStr, passedStr, leftStr);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = ctx.text.toFont(ctx.scale);
        g.setFont(font);
        g.setColor(ctx.text.color.value);

        ctx.textAlign.drawText(g, text, ctx.width, ctx.height);
    }

    private static String formatDuration(long durationMs, DateTimeFormatter fmt) {
        LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(Math.max(0, durationMs)),
                ZoneId.of("UTC"));
        return fmt.format(dt);
    }

    private static DateTimeFormatter parseFormatter(String pattern, String fallback) {
        try {
            return DateTimeFormatter.ofPattern(pattern);
        } catch (Exception ex) {
            return DateTimeFormatter.ofPattern(fallback);
        }
    }

    static class TimeTextTemplateProperty extends TextTemplateProperty {

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
                new Var("curr", "%s", localized(KEY_PROPERTY_VAR + ".curr")),
                new Var("tzid", "%s", localized(KEY_PROPERTY_VAR + ".tzid")),
                new Var("passed", "%s", localized(KEY_PROPERTY_VAR + ".passed")),
                new Var("left", "%s", localized(KEY_PROPERTY_VAR + ".left"))
            };
        }

        public IndicatorSkinTransfer skinCompact() {
            return skin(localized(KEY_PROPERTY_TEMPLATE + ".compact"));
        }

        public IndicatorSkinTransfer skinWithZone() {
            return skin(localized(KEY_PROPERTY_TEMPLATE + ".withZone"));
        }
    }

    public class TimeTextContext extends IndicatorContext {

        public final FontColorProperty.FontColorContext text;
        public final TextAlignProperty.TextAlignContext textAlign;
        public final TextTemplateProperty.TextTemplateContext template;
        public final TimeZoneProperty.TimeZoneContext timeZone;
        public final StringProperty.StringContext timeFormat;
        public final StringProperty.StringContext durationFormat;

        public TimeTextContext(Player player, Canvas canvas) {
            super(player, canvas);
            text = mFont.createContext();
            textAlign = mTextAlign.createContext();
            template = getTemplate().createContext();
            timeZone = mTimeZone.createContext();
            timeFormat = mTimeFormat.createContext();
            durationFormat = mDurationFormat.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new TimeTextContext(player, canvas);
    }
}
