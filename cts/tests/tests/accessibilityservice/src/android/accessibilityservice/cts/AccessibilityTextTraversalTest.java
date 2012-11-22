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

import android.os.Bundle;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.Selection;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.android.cts.accessibilityservice.R;

/**
 * Test cases for testing the accessibility APIs for traversing the text content of
 * a View at several granularities.
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
public class AccessibilityTextTraversalTest
        extends AccessibilityActivityTestCase<AccessibilityTextTraversalActivity>{

    public AccessibilityTextTraversalTest() {
        super(AccessibilityTextTraversalActivity.class);
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityCharacterOverContentDescription()
            throws Exception {
        final View view = getActivity().findViewById(R.id.view);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                view.setContentDescription(getString(R.string.a_b));
            }
        });

        final AccessibilityNodeInfo text = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.a_b));

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);

        // Move to the next character and wait for an event.
        AccessibilityEvent firstExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent secondExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent thirdExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Make sure there is no next.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Move to the next character and wait for an event.
        AccessibilityEvent fifthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent sixthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Make sure there is no previous.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityWordOverContentDescription()
            throws Exception {
        final View view = getActivity().findViewById(R.id.view);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                view.setContentDescription(getString(R.string.foo_bar_baz));
            }
        });

        final AccessibilityNodeInfo text = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.foo_bar_baz));

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);

        // Move to the next character and wait for an event.
        AccessibilityEvent firstExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent secondExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent thirdExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Make sure there is no next.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Move to the next character and wait for an event.
        AccessibilityEvent fourthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent fifthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent sixthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Make sure there is no previous.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityCharacterOverText()
            throws Exception {
        final TextView textView = (TextView) getActivity().findViewById(R.id.text);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(getString(R.string.a_b));
            }
        });

        final AccessibilityNodeInfo text = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.a_b));

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);

        // Move to the next character and wait for an event.
        AccessibilityEvent firstExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(1, Selection.getSelectionStart(textView.getText()));
        assertEquals(1, Selection.getSelectionEnd(textView.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent secondExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(textView.getText()));
        assertEquals(2, Selection.getSelectionEnd(textView.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent thirdExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(textView.getText()));
        assertEquals(3, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no next.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(textView.getText()));
        assertEquals(3, Selection.getSelectionEnd(textView.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent fifthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(textView.getText()));
        assertEquals(2, Selection.getSelectionEnd(textView.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent sixthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(1, Selection.getSelectionStart(textView.getText()));
        assertEquals(1, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no previous.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(1, Selection.getSelectionStart(textView.getText()));
        assertEquals(1, Selection.getSelectionEnd(textView.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityWordOverText() throws Exception {
        final TextView textView = (TextView) getActivity().findViewById(R.id.text);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(getString(R.string.foo_bar_baz));
            }
        });

        final AccessibilityNodeInfo text = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.foo_bar_baz));

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);

        // Move to the next word and wait for an event.
        AccessibilityEvent firstExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(textView.getText()));
        assertEquals(3, Selection.getSelectionEnd(textView.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent secondExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(7, Selection.getSelectionStart(textView.getText()));
        assertEquals(7, Selection.getSelectionEnd(textView.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent thirdExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(textView.getText()));
        assertEquals(11, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no next.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(textView.getText()));
        assertEquals(11, Selection.getSelectionEnd(textView.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent fourthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(8, Selection.getSelectionStart(textView.getText()));
        assertEquals(8, Selection.getSelectionEnd(textView.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent fifthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(4, Selection.getSelectionStart(textView.getText()));
        assertEquals(4, Selection.getSelectionEnd(textView.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent sixthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no previous.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityLineOverText() throws Exception {
        final TextView textView = (TextView) getActivity().findViewById(R.id.text);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(getString(R.string.android_wiki_short));
            }
        });

        final AccessibilityNodeInfo text = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.android_wiki_short));

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);

        // Move to the next line and wait for an event.
        AccessibilityEvent firstExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 25
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(25, Selection.getSelectionStart(textView.getText()));
        assertEquals(25, Selection.getSelectionEnd(textView.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent secondExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 25
                        && event.getToIndex() == 53
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(textView.getText()));
        assertEquals(53, Selection.getSelectionEnd(textView.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent thirdExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 53
                        && event.getToIndex() == 60
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(60, Selection.getSelectionStart(textView.getText()));
        assertEquals(60, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no next.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(60, Selection.getSelectionStart(textView.getText()));
        assertEquals(60, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent fourthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 53
                        && event.getToIndex() == 60
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(textView.getText()));
        assertEquals(53, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent fifthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 25
                        && event.getToIndex() == 53
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(25, Selection.getSelectionStart(textView.getText()));
        assertEquals(25, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent sixthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 25
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no previous.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityPageOverText() throws Exception {
        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setText(getString(R.string.android_wiki));
                Selection.removeSelection(editText.getText());
            }
        });

        final AccessibilityNodeInfo text = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.android_wiki));

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        // Move to the next page and wait for an event.
        AccessibilityEvent firstExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 139
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(139, Selection.getSelectionStart(editText.getText()));
        assertEquals(139, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent secondExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 139
                        && event.getToIndex() == 285
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(285, Selection.getSelectionStart(editText.getText()));
        assertEquals(285, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent thirdExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 285
                        && event.getToIndex() == 436
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(436, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(436, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent fourthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 285
                        && event.getToIndex() == 436
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(285, Selection.getSelectionStart(editText.getText()));
        assertEquals(285, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent fifthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 139
                        && event.getToIndex() == 285
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(139, Selection.getSelectionStart(editText.getText()));
        assertEquals(139, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent sixthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 139
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityParagraphOverText() throws Exception {
        final TextView textView = (TextView) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(getString(R.string.android_wiki_paragraphs));
            }
        });

        final AccessibilityNodeInfo text = getInteractionBridge()
               .findAccessibilityNodeInfoByTextFromRoot(getString(R.string.android_wiki_short));

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent firstExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 104
                        && event.getMovementGranularity()
                            == AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(104, Selection.getSelectionStart(textView.getText()));
        assertEquals(104, Selection.getSelectionEnd(textView.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent secondExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 106
                        && event.getToIndex() == 267
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(267, Selection.getSelectionStart(textView.getText()));
        assertEquals(267, Selection.getSelectionEnd(textView.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent thirdExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 268
                        && event.getToIndex() == 582
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(textView.getText()));
        assertEquals(582, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no next.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(textView.getText()));
        assertEquals(582, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent fourthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 268
                        && event.getToIndex() == 582
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(268, Selection.getSelectionStart(textView.getText()));
        assertEquals(268, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent fifthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 106
                        && event.getToIndex() == 267
                        && event.getMovementGranularity() ==
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(106, Selection.getSelectionStart(textView.getText()));
        assertEquals(106, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent sixthExpected = getInteractionBridge()
                .executeCommandAndWaitForAccessibilityEvent(new Runnable() {
            @Override
            public void run() {
                getInteractionBridge().performAction(text,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 104
                        && event.getMovementGranularity()
                                == AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(textView.getText()));
        assertEquals(2, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no previous.
        assertFalse(getInteractionBridge().performAction(text,
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(textView.getText()));
        assertEquals(2, Selection.getSelectionEnd(textView.getText()));
    }
}
