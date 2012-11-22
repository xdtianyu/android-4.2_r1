/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.chimpchat.core;

import com.android.chimpchat.core.IChimpDevice;

/**
 * Interface between the ChimpChat API and the ChimpChat backend that communicates
 * with Monkey.
 */
public interface IChimpBackend {
    /**
     * Wait for a default device to connect to the backend.
     *
     * @return the connected device (or null if timeout);
     */
    IChimpDevice waitForConnection();

    /**
     * Wait for a device to connect to the backend.
     *
     * @param timeoutMs how long (in ms) to wait
     * @param deviceIdRegex the regular expression to specify which device to wait for.
     * @return the connected device (or null if timeout);
     */
    IChimpDevice waitForConnection(long timeoutMs, String deviceIdRegex);

    /**
     * Shutdown the backend and cleanup any resources it was using.
     */
    void shutdown();
}
