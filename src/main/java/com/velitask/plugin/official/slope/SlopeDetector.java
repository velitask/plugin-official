package com.velitask.plugin.official.slope;

import com.velitask.sdk.data.SlopeSensorAtom;
import java.util.ArrayList;
import java.util.List;

public class SlopeDetector {

    private SlopeGroup mCurrentGroup;
    private final List<SlopeGroup> mGroups = new ArrayList<>();

    public List<SlopeGroup> getGroups() {
        return mGroups;
    }

    public SlopeGroup getCurrentGroup() {
        return mCurrentGroup;
    }

    public boolean add(SlopeSensorAtom a) {
        if (mCurrentGroup == null) {
            mGroups.add(mCurrentGroup = new SlopeGroup(a));
            return true;
        }
        if (mCurrentGroup.getType() == a.slopeType) {
            mCurrentGroup.add(a);
            return false;
        }
        mGroups.add(mCurrentGroup = new SlopeGroup(a));
        return true;
    }
}
