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

package com.android.keychain;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyStore;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.harmony.xnet.provider.jsse.TrustedCertificateStore;

public class KeyChainService extends IntentService {

    private static final String TAG = "KeyChain";

    private static final String DATABASE_NAME = "grants.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_GRANTS = "grants";
    private static final String GRANTS_ALIAS = "alias";
    private static final String GRANTS_GRANTEE_UID = "uid";

    /** created in onCreate(), closed in onDestroy() */
    public DatabaseHelper mDatabaseHelper;

    private static final String SELECTION_COUNT_OF_MATCHING_GRANTS =
            "SELECT COUNT(*) FROM " + TABLE_GRANTS
                    + " WHERE " + GRANTS_GRANTEE_UID + "=? AND " + GRANTS_ALIAS + "=?";

    private static final String SELECT_GRANTS_BY_UID_AND_ALIAS =
            GRANTS_GRANTEE_UID + "=? AND " + GRANTS_ALIAS + "=?";

    private static final String SELECTION_GRANTS_BY_UID = GRANTS_GRANTEE_UID + "=?";

    public KeyChainService() {
        super(KeyChainService.class.getSimpleName());
    }

    @Override public void onCreate() {
        super.onCreate();
        mDatabaseHelper = new DatabaseHelper(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabaseHelper.close();
        mDatabaseHelper = null;
    }

    private final IKeyChainService.Stub mIKeyChainService = new IKeyChainService.Stub() {
        private final KeyStore mKeyStore = KeyStore.getInstance();
        private final TrustedCertificateStore mTrustedCertificateStore
                = new TrustedCertificateStore();

        @Override
        public String requestPrivateKey(String alias) {
            checkArgs(alias);

            final String keystoreAlias = Credentials.USER_PRIVATE_KEY + alias;
            final int uid = Binder.getCallingUid();
            if (!mKeyStore.grant(keystoreAlias, uid)) {
                return null;
            }

            final StringBuilder sb = new StringBuilder();
            sb.append(Process.SYSTEM_UID);
            sb.append('_');
            sb.append(keystoreAlias);

            return sb.toString();
        }

        @Override public byte[] getCertificate(String alias) {
            checkArgs(alias);
            return mKeyStore.get(Credentials.USER_CERTIFICATE + alias);
        }

        private void checkArgs(String alias) {
            if (alias == null) {
                throw new NullPointerException("alias == null");
            }
            if (!isKeyStoreUnlocked()) {
                throw new IllegalStateException("keystore is "
                        + mKeyStore.state().toString());
            }

            final int callingUid = getCallingUid();
            if (!hasGrantInternal(mDatabaseHelper.getReadableDatabase(), callingUid, alias)) {
                throw new IllegalStateException("uid " + callingUid
                        + " doesn't have permission to access the requested alias");
            }
        }

        private boolean isKeyStoreUnlocked() {
            return (mKeyStore.state() == KeyStore.State.UNLOCKED);
        }

        @Override public void installCaCertificate(byte[] caCertificate) {
            checkCertInstallerOrSystemCaller();
            try {
                synchronized (mTrustedCertificateStore) {
                    mTrustedCertificateStore.installCertificate(parseCertificate(caCertificate));
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (CertificateException e) {
                throw new IllegalStateException(e);
            }
            broadcastStorageChange();
        }

        private X509Certificate parseCertificate(byte[] bytes) throws CertificateException {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
        }

        @Override public boolean reset() {
            // only Settings should be able to reset
            checkSystemCaller();
            removeAllGrants(mDatabaseHelper.getWritableDatabase());
            boolean ok = true;
            synchronized (mTrustedCertificateStore) {
                // delete user-installed CA certs
                for (String alias : mTrustedCertificateStore.aliases()) {
                    if (TrustedCertificateStore.isUser(alias)) {
                        if (!deleteCertificateEntry(alias)) {
                            ok = false;
                        }
                    }
                }
            }
            broadcastStorageChange();
            return ok;
        }

        @Override public boolean deleteCaCertificate(String alias) {
            // only Settings should be able to delete
            checkSystemCaller();
            boolean ok = true;
            synchronized (mTrustedCertificateStore) {
                ok = deleteCertificateEntry(alias);
            }
            broadcastStorageChange();
            return ok;
        }

        private boolean deleteCertificateEntry(String alias) {
            try {
                mTrustedCertificateStore.deleteCertificateEntry(alias);
                return true;
            } catch (IOException e) {
                Log.w(TAG, "Problem removing CA certificate " + alias, e);
                return false;
            } catch (CertificateException e) {
                Log.w(TAG, "Problem removing CA certificate " + alias, e);
                return false;
            }
        }

        private void checkCertInstallerOrSystemCaller() {
            String actual = checkCaller("com.android.certinstaller");
            if (actual == null) {
                return;
            }
            checkSystemCaller();
        }
        private void checkSystemCaller() {
            String actual = checkCaller("android.uid.system:1000");
            if (actual != null) {
                throw new IllegalStateException(actual);
            }
        }
        /**
         * Returns null if actually caller is expected, otherwise return bad package to report
         */
        private String checkCaller(String expectedPackage) {
            String actualPackage = getPackageManager().getNameForUid(getCallingUid());
            return (!expectedPackage.equals(actualPackage)) ? actualPackage : null;
        }

        @Override public boolean hasGrant(int uid, String alias) {
            checkSystemCaller();
            return hasGrantInternal(mDatabaseHelper.getReadableDatabase(), uid, alias);
        }

        @Override public void setGrant(int uid, String alias, boolean value) {
            checkSystemCaller();
            setGrantInternal(mDatabaseHelper.getWritableDatabase(), uid, alias, value);
            broadcastStorageChange();
        }
    };

    private boolean hasGrantInternal(final SQLiteDatabase db, final int uid, final String alias) {
        final long numMatches = DatabaseUtils.longForQuery(db, SELECTION_COUNT_OF_MATCHING_GRANTS,
                new String[]{String.valueOf(uid), alias});
        return numMatches > 0;
    }

    private void setGrantInternal(final SQLiteDatabase db,
            final int uid, final String alias, final boolean value) {
        if (value) {
            if (!hasGrantInternal(db, uid, alias)) {
                final ContentValues values = new ContentValues();
                values.put(GRANTS_ALIAS, alias);
                values.put(GRANTS_GRANTEE_UID, uid);
                db.insert(TABLE_GRANTS, GRANTS_ALIAS, values);
            }
        } else {
            db.delete(TABLE_GRANTS, SELECT_GRANTS_BY_UID_AND_ALIAS,
                    new String[]{String.valueOf(uid), alias});
        }
    }

    private void removeAllGrants(final SQLiteDatabase db) {
        db.delete(TABLE_GRANTS, null /* whereClause */, null /* whereArgs */);
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null /* CursorFactory */, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_GRANTS + " (  "
                    + GRANTS_ALIAS + " STRING NOT NULL,  "
                    + GRANTS_GRANTEE_UID + " INTEGER NOT NULL,  "
                    + "UNIQUE (" + GRANTS_ALIAS + "," + GRANTS_GRANTEE_UID + "))");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, int oldVersion, final int newVersion) {
            Log.e(TAG, "upgrade from version " + oldVersion + " to version " + newVersion);

            if (oldVersion == 1) {
                // the first upgrade step goes here
                oldVersion++;
            }
        }
    }

    @Override public IBinder onBind(Intent intent) {
        if (IKeyChainService.class.getName().equals(intent.getAction())) {
            return mIKeyChainService;
        }
        return null;
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
            purgeOldGrants();
        }
    }

    private void purgeOldGrants() {
        final PackageManager packageManager = getPackageManager();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.query(TABLE_GRANTS,
                    new String[]{GRANTS_GRANTEE_UID}, null, null, GRANTS_GRANTEE_UID, null, null);
            while (cursor.moveToNext()) {
                final int uid = cursor.getInt(0);
                final boolean packageExists = packageManager.getPackagesForUid(uid) != null;
                if (packageExists) {
                    continue;
                }
                Log.d(TAG, "deleting grants for UID " + uid
                        + " because its package is no longer installed");
                db.delete(TABLE_GRANTS, SELECTION_GRANTS_BY_UID,
                        new String[]{Integer.toString(uid)});
            }
            db.setTransactionSuccessful();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.endTransaction();
        }
    }

    private void broadcastStorageChange() {
        Intent intent = new Intent(KeyChain.ACTION_STORAGE_CHANGED);
        sendBroadcast(intent);
    }

}
