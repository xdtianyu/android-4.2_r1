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

package com.android.builder.signing;

import com.android.annotations.NonNull;
import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.utils.ILogger;

import java.io.FileNotFoundException;

public class DebugKeyHelper {

    private static final String PASSWORD_STRING = "android";
    private static final String DEBUG_ALIAS = "AndroidDebugKey";

    // Certificate CN value. This is a hard-coded value for the debug key.
    // Android Market checks against this value in order to refuse applications signed with
    // debug keys.
    private static final String CERTIFICATE_DESC = "CN=Android Debug,O=Android,C=US";

    /**
     * Returns the location of the default debug keystore.
     *
     * @return The location of the default debug keystore.
     * @throws AndroidLocationException if the location cannot be computed
     */

    public static String defaultDebugKeyStoreLocation() throws AndroidLocationException {
        // this is guaranteed to either return a non null value (terminated with a platform
        // specific separator), or throw.
        String folder = AndroidLocation.getFolder();

        return folder + "debug.keystore";
    }

    /**
     * Creates a new store
     * @param osKeyStorePath the location of the store
     * @param storeType an optional keystore type, or <code>null</code> if the default is to
     * be used.
     * @param logger a logger object to receive the log of the creation.
     * @throws KeytoolException
     */
    public static boolean createNewStore(@NonNull String keyStoreLocation,
            String storeType, @NonNull ILogger logger) throws KeytoolException {

        return KeystoreHelper.createNewStore(
                keyStoreLocation, storeType, PASSWORD_STRING,
                DEBUG_ALIAS, PASSWORD_STRING,
                CERTIFICATE_DESC, 30 /* validity*/,
                logger);
    }

    public static SigningInfo getDebugKey(@NonNull String keyStoreLocation, String storeStype)
            throws KeytoolException, FileNotFoundException {

        return KeystoreHelper.getSigningInfo(
                keyStoreLocation, PASSWORD_STRING, storeStype, DEBUG_ALIAS, PASSWORD_STRING);
    }
}
