/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.traceview;

import org.eclipse.swt.graphics.Color;

class Call implements TimeLineView.Block {
    final private ThreadData mThreadData;
    final private MethodData mMethodData;
    final Call mCaller; // the caller, or null if this is the root

    private String mName;
    private boolean mIsRecursive;

    long mGlobalStartTime;
    long mGlobalEndTime;

    long mThreadStartTime;
    long mThreadEndTime;

    long mInclusiveRealTime; // real time spent in this call including its children
    long mExclusiveRealTime; // real time spent in this call including its children

    long mInclusiveCpuTime; // cpu time spent in this call including its children
    long mExclusiveCpuTime; // cpu time spent in this call excluding its children

    Call(ThreadData threadData, MethodData methodData, Call caller) {
        mThreadData = threadData;
        mMethodData = methodData;
        mName = methodData.getProfileName();
        mCaller = caller;
    }

    public void updateName() {
        mName = mMethodData.getProfileName();
    }

    @Override
    public double addWeight(int x, int y, double weight) {
        return mMethodData.addWeight(x, y, weight);
    }

    @Override
    public void clearWeight() {
        mMethodData.clearWeight();
    }

    @Override
    public long getStartTime() {
        return mGlobalStartTime;
    }

    @Override
    public long getEndTime() {
        return mGlobalEndTime;
    }

    @Override
    public long getExclusiveCpuTime() {
        return mExclusiveCpuTime;
    }

    @Override
    public long getInclusiveCpuTime() {
        return mInclusiveCpuTime;
    }

    @Override
    public long getExclusiveRealTime() {
        return mExclusiveRealTime;
    }

    @Override
    public long getInclusiveRealTime() {
        return mInclusiveRealTime;
    }

    @Override
    public Color getColor() {
        return mMethodData.getColor();
    }

    @Override
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public ThreadData getThreadData() {
        return mThreadData;
    }

    public int getThreadId() {
        return mThreadData.getId();
    }

    @Override
    public MethodData getMethodData() {
        return mMethodData;
    }

    @Override
    public boolean isContextSwitch() {
        return mMethodData.getId() == -1;
    }

    @Override
    public boolean isIgnoredBlock() {
        // Ignore the top-level call or context switches within the top-level call.
        return mCaller == null || isContextSwitch() && mCaller.mCaller == null;
    }

    @Override
    public TimeLineView.Block getParentBlock() {
        return mCaller;
    }

    public boolean isRecursive() {
        return mIsRecursive;
    }

    void setRecursive(boolean isRecursive) {
        mIsRecursive = isRecursive;
    }

    void addCpuTime(long elapsedCpuTime) {
        mExclusiveCpuTime += elapsedCpuTime;
        mInclusiveCpuTime += elapsedCpuTime;
    }

    /**
     * Record time spent in the method call.
     */
    void finish() {
        if (mCaller != null) {
            mCaller.mInclusiveCpuTime += mInclusiveCpuTime;
            mCaller.mInclusiveRealTime += mInclusiveRealTime;
        }

        mMethodData.addElapsedExclusive(mExclusiveCpuTime, mExclusiveRealTime);
        if (!mIsRecursive) {
            mMethodData.addTopExclusive(mExclusiveCpuTime, mExclusiveRealTime);
        }
        mMethodData.addElapsedInclusive(mInclusiveCpuTime, mInclusiveRealTime,
                mIsRecursive, mCaller);
    }

    public static final class TraceAction {
        public static final int ACTION_ENTER = 0;
        public static final int ACTION_EXIT = 1;

        public final int mAction;
        public final Call mCall;

        public TraceAction(int action, Call call) {
            mAction = action;
            mCall = call;
        }
    }
}
