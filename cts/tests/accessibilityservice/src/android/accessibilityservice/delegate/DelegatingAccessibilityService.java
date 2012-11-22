/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.accessibilityservice.delegate;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceDelegate;
import android.accessibilityservice.IAccessibilityServiceDelegateConnection;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * This class is an accessibility service mock to which the system is bound and
 * exposes a mock interface to the CTS accessibility tests.
 * </p>
 * Note: The end-to-end test is composed of two APKs, one with a mock accessibility
 * service, another with the instrumented activity and test cases. The
 * motivation for two APKs design is that CTS tests cannot access the secure
 * settings which is required for enabling accessibility and accessibility
 * services. Therefore, manual installation of this package is required. Once
 * the package has been installed accessibility must be enabled (Settings ->
 * Accessibility), the mock service must be enabled (Settings -> Accessibility
 * -> Mock Accessibility Service), and then the CTS tests in this package
 * <strong>CtsAccessibilityServiceTestCases.apk</strong> located in
 * <strong>cts/tests/tests/accessibility</strong> can be successfully run.
 * Further, the mock and tests run in separate processes since the
 * instrumentation restarts the process in which it is running and this breaks
 * the binding between the mock accessibility service and the system.
 */
public class DelegatingAccessibilityService extends AccessibilityService {

    /**
     * Tag used for logging.
     */
    private static final String LOG_TAG = "AccessibilityServiceDelegate";

    /**
     * Handle to the instance used by the accessibility service connection.
     */
    static DelegatingAccessibilityService sServiceDelegate;

    /**
     * Interface for delegating events and interrupt requests.
     */
    private IAccessibilityServiceDelegate mDelegateInterface;

    @Override
    protected void onServiceConnected() {
        // the service is ready to be used only
        // after the system has bound to it
        sServiceDelegate = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mDelegateInterface == null) {
            return;
        }

        try {
            mDelegateInterface.onAccessibilityEvent(event);
        } catch (RemoteException re) {
            Log.i(LOG_TAG, "Dead: " + mDelegateInterface.toString() + " cleaning up.");
            mDelegateInterface = null;
        }
    }

    @Override
    public void onInterrupt() {
        if (mDelegateInterface == null) {
            return;
        }

        try {
            mDelegateInterface.onInterrupt();
        } catch (RemoteException re) {
            Log.i(LOG_TAG, "Dead: " + mDelegateInterface.toString() + " cleaning up.");
            mDelegateInterface = null;
        }
    }

    /**
     * Sets the interface to which to delegate.
     *
     * @param delegateInterface The delegate interface.
     */
    private void setDelegateInterface(IAccessibilityServiceDelegate delegateInterface) {
        mDelegateInterface = delegateInterface;
    }

    /**
     * This is a service to which the end-to-end CTS test connects to pass a
     * delegate interface to which the {@link DelegatingAccessibilityService}
     * to delegate.
     */
    public static class DelegatingConnectionService extends Service {

        @Override
        public IBinder onBind(Intent intent) {
            if (sServiceDelegate == null) {
                return null;
            }
            return new AccessibilityServiceDelegateConnection();
        }

        /**
         * This class is the connection wrapper passed to the end-to-end CTS
         * test, so the latter can pass a delegating interface.
         */
        private class AccessibilityServiceDelegateConnection extends
                IAccessibilityServiceDelegateConnection.Stub {

            @Override
            public void setAccessibilityServiceDelegate(IBinder binder) {
                sServiceDelegate.setDelegateInterface(IAccessibilityServiceDelegate.Stub
                        .asInterface(binder));
            }

            @Override
            public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(
                    AccessibilityNodeInfo root, String text) {
                return root.findAccessibilityNodeInfosByText(text);
            }

            @Override
            public AccessibilityNodeInfo getChild(AccessibilityNodeInfo parent, int index) {
                return parent.getChild(index);
            }

            @Override
            public AccessibilityNodeInfo getParent(AccessibilityNodeInfo child) {
                return child.getParent();
            }

            @Override
            public AccessibilityNodeInfo findFocus(AccessibilityNodeInfo root, int focusType) {
                return root.findFocus(focusType);
            }

            @Override
            public AccessibilityNodeInfo focusSearch(AccessibilityNodeInfo current, int direction) {
                return current.focusSearch(direction);
            }

            @Override
            public AccessibilityNodeInfo getSource(AccessibilityEvent event) {
                return event.getSource();
            }

            @Override
            public boolean performAccessibilityAction(AccessibilityNodeInfo target, int action,
                    Bundle arguments) {
                return target.performAction(action, arguments);
            }

            @Override
            public void setFetchViewsNotExposedForAccessibility(boolean fetch) {
                AccessibilityServiceInfo info = sServiceDelegate.getServiceInfo();
                if (fetch) {
                    info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
                } else {
                    info.flags &= ~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
                }
                sServiceDelegate.setServiceInfo(info);
            }

            @Override
            public boolean performGlobalAction(int action) {
                return sServiceDelegate.performGlobalAction(action);
            }

            @Override
            public AccessibilityNodeInfo getRootInActiveWindow() {
                return sServiceDelegate.getRootInActiveWindow();
            }
        }
    }
}
