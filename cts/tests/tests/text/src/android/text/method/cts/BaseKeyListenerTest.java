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

package android.text.method.cts;

import com.android.cts.stub.R;


import android.app.Activity;
import android.app.Instrumentation;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.method.BaseKeyListener;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.BufferType;

/**
 * Test the main functionalities of the BaseKeyListener.
 */
public class BaseKeyListenerTest extends
        ActivityInstrumentationTestCase2<KeyListenerStubActivity> {
    private static final CharSequence TEST_STRING = "123456";
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private TextView mTextView;

    public BaseKeyListenerTest(){
        super("com.android.cts.stub", KeyListenerStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mTextView = (TextView) mActivity.findViewById(R.id.keylistener_textview);
    }

    public void testBackspace() {
        final Editable content = Editable.Factory.getInstance().newEditable(TEST_STRING);
        setTextViewText(content);

        // Nothing to delete when the cursor is at the beginning.
        final MockBaseKeyListener baseKeyListener = new MockBaseKeyListener();
        KeyEvent delKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);
        Selection.setSelection(content, 0, 0);
        baseKeyListener.backspace(mTextView, content, KeyEvent.KEYCODE_DEL, delKeyEvent);
        assertEquals("123456", content.toString());

        // Delete the first three letters using a selection.
        setTextViewText(content);
        Selection.setSelection(content, 0, 3);
        baseKeyListener.backspace(mTextView, content, KeyEvent.KEYCODE_DEL, delKeyEvent);
        assertEquals("456", content.toString());

        // Delete the entire line wit ALT + DEL
        setTextViewText(content);
        KeyEvent altDelKeyEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL,
                0, KeyEvent.META_ALT_ON);
        Selection.setSelection(content, 0, 0);
        baseKeyListener.backspace(mTextView, content, KeyEvent.KEYCODE_DEL, altDelKeyEvent);
        assertEquals("", content.toString());
    }

    private void setTextViewText(final CharSequence content) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText(content, BufferType.EDITABLE);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    public void testBackspace_withSendKeys() {
        final MockBaseKeyListener baseKeyListener = new MockBaseKeyListener();
        final String str = "123456";

        // Delete the first character '1'
        prepareTextView(str, baseKeyListener, 1, 1);
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals("23456", mTextView.getText().toString());

        // Delete character '2' and '3'
        prepareTextView(str, baseKeyListener, 1, 3);
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals("1456", mTextView.getText().toString());

        // Delete everything on the line the cursor is on.
        prepareTextView(str, baseKeyListener, 0, 0);
        sendAltDelete();
        assertEquals("", mTextView.getText().toString());

        // ALT+DEL deletes the selection only.
        prepareTextView(str, baseKeyListener, 2, 4);
        sendAltDelete();
        assertEquals("1256", mTextView.getText().toString());

        // DEL key does not take effect when TextView does not have BaseKeyListener.
        prepareTextView(str, null, 1, 1);
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals(str, mTextView.getText().toString());
    }

    private void prepareTextView(final CharSequence content, final BaseKeyListener keyListener,
            final int selectionStart, final int selectionEnd) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText(content, BufferType.EDITABLE);
                mTextView.setKeyListener(keyListener);
                mTextView.requestFocus();
                Selection.setSelection((Editable) mTextView.getText(), selectionStart,
                        selectionEnd);
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void sendAltDelete() {
        mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT));
        sendKeys(KeyEvent.KEYCODE_DEL);
        mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_LEFT));
    }

    /**
     * Check point:
     * 1. Press 0 key, the content of TextView does not changed.
     * 2. Set a selection and press DEL key, the selection is deleted.
     * 3. ACTION_MULTIPLE KEYCODE_UNKNOWN by inserting the event's text into the content.
     */
    public void testPressKey() {
        final CharSequence str = "123456";
        final MockBaseKeyListener baseKeyListener = new MockBaseKeyListener();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText(str, BufferType.EDITABLE);
                mTextView.setKeyListener(baseKeyListener);
                mTextView.requestFocus();
                Selection.setSelection((Editable) mTextView.getText(), 0, 0);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("123456", mTextView.getText().toString());
        // press '0' key.
        sendKeys(KeyEvent.KEYCODE_0);
        assertEquals("123456", mTextView.getText().toString());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Selection.setSelection((Editable) mTextView.getText(), 1, 2);
            }
        });
        mInstrumentation.waitForIdleSync();
        // delete character '2'
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals("13456", mTextView.getText().toString());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Selection.setSelection((Editable) mTextView.getText(), 2, 2);
            }
        });
        mInstrumentation.waitForIdleSync();
        // test ACTION_MULTIPLE KEYCODE_UNKNOWN key event.
        KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(), "abcd",
                KeyCharacterMap.BUILT_IN_KEYBOARD, 0);
        mInstrumentation.sendKeySync(event);
        mInstrumentation.waitForIdleSync();
        // the text of TextView is never changed, onKeyOther never works.
//        assertEquals("13abcd456", mTextView.getText().toString());
    }

    private class MockBaseKeyListener extends BaseKeyListener {
        public int getInputType() {
            return InputType.TYPE_CLASS_DATETIME
                    | InputType.TYPE_DATETIME_VARIATION_DATE;
        }
    }
}
