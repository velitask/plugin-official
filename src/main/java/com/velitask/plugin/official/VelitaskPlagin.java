package com.velitask.plugin.official;

import com.velitask.sdk.IIndicator;
import com.velitask.sdk.IPlagin;
import com.velitask.sdk.i18n.Localization;

public class VelitaskPlagin implements IPlagin {

    @Override
    public String getUID() {
        return "com.velitask.plagin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String[] defineAdditionLocales() {
        return new String[]{"ru"};
    }

    public void registerLocalization() {
        Localization.instance().registerBundle(
                getClass().getClassLoader(), "strings/strings");
    }

    @Override
    public IIndicator[] defineIndicators() {
        return new IIndicator[]{
            new VideoIndicator(),
            new SpeedometerIndicator(),
            new DistanceTextIndicator(),
            new SlopeChartIndicator(),
            new SlopeTextIndicator(),
            new TimeTextIndicator(),
            new GeoMapIndicator(),
            new CompassIndicator(),
            new RectangleIndicator(),
            new EllipseIndicator(),
            new LineIndicator()
        };
    }

}
