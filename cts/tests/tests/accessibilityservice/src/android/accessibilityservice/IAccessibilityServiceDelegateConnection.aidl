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
package android.accessibilityservice;

import android.os.Bundle;
import android.os.IBinder;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Interface for registering an accessibility service delegate
 * and asking it to perform some querying of the window for us.
 */
interface IAccessibilityServiceDelegateConnection {

    void setAccessibilityServiceDelegate(in IBinder binder);

    List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(in AccessibilityNodeInfo root,
        String text);

    AccessibilityNodeInfo getParent(in AccessibilityNodeInfo child);

    AccessibilityNodeInfo getChild(in AccessibilityNodeInfo parent, int index);

    AccessibilityNodeInfo findFocus(in AccessibilityNodeInfo root, int focusType);

    AccessibilityNodeInfo focusSearch(in AccessibilityNodeInfo current, int direction);

    boolean performAccessibilityAction(in AccessibilityNodeInfo target, int action,
           in Bundle arguments);

    AccessibilityNodeInfo getSource(in AccessibilityEvent event);

    void setFetchViewsNotExposedForAccessibility(boolean fetch);

    boolean performGlobalAction(int action);

    AccessibilityNodeInfo getRootInActiveWindow();
}
