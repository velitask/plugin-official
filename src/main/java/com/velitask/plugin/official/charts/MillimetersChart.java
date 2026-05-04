package com.velitask.plugin.official.charts;

import org.abricos.geo.GeoUtils;

public class MillimetersChart {

    private final int mWidth;
    private final int mHeight;
    private final int mGeoZoom;

    private final long mMmPerPixel;

    private final long mCenterMmX;

    private final long mCenterMmY;

    private final long mMmWidth;
    private final long mMmHeight;

    private final long mMinMmX;
    private final long mMaxMmX;

    private final long mMinMmY;
    private final long mMaxMmY;

    private final double mKoefX;
    private final double mKoefY;

    private final int mScaleX;
    private final int mScaleY;

    public MillimetersChart(
            int width, int height,
            int geoZoom, double latitude,
            long centerMmX, long centerMmY,
            int scaleX, int scaleY
    ) {
        mWidth = width;
        mHeight = height;
        mGeoZoom = geoZoom;
        mCenterMmX = centerMmX;
        mCenterMmY = centerMmY;

        mMmPerPixel = ((Double) (GeoUtils.calcMetersPerPixel(geoZoom) * 1000d)).longValue();

        mScaleX = scaleX != 0 ? scaleX : 1;
        mScaleY = scaleY != 0 ? scaleY : 1;

        mMmWidth = mWidth * (long) ((double) mMmPerPixel / (double) mScaleX);
        mMinMmX = centerMmX - mMmWidth / 2;
        mMaxMmX = mMinMmX + mMmWidth;

        mMmHeight = mHeight * (long) ((double) mMmPerPixel / (double) mScaleY);
        mMinMmY = centerMmY - mMmHeight / 2;
        mMaxMmY = mMinMmY + mMmHeight;

        mKoefX = (double) mWidth / (double) mMmWidth;
        mKoefY = (double) mHeight / (double) mMmHeight;
    }

    public int getGeoZoom() {
        return mGeoZoom;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public long getMmPerPixel() {
        return mMmPerPixel;
    }

    public long getMinMmX() {
        return mMinMmX;
    }

    public long getMaxMmX() {
        return mMaxMmX;
    }

    public long getMmWidth() {
        return mMmWidth;
    }

    public long getMinMmY() {
        return mMinMmY;
    }

    public long getMaxMmY() {
        return mMaxMmY;
    }

    public long getMmHeight() {
        return mMmHeight;
    }

    public int convertMmToPxX(long mm) {
        return (int) (mKoefX * (mm - mMinMmX));
    }

    public int convertMmToPxY(long mm) {
        return (int) (mHeight - mKoefY * (mm - mMinMmY));
    }

}
