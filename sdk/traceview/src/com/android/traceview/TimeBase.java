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

interface TimeBase {
    public static final TimeBase CPU_TIME = new CpuTimeBase();
    public static final TimeBase REAL_TIME = new RealTimeBase();

    public long getTime(ThreadData threadData);
    public long getElapsedInclusiveTime(MethodData methodData);
    public long getElapsedExclusiveTime(MethodData methodData);
    public long getElapsedInclusiveTime(ProfileData profileData);

    public static final class CpuTimeBase implements TimeBase {
        @Override
        public long getTime(ThreadData threadData) {
            return threadData.getCpuTime();
        }

        @Override
        public long getElapsedInclusiveTime(MethodData methodData) {
            return methodData.getElapsedInclusiveCpuTime();
        }

        @Override
        public long getElapsedExclusiveTime(MethodData methodData) {
            return methodData.getElapsedExclusiveCpuTime();
        }

        @Override
        public long getElapsedInclusiveTime(ProfileData profileData) {
            return profileData.getElapsedInclusiveCpuTime();
        }
    }

    public static final class RealTimeBase implements TimeBase {
        @Override
        public long getTime(ThreadData threadData) {
            return threadData.getRealTime();
        }

        @Override
        public long getElapsedInclusiveTime(MethodData methodData) {
            return methodData.getElapsedInclusiveRealTime();
        }

        @Override
        public long getElapsedExclusiveTime(MethodData methodData) {
            return methodData.getElapsedExclusiveRealTime();
        }

        @Override
        public long getElapsedInclusiveTime(ProfileData profileData) {
            return profileData.getElapsedInclusiveRealTime();
        }
    }
}
