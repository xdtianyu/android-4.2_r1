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

package com.android.pts.util;

/**
 * Utilities for doing statistics
 *
 */
public class Stat {

    public static class StatResult {
        public double mAverage;
        public double mMin;
        public double mMax;
        public double mStddev;
        public StatResult(double average, double min, double max, double stddev) {
            mAverage = average;
            mMin = min;
            mMax = max;
            mStddev = stddev;
        }
    }

    public static StatResult getStat(double[] data) {
        double average = data[0];
        double min = data[0];
        double max = data[0];
        double eX2 = data[0] * data[0]; // will become E[X^2]
        for (int i = 1; i < data.length; i++) {
            average += data[i];
            eX2 += data[i] * data[i];
            if (data[i] > max) {
                max = data[i];
            }
            if (data[i] < min) {
                min = data[i];
            }
        }
        average /= data.length;
        eX2 /= data.length;
        // stddev = sqrt(E[X^2] - (E[X])^2)
        double stddev = Math.sqrt(eX2 - average * average);
        return new StatResult(average, min, max, stddev);
    }

    public static double getMin(double[] data) {
        double min = data[0];
        for (int i = 1; i < data.length; i++) {
            if (data[i] < min) {
                min = data[i];
            }
        }
        return min;
    }

    public static double getMax(double[] data) {
        double max = data[0];
        for (int i = 1; i < data.length; i++) {
            if (data[i] > max) {
                max = data[i];
            }
        }
        return max;
    }
}
