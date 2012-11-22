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

import android.util.Log;
import android.content.Intent;
import android.speech.srec.Recognizer;

import java.io.IOException;
import java.util.ArrayList;

public class PhoneTypeChoiceRecognizerEngine extends RecognizerEngine {
    /**
     * Constructor.
     */
    public PhoneTypeChoiceRecognizerEngine() {

    }

    protected void setupGrammar() throws IOException, InterruptedException {
        if (mSrecGrammar == null) {
            if (false) Log.d(TAG, "start new Grammar");
            mSrecGrammar = mSrec.new Grammar(SREC_DIR + "/grammars/phone_type_choice.g2g");
            mSrecGrammar.setupRecognizer();
        }
    }

    /**
     * Called when recognition succeeds.  It receives a list
     * of results, builds a corresponding list of Intents, and
     * passes them to the {@link RecognizerClient}, which selects and
     * performs a corresponding action.
     * @param recognizerClient the client that will be sent the results
     */
    protected void onRecognitionSuccess(RecognizerClient recognizerClient) throws InterruptedException {
        if (false) Log.d(TAG, "onRecognitionSuccess " + mSrec.getResultCount());

        if (mLogger != null) mLogger.logNbestHeader();

        ArrayList<Intent> intents = new ArrayList<Intent>();

        for (int result = 0; result < mSrec.getResultCount() &&
                intents.size() < RESULT_LIMIT; result++) {

            // parse the semanticMeaning string and build an Intent
            String conf = mSrec.getResult(result, Recognizer.KEY_CONFIDENCE);
            String literal = mSrec.getResult(result, Recognizer.KEY_LITERAL);
            String semantic = mSrec.getResult(result, Recognizer.KEY_MEANING);
            String msg = "conf=" + conf + " lit=" + literal + " sem=" + semantic;
            if (false) Log.d(TAG, msg);
        }

        // we only pay attention to the first result.
        if (mSrec.getResultCount() > 0) {
            // parse the semanticMeaning string and build an Intent
            String conf = mSrec.getResult(0, Recognizer.KEY_CONFIDENCE);
            String literal = mSrec.getResult(0, Recognizer.KEY_LITERAL);
            String semantic = mSrec.getResult(0, Recognizer.KEY_MEANING);
            String msg = "conf=" + conf + " lit=" + literal + " sem=" + semantic;
            if (false) Log.d(TAG, msg);
            if (mLogger != null) mLogger.logLine(msg);

            if (("H".equalsIgnoreCase(semantic)) ||
                ("M".equalsIgnoreCase(semantic)) ||
                ("W".equalsIgnoreCase(semantic)) ||
                ("O".equalsIgnoreCase(semantic)) ||
                ("R".equalsIgnoreCase(semantic)) ||
                ("X".equalsIgnoreCase(semantic))) {
                if (false) Log.d(TAG, " got valid response");
                Intent intent = new Intent(RecognizerEngine.ACTION_RECOGNIZER_RESULT, null);
                intent.putExtra(RecognizerEngine.SENTENCE_EXTRA, literal);
                intent.putExtra(RecognizerEngine.SEMANTIC_EXTRA, semantic);
                addIntent(intents, intent);
            } else {
                // Anything besides yes or no is a failure.
            }
        }

        // log if requested
        if (mLogger != null) mLogger.logIntents(intents);

        // bail out if cancelled
        if (Thread.interrupted()) throw new InterruptedException();

        if (intents.size() == 0) {
            if (false) Log.d(TAG, " no intents");
            recognizerClient.onRecognitionFailure("No Intents generated");
        }
        else {
            if (false) Log.d(TAG, " success");
            recognizerClient.onRecognitionSuccess(
                    intents.toArray(new Intent[intents.size()]));
        }
    }
}
