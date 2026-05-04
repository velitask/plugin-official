package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.properties.BorderWidthProperty;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.DisplayHint;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.IntegerProperty;
import com.velitask.sdk.properties.PropertyGroup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class EllipseIndicator extends Indicator {

    public static final String NAME = "ellipse";

    private static final String KEY = _KEY + "." + NAME;

    private static final String FILL_COLOR = "fillColor";
    private final ColorProperty mFillColor = new ColorProperty(
            new Color(0, 0, 0, 128),
            FILL_COLOR,
            localized(KEY + "." + FILL_COLOR + ".title")
    );

    private static final String BORDER_COLOR = "borderColor";
    private final ColorProperty mBorderColor = new ColorProperty(
            Color.WHITE,
            BORDER_COLOR,
            localized(KEY + "." + BORDER_COLOR + ".title")
    );

    private final BorderWidthProperty mBorderWidth = new BorderWidthProperty() {
        {
            setRange(0, 50);
        }

        @Override
        public Integer getDefault() {
            return 0;
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
        maket.setHorizontal(HorizontalAlign.LEFT);
        maket.setLayerMargin(100d, 100d, null, null);
        maket.setLayerSize(200d, 200d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{mFillColor, mBorderColor, mBorderWidth};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mFillColor, PropertyGroup.APPEARANCE);
        config.set(mBorderColor, PropertyGroup.APPEARANCE);
        config.set(mBorderWidth, PropertyGroup.APPEARANCE, DisplayHint.SPINNER);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("outline", localized(KEY + ".skin.outline"))
            .set(mFillColor.skin(0, 0, 0, 0))
            .set(mBorderColor.skin(Color.WHITE))
            .set(mBorderWidth.skin(3))
            .build(),
            IndicatorSkin.builder("solidLight", localized(KEY + ".skin.solidLight"))
            .set(mFillColor.skin(255, 255, 255, 180))
            .set(mBorderColor.skin(Color.BLACK))
            .set(mBorderWidth.skin(2))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        EllipseContext ctx = (EllipseContext) indicatorContext;
        Graphics2D g = ctx.graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int bw = (int) (ctx.borderWidth.value * ctx.scale);
        int x = bw / 2, y = bw / 2;
        int w = Math.max(1, ctx.width - bw);
        int h = Math.max(1, ctx.height - bw);

        g.setColor(ctx.fillColor.value);
        g.fillOval(x, y, w, h);

        if (bw > 0) {
            g.setColor(ctx.borderColor.value);
            g.setStroke(new BasicStroke(bw));
            g.drawOval(x, y, w, h);
        }
    }

    public class EllipseContext extends IndicatorContext {

        public final ColorProperty.ColorContext fillColor;
        public final ColorProperty.ColorContext borderColor;
        public final IntegerProperty.IntegerContext borderWidth;

        public EllipseContext(Player player, Canvas canvas) {
            super(player, canvas);
            fillColor = mFillColor.createContext();
            borderColor = mBorderColor.createContext();
            borderWidth = mBorderWidth.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new EllipseContext(player, canvas);
    }

}
