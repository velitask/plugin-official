package com.velitask.plugin.official;

import com.velitask.sdk.Indicator;
import com.velitask.sdk.IndicatorContext;
import com.velitask.sdk.IndicatorSkin;
import com.velitask.sdk.properties.BorderProperty;
import com.velitask.sdk.properties.DisplayConfig;
import com.velitask.sdk.properties.IProperty;
import com.velitask.sdk.properties.PropertyGroup;
import com.velitask.sdk.properties.VideoSourceProperty;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import org.abricos.core.state.maket.HorizontalAlign;
import org.abricos.core.state.maket.Maket;
import org.abricos.core.state.maket.VerticalAlign;

public class VideoIndicator extends Indicator {

    public static final String NAME = "video";

    private static final String KEY = _KEY + "." + NAME;

    private final VideoSourceProperty mVideo = new VideoSourceProperty();

    private final BorderProperty mBorder = new BorderProperty();

    public VideoSourceProperty getVideo() {
        return mVideo;
    }

    public BorderProperty getBorder() {
        return mBorder;
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
        maket.setVertical(VerticalAlign.BOTTOM);
        maket.setHorizontal(HorizontalAlign.RIGHT);
        maket.setLayerMargin(null, null, 50d, 50d);
        maket.setLayerSize(480d, 270d);
    }

    @Override
    public IProperty[] defineProperties() {
        return new IProperty[]{
            mVideo, mBorder
        };
    }

    @Override
    public void configureDisplay(DisplayConfig config) {
        config.set(mBorder, PropertyGroup.APPEARANCE);
    }

    @Override
    public IndicatorSkin[] defineSkins() {
        return new IndicatorSkin[]{
            IndicatorSkin.builder("framed", localized(KEY + ".skin.framed"))
            .set(mBorder.getColor().skin(Color.WHITE))
            .set(mBorder.getThickness().skin(4))
            .set(mBorder.getRadius().skin(0))
            .build(),
            IndicatorSkin.builder("rounded", localized(KEY + ".skin.rounded"))
            .set(mBorder.getColor().skin(Color.WHITE))
            .set(mBorder.getThickness().skin(0))
            .set(mBorder.getRadius().skin(30))
            .build()
        };
    }

    @Override
    public void render(IndicatorContext indicatorContext) {
        VideoContext ctx = (VideoContext) indicatorContext;
        Graphics2D g = ctx.graphics;

        int bw = (int) (ctx.border.thickness * ctx.scale);
        int cr = (int) (ctx.border.radius * ctx.scale);
        int innerW = Math.max(1, ctx.width - bw);
        int innerH = Math.max(1, ctx.height - bw);

        long rawTime = mVideo.convertToRawTime(ctx.player.time);
        if (ctx.player.isPreview) {
            rawTime = mVideo.clampToSensorRange(rawTime);
        }
        BufferedImage frame = ctx.video.frameAt(rawTime, innerW, innerH);
        if (frame == null) {
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Shape originalClip = g.getClip();
        if (cr > 0) {
            g.setClip(new RoundRectangle2D.Float(
                    bw / 2f, bw / 2f, innerW, innerH, cr, cr)
            );
        }
        g.drawImage(frame, bw / 2, bw / 2, innerW, innerH, null);
        g.setClip(originalClip);

        if (bw > 0) {
            g.setColor(ctx.border.color);
            g.setStroke(new BasicStroke(bw));
            if (cr > 0) {
                g.drawRoundRect(bw / 2, bw / 2, innerW, innerH, cr, cr);
            } else {
                g.drawRect(bw / 2, bw / 2, innerW, innerH);
            }
        }
    }

    public class VideoContext extends IndicatorContext {

        public final BorderProperty.BorderContext border;
        public final VideoSourceProperty.VideoSourceContext video;

        public VideoContext(Player player, Canvas canvas) {
            super(player, canvas);
            border = mBorder.createContext();
            video = mVideo.createContext();
        }
    }

    @Override
    public IndicatorContext createContext(IndicatorContext.Player player, IndicatorContext.Canvas canvas) {
        return new VideoContext(player, canvas);
    }
}
