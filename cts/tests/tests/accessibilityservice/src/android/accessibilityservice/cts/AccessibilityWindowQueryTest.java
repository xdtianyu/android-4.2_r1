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

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_SELECTION;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SELECT;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.cts.accessibilityservice.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Test cases for testing the accessibility APIs for querying of the screen content.
 * These APIs allow exploring the screen and requesting an action to be performed
 * on a given view from an AccessiiblityService.
 * <p>
 * Note: The accessibility CTS tests are composed of two APKs, one with delegating
 * accessibility service and another with the instrumented activity and test cases.
 * The delegating service is installed and enabled during test execution. It serves
 * as a proxy to the system used by the tests. This indirection is needed since the
 * test runner stops the package before running the tests. Hence, if the accessibility
 * service is in the test package running the tests would break the binding between
 * the service and the system.  The delegating service is in
 * <strong>CtsDelegatingAccessibilityService.apk</strong> whose source is located at
 * <strong>cts/tests/accessibilityservice</strong>.
 * </p>
 */
public class AccessibilityWindowQueryTest
        extends AccessibilityActivityTestCase<AccessibilityWindowQueryActivity> {

    public AccessibilityWindowQueryTest() {
        super(AccessibilityWindowQueryActivity.class);
    }

    @MediumTest
    public void testFindByText() throws Exception {
        // find a view by text
        List<AccessibilityNodeInfo> buttons =
            getInteractionBridge().findAccessibilityNodeInfosByText("butto");
        assertEquals(9, buttons.size());
    }

    @MediumTest
    public void testFindByContentDescription() throws Exception {
        // find a view by text
        AccessibilityNodeInfo button = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.contentDescription));
        assertNotNull(button);
    }

    @MediumTest
    public void testTraverseWindow() throws Exception {
        try {
            getInteractionBridge().setRegardViewsNotImportantForAccessibility(true);

            // make list of expected nodes
            List<String> classNameAndTextList = new ArrayList<String>();
            classNameAndTextList.add("android.widget.FrameLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.FrameLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.LinearLayout");
            classNameAndTextList.add("android.widget.ButtonButton1");
            classNameAndTextList.add("android.widget.ButtonButton2");
            classNameAndTextList.add("android.widget.ButtonButton3");
            classNameAndTextList.add("android.widget.ButtonButton4");
            classNameAndTextList.add("android.widget.ButtonButton5");
            classNameAndTextList.add("android.widget.ButtonButton6");
            classNameAndTextList.add("android.widget.ButtonButton7");
            classNameAndTextList.add("android.widget.ButtonButton8");
            classNameAndTextList.add("android.widget.ButtonButton9");

            Queue<AccessibilityNodeInfo> fringe = new LinkedList<AccessibilityNodeInfo>();
            fringe.add(getInteractionBridge().getRootInActiveWindow());

            // do a BFS traversal and check nodes
            while (!fringe.isEmpty()) {
                AccessibilityNodeInfo current = fringe.poll();

                CharSequence text = current.getText();
                String receivedClassNameAndText = current.getClassName().toString()
                    + ((text != null) ? text.toString() : "");
                String expectedClassNameAndText = classNameAndTextList.remove(0);

                assertEquals("Did not get the expected node info",
                        expectedClassNameAndText, receivedClassNameAndText);

                final int childCount = current.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    AccessibilityNodeInfo child = getInteractionBridge().getChild(current, i);
                    fringe.add(child);
                }
            }
        } finally {
            getInteractionBridge().setRegardViewsNotImportantForAccessibility(false);
        }
    }

    @MediumTest
    public void testPerformActionFocus() throws Exception {
        // find a view and make sure it is not focused
        AccessibilityNodeInfo button = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertFalse(button.isFocused());

        // focus the view
        assertTrue(getInteractionBridge().performAction(button, ACTION_FOCUS));

        // find the view again and make sure it is focused
        button = getInteractionBridge().findAccessibilityNodeInfoByTextFromRoot(
                getString(R.string.button5));
        assertTrue(button.isFocused());
    }

    @MediumTest
    public void testPerformActionClearFocus() throws Exception {
        // find a view and make sure it is not focused
        AccessibilityNodeInfo button = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertFalse(button.isFocused());

        // focus the view
        assertTrue(getInteractionBridge().performAction(button, ACTION_FOCUS));

        // find the view again and make sure it is focused
        button = getInteractionBridge().findAccessibilityNodeInfoByTextFromRoot(
                getString(R.string.button5));
        assertTrue(button.isFocused());

        // unfocus the view
        assertTrue(getInteractionBridge().performAction(button, ACTION_CLEAR_FOCUS));

        // find the view again and make sure it is not focused
        button = getInteractionBridge().findAccessibilityNodeInfoByTextFromRoot(getString(
                R.string.button5));
        assertFalse(button.isFocused());
    }

    @MediumTest
    public void testPerformActionSelect() throws Exception {
        // find a view and make sure it is not selected
        AccessibilityNodeInfo button = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertFalse(button.isSelected());

        // select the view
        assertTrue(getInteractionBridge().performAction(button, ACTION_SELECT));

        // find the view again and make sure it is selected
        button = getInteractionBridge().findAccessibilityNodeInfoByTextFromRoot(
                getString(R.string.button5));
        assertTrue(button.isSelected());
    }

    @MediumTest
    public void testPerformActionClearSelection() throws Exception {
        // find a view and make sure it is not selected
        AccessibilityNodeInfo button = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertFalse(button.isSelected());

        // select the view
        assertTrue(getInteractionBridge().performAction(button, ACTION_SELECT));

        // find the view again and make sure it is selected
        button = getInteractionBridge().findAccessibilityNodeInfoByTextFromRoot(
                getString(R.string.button5));

        assertTrue(button.isSelected());

        // unselect the view
        assertTrue(getInteractionBridge().performAction(button, ACTION_CLEAR_SELECTION));

        // find the view again and make sure it is not selected
        button = getInteractionBridge().findAccessibilityNodeInfoByTextFromRoot(
                getString(R.string.button5));
        assertFalse(button.isSelected());
    }

    @MediumTest
    public void testPerformActionClick() throws Exception {
        // find a view and make sure it is not selected
        final AccessibilityNodeInfo button = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertFalse(button.isSelected());

        // Make an action and wait for an event.
        AccessibilityEvent expected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(button, ACTION_CLICK);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
        },
        TIMEOUT_ASYNC_PROCESSING);

        // Make sure the expected event was received.
        assertNotNull(expected);
    }

    @MediumTest
    public void testPerformActionLongClick() throws Exception {
        // find a view and make sure it is not selected
        final AccessibilityNodeInfo button = getInteractionBridge()
                .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
        assertFalse(button.isSelected());

        // Make an action and wait for an event.
        AccessibilityEvent expected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(button, ACTION_LONG_CLICK);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return (event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
            }
        },
        TIMEOUT_ASYNC_PROCESSING);

        // Make sure the expected event was received.
        assertNotNull(expected);
    }

    @MediumTest
    public void testGetEventSource() throws Exception {
        // find a view and make sure it is not focused
        final AccessibilityNodeInfo button =
            getInteractionBridge().findAccessibilityNodeInfoByTextFromRoot(
                    getString(R.string.button5));
        assertFalse(button.isSelected());

        // focus and wait for the event
        AccessibilityEvent awaitedEvent =
            getInteractionBridge().executeCommandAndWaitForAccessibilityEvent(
                new Runnable() {
            @Override
            public void run() {
                assertTrue(getInteractionBridge().performAction(button, ACTION_FOCUS));
            }
        },
                new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        },
        TIMEOUT_ASYNC_PROCESSING);

        assertNotNull(awaitedEvent);

        // check that last event source
        AccessibilityNodeInfo source = getInteractionBridge().getSource(awaitedEvent);
        assertNotNull(source);

        // bounds
        Rect buttonBounds = new Rect();
        button.getBoundsInParent(buttonBounds);
        Rect sourceBounds = new Rect();
        source.getBoundsInParent(sourceBounds);

        assertEquals(buttonBounds.left, sourceBounds.left);
        assertEquals(buttonBounds.right, sourceBounds.right);
        assertEquals(buttonBounds.top, sourceBounds.top);
        assertEquals(buttonBounds.bottom, sourceBounds.bottom);

        // char sequence attributes
        assertEquals(button.getPackageName(), source.getPackageName());
        assertEquals(button.getClassName(), source.getClassName());
        assertEquals(button.getText(), source.getText());
        assertSame(button.getContentDescription(), source.getContentDescription());

        // boolean attributes
        assertSame(button.isFocusable(), source.isFocusable());
        assertSame(button.isClickable(), source.isClickable());
        assertSame(button.isEnabled(), source.isEnabled());
        assertNotSame(button.isFocused(), source.isFocused());
        assertSame(button.isLongClickable(), source.isLongClickable());
        assertSame(button.isPassword(), source.isPassword());
        assertSame(button.isSelected(), source.isSelected());
        assertSame(button.isCheckable(), source.isCheckable());
        assertSame(button.isChecked(), source.isChecked());
    }

    @MediumTest
    public void testPerformGlobalActionBack() throws Exception {
        assertTrue(getInteractionBridge().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testPerformGlobalActionHome() throws Exception {
        assertTrue(getInteractionBridge().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_HOME));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testPerformGlobalActionRecents() throws Exception {
        // Check whether the action succeeded.
        assertTrue(getInteractionBridge().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_RECENTS));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);

        // Clean up.
        getInteractionBridge().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK);

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testPerformGlobalActionNotifications() throws Exception {
        // Perform the action under test
        assertTrue(getInteractionBridge().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);

        // Clean up.
        assertTrue(getInteractionBridge().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testPerformGlobalActionQuickSettings() throws Exception {
        // Check whether the action succeeded.
        assertTrue(getInteractionBridge().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);

        // Clean up.
        getInteractionBridge().performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK);

        // Sleep a bit so the UI is settles.
        SystemClock.sleep(3000);
    }

    @MediumTest
    public void testObjectContract() throws Exception {
        try {
            getInteractionBridge().setRegardViewsNotImportantForAccessibility(true);
            // find a view and make sure it is not focused
            AccessibilityNodeInfo button = getInteractionBridge()
                    .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.button5));
            AccessibilityNodeInfo parent = getInteractionBridge().getParent(button);
            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = getInteractionBridge().getChild(parent, i);
                assertNotNull(child);
                if (child.equals(button)) {
                    assertEquals("Equal objects must have same hasCode.", button.hashCode(),
                            child.hashCode());
                    return;
                }
            }
            fail("Parent's children do not have the info whose parent is the parent.");
        } finally {
            getInteractionBridge().setRegardViewsNotImportantForAccessibility(false);
        }
    }

    @Override
    protected void scrubClass(Class<?> testCaseClass) {
        /* intentionally do not scrub */
    }
}
