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

import java.util.ArrayList;
import java.util.HashMap;

class ThreadData implements TimeLineView.Row {

    private int mId;
    private String mName;
    private boolean mIsEmpty;

    private Call mRootCall;
    private ArrayList<Call> mStack = new ArrayList<Call>();

    // This is a hash of all the methods that are currently on the stack.
    private HashMap<MethodData, Integer> mStackMethods = new HashMap<MethodData, Integer>();

    boolean mHaveGlobalTime;
    long mGlobalStartTime;
    long mGlobalEndTime;

    boolean mHaveThreadTime;
    long mThreadStartTime;
    long mThreadEndTime;

    long mThreadCurrentTime; // only used while parsing thread-cpu clock

    ThreadData(int id, String name, MethodData topLevel) {
        mId = id;
        mName = String.format("[%d] %s", id, name);
        mIsEmpty = true;
        mRootCall = new Call(this, topLevel, null);
        mRootCall.setName(mName);
        mStack.add(mRootCall);
    }

    @Override
    public String getName() {
        return mName;
    }

    public Call getRootCall() {
        return mRootCall;
    }

    /**
     * Returns true if no calls have ever been recorded for this thread.
     */
    public boolean isEmpty() {
        return mIsEmpty;
    }

    Call enter(MethodData method, ArrayList<TraceAction> trace) {
        if (mIsEmpty) {
            mIsEmpty = false;
            if (trace != null) {
                trace.add(new TraceAction(TraceAction.ACTION_ENTER, mRootCall));
            }
        }

        Call caller = top();
        Call call = new Call(this, method, caller);
        mStack.add(call);

        if (trace != null) {
            trace.add(new TraceAction(TraceAction.ACTION_ENTER, call));
        }

        Integer num = mStackMethods.get(method);
        if (num == null) {
            num = 0;
        } else if (num > 0) {
            call.setRecursive(true);
        }
        mStackMethods.put(method, num + 1);

        return call;
    }

    Call exit(MethodData method, ArrayList<TraceAction> trace) {
        Call call = top();
        if (call.mCaller == null) {
            return null;
        }

        if (call.getMethodData() != method) {
            String error = "Method exit (" + method.getName()
                    + ") does not match current method (" + call.getMethodData().getName()
                    + ")";
            throw new RuntimeException(error);
        }

        mStack.remove(mStack.size() - 1);

        if (trace != null) {
            trace.add(new TraceAction(TraceAction.ACTION_EXIT, call));
        }

        Integer num = mStackMethods.get(method);
        if (num != null) {
            if (num == 1) {
                mStackMethods.remove(method);
            } else {
                mStackMethods.put(method, num - 1);
            }
        }

        return call;
    }

    Call top() {
        return mStack.get(mStack.size() - 1);
    }

    void endTrace(ArrayList<TraceAction> trace) {
        for (int i = mStack.size() - 1; i >= 1; i--) {
            Call call = mStack.get(i);
            call.mGlobalEndTime = mGlobalEndTime;
            call.mThreadEndTime = mThreadEndTime;
            if (trace != null) {
                trace.add(new TraceAction(TraceAction.ACTION_INCOMPLETE, call));
            }
        }
        mStack.clear();
        mStackMethods.clear();
    }

    void updateRootCallTimeBounds() {
        if (!mIsEmpty) {
            mRootCall.mGlobalStartTime = mGlobalStartTime;
            mRootCall.mGlobalEndTime = mGlobalEndTime;
            mRootCall.mThreadStartTime = mThreadStartTime;
            mRootCall.mThreadEndTime = mThreadEndTime;
        }
    }

    @Override
    public String toString() {
        return mName;
    }

    @Override
    public int getId() {
        return mId;
    }

    public long getCpuTime() {
        return mRootCall.mInclusiveCpuTime;
    }

    public long getRealTime() {
        return mRootCall.mInclusiveRealTime;
    }
}
