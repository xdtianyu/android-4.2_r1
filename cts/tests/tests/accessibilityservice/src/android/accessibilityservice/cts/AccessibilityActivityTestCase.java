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

package android.accessibilityservice.cts;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceDelegate;
import android.accessibilityservice.IAccessibilityServiceDelegateConnection;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Base text case for testing accessibility APIs by instrumenting an Activity.
 */
public abstract class AccessibilityActivityTestCase<T extends Activity>
        extends ActivityInstrumentationTestCase2<T> {

    public interface AccessibilityEventFilter {
        public boolean accept(AccessibilityEvent event);
    }

    private static final boolean DEBUG = false;

    private static final String LOG_TAG = AccessibilityActivityTestCase.class.getSimpleName();

    /**
     * Timeout required for pending Binder calls or event processing to
     * complete.
     */
    public static final long TIMEOUT_ASYNC_PROCESSING = 5000;

    /**
     * The timeout after the last accessibility event to consider the device idle.
     */
    public static final long TIMEOUT_ACCESSIBILITY_STATE_IDLE = 100;

    /**
     * Instance for detecting the next accessibility event.
     */
    private static final NextAccessibilityEventWatcher sNextEventWatcher =
            new NextAccessibilityEventWatcher();

    private static AccessibilityInteractionBridge sInteractionBridge;

    /**
     * @param activityClass
     */
    public AccessibilityActivityTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        waitForAccessibilityStateIdle();
        startActivityAndWaitForFirstEvent();
    }

    /**
     * Gets the bridge for interacting with the view hierarchy via
     * the accessibility APIs.
     *
     * @return The bridge.
     */
    public AccessibilityInteractionBridge getInteractionBridge() {
        if (sInteractionBridge == null) {
            sInteractionBridge = new AccessibilityInteractionBridge(
                    getInstrumentation().getContext());
        }
        return sInteractionBridge;
    }

    /**
     * @return The string for a given <code>resId</code>.
     */
    public String getString(int resId) {
        return getInstrumentation().getContext().getString(resId);
    }

    /**
     * Starts the activity under tests and waits for the first accessibility
     * event from that activity.
     */
    private void startActivityAndWaitForFirstEvent() throws Exception {
        AccessibilityEvent awaitedEvent =
            getInteractionBridge().executeCommandAndWaitForAccessibilityEvent(
                new Runnable() {
            @Override
            public void run() {
                getActivity();
                getInstrumentation().waitForIdleSync();
            }
        },
                new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                final int eventType = event.getEventType();
                CharSequence packageName = event.getPackageName();
                Context targetContext = getInstrumentation().getTargetContext();
                return (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                        && targetContext.getPackageName().equals(packageName));
            }
        },
        TIMEOUT_ASYNC_PROCESSING);
        assertNotNull(awaitedEvent);
    }

    /**
     * Waits for idle accessibility state.
     */
    private void waitForAccessibilityStateIdle() throws Exception {
        AccessibilityEvent awaitedEvent = null;
        try {
            do {
                awaitedEvent = getInteractionBridge().executeCommandAndWaitForAccessibilityEvent(
                        sNextEventWatcher, sNextEventWatcher, TIMEOUT_ACCESSIBILITY_STATE_IDLE);
            } while (awaitedEvent != null);
        } catch (TimeoutException te) {
            /* success - no event within the timeout - do nothing */
        }
    }

    /**
     * Dummy implementation that matches every event and does nothing.
     */
    private static class NextAccessibilityEventWatcher implements Runnable,
            AccessibilityEventFilter {
        @Override
        public boolean accept(AccessibilityEvent event) {
            if (DEBUG) {
                Log.i(LOG_TAG, "Watcher event: " + event);
            }
            return true;
        }
        @Override
        public void run() {
            /* do nothing */
        }
    }

    /**
     * This class serves as a bridge for querying the screen content.
     * The bride is connected of a delegating accessibility service.
     */
    static class AccessibilityInteractionBridge implements ServiceConnection {

        /**
         * The package of the accessibility service mock interface.
         */
        private static final String DELEGATING_SERVICE_PACKAGE =
            "android.accessibilityservice.delegate";

        /**
         * The package of the delegating accessibility service interface.
         */
        private static final String DELEGATING_SERVICE_CLASS_NAME =
            "android.accessibilityservice.delegate.DelegatingAccessibilityService";

        /**
         * The package of the delegating accessibility service connection interface.
         */
        private static final String DELEGATING_SERVICE_CONNECTION_CLASS_NAME =
            "android.accessibilityservice.delegate."
                + "DelegatingAccessibilityService$DelegatingConnectionService";

        /**
         * Lock for synchronization.
         */
        private final Object mLock = new Object();

        /**
         * Whether this delegate is initialized.
         */
        private boolean mInitialized;

        /**
         * Query connection to the delegating accessibility service.
         */
        private IAccessibilityServiceDelegateConnection mQueryConnection;

        /**
         * Flag whether we are waiting for a specific event.
         */
        private boolean mWaitingForEventDelivery;

        /**
         * Queue with received events.
         */
        private final ArrayList<AccessibilityEvent> mEventQueue =
            new ArrayList<AccessibilityEvent>(10);

        public AccessibilityInteractionBridge(Context context) {
            bindToDelegatingAccessibilityService(context);
        }

        public void onAccessibilityEvent(AccessibilityEvent event) {
            synchronized (mLock) {
                mLock.notifyAll();
                if (mWaitingForEventDelivery) {
                    mEventQueue.add(AccessibilityEvent.obtain(event));
                }
            }
        }

        /**
         * Ensures the required setup for the test performed and that it is bound to the
         * DelegatingAccessibilityService which runs in another process. The setup is
         * enabling accessibility and installing and enabling the delegating accessibility
         * service this test binds to.
         * </p>
         * Note: Please look at the class description for information why such an
         *       approach is taken.
         */
        public void bindToDelegatingAccessibilityService(Context context) {
            // check if accessibility is enabled
            AccessibilityManager accessibilityManager = (AccessibilityManager) context
                    .getSystemService(Service.ACCESSIBILITY_SERVICE);

            if (!accessibilityManager.isEnabled()) {
                throw new IllegalStateException("Delegating service not enabled.");
            }

            // check if the delegating service is running
            List<AccessibilityServiceInfo> enabledServices =
                accessibilityManager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            boolean delegatingServiceRunning = false;
            for (AccessibilityServiceInfo enabledService : enabledServices) {
                ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
                if (DELEGATING_SERVICE_PACKAGE.equals(serviceInfo.packageName)
                        && DELEGATING_SERVICE_CLASS_NAME.equals(serviceInfo.name)) {
                    delegatingServiceRunning = true;
                    break;
                }
            }

            if (!delegatingServiceRunning) {
                // delegating service not running, so check if it is installed at all
                try {
                    PackageManager packageManager = context.getPackageManager();
                    packageManager.getServiceInfo(new ComponentName(DELEGATING_SERVICE_PACKAGE,
                            DELEGATING_SERVICE_CLASS_NAME), 0);
                } catch (NameNotFoundException nnfe) {
                    throw new IllegalStateException("CtsDelegatingAccessibilityService.apk" +
                            " not installed.");
                }

                throw new IllegalStateException("Delegating Accessibility Service not running.");
            }

            Intent intent = new Intent().setClassName(DELEGATING_SERVICE_PACKAGE,
                    DELEGATING_SERVICE_CONNECTION_CLASS_NAME);
            context.bindService(intent, this, Context.BIND_AUTO_CREATE);

            final long beginTime = SystemClock.uptimeMillis();
            synchronized (mLock) {
                while (true) {
                    if (mInitialized) {
                        return;
                    }
                    final long elapsedTime = (SystemClock.uptimeMillis() - beginTime);
                    final long remainingTime = TIMEOUT_ASYNC_PROCESSING - elapsedTime;
                    if (remainingTime <= 0) {
                        if (!mInitialized) {
                            throw new IllegalStateException("Cound not connect to the delegating"
                                    + " accessibility service");
                        }
                        return;
                    }
                    try {
                        mLock.wait(remainingTime);
                    } catch (InterruptedException ie) {
                        /* ignore */
                    }
                }
            }
        }

        /**
         * {@inheritDoc ServiceConnection#onServiceConnected(ComponentName,IBinder)}
         */
        public void onServiceConnected(ComponentName name, IBinder service) {
            mQueryConnection = IAccessibilityServiceDelegateConnection.Stub.asInterface(service);
            try {
                mQueryConnection.setAccessibilityServiceDelegate(
                        new IAccessibilityServiceDelegate.Stub() {
                    @Override
                    public void onAccessibilityEvent(AccessibilityEvent event) {
                        AccessibilityInteractionBridge.this.onAccessibilityEvent(event);
                    }
                    @Override
                    public void onInterrupt() {
                        /* do nothing */
                    }
                });
                synchronized (mLock) {
                    mInitialized = true;
                    mLock.notifyAll();
                }
            } catch (RemoteException re) {
                fail("Could not set delegate to the delegating service.");
            }
        }

        /**
         * {@inheritDoc ServiceConnection#onServiceDisconnected(ComponentName)}
         */
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mLock) {
                mInitialized = false;
            }
        }

        /**
         * Gets the query connection to the delegating accessibility service.
         *
         * @return The connection.
         */
        public IAccessibilityServiceDelegateConnection getQueryConnection() {
            return mQueryConnection;
        }

        /**
         * Finds the first accessibility info that contains text. The search starts
         * from the given <code>root</code>
         *
         * @param text The searched text.
         * @return The node with this text or null.
         */
        public AccessibilityNodeInfo findAccessibilityNodeInfoByTextFromRoot(String text) {
            List<AccessibilityNodeInfo> nodes = findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) {
                return nodes.get(0);
            }
            return null;
        }

        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String text) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                // Sending a node info across processes recycles
                // it so use a clone to avoid losing state
                AccessibilityNodeInfo rootClone = AccessibilityNodeInfo.obtain(root);
                try {
                     return getQueryConnection().findAccessibilityNodeInfosByText(rootClone, text);
                } catch (RemoteException re) {
                    /* ignore */
                }
            }
            return Collections.emptyList();
        }

        public AccessibilityNodeInfo getParent(AccessibilityNodeInfo child) {
            try {
                // Sending a node info across processes recycles
                // it so use a clone to avoid losing state
                AccessibilityNodeInfo childClone = AccessibilityNodeInfo.obtain(child);
                return getQueryConnection().getParent(childClone);
            } catch (RemoteException re) {
                /* ignore */
            }
            return null;
        }

        public AccessibilityNodeInfo getChild(AccessibilityNodeInfo parent, int index) {
            try {
                // Sending a node info across processes recycles
                // it so use a clone to avoid losing state
                AccessibilityNodeInfo parentClone = AccessibilityNodeInfo.obtain(parent);
                return getQueryConnection().getChild(parentClone, index);
            } catch (RemoteException re) {
                /* ignore */
            }
            return null;
        }

        public boolean performAction(AccessibilityNodeInfo target, int action) {
            return performAction(target, action, null);
        }

        public boolean performAction(AccessibilityNodeInfo target, int action, Bundle arguments) {
            try {
                // Sending a node info across processes recycles
                // it so use a clone to avoid losing state
                AccessibilityNodeInfo targetClone = AccessibilityNodeInfo.obtain(target);
                return getQueryConnection().performAccessibilityAction(targetClone, action,
                        arguments);
            } catch (RemoteException re) {
                /* ignore */
            }
            return false;
        }

        public boolean performGlobalAction(int action) {
            try {
                return getQueryConnection().performGlobalAction(action);
            } catch (RemoteException re) {
                /* ignore */
            }
            return false;
        }

        public AccessibilityNodeInfo getSource(AccessibilityEvent event) {
            try {
                return getQueryConnection().getSource(event);
            } catch (RemoteException re) {
                /* ignore */
            }
            return null;
        }

        public AccessibilityNodeInfo findAccessibilityFocus(AccessibilityNodeInfo root) {
            try {
                return getQueryConnection().findFocus(root,
                        AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            } catch (RemoteException re) {
                /* ignore */
            }
            return null;
        }

        public AccessibilityNodeInfo findInputFocus(AccessibilityNodeInfo root) {
            try {
                return getQueryConnection().findFocus(root, AccessibilityNodeInfo.FOCUS_INPUT);
            } catch (RemoteException re) {
                /* ignore */
            }
            return null;
        }

        public AccessibilityNodeInfo accessibilityFocusSearch(AccessibilityNodeInfo current,
                int direction) {
            try {
                // Sending a node info across processes recycles
                // it so use a clone to avoid losing state
                AccessibilityNodeInfo currentClone = AccessibilityNodeInfo.obtain(current);
                return getQueryConnection().focusSearch(currentClone, direction);
            } catch (RemoteException re) {
                /* ignore */
            }
            return null;
        }

        public AccessibilityNodeInfo inputFocusSearch(AccessibilityNodeInfo current,
                int direction) {
            try {
                // Sending a node info across processes recycles
                // it so use a clone to avoid losing state
                AccessibilityNodeInfo currentClone = AccessibilityNodeInfo.obtain(current);
                return getQueryConnection().focusSearch(currentClone, direction);
            } catch (RemoteException re) {
                /* ignore */
            }
            return null;
        }

        public void setRegardViewsNotImportantForAccessibility(boolean fetch) {
            try {
                getQueryConnection().setFetchViewsNotExposedForAccessibility(fetch);
            } catch (RemoteException re) {
                /* ignore */
            }
        }

        public AccessibilityNodeInfo getRootInActiveWindow() {
            try {
                return getQueryConnection().getRootInActiveWindow();
            } catch (RemoteException re) {
                /* ignore */
            }
            return null;
        }

        /**
         * Executes a command and waits for a specific accessibility event type up
         * to a given timeout.
         *
         * @param command The command to execute before starting to wait for the event.
         * @param filter Filter that recognizes the expected event.
         * @param timeoutMillis The max wait time in milliseconds.
         */
        public AccessibilityEvent executeCommandAndWaitForAccessibilityEvent(Runnable command,
                AccessibilityEventFilter filter, long timeoutMillis)
                throws TimeoutException, Exception {
            synchronized (mLock) {
                mEventQueue.clear();
                // Prepare to wait for an event.
                mWaitingForEventDelivery = true;
                // Execute the command.
                command.run();
                try {
                    // Wait for the event.
                    final long startTimeMillis = SystemClock.uptimeMillis();
                    while (true) {
                        // Drain the event queue
                        while (!mEventQueue.isEmpty()) {
                            AccessibilityEvent event = mEventQueue.remove(0);
                            if (filter.accept(event)) {
                                return event;
                            } else {
                                event.recycle();
                            }
                        }
                        // Check if timed out and if not wait.
                        final long elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
                        final long remainingTimeMillis = timeoutMillis - elapsedTimeMillis;
                        if (remainingTimeMillis <= 0) {
                            throw new TimeoutException("Expected event not received within: "
                                    + timeoutMillis + " ms.");
                        }
                        mLock.notifyAll();
                        try {
                            mLock.wait(remainingTimeMillis);
                        } catch (InterruptedException ie) {
                            /* ignore */
                        }
                    }
                } finally {
                    mWaitingForEventDelivery = false;
                    mEventQueue.clear();
                    mLock.notifyAll();
                }
            }
        }
    }
}
