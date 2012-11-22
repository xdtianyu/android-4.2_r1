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

package com.android.voicedialer;

import android.content.Intent;

import java.io.InputStream;

/**
 * This is an interface for clients of the RecognizerEngine.
 * A user should implement this interface, and then pass that implementation
 * into a RecognizerEngine.  The RecognizerEngine will call the
 * appropriate function when the recognition completes.
 */

interface RecognizerClient {

    public void onRecognitionSuccess(final Intent[] intents);

    public void onRecognitionFailure(String msg);

    public void onRecognitionError(String err);

    public void onMicrophoneStart(InputStream mic);
}
