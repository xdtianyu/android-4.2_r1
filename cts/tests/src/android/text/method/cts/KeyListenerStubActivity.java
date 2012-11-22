/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.os.Bundle;
import android.text.method.BaseKeyListener;
import android.text.method.DateKeyListener;
import android.text.method.DateTimeKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.MultiTapKeyListener;
import android.text.method.NumberKeyListener;
import android.text.method.QwertyKeyListener;
import android.text.method.TextKeyListener;
import android.text.method.TimeKeyListener;

/**
 * This Activity is used for testing:
 * {@link DigitsKeyListener}
 * {@link BaseKeyListener}
 * {@link MultiTapKeyListener}
 * {@link NumberKeyListener}
 * {@link QwertyKeyListener}
 * {@link TextKeyListener}
 * {@link DateKeyListener}
 * {@link DateTimeKeyListener}
 * {@link TimeKeyListener}
 *
 * @see DigitsKeyListener
 * @see BaseKeyListener
 * @see MultiTapKeyListener
 * @see NumberKeyListener
 * @see QwertyKeyListener
 * @see TextKeyListener
 * @see DateKeyListener
 * @see DateTimeKeyListener
 * @see TimeKeyListener
 */

public class KeyListenerStubActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keylistener_layout);
    }
}
