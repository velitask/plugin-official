package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.properties.FontColorProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.StringProperty;
import com.velitask.sdk.properties.TextAlignProperty;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class TextIndicator extends Indicator {

    public static final String NAME = "text";

    private static final String KEY = _KEY + "." + NAME;

    private final StringProperty mValue = new StringProperty(
            localized(KEY + ".value.default"),
            "value",
            localized(KEY + ".value.title")
    );

    private final FontColorProperty mFont = new FontColorProperty();

    private final TextAlignProperty mTextAlign = new TextAlignProperty();

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
        maket.setLayerMargin(null, 100d, 100d, null);
        maket.setLayerSize(600d, 120d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mValue,
            mFont,
            mTextAlign
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        TextContext ctx = (TextContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        String text = ctx.value.value;
        if (text == null || text.isEmpty()) {
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = ctx.font.toFont(ctx.scale);
        g.setFont(font);
        g.setColor(ctx.font.color.value);

        ctx.textAlign.drawText(g, text, ctx.width, ctx.height);
    }

    public class TextContext extends IndicatorContext {

        public final StringProperty.StringContext value;
        public final FontColorProperty.FontColorContext font;
        public final TextAlignProperty.TextAlignContext textAlign;

        public TextContext(Player player, Canvas canvas) {
            super(player, canvas);
            value = mValue.createContext();
            font = mFont.createContext();
            textAlign = mTextAlign.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new TextContext(player, canvas);
    }
}
