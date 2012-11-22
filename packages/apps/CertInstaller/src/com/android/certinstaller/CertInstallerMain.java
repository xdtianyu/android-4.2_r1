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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.Credentials;
import android.security.KeyChain;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import libcore.io.IoUtils;

/**
 * The main class for installing certificates to the system keystore. It reacts
 * to the public {@link Credentials#INSTALL_ACTION} intent.
 */
public class CertInstallerMain extends CertFile implements Runnable {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // don't want to call startActivityForResult() (invoked in
                // installFromFile()) here as it makes the new activity (thus
                // the whole display) get stuck for about 5 seconds
                runOnUiThread(CertInstallerMain.this);
            }
        }).start();
    }

    @Override
    public void run() {
        Intent intent = getIntent();
        String action = (intent == null) ? null : intent.getAction();

        if (Credentials.INSTALL_ACTION.equals(action)) {
            Bundle bundle = intent.getExtras();
            // If bundle is empty of any actual credentials, install from external storage.
            // Otherwise, pass extras to CertInstaller to install those credentials.
            // Either way, we use KeyChain.EXTRA_NAME as the default name if available.
            if (bundle == null
                    || bundle.isEmpty()
                    || (bundle.size() == 1 && bundle.containsKey(KeyChain.EXTRA_NAME))) {
                if (!isSdCardPresent()) {
                    Toast.makeText(this, R.string.sdcard_not_present,
                            Toast.LENGTH_SHORT).show();
                } else {
                    List<File> allFiles = getAllCertFiles();
                    if (allFiles.isEmpty()) {
                        Toast.makeText(this, R.string.no_cert_file_found,
                                Toast.LENGTH_SHORT).show();
                    } else if (allFiles.size() == 1) {
                        installFromFile(allFiles.get(0));
                        return;
                    } else {
                        Intent newIntent = new Intent(this, CertFileList.class);
                        newIntent.putExtras(intent);
                        startActivityForResult(newIntent, REQUEST_INSTALL_CODE);
                        return;
                    }
                }
            } else {
                Intent newIntent = new Intent(this, CertInstaller.class);
                newIntent.putExtras(intent);
                startActivityForResult(newIntent, REQUEST_INSTALL_CODE);
                return;
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            String type = intent.getType();
            if ((data != null) && (type != null)) {
                byte[] payload = null;
                InputStream is = null;
                try {
                    is = getContentResolver().openInputStream(data);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int read = 0;
                    while ((read = is.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    payload = out.toByteArray();
                } catch (IOException ignored) {
                    // Not much we can do - it will be logged below as an error.
                } finally {
                    IoUtils.closeQuietly(is);
                }
                if (payload == null) {
                    Log.e("CertInstaller", "Unable to read stream for for certificate");
                } else {
                    installByType(type, payload);
                }
            }
        }
        finish();
    }

    private void installByType(String type, byte[] value) {
        Intent intent = new Intent(this, CertInstaller.class);
        if ("application/x-pkcs12".equals(type)) {
            intent.putExtra(KeyChain.EXTRA_PKCS12, value);
        } else if ("application/x-x509-ca-cert".equals(type)
                || "application/x-x509-user-cert".equals(type)) {
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, value);
        } else {
            throw new AssertionError("Unknown type: " + type);
        }
        startActivityForResult(intent, REQUEST_INSTALL_CODE);
    }

    @Override
    protected void onInstallationDone(boolean success) {
        super.onInstallationDone(success);
        finish();
    }

    @Override
    protected void onError(int errorId) {
        finish();
    }
}
