package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.EnumArrayProperty;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.LineProperty;
import com.velitask.sdk.properties.LineStyle;
import com.velitask.sdk.properties.PropertyGroup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class LineIndicator extends Indicator {

    public static enum LineDirection {
        HORIZONTAL,
        VERTICAL,
        DIAGONAL_DOWN,
        DIAGONAL_UP
    }

    public static final String NAME = "line";

    private static final String KEY = _KEY + "." + NAME;

    private final LineProperty mLine = new LineProperty();

    private final EnumArrayProperty<LineDirection> mDirection
            = new EnumArrayProperty<>(LineDirection.class) {
        @Override
        public String getName() {
            return "direction";
        }

        @Override
        public String getTitle() {
            return localized(KEY + ".direction.title");
        }

        @Override
        protected String[] defineTitles() {
            LineDirection[] vs = LineDirection.values();
            String[] t = new String[vs.length];
            for (int i = 0; i < vs.length; i++) {
                t[i] = localized(KEY + ".direction." + vs[i].name().toLowerCase());
            }
            return t;
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
        maket.setLayerSize(400d, 20d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{mLine, mDirection};
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mLine, PropertyGroup.APPEARANCE);
        config.set(mDirection, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("dashed", localized(KEY + ".skin.dashed"))
            .set(mLine.getColor().skin(Color.WHITE))
            .set(mLine.getThickness().skin(2))
            .set(mLine.getStyle().skin(LineStyle.DASH))
            .build(),
            IndicatorSkin.builder("bold", localized(KEY + ".skin.bold"))
            .set(mLine.getColor().skin(Color.WHITE))
            .set(mLine.getThickness().skin(8))
            .set(mLine.getStyle().skin(LineStyle.SOLID))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        LineIndicatorContext ctx = (LineIndicatorContext) indicatorContext;
        Graphics2D g = ctx.graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float stroke = (float) (ctx.line.thickness * ctx.scale);
        if (stroke < 1) {
            stroke = 1;
        }

        g.setColor(ctx.line.color);
        g.setStroke(buildStroke(stroke, ctx.line.dashPattern));

        int x1, y1, x2, y2;
        LineDirection dir = ctx.direction == null ? LineDirection.HORIZONTAL : ctx.direction;
        switch (dir) {
            case VERTICAL -> {
                x1 = x2 = ctx.width / 2;
                y1 = 0;
                y2 = ctx.height;
            }
            case DIAGONAL_DOWN -> {
                x1 = 0;
                y1 = 0;
                x2 = ctx.width;
                y2 = ctx.height;
            }
            case DIAGONAL_UP -> {
                x1 = 0;
                y1 = ctx.height;
                x2 = ctx.width;
                y2 = 0;
            }
            default -> {
                x1 = 0;
                x2 = ctx.width;
                y1 = y2 = ctx.height / 2;
            }
        }
        g.drawLine(x1, y1, x2, y2);
    }

    private static BasicStroke buildStroke(float width, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        }
        String[] parts = pattern.split(",");
        float[] dash = new float[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                dash[i] = Float.parseFloat(parts[i].trim());
                if (dash[i] <= 0) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException ex) {
            return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        }
        return new BasicStroke(width,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, dash, 0f
        );
    }

    public class LineIndicatorContext extends IndicatorContext {

        public final LineProperty.LineContext line;
        public final LineDirection direction;

        public LineIndicatorContext(Player player, Canvas canvas) {
            super(player, canvas);
            line = mLine.createContext();
            direction = mDirection.get();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new LineIndicatorContext(player, canvas);
    }

}
