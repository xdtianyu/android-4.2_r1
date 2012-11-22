/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.verifier.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class LocationVerifier implements Handler.Callback {
    public static final String TAG = "CtsVerifierLocation";

    private static final int MSG_TIMEOUT = 1;

    private final LocationManager mLocationManager;
    private final PassFailLog mCb;
    private final String mProvider;
    private final long mInterval;
    private final long mMinActiveInterval;
    private final long mMinPassiveInterval;
    private final long mTimeout;
    private final Handler mHandler;
    private final int mRequestedUpdates;
    private final ActiveListener mActiveListener;
    private final PassiveListener mPassiveListener;

    private long mLastActiveTimestamp = -1;
    private long mLastPassiveTimestamp = -1;
    private int mNumActiveUpdates = 0;
    private int mNumPassiveUpdates = 0;
    private boolean mRunning = false;
    private boolean mActiveLocationArrive = false;

    private class ActiveListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (!mRunning) return;

            mActiveLocationArrive = true;
            mNumActiveUpdates++;
            scheduleTimeout();

            long timestamp = location.getTime();
            long delta = timestamp - mLastActiveTimestamp;
            mLastActiveTimestamp = timestamp;

            if (location.getAccuracy() <= 0.0) {
                fail(mProvider + " location has invalid accuracy: " + location.getAccuracy());
            }
            if (location.getElapsedRealtimeNanos() <= 0) {
                fail(mProvider + " location has invalid elapsed realtime: " +
                        location.getElapsedRealtimeNanos());
            }

            if (mNumActiveUpdates != 1 && delta < mMinActiveInterval) {
                fail(mProvider + " location updated too fast: " + delta + "ms < " +
                        mMinActiveInterval + "ms");
                return;
            }

            mCb.log("active " + mProvider + " update (" + delta + "ms)");

            if (!mProvider.equals(location.getProvider())) {
                fail("wrong provider in callback, actual: " + location.getProvider() +
                        " expected: " + mProvider);
                return;
            }

            if (mNumActiveUpdates >= mRequestedUpdates) {
                if (mNumPassiveUpdates < mRequestedUpdates - 1) {
                    fail("passive location updates not working (expected: " + mRequestedUpdates +
                            " received: " + mNumPassiveUpdates + ")");
                }
                pass();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    }

    private class PassiveListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (!mRunning) return;
            if (!location.getProvider().equals(mProvider)) return;

            // When a test round start, passive listener shouldn't recevice location before active listener.
            // If this situation occurs, we treat this location as overdue location.
            // (The overdue location comes from previous test round, it occurs occasionally)
            // We have to skip it to prevent wrong calculation of time interval.
            if (!mActiveLocationArrive) {
                mCb.log("ignoring passive " + mProvider + " update");
                return;
            }

            mNumPassiveUpdates++;
            long timestamp = location.getTime();
            long delta = timestamp - mLastPassiveTimestamp;
            mLastPassiveTimestamp = timestamp;

            if (location.getAccuracy() <= 0.0) {
                fail(mProvider + " location has invalid accuracy: " + location.getAccuracy());
            }
            if (location.getElapsedRealtimeNanos() <= 0) {
                fail(mProvider + " location has invalid elapsed realtime: " +
                        location.getElapsedRealtimeNanos());
            }

            if (mNumPassiveUpdates != 1 && delta < mMinPassiveInterval) {
                fail("passive " + mProvider + " location updated too fast: " + delta + "ms < " +
                        mMinPassiveInterval + "ms");
                mCb.log("when passive updates are much much faster than active updates it " +
                        "suggests the location provider implementation is not power efficient");
                if (LocationManager.GPS_PROVIDER.equals(mProvider)) {
                    mCb.log("check GPS_CAPABILITY_SCHEDULING in GPS driver");
                }
                return;
            }

            mCb.log("passive " + mProvider + " update (" + delta + "ms)");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    }

    public LocationVerifier(PassFailLog cb, LocationManager locationManager,
            String provider, long requestedInterval, int numUpdates) {
        mProvider = provider;
        mInterval = requestedInterval;
        // Updates can be up to 100ms ahead of schedule
        mMinActiveInterval = Math.max(0, requestedInterval - 100);
        // Allow passive updates to be up to 10x faster than active updates,
        // beyond that it is very likely the implementation is not taking
        // advantage of the interval to be power efficient
        mMinPassiveInterval = mMinActiveInterval / 10;
        // timeout at 60 seconds after interval time
        mTimeout = requestedInterval + 60 * 1000;
        mRequestedUpdates = numUpdates;
        mLocationManager = locationManager;
        mCb = cb;
        mHandler = new Handler(this);
        mActiveListener = new ActiveListener();
        mPassiveListener = new PassiveListener();
    }

    public void start() {
        mRunning = true;
        scheduleTimeout();
        mLastActiveTimestamp = System.currentTimeMillis();
        mLastPassiveTimestamp = mLastActiveTimestamp;
        mCb.log("enabling passive listener");
        mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0,
                mPassiveListener);
        mCb.log("enabling " + mProvider + " (minTime=" + mInterval + "ms)");
        mLocationManager.requestLocationUpdates(mProvider, mInterval, 0,
                mActiveListener);
    }

    public void stop() {
        mRunning = false;
        mCb.log("disabling " + mProvider);
        mLocationManager.removeUpdates(mActiveListener);
        mCb.log("disabling passive listener");
        mLocationManager.removeUpdates(mPassiveListener);
        mHandler.removeMessages(MSG_TIMEOUT);
    }

    private void pass() {
        stop();
        mCb.pass();
    }

    private void fail(String s) {
        stop();
        mCb.fail(s);
    }

    private void scheduleTimeout() {
        mHandler.removeMessages(MSG_TIMEOUT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_TIMEOUT), mTimeout);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (!mRunning) return true;
        fail("timeout (" + mTimeout + "ms) waiting for " +
                mProvider + " location change");
        return true;
    }
}
