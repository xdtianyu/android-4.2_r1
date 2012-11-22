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
import com.android.annotations.Nullable;
import com.android.sdklib.util.GrabProcessOutput;
import com.android.sdklib.util.GrabProcessOutput.IProcessOutput;
import com.android.sdklib.util.GrabProcessOutput.Wait;
import com.android.utils.ILogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * A Helper to create new keystore/key.
 */
public final class KeystoreHelper {

    /**
     * Creates a new store
     * @param osKeyStorePath the location of the store
     * @param storeType an optional keystore type, or <code>null</code> if the default is to
     * be used.
     * @param storePassword
     * @param alias
     * @param keyPassword
     * @param description
     * @param validityYears
     * @param logger
     * @throws KeytoolException
     */
    public static boolean createNewStore(
            @NonNull String osKeyStorePath,
            String storeType,
            @NonNull String storePassword,
            @NonNull String alias,
            @NonNull String keyPassword,
            @NonNull String description,
            int validityYears,
            @NonNull final ILogger logger)
            throws KeytoolException {

        // get the executable name of keytool depending on the platform.
        String os = System.getProperty("os.name");

        String keytoolCommand;
        if (os.startsWith("Windows")) {
            keytoolCommand = "keytool.exe";
        } else {
            keytoolCommand = "keytool";
        }

        String javaHome = System.getProperty("java.home");

        if (javaHome != null && javaHome.length() > 0) {
            keytoolCommand = javaHome + File.separator + "bin" + File.separator + keytoolCommand;
        }

        // create the command line to call key tool to build the key with no user input.
        ArrayList<String> commandList = new ArrayList<String>();
        commandList.add(keytoolCommand);
        commandList.add("-genkey");
        commandList.add("-alias");
        commandList.add(alias);
        commandList.add("-keyalg");
        commandList.add("RSA");
        commandList.add("-dname");
        commandList.add(description);
        commandList.add("-validity");
        commandList.add(Integer.toString(validityYears * 365));
        commandList.add("-keypass");
        commandList.add(keyPassword);
        commandList.add("-keystore");
        commandList.add(osKeyStorePath);
        commandList.add("-storepass");
        commandList.add(storePassword);
        if (storeType != null) {
            commandList.add("-storetype");
            commandList.add(storeType);
        }

        String[] commandArray = commandList.toArray(new String[commandList.size()]);

        // launch the command line process
        int result = 0;
        try {
            Process process = Runtime.getRuntime().exec(commandArray);
            result = GrabProcessOutput.grabProcessOutput(
                    process,
                    Wait.WAIT_FOR_READERS,
                    new IProcessOutput() {
                        @Override
                        public void out(@Nullable String line) {
                            if (line != null) {
                                if (logger != null) {
                                    logger.info(line);
                                }
                            }
                        }

                        @Override
                        public void err(@Nullable String line) {
                            if (line != null) {
                                if (logger != null) {
                                    logger.error(null /*throwable*/, line);
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            // create the command line as one string for debugging purposes
            StringBuilder builder = new StringBuilder();
            boolean firstArg = true;
            for (String arg : commandArray) {
                boolean hasSpace = arg.indexOf(' ') != -1;

                if (firstArg == true) {
                    firstArg = false;
                } else {
                    builder.append(' ');
                }

                if (hasSpace) {
                    builder.append('"');
                }

                builder.append(arg);

                if (hasSpace) {
                    builder.append('"');
                }
            }

            throw new KeytoolException("Failed to create key: " + e.getMessage(),
                    javaHome, builder.toString());
        }

        if (result != 0) {
            return false;
        }

        return true;
    }

    public static SigningInfo getSigningInfo(
            @NonNull String keyStoreLocation,
            @NonNull String keyStorePassword,
            String keyStoreType,
            @NonNull String keyAlias,
            @NonNull String keyPassword) throws KeytoolException, FileNotFoundException {

        try {
            KeyStore keyStore = KeyStore.getInstance(
                    keyStoreType != null ? keyStoreType : KeyStore.getDefaultType());

            FileInputStream fis = new FileInputStream(keyStoreLocation);
            keyStore.load(fis, keyStorePassword.toCharArray());
            fis.close();
            PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                    keyAlias, new KeyStore.PasswordProtection(keyPassword.toCharArray()));

            if (entry != null) {
                return new SigningInfo(entry.getPrivateKey(), (X509Certificate) entry.getCertificate());
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new KeytoolException(
                    String.format("Failed to read key %1$s from store \"%2$s\": %3$s",
                            keyAlias, keyStoreLocation, e.getMessage()),
                    e);
        }

        return null;
    }
}
