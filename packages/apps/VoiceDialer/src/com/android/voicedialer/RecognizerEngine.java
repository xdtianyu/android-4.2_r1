/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.speech.srec.MicrophoneInputStream;
import android.speech.srec.Recognizer;
import android.speech.srec.WaveHeader;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * This class is a framework for recognizing speech.  It must be extended to use.
 * The child class must timplement setupGrammar and onRecognitionSuccess.
 * A usage cycle is as follows:
 * <ul>
 * <li>Create with a reference to the {@link VoiceDialerActivity}.
 * <li>Signal the user to start speaking with the Vibrator or beep.
 * <li>Start audio input by creating a {@link MicrophoneInputStream}.
 * <li>Create and configure a {@link Recognizer}.
 * <li>Set up the grammar using setupGrammar.
 * <li>Start the {@link Recognizer} running using data already being
 * collected by the microphone.
 * <li>Wait for the {@link Recognizer} to complete.
 * <li>Process the results using onRecognitionSuccess, which will pass
 * a list of intents to the {@RecogizerClient}.
 * <li>Shut down and clean up.
 * </ul>
 * Notes:
 * <ul>
 * <li>Audio many be read from a file.
 * <li>A directory tree of audio files may be stepped through.
 * <li>A contact list may be read from a file.
 * <li>A {@link RecognizerLogger} may generate a set of log files from
 * a recognition session.
 * <li>A static instance of this class is held and reused by the
 * {@link VoiceDialerActivity}, which saves setup time.
 * </ul>
 */
abstract public class RecognizerEngine {

    protected static final String TAG = "RecognizerEngine";

    protected static final String ACTION_RECOGNIZER_RESULT =
            "com.android.voicedialer.ACTION_RECOGNIZER_RESULT";
    public static final String SENTENCE_EXTRA = "sentence";
    public static final String SEMANTIC_EXTRA = "semantic";

    protected final String SREC_DIR = Recognizer.getConfigDir(null);

    protected static final String OPEN_ENTRIES = "openentries.txt";

    protected static final int RESULT_LIMIT = 5;

    protected Activity mActivity;
    protected Recognizer mSrec;
    protected Recognizer.Grammar mSrecGrammar;
    protected RecognizerLogger mLogger;
    protected int mSampleRate;

    /**
     * Constructor.
     */
    public RecognizerEngine() {
        mSampleRate = 0;
    }

    abstract protected void setupGrammar() throws IOException, InterruptedException;

    abstract protected void onRecognitionSuccess(RecognizerClient recognizerClient)
            throws InterruptedException;

    /**
     * Start the recognition process.
     *
     * <ul>
     * <li>Create and start the microphone.
     * <li>Create a Recognizer.
     * <li>set up the grammar (implementation is in child class)
     * <li>Start the Recognizer.
     * <li>Feed the Recognizer audio until it provides a result.
     * <li>Build a list of Intents corresponding to the results. (implementation
     * is in child class)
     * <li>Stop the microphone.
     * <li>Stop the Recognizer.
     * </ul>
     *
     * @param recognizerClient client to be given the results
     * @param activity the Activity this recognition is being run from.
     * @param micFile optional audio input from this file, or directory tree.
     * @param sampleRate the same rate coming from the mic or micFile
     */
    public void recognize(RecognizerClient recognizerClient, Activity activity,
            File micFile, int sampleRate) {
        InputStream mic = null;
        boolean recognizerStarted = false;
        try {
            mActivity = activity;
            // set up logger
            mLogger = null;
            if (RecognizerLogger.isEnabled(mActivity)) {
                mLogger = new RecognizerLogger(mActivity);
            }

            if (mSampleRate != sampleRate) {
                // sample rate has changed since we last used this recognizerEngine.
                // destroy the grammar and regenerate.
                if (mSrecGrammar != null) {
                    mSrecGrammar.destroy();
                }
                mSrecGrammar = null;
                mSampleRate = sampleRate;
            }

            // create a new recognizer
            if (false) Log.d(TAG, "start new Recognizer");
            if (mSrec == null) {
                String parFilePath = SREC_DIR + "/baseline11k.par";
                if (sampleRate == 8000) {
                    parFilePath = SREC_DIR + "/baseline8k.par";
                }
                mSrec = new Recognizer(parFilePath);
            }

            // start audio input
            if (micFile != null) {
                if (false) Log.d(TAG, "using mic file");
                mic = new FileInputStream(micFile);
                WaveHeader hdr = new WaveHeader();
                hdr.read(mic);
            } else {
                if (false) Log.d(TAG, "start new MicrophoneInputStream");
                mic = new MicrophoneInputStream(sampleRate, sampleRate * 15);
            }

            // notify UI
            recognizerClient.onMicrophoneStart(mic);

            // log audio if requested
            if (mLogger != null) mic = mLogger.logInputStream(mic, sampleRate);

            setupGrammar();

            // start the recognition process
            if (false) Log.d(TAG, "start mSrec.start");
            mSrec.start();
            recognizerStarted = true;

            // recognize
            while (true) {
                if (Thread.interrupted()) throw new InterruptedException();
                int event = mSrec.advance();
                if (event != Recognizer.EVENT_INCOMPLETE &&
                        event != Recognizer.EVENT_NEED_MORE_AUDIO) {
                    Log.d(TAG, "start advance()=" +
                            Recognizer.eventToString(event) +
                            " avail " + mic.available());
                }
                switch (event) {
                case Recognizer.EVENT_INCOMPLETE:
                case Recognizer.EVENT_STARTED:
                case Recognizer.EVENT_START_OF_VOICING:
                case Recognizer.EVENT_END_OF_VOICING:
                    continue;
                case Recognizer.EVENT_RECOGNITION_RESULT:
                    onRecognitionSuccess(recognizerClient);
                    break;
                case Recognizer.EVENT_NEED_MORE_AUDIO:
                    mSrec.putAudio(mic);
                    continue;
                default:
                    Log.d(TAG, "unknown event " + event);
                    recognizerClient.onRecognitionFailure(Recognizer.eventToString(event));
                    break;
                }
                break;
            }

        } catch (InterruptedException e) {
            if (false) Log.d(TAG, "start interrupted " + e);
            recognizerClient.onRecognitionError(e.toString());
        } catch (IOException e) {
            if (false) Log.d(TAG, "start new Srec failed " + e);
            recognizerClient.onRecognitionError(e.toString());
        } catch (Exception e) {
            if (false) Log.d(TAG, "exception " + e);
            recognizerClient.onRecognitionError(e.toString());
        } finally {
            if (false) Log.d(TAG, "start mSrec.stop");
            if (mSrec != null && recognizerStarted) mSrec.stop();

            // stop microphone
            try {
                if (mic != null) mic.close();
            }
            catch (IOException ex) {
                if (false) Log.d(TAG, "start - mic.close failed - " + ex);
            }
            mic = null;

            // close logger
            try {
                if (mLogger != null) mLogger.close();
            }
            catch (IOException ex) {
                if (false) Log.d(TAG, "start - mLoggger.close failed - " + ex);
            }
            mLogger = null;
        }
        if (false) Log.d(TAG, "start bye");
    }

    protected static void addIntent(ArrayList<Intent> intents, Intent intent) {
        for (Intent in : intents) {
            if (in.getAction() != null &&
                    in.getAction().equals(intent.getAction()) &&
                    in.getData() != null &&
                    in.getData().equals(intent.getData())) {
                return;
            }
        }
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
        intents.add(intent);
    }
}
