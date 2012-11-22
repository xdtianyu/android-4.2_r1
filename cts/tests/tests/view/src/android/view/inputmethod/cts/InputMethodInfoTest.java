/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.view.inputmethod.cts;


import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.Printer;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodInfo;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class InputMethodInfoTest extends AndroidTestCase {
    private InputMethodInfo mInputMethodInfo;
    private String mPackageName;
    private String mClassName;
    private CharSequence mLabel;
    private String mSettingsActivity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageName = mContext.getPackageName();
        mClassName = InputMethodInfoStub.class.getName();
        mLabel = "test";
        mSettingsActivity = "android.view.inputmethod.cts.InputMethodInfoStub";
        mInputMethodInfo = new InputMethodInfo(mPackageName, mClassName, mLabel, mSettingsActivity);
    }

    public void testInputMethodInfoProperties() throws XmlPullParserException, IOException {
        assertEquals(0, mInputMethodInfo.describeContents());
        assertNotNull(mInputMethodInfo.toString());

        assertInfo(mInputMethodInfo);
        assertEquals(0, mInputMethodInfo.getIsDefaultResourceId());

        Intent intent = new Intent(InputMethod.SERVICE_INTERFACE);
        intent.setClass(mContext, InputMethodInfoStub.class);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentServices(intent, PackageManager.GET_META_DATA);
        for (int i = 0; i < ris.size(); i++) {
            ResolveInfo resolveInfo = ris.get(i);
            mInputMethodInfo = new InputMethodInfo(mContext, resolveInfo);
            assertService(resolveInfo.serviceInfo, mInputMethodInfo.getServiceInfo());
            assertInfo(mInputMethodInfo);
        }
    }

    private void assertService(ServiceInfo expected, ServiceInfo actual) {
        assertEquals(expected.getIconResource(), actual.getIconResource());
        assertEquals(expected.labelRes, actual.labelRes);
        assertEquals(expected.nonLocalizedLabel, actual.nonLocalizedLabel);
        assertEquals(expected.icon, actual.icon);
        assertEquals(expected.permission, actual.permission);
    }

    private void assertInfo(InputMethodInfo info) {
        assertEquals(mPackageName, info.getPackageName());
        assertEquals(mSettingsActivity, info.getSettingsActivity());
        ComponentName component = info.getComponent();
        assertEquals(mClassName, component.getClassName());
        String expectedId = component.flattenToShortString();
        assertEquals(expectedId, info.getId());
        assertEquals(mClassName, info.getServiceName());
    }

    public void testDump() {
        MockPrinter printer = new MockPrinter();
        String prefix = "test";
        mInputMethodInfo.dump(printer, prefix);
    }

    public void testLoadIcon() {
        PackageManager pm = mContext.getPackageManager();
        assertNotNull(mInputMethodInfo.loadIcon(pm));
    }

    public void testEquals() {
        InputMethodInfo inputMethodInfo = new InputMethodInfo(mPackageName, mClassName, mLabel,
                mSettingsActivity);
        assertTrue(inputMethodInfo.equals(mInputMethodInfo));
    }

    public void testLoadLabel() {
        CharSequence expected = "test";
        PackageManager pm = mContext.getPackageManager();
        assertEquals(expected.toString(), mInputMethodInfo.loadLabel(pm).toString());
    }

    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();
        mInputMethodInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        InputMethodInfo inputMethodInfo = InputMethodInfo.CREATOR.createFromParcel(p);

        assertEquals(mInputMethodInfo.getPackageName(), inputMethodInfo.getPackageName());
        assertEquals(mInputMethodInfo.getServiceName(), inputMethodInfo.getServiceName());
        assertEquals(mInputMethodInfo.getSettingsActivity(), inputMethodInfo.getSettingsActivity());
        assertEquals(mInputMethodInfo.getId(), inputMethodInfo.getId());
        assertEquals(mInputMethodInfo.getIsDefaultResourceId(), inputMethodInfo
                .getIsDefaultResourceId());
        assertService(mInputMethodInfo.getServiceInfo(), inputMethodInfo.getServiceInfo());
    }

    class MockPrinter implements Printer {
        public void println(String x) {
        }
    }
}
