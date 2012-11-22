/**
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.accessibilityservice.cts;

import android.test.suitebuilder.annotation.MediumTest;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.cts.accessibilityservice.R;

/**
 * Test cases for testing the accessibility focus APIs exposed to accessibility
 * services. This test checks how the view hierarchy is reported to accessibility
 * services.
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
public class AccessibilityViewTreeReportingTest
        extends AccessibilityActivityTestCase<AccessibilityViewTreeReportingActivity>{

    public AccessibilityViewTreeReportingTest() {
        super(AccessibilityViewTreeReportingActivity.class);
    }

    @MediumTest
    public void testDescendantsOfNotImportantViewReportedInOrder1() throws Exception {
        AccessibilityNodeInfo firstFrameLayout = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstFrameLayout));
        assertNotNull(firstFrameLayout);
        assertSame(3, firstFrameLayout.getChildCount());

        // Check if the first child is the right one.
        AccessibilityNodeInfo firstTextView = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstTextView));
        assertEquals(firstTextView, getInteractionBridge().getChild(firstFrameLayout, 0));

        // Check if the second child is the right one.
        AccessibilityNodeInfo firstEditText = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstEditText));
        assertEquals(firstEditText, getInteractionBridge().getChild(firstFrameLayout, 1));

        // Check if the third child is the right one.
        AccessibilityNodeInfo firstButton = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstButton));
        assertEquals(firstButton, getInteractionBridge().getChild(firstFrameLayout, 2));
    }

    @MediumTest
    public void testDescendantsOfNotImportantViewReportedInOrder2() throws Exception {
        AccessibilityNodeInfo secondFrameLayout = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondFrameLayout));
        assertNotNull(secondFrameLayout);
        assertSame(3, secondFrameLayout.getChildCount());

        // Check if the first child is the right one.
        AccessibilityNodeInfo secondTextView = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondTextView));
        assertEquals(secondTextView, getInteractionBridge().getChild(secondFrameLayout, 0));

        // Check if the second child is the right one.
        AccessibilityNodeInfo secondEditText = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));
        assertEquals(secondEditText, getInteractionBridge().getChild(secondFrameLayout, 1));

        // Check if the third child is the right one.
        AccessibilityNodeInfo secondButton = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondButton));
        assertEquals(secondButton, getInteractionBridge().getChild(secondFrameLayout, 2));
    }

    @MediumTest
    public void testDescendantsOfNotImportantViewReportedInOrder3() throws Exception {
        AccessibilityNodeInfo rootLinearLayout = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.rootLinearLayout));
        assertNotNull(rootLinearLayout);
        assertSame(4, rootLinearLayout.getChildCount());

        // Check if the first child is the right one.
        AccessibilityNodeInfo firstFrameLayout = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.firstFrameLayout));
        assertEquals(firstFrameLayout, getInteractionBridge().getChild(rootLinearLayout, 0));

        // Check if the second child is the right one.
        AccessibilityNodeInfo secondTextView = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondTextView));
        assertEquals(secondTextView, getInteractionBridge().getChild(rootLinearLayout, 1));

        // Check if the third child is the right one.
        AccessibilityNodeInfo secondEditText = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondEditText));
        assertEquals(secondEditText, getInteractionBridge().getChild(rootLinearLayout, 2));

        // Check if the fourth child is the right one.
        AccessibilityNodeInfo secondButton = getInteractionBridge()
            .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.secondButton));
        assertEquals(secondButton, getInteractionBridge().getChild(rootLinearLayout, 3));
    }
}
