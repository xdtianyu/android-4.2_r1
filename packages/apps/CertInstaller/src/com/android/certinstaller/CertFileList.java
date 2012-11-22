/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.certinstaller;

import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Lists certificate files in the SD card. User may click one to install it
 * to the system keystore.
 */
public class CertFileList extends CertFile
        implements Preference.OnPreferenceClickListener {
    private static final String TAG = "CertFileList";

    private static final String DOWNLOAD_DIR = "download";

    private SdCardMonitor mSdCardMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pick_file_pref);
        createFileList();
        startSdCardMonitor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSdCardMonitor();
    }

    @Override
    protected void onInstallationDone(boolean fileDeleted) {
        super.onInstallationDone(fileDeleted);
        if (!fileDeleted) {
            if (isSdCardPresent()) {
                setAllFilesEnabled(true);
            } else {
                Toast.makeText(this, R.string.sdcard_not_present,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onError(int errorId) {
        if (errorId == CERT_FILE_MISSING_ERROR) {
            createFileList();
        }
    }

    private void setAllFilesEnabled(boolean enabled) {
        PreferenceScreen root = getPreferenceScreen();
        for (int i = 0, n = root.getPreferenceCount(); i < n; i++) {
            root.getPreference(i).setEnabled(enabled);
        }
    }

    public boolean onPreferenceClick(Preference pref) {
        File file = new File(Environment.getExternalStorageDirectory(),
                pref.getTitle().toString());
        if (file.isDirectory()) {
            Log.w(TAG, "impossible to pick a directory! " + file);
        } else {
            setAllFilesEnabled(false);
            installFromFile(file);
        }
        return true;
    }

    private void createFileList() {
        if (isFinishing()) {
            Log.d(TAG, "finishing, exit createFileList()");
            return;
        } 
        if (!isSdCardPresent()) {
            Toast.makeText(this, R.string.sdcard_not_present,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            PreferenceScreen root = getPreferenceScreen();
            root.removeAll();

            List<File> allFiles = getAllCertFiles();
            if (allFiles.isEmpty()) {
                Toast.makeText(this, R.string.no_cert_file_found,
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            } else {
                int prefixEnd = Environment.getExternalStorageDirectory()
                        .getCanonicalPath().length() + 1;
                for (File file : allFiles) {
                    Preference pref = new Preference(this);
                    pref.setTitle(file.getCanonicalPath().substring(prefixEnd));
                    root.addPreference(pref);
                    pref.setOnPreferenceClickListener(this);
                }
            }
        } catch (IOException e) {
            // should not occur
            Log.w(TAG, "createFileList(): " + e);
            throw new RuntimeException(e);
        }
    }

    private void startSdCardMonitor() {
        if (mSdCardMonitor == null) {
            mSdCardMonitor = new SdCardMonitor();
        }
        mSdCardMonitor.startWatching();
    }

    private void stopSdCardMonitor() {
        if (mSdCardMonitor != null) {
            mSdCardMonitor.stopWatching();
        }
    }

    private class SdCardMonitor {
        FileObserver mRootMonitor;
        FileObserver mDownloadMonitor;

        SdCardMonitor() {
            File root = Environment.getExternalStorageDirectory();
            mRootMonitor = new FileObserver(root.getPath()) {
                @Override
                public void onEvent(int evt, String path) {
                    commonHandler(evt, path);
                }
            };

            File download = new File(root, DOWNLOAD_DIR);
            mDownloadMonitor = new FileObserver(download.getPath()) {
                @Override
                public void onEvent(int evt, String path) {
                    commonHandler(evt, path);
                }
            };
        }

        private void commonHandler(int evt, String path) {
            switch (evt) {
                case FileObserver.CREATE:
                case FileObserver.DELETE:
                    if (isFileAcceptable(path)) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                createFileList();
                            }
                        });
                    }
                    break;
            }
        };

        void startWatching() {
            mRootMonitor.startWatching();
            mDownloadMonitor.startWatching();
        }

        void stopWatching() {
            mRootMonitor.stopWatching();
            mDownloadMonitor.stopWatching();
        }
    }
}
