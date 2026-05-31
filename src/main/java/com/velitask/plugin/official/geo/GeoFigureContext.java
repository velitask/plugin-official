package com.velitask.plugin.official.geo;

import com.velitask.plugin.official.GeoMapIndicator;
import com.velitask.sdk.figures.FigureContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import org.abricos.geo.MapZoom;

public class GeoFigureContext extends FigureContext<GeoMapIndicator.GeoMapContext> {

    public MapZoom zoom;

    public double centerPx;
    public double centerPy;

    public AffineTransform mapTransform;

    public GeoFigureContext(GeoMapIndicator.GeoMapContext indCtx) {
        super(indCtx);
        zoom = indCtx.zoom;
        centerPx = indCtx.centerPx;
        centerPy = indCtx.centerPy;
        mapTransform = indCtx.mapTransform;
    }

    public Point2D.Double toScreen(double lat, double lon) {
        Point2D.Double p = new Point2D.Double(zoom.pixelX(lon), zoom.pixelY(lat));
        mapTransform.transform(p, p);
        return p;
    }
}
