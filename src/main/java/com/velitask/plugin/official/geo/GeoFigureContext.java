package com.velitask.plugin.official.geo;

import com.velitask.plugin.official.GeoMapIndicator;
import com.velitask.sdk.figures.FigureContext;
import org.abricos.geo.MapZoom;

public class GeoFigureContext extends FigureContext<GeoMapIndicator.GeoMapContext> {

    public MapZoom zoom;

    public double centerPx;
    public double centerPy;

    public GeoFigureContext(GeoMapIndicator.GeoMapContext indCtx) {
        super(indCtx);
        zoom = indCtx.zoom;
        centerPx = indCtx.centerPx;
        centerPy = indCtx.centerPy;
    }

    public int toScreenX(double lon) {
        double globalX = zoom.pixelX(lon);
        return (int) (globalX - centerPx + width / 2.0);
    }

    public int toScreenY(double lat) {
        double globalY = zoom.pixelY(lat);
        return (int) (globalY - centerPy + height / 2.0);
    }
}
