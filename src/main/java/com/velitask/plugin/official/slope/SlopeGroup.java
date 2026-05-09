package com.velitask.plugin.official.slope;

import com.velitask.sdk.data.SlopeSensorAtom;
import java.util.ArrayList;
import java.util.List;

public class SlopeGroup {

    private long mId;

    private final int mType;
    private final List<SlopeSensorAtom> mList = new ArrayList<>();

    public SlopeGroup(SlopeSensorAtom first) {
        mType = first.slopeType;
        mList.add(first);
    }

    public int getType() {
        return mType;
    }

    public List<SlopeSensorAtom> getList() {
        return mList;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public void add(SlopeSensorAtom atom) {
        mList.add(atom);
    }

    public SlopeGroupAtom buildAtom(long sensorId, int measureIndex) {
        SlopeGroupAtom g = new SlopeGroupAtom();
        g.id = mId;
        g.sensor_id = sensorId;
        g.measureIndex = measureIndex;
        g.slopeType = mType;

        if (mList.isEmpty()) {
            return g;
        }
        SlopeSensorAtom first = mList.get(0);
        SlopeSensorAtom last = mList.get(mList.size() - 1);

        g.timeRaw = first.timeRaw;
        g.duration = (last.timeRaw + last.duration) - first.timeRaw;
        g.distance = first.distance;
        g.elevation = first.elevation;

        long endDistance = last.distance + last.distanceDelta;
        int endElevation = last.elevation + last.elevationDelta;
        g.distanceDelta = endDistance - first.distance;
        g.elevationDelta = endElevation - first.elevation;

        int eleUp = 0;
        int eleDown = 0;
        for (SlopeSensorAtom a : mList) {
            if (a.elevationDelta > 0) {
                eleUp += a.elevationDelta;
            } else if (a.elevationDelta < 0) {
                eleDown += a.elevationDelta;
            }
        }
        g.eleUp = eleUp;
        g.eleDown = eleDown;

        if (g.distanceDelta > 0) {
            g.slopePercent = (int) (g.elevationDelta * 10000L / g.distanceDelta);
        }
        return g;
    }
}
