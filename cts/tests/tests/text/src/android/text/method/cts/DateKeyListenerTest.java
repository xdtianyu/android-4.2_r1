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
import android.test.ActivityInstrumentationTestCase2;
import android.text.InputType;
import android.text.method.DateKeyListener;
import android.view.KeyEvent;
import android.widget.TextView;

/**
 * Test {@link DateKeyListener}.
 */
public class DateKeyListenerTest extends
        ActivityInstrumentationTestCase2<KeyListenerStubActivity> {
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private TextView mTextView;

    public DateKeyListenerTest(){
        super("com.android.cts.stub", KeyListenerStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mTextView = (TextView) mActivity.findViewById(R.id.keylistener_textview);
    }

    public void testConstructor() {
        new DateKeyListener();
    }

    public void testGetInstance() {
        DateKeyListener listener1 = DateKeyListener.getInstance();
        DateKeyListener listener2 = DateKeyListener.getInstance();

        assertNotNull(listener1);
        assertNotNull(listener2);
        assertSame(listener1, listener2);
    }

    public void testGetAcceptedChars() {
        MyDataKeyListener dataKeyListener = new MyDataKeyListener();

        TextMethodUtils.assertEquals(DateKeyListener.CHARACTERS,
                dataKeyListener.getAcceptedChars());
    }

    public void testGetInputType() {
        MyDataKeyListener dataKeyListener = new MyDataKeyListener();

        int expected = InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_DATE;
        assertEquals(expected, dataKeyListener.getInputType());
    }

    /**
     * Scenario description:
     * 1. Press '1' key and check if the content of TextView becomes "1"
     * 2. Press '2' key and check if the content of TextView becomes "12"
     * 3. Press an unaccepted key if it exists and this key will not be accepted.
     * 4. Press '-' key and check if the content of TextView becomes "12-"
     * 5. Press '/' key and check if the content of TextView becomes "12-/"
     * 6. remove DateKeyListener and Press '/' key, this key will not be accepted
     */
    public void testDateTimeKeyListener() {
        final DateKeyListener dateKeyListener = DateKeyListener.getInstance();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(dateKeyListener);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("", mTextView.getText().toString());

        // press '1' key.
        sendKeys(KeyEvent.KEYCODE_1);
        assertEquals("1", mTextView.getText().toString());

        // press '2' key.
        sendKeys(KeyEvent.KEYCODE_2);
        assertEquals("12", mTextView.getText().toString());

        // press an unaccepted key if it exists.
        int keyCode = TextMethodUtils.getUnacceptedKeyCode(DateKeyListener.CHARACTERS);
        if (-1 != keyCode) {
            sendKeys(keyCode);
            assertEquals("12", mTextView.getText().toString());
        }

        // press '-' key.
        sendKeys(KeyEvent.KEYCODE_MINUS);
        assertEquals("12-", mTextView.getText().toString());

        // press '/' key.
        sendKeys(KeyEvent.KEYCODE_SLASH);
        assertEquals("12-/", mTextView.getText().toString());

        // remove DateKeyListener
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(null);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("12-/", mTextView.getText().toString());

        // press '/' key, it will not be accepted.
        sendKeys(KeyEvent.KEYCODE_SLASH);
        assertEquals("12-/", mTextView.getText().toString());
    }

    private class MyDataKeyListener extends DateKeyListener {
        @Override
        protected char[] getAcceptedChars() {
            return super.getAcceptedChars();
        }
    }
}
