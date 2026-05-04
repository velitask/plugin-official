package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.properties.BorderProperty;
import com.velitask.sdk.properties.ColorProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class RectangleIndicator extends Indicator {

    public static final String NAME = "rectangle";

    private static final String KEY = _KEY + "." + NAME;

    private static final String FILL_COLOR = "fillColor";
    private final ColorProperty mFillColor = new ColorProperty(
            new Color(0, 0, 0, 128),
            FILL_COLOR,
            localized(KEY + "." + FILL_COLOR + ".title")
    );

    private final BorderProperty mBorder = new BorderProperty();

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
        maket.setLayerSize(400d, 200d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{mFillColor, mBorder};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mFillColor, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("outline", localized(KEY + ".skin.outline"))
            .set(mFillColor.skin(0, 0, 0, 0))
            .set(mBorder.getColor().skin(Color.WHITE))
            .set(mBorder.getThickness().skin(3))
            .set(mBorder.getRadius().skin(0))
            .build(),
            IndicatorSkin.builder("rounded", localized(KEY + ".skin.rounded"))
            .set(mFillColor.skin(0, 0, 0, 160))
            .set(mBorder.getColor().skin(Color.WHITE))
            .set(mBorder.getThickness().skin(0))
            .set(mBorder.getRadius().skin(20))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        RectangleContext ctx = (RectangleContext) indicatorContext;
        Graphics2D g = ctx.graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int bw = (int) (ctx.border.thickness * ctx.scale);
        int cr = (int) (ctx.border.radius * ctx.scale);

        int x = bw / 2, y = bw / 2;
        int w = Math.max(1, ctx.width - bw);
        int h = Math.max(1, ctx.height - bw);

        g.setColor(ctx.fillColor.value);
        if (cr > 0) {
            g.fillRoundRect(x, y, w, h, cr, cr);
        } else {
            g.fillRect(x, y, w, h);
        }

        if (bw > 0) {
            g.setColor(ctx.border.color);
            g.setStroke(new BasicStroke(bw));
            if (cr > 0) {
                g.drawRoundRect(x, y, w, h, cr, cr);
            } else {
                g.drawRect(x, y, w, h);
            }
        }
    }

    public class RectangleContext extends IndicatorContext {

        public final ColorProperty.ColorContext fillColor;
        public final BorderProperty.BorderContext border;

        public RectangleContext(Player player, Canvas canvas) {
            super(player, canvas);
            fillColor = mFillColor.createContext();
            border = mBorder.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new RectangleContext(player, canvas);
    }

}
