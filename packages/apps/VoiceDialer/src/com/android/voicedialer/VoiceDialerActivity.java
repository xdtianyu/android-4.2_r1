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

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * TODO: get rid of the anonymous classes
 *
 * This class is the user interface of the VoiceDialer application.
 * It begins in the INITIALIZING state.
 *
 * INITIALIZING :
 *  This transitions out on events from TTS and the BluetoothHeadset
 *   once TTS initialized and SCO channel set up:
 *     * prompt the user "speak now"
 *     * transition to the SPEAKING_GREETING state
 *
 * SPEAKING_GREETING:
 *  This transitions out only on events from TTS or the fallback runnable
 *   once the greeting utterance completes:
 *     * begin listening for the command using the {@link CommandRecognizerEngine}
 *     * transition to the WAITING_FOR_COMMAND state
 *
 * WAITING_FOR_COMMAND :
 * This transitions out only on events from the recognizer
 *   on RecognitionFailure or RecognitionError:
 *     * begin speaking "try again."
 *     * transition to state SPEAKING_TRY_AGAIN
 *   on RecognitionSuccess:
 *     single result:
 *       * begin speaking the sentence describing the intent
 *       * transition to the SPEAKING_CHOSEN_ACTION
 *     multiple results:
 *       * begin speaking each of the choices in order
 *       * transition to the SPEAKING_CHOICES state
 *
 * SPEAKING_TRY_AGAIN:
 * This transitions out only on events from TTS or the fallback runnable
 *   once the try again utterance completes:
 *     * begin listening for the command using the {@link CommandRecognizerEngine}
 *     * transition to the LISTENING_FOR_COMMAND state
 *
 * SPEAKING_CHOSEN_ACTION:
 *  This transitions out only on events from TTS or the fallback runnable
 *   once the utterance completes:
 *     * dispatch the intent that was chosen
 *     * transition to the EXITING state
 *     * finish the activity
 *
 * SPEAKING_CHOICES:
 *  This transitions out only on events from TTS or the fallback runnable
 *   once the utterance completes:
 *     * begin listening for the user's choice using the
 *         {@link PhoneTypeChoiceRecognizerEngine}
 *     * transition to the WAITING_FOR_CHOICE state.
 *
 * WAITING_FOR_CHOICE:
 *  This transitions out only on events from the recognizer
 *   on RecognitionFailure or RecognitionError:
 *     * begin speaking the "invalid choice" message, along with the list
 *       of choices
 *     * transition to the SPEAKING_CHOICES state
 *   on RecognitionSuccess:
 *     if the result is "try again", prompt the user to say a command, begin
 *       listening for the command, and transition back to the WAITING_FOR_COMMAND
 *       state.
 *     if the result is "exit", then being speaking the "goodbye" message and
 *       transition to the SPEAKING_GOODBYE state.
 *     if the result is a valid choice, begin speaking the action chosen,initiate
 *       the command the user has choose and exit.
 *     if not a valid choice, speak the "invalid choice" message, begin
 *       speaking the choices in order again, transition to the
 *       SPEAKING_CHOICES
 *
 * SPEAKING_GOODBYE:
 *  This transitions out only on events from TTS or the fallback runnable
 *   after a time out, finish the activity.
 *
 */

public class VoiceDialerActivity extends Activity {

    private static final String TAG = "VoiceDialerActivity";

    private static final String MICROPHONE_EXTRA = "microphone";
    private static final String CONTACTS_EXTRA = "contacts";

    private static final String SPEAK_NOW_UTTERANCE = "speak_now";
    private static final String TRY_AGAIN_UTTERANCE = "try_again";
    private static final String CHOSEN_ACTION_UTTERANCE = "chose_action";
    private static final String GOODBYE_UTTERANCE = "goodbye";
    private static final String CHOICES_UTTERANCE = "choices";

    private static final int FIRST_UTTERANCE_DELAY = 300;
    private static final int MAX_TTS_DELAY = 6000;
    private static final int EXIT_DELAY = 2000;

    private static final int BLUETOOTH_SAMPLE_RATE = 8000;
    private static final int REGULAR_SAMPLE_RATE = 11025;

    private static final int INITIALIZING = 0;
    private static final int SPEAKING_GREETING = 1;
    private static final int WAITING_FOR_COMMAND = 2;
    private static final int SPEAKING_TRY_AGAIN = 3;
    private static final int SPEAKING_CHOICES = 4;
    private static final int WAITING_FOR_CHOICE = 5;
    private static final int WAITING_FOR_DIALOG_CHOICE = 6;
    private static final int SPEAKING_CHOSEN_ACTION = 7;
    private static final int SPEAKING_GOODBYE = 8;
    private static final int EXITING = 9;

    private static final CommandRecognizerEngine mCommandEngine =
            new CommandRecognizerEngine();
    private static final PhoneTypeChoiceRecognizerEngine mPhoneTypeChoiceEngine =
            new PhoneTypeChoiceRecognizerEngine();
    private CommandRecognizerClient mCommandClient;
    private ChoiceRecognizerClient mChoiceClient;
    private ToneGenerator mToneGenerator;
    private Handler mHandler;
    private Thread mRecognizerThread = null;
    private AudioManager mAudioManager;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter mAdapter;
    private TextToSpeech mTts;
    private HashMap<String, String> mTtsParams;
    private VoiceDialerBroadcastReceiver mReceiver;
    private boolean mWaitingForTts;
    private boolean mWaitingForScoConnection;
    private Intent[] mAvailableChoices;
    private Intent mChosenAction;
    private int mBluetoothVoiceVolume;
    private int mState;
    private AlertDialog mAlertDialog;
    private Runnable mFallbackRunnable;
    private boolean mUsingBluetooth = false;
    private int mSampleRate;
    private WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // TODO: All of this state management and holding of
        // connections to the TTS engine and recognizer really
        // belongs in a service.  The activity can be stopped or deleted
        // and recreated for lots of reasons.
        // It's way too late in the ICS release cycle for a change
        // like this now though.
        // MHibdon Sept 20 2011
        mHandler = new Handler();
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        mToneGenerator = new ToneGenerator(AudioManager.STREAM_RING,
                ToneGenerator.MAX_VOLUME);

        acquireWakeLock(this);

        mState = INITIALIZING;
        mChosenAction = null;
        mAudioManager.requestAudioFocus(
                null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        // set this flag so this activity will stay in front of the keyguard
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        getWindow().addFlags(flags);

        // open main window
        setTheme(android.R.style.Theme_Dialog);
        setTitle(R.string.title);
        setContentView(R.layout.voice_dialing);
        findViewById(R.id.microphone_view).setVisibility(View.INVISIBLE);
        findViewById(R.id.retry_view).setVisibility(View.INVISIBLE);
        findViewById(R.id.microphone_loading_view).setVisibility(View.VISIBLE);
        if (RecognizerLogger.isEnabled(this)) {
            ((TextView) findViewById(R.id.substate)).setText(R.string.logging_enabled);
        }

        // Get handle to BluetoothHeadset object
        IntentFilter audioStateFilter;
        audioStateFilter = new IntentFilter();
        audioStateFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        audioStateFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        mReceiver = new VoiceDialerBroadcastReceiver();
        registerReceiver(mReceiver, audioStateFilter);

        mCommandEngine.setContactsFile(newFile(getArg(CONTACTS_EXTRA)));
        mCommandEngine.setMinimizeResults(true);
        mCommandEngine.setAllowOpenEntries(false);
        mCommandClient = new CommandRecognizerClient();
        mChoiceClient = new ChoiceRecognizerClient();

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (BluetoothHeadset.isBluetoothVoiceDialingEnabled(this) && mAdapter != null) {
           if (!mAdapter.getProfileProxy(this, mBluetoothHeadsetServiceListener,
                                         BluetoothProfile.HEADSET)) {
               Log.e(TAG, "Getting Headset Proxy failed");
           }

        } else {
            mUsingBluetooth = false;
            if (false) Log.d(TAG, "bluetooth unavailable");
            mSampleRate = REGULAR_SAMPLE_RATE;
            mCommandEngine.setMinimizeResults(false);
            mCommandEngine.setAllowOpenEntries(true);

            // we're not using bluetooth apparently, just start listening.
            listenForCommand();
        }

    }

    class ErrorRunnable implements Runnable {
        private int mErrorMsg;
        public ErrorRunnable(int errorMsg) {
            mErrorMsg = errorMsg;
        }

        public void run() {
            // put up an error and exit
            mHandler.removeCallbacks(mMicFlasher);
            ((TextView)findViewById(R.id.state)).setText(R.string.failure);
            ((TextView)findViewById(R.id.substate)).setText(mErrorMsg);
            ((TextView)findViewById(R.id.substate)).setText(
                    R.string.headset_connection_lost);
            findViewById(R.id.microphone_view).setVisibility(View.INVISIBLE);
            findViewById(R.id.retry_view).setVisibility(View.VISIBLE);


            if (!mUsingBluetooth) {
                playSound(ToneGenerator.TONE_PROP_NACK);
            }
        }
    }

    class OnTtsCompletionRunnable implements Runnable {
        private boolean mFallback;

        OnTtsCompletionRunnable(boolean fallback) {
            mFallback = fallback;
        }

        public void run() {
            if (mFallback) {
                Log.e(TAG, "utterance completion not delivered, using fallback");
            }
            Log.d(TAG, "onTtsCompletionRunnable");
            if (mState == SPEAKING_GREETING || mState == SPEAKING_TRY_AGAIN) {
                listenForCommand();
            } else if (mState == SPEAKING_CHOICES) {
                listenForChoice();
            } else if (mState == SPEAKING_GOODBYE) {
                mState = EXITING;
                finish();
            } else if (mState == SPEAKING_CHOSEN_ACTION) {
                mState = EXITING;
                startActivityHelp(mChosenAction);
                finish();
            }
        }
    }

    class GreetingRunnable implements Runnable {
        public void run() {
            mState = SPEAKING_GREETING;
            mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    SPEAK_NOW_UTTERANCE);
            mTts.speak(getString(R.string.speak_now_tts),
                TextToSpeech.QUEUE_FLUSH,
                mTtsParams);
            // Normally, we will begin listening for the command after the
            // utterance completes.  As a fallback in case the utterance
            // does not complete, post a delayed runnable to fire
            // the intent.
            mFallbackRunnable = new OnTtsCompletionRunnable(true);
            mHandler.postDelayed(mFallbackRunnable, MAX_TTS_DELAY);
        }
    }

    class TtsInitListener implements TextToSpeech.OnInitListener {
        public void onInit(int status) {
            // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
            if (false) Log.d(TAG, "onInit for tts");
            if (status != TextToSpeech.SUCCESS) {
                // Initialization failed.
                Log.e(TAG, "Could not initialize TextToSpeech.");
                mHandler.post(new ErrorRunnable(R.string.recognition_error));
                exitActivity();
                return;
            }

            if (mTts == null) {
                Log.e(TAG, "null tts");
                mHandler.post(new ErrorRunnable(R.string.recognition_error));
                exitActivity();
                return;
            }

            mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener());

            // The TTS engine has been successfully initialized.
            mWaitingForTts = false;

            // TTS over bluetooth is really loud,
            // Limit volume to -18dB. Stream volume range represents approximately 50dB
            // (See AudioSystem.cpp linearToLog()) so the number of steps corresponding
            // to 18dB is 18 / (50 / maxSteps).
            mBluetoothVoiceVolume = mAudioManager.getStreamVolume(
                    AudioManager.STREAM_BLUETOOTH_SCO);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_BLUETOOTH_SCO);
            int volume = maxVolume - ((18 / (50/maxVolume)) + 1);
            if (mBluetoothVoiceVolume > volume) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO, volume, 0);
            }

            if (mWaitingForScoConnection) {
                // the bluetooth connection is not up yet, still waiting.
            } else {
                // we now have SCO connection and TTS, so we can start.
                mHandler.postDelayed(new GreetingRunnable(), FIRST_UTTERANCE_DELAY);
            }
        }
    }

    class OnUtteranceCompletedListener
            implements TextToSpeech.OnUtteranceCompletedListener {
        public void onUtteranceCompleted(String utteranceId) {
            if (false) Log.d(TAG, "onUtteranceCompleted " + utteranceId);
            // since the utterance has completed, we no longer need the fallback.
            mHandler.removeCallbacks(mFallbackRunnable);
            mFallbackRunnable = null;
            mHandler.post(new OnTtsCompletionRunnable(false));
        }
    }

    private void updateBluetoothParameters(boolean connected) {
        if (connected) {
            if (false) Log.d(TAG, "using bluetooth");
            mUsingBluetooth = true;

            mBluetoothHeadset.startVoiceRecognition(mBluetoothDevice);

            mSampleRate = BLUETOOTH_SAMPLE_RATE;
            mCommandEngine.setMinimizeResults(true);
            mCommandEngine.setAllowOpenEntries(false);

            // we can't start recognizing until we get connected to the BluetoothHeadset
            // and have a connected audio state.  We will listen for these
            // states to change.
            mWaitingForScoConnection = true;

            // initialize the text to speech system
            mWaitingForTts = true;
            mTts = new TextToSpeech(VoiceDialerActivity.this, new TtsInitListener());
            mTtsParams = new HashMap<String, String>();
            mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(AudioManager.STREAM_VOICE_CALL));
            // we need to wait for the TTS system and the SCO connection
            // before we can start listening.
        } else {
            if (false) Log.d(TAG, "not using bluetooth");
            mUsingBluetooth = false;
            mSampleRate = REGULAR_SAMPLE_RATE;
            mCommandEngine.setMinimizeResults(false);
            mCommandEngine.setAllowOpenEntries(true);

            // we're not using bluetooth apparently, just start listening.
            listenForCommand();
        }
    }

    private BluetoothProfile.ServiceListener mBluetoothHeadsetServiceListener =
            new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (false) Log.d(TAG, "onServiceConnected");
            mBluetoothHeadset = (BluetoothHeadset) proxy;

            List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

            if (deviceList.size() > 0) {
                mBluetoothDevice = deviceList.get(0);
                int state = mBluetoothHeadset.getConnectionState(mBluetoothDevice);
                if (false) Log.d(TAG, "headset status " + state);

                // We are already connnected to a headset
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    updateBluetoothParameters(true);
                    return;
                }
            }
            updateBluetoothParameters(false);
        }

        public void onServiceDisconnected(int profile) {
            mBluetoothHeadset = null;
        }
    };

    private class VoiceDialerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);

                if (false) Log.d(TAG, "HEADSET STATE -> " + state);

                if (state == BluetoothProfile.STATE_CONNECTED) {
                    if (device == null) {
                        return;
                    }
                    mBluetoothDevice = device;
                    updateBluetoothParameters(true);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    mBluetoothDevice = null;
                    updateBluetoothParameters(false);
                }
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
                if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED &&
                    mWaitingForScoConnection) {
                    // SCO channel has just become available.
                    mWaitingForScoConnection = false;
                    if (mWaitingForTts) {
                        // still waiting for the TTS to be set up.
                    } else {
                        // we now have SCO connection and TTS, so we can start.
                        mHandler.postDelayed(new GreetingRunnable(), FIRST_UTTERANCE_DELAY);
                    }
                } else if (prevState == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                    if (!mWaitingForScoConnection && mState != EXITING) {
                        // apparently our connection to the headset has dropped.
                        // we won't be able to continue voicedialing.
                        if (false) Log.d(TAG, "lost sco connection");

                        mHandler.post(new ErrorRunnable(
                                R.string.headset_connection_lost));

                        exitActivity();
                    }
                }
            }
        }
    }

    private void askToTryAgain() {
        // get work off UAPI thread
        mHandler.post(new Runnable() {
            public void run() {
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }

                mHandler.removeCallbacks(mMicFlasher);
                ((TextView)findViewById(R.id.state)).setText(R.string.please_try_again);
                findViewById(R.id.state).setVisibility(View.VISIBLE);
                findViewById(R.id.microphone_view).setVisibility(View.INVISIBLE);
                findViewById(R.id.retry_view).setVisibility(View.VISIBLE);

                if (mUsingBluetooth) {
                    mState = SPEAKING_TRY_AGAIN;
                    mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                            TRY_AGAIN_UTTERANCE);
                    mTts.speak(getString(R.string.no_results_tts),
                        TextToSpeech.QUEUE_FLUSH,
                        mTtsParams);

                    // Normally, the we will start listening after the
                    // utterance completes.  As a fallback in case the utterance
                    // does not complete, post a delayed runnable to fire
                    // the intent.
                    mFallbackRunnable = new OnTtsCompletionRunnable(true);
                    mHandler.postDelayed(mFallbackRunnable, MAX_TTS_DELAY);
                } else {
                    try {
                        Thread.sleep(playSound(ToneGenerator.TONE_PROP_NACK));
                    } catch (InterruptedException e) {
                    }
                    // we are not using tts, so we just start listening again.
                    listenForCommand();
                }
            }
        });
    }

    private void performChoice() {
        if (mUsingBluetooth) {
            String sentenceSpoken = spaceOutDigits(
                    mChosenAction.getStringExtra(
                        RecognizerEngine.SENTENCE_EXTRA));

            mState = SPEAKING_CHOSEN_ACTION;
            mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    CHOSEN_ACTION_UTTERANCE);
            mTts.speak(sentenceSpoken,
                TextToSpeech.QUEUE_FLUSH,
                mTtsParams);

            // Normally, the intent will be dispatched after the
            // utterance completes.  As a fallback in case the utterance
            // does not complete, post a delayed runnable to fire
            // the intent.
            mFallbackRunnable = new OnTtsCompletionRunnable(true);
            mHandler.postDelayed(mFallbackRunnable, MAX_TTS_DELAY);
        } else {
            // just dispatch the intent
            startActivityHelp(mChosenAction);
            finish();
        }
    }

    private void waitForChoice() {
        if (mUsingBluetooth) {
            // We are running in bluetooth mode, and we have
            // multiple matches.  Speak the choices and let
            // the user choose.

            // We will not start listening until the utterance
            // of the choice list completes.
            speakChoices();

            // Normally, listening will begin after the
            // utterance completes.  As a fallback in case the utterance
            // does not complete, post a delayed runnable to begin
            // listening.
            mFallbackRunnable = new OnTtsCompletionRunnable(true);
            mHandler.postDelayed(mFallbackRunnable, MAX_TTS_DELAY);
        } else {
            // We are not running in bluetooth mode, so all
            // we need to do is wait for the user to select
            // a choice from the alert dialog.  We will wait
            // indefinitely for this.
            mState = WAITING_FOR_DIALOG_CHOICE;
        }
    }

    private class CommandRecognizerClient implements RecognizerClient {
         static final int MIN_VOLUME_TO_SKIP = 2;
       /**
         * Called by the {@link RecognizerEngine} when the microphone is started.
         */
        public void onMicrophoneStart(InputStream mic) {
            if (false) Log.d(TAG, "onMicrophoneStart");

           if (!mUsingBluetooth) {
               playSound(ToneGenerator.TONE_PROP_BEEP);

                int ringVolume = mAudioManager.getStreamVolume(
                        AudioManager.STREAM_RING);
                Log.d(TAG, "ringVolume " + ringVolume);

                if (ringVolume >= MIN_VOLUME_TO_SKIP) {
                    // now we're playing a sound, and corrupting the input sample.
                    // So we need to pull that junk off of the input stream so that the
                    // recognizer won't see it.
                    try {
                        skipBeep(mic);
                    } catch (java.io.IOException e) {
                        Log.e(TAG, "IOException " + e);
                    }
                } else {
                    if (false) Log.d(TAG, "no tone");
                }
            }

            mHandler.post(new Runnable() {
                public void run() {
                    findViewById(R.id.retry_view).setVisibility(View.INVISIBLE);
                    findViewById(R.id.microphone_loading_view).setVisibility(
                            View.INVISIBLE);
                    ((TextView)findViewById(R.id.state)).setText(R.string.listening);
                    mHandler.post(mMicFlasher);
                }
            });
        }

        /**
         *  Beep detection
         */
        private static final int START_WINDOW_MS = 500;  // Beep detection window duration in ms
        private static final int SINE_FREQ = 400;        // base sine frequency on beep
        private static final int NUM_PERIODS_BLOCK = 10; // number of sine periods in one energy averaging block
        private static final int THRESHOLD = 8;          // absolute pseudo energy threshold
        private static final int START = 0;              // beep detection start
        private static final int RISING = 1;             // beep rising edge start
        private static final int TOP = 2;                // beep constant energy detected

        void skipBeep(InputStream is) throws IOException {
            int sampleCount = ((mSampleRate / SINE_FREQ) * NUM_PERIODS_BLOCK);
            int blockSize = 2 * sampleCount; // energy averaging block

            if (is == null || blockSize == 0) {
                return;
            }

            byte[] buf = new byte[blockSize];
            int maxBytes = 2 * ((START_WINDOW_MS * mSampleRate) / 1000);
            maxBytes = ((maxBytes-1) / blockSize + 1) * blockSize;

            int count = 0;
            int state = START;  // detection state
            long prevE = 0; // previous pseudo energy
            long peak = 0;
            int threshold =  THRESHOLD*sampleCount;  // absolute energy threshold
            Log.d(TAG, "blockSize " + blockSize);

            while (count < maxBytes) {
                int cnt = 0;
                while (cnt < blockSize) {
                    int n = is.read(buf, cnt, blockSize-cnt);
                    if (n < 0) {
                        throw new java.io.IOException();
                    }
                    cnt += n;
                }

                // compute pseudo energy
                cnt = blockSize;
                long sumx = 0;
                long sumxx = 0;
                while (cnt >= 2) {
                    short smp = (short)((buf[cnt - 1] << 8) + (buf[cnt - 2] & 0xFF));
                    sumx += smp;
                    sumxx += smp*smp;
                    cnt -= 2;
                }
                long energy = (sumxx*sampleCount - sumx*sumx)/(sampleCount*sampleCount);
                Log.d(TAG, "sumx " + sumx + " sumxx " + sumxx + " ee " + energy);

                switch (state) {
                    case START:
                        if (energy > threshold && energy > (prevE * 2) && prevE != 0) {
                            // rising edge if energy doubled and > abs threshold
                            state = RISING;
                            if (false) Log.d(TAG, "start RISING: " + count +" time: "+ (((1000*count)/2)/mSampleRate));
                        }
                        break;
                    case RISING:
                        if (energy < threshold || energy < (prevE / 2)){
                            // energy fell back below half of previous, back to start
                            if (false) Log.d(TAG, "back to START: " + count +" time: "+ (((1000*count)/2)/mSampleRate));
                            peak = 0;
                            state = START;
                        } else if (energy > (prevE / 2) && energy < (prevE * 2)) {
                            // Start of constant energy
                            if (false) Log.d(TAG, "start TOP: " + count +" time: "+ (((1000*count)/2)/mSampleRate));
                            if (peak < energy) {
                                peak = energy;
                            }
                            state = TOP;
                        }
                        break;
                    case TOP:
                        if (energy < threshold || energy < (peak / 2)) {
                            // e went to less than half of the peak
                            if (false) Log.d(TAG, "end TOP: " + count +" time: "+ (((1000*count)/2)/mSampleRate));
                            return;
                        }
                        break;
                    }
                prevE = energy;
                count += blockSize;
            }
            if (false) Log.d(TAG, "no beep detected, timed out");
        }

        /**
         * Called by the {@link RecognizerEngine} if the recognizer fails.
         */
        public void onRecognitionFailure(final String msg) {
            if (false) Log.d(TAG, "onRecognitionFailure " + msg);
            // we had zero results.  Just try again.
            askToTryAgain();
        }

        /**
         * Called by the {@link RecognizerEngine} on an internal error.
         */
        public void onRecognitionError(final String msg) {
            if (false) Log.d(TAG, "onRecognitionError " + msg);
            mHandler.post(new ErrorRunnable(R.string.recognition_error));
            exitActivity();
        }

        /**
         * Called by the {@link RecognizerEngine} when is succeeds.  If there is
         * only one item, then the Intent is dispatched immediately.
         * If there are more, then an AlertDialog is displayed and the user is
         * prompted to select.
         * @param intents a list of Intents corresponding to the sentences.
         */
        public void onRecognitionSuccess(final Intent[] intents) {
            if (false) Log.d(TAG, "CommandRecognizerClient onRecognitionSuccess " +
                    intents.length);
            if (mState != WAITING_FOR_COMMAND) {
                if (false) Log.d(TAG, "not waiting for command, ignoring");
                return;
            }

            // store the intents in a member variable so that we can access it
            // later when the user chooses which action to perform.
            mAvailableChoices = intents;

            mHandler.post(new Runnable() {
                public void run() {
                    if (!mUsingBluetooth) {
                        playSound(ToneGenerator.TONE_PROP_ACK);
                    }
                    mHandler.removeCallbacks(mMicFlasher);

                    String[] sentences = new String[intents.length];
                    for (int i = 0; i < intents.length; i++) {
                        sentences[i] = intents[i].getStringExtra(
                                RecognizerEngine.SENTENCE_EXTRA);
                    }

                    if (intents.length == 0) {
                        onRecognitionFailure("zero intents");
                        return;
                    }

                    if (intents.length > 0) {
                        // see if we the response was "exit" or "cancel".
                        String value = intents[0].getStringExtra(
                            RecognizerEngine.SEMANTIC_EXTRA);
                        if (false) Log.d(TAG, "value " + value);
                        if ("X".equals(value)) {
                            exitActivity();
                            return;
                        }
                    }

                    if (mUsingBluetooth &&
                            (intents.length == 1 ||
                             !Intent.ACTION_CALL_PRIVILEGED.equals(
                                    intents[0].getAction()))) {
                        // When we're running in bluetooth mode, we expect
                        // that the user is not looking at the screen and cannot
                        // interact with the device in any way besides voice
                        // commands.  In this case we need to minimize how many
                        // interactions the user has to perform in order to call
                        // someone.
                        // So if there is only one match, instead of making the
                        // user confirm, we just assume it's correct, speak
                        // the choice over TTS, and then dispatch it.
                        // If there are multiple matches for some intent type
                        // besides "call", it's too difficult for the user to
                        // explain which one they meant, so we just take the highest
                        // confidence match and dispatch that.

                        // Speak the sentence for the action we are about
                        // to dispatch so that the user knows what is happening.
                        mChosenAction = intents[0];
                        performChoice();

                        return;
                    } else {
                        // Either we are not running in bluetooth mode,
                        // or we had multiple matches.  Either way, we need
                        // the user to confirm the choice.
                        // Put up a dialog from which the user can select
                        // his/her choice.
                        DialogInterface.OnCancelListener cancelListener =
                            new DialogInterface.OnCancelListener() {

                            public void onCancel(DialogInterface dialog) {
                                if (false) {
                                    Log.d(TAG, "cancelListener.onCancel");
                                }
                                dialog.dismiss();
                                finish();
                            }
                       };

                        DialogInterface.OnClickListener clickListener =
                            new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                if (false) {
                                    Log.d(TAG, "clickListener.onClick " + which);
                                }
                                startActivityHelp(intents[which]);
                                dialog.dismiss();
                                finish();
                            }
                        };

                        DialogInterface.OnClickListener negativeListener =
                            new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                if (false) {
                                    Log.d(TAG, "negativeListener.onClick " +
                                        which);
                                }
                                dialog.dismiss();
                                finish();
                            }
                        };

                        mAlertDialog =
                                new AlertDialog.Builder(VoiceDialerActivity.this,
                                        AlertDialog.THEME_HOLO_DARK)
                                .setTitle(R.string.title)
                                .setItems(sentences, clickListener)
                                .setOnCancelListener(cancelListener)
                                .setNegativeButton(android.R.string.cancel,
                                        negativeListener)
                                .show();

                        waitForChoice();
                    }
                }
            });
        }
    }

    private class ChoiceRecognizerClient implements RecognizerClient {
        public void onRecognitionSuccess(final Intent[] intents) {
            if (false) Log.d(TAG, "ChoiceRecognizerClient onRecognitionSuccess");
            if (mState != WAITING_FOR_CHOICE) {
                if (false) Log.d(TAG, "not waiting for choice, ignoring");
                return;
            }

            if (mAlertDialog != null) {
                mAlertDialog.dismiss();
            }

            // disregard all but the first intent.
            if (intents.length > 0) {
                String value = intents[0].getStringExtra(
                    RecognizerEngine.SEMANTIC_EXTRA);
                if (false) Log.d(TAG, "value " + value);
                if ("R".equals(value)) {
                    if (mUsingBluetooth) {
                        mHandler.post(new GreetingRunnable());
                    } else {
                        listenForCommand();
                    }
                } else if ("X".equals(value)) {
                    exitActivity();
                } else {
                    // it's a phone type response
                    mChosenAction = null;
                    for (int i = 0; i < mAvailableChoices.length; i++) {
                        if (value.equalsIgnoreCase(
                                mAvailableChoices[i].getStringExtra(
                                        CommandRecognizerEngine.PHONE_TYPE_EXTRA))) {
                            mChosenAction = mAvailableChoices[i];
                        }
                    }

                    if (mChosenAction != null) {
                        performChoice();
                    } else {
                        // invalid choice
                        if (false) Log.d(TAG, "invalid choice" + value);

                        if (mUsingBluetooth) {
                            mTtsParams.remove(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                            mTts.speak(getString(R.string.invalid_choice_tts),
                                TextToSpeech.QUEUE_FLUSH,
                                mTtsParams);
                        }
                        waitForChoice();
                    }
                }
            }
        }

        public void onRecognitionFailure(String msg) {
            if (false) Log.d(TAG, "ChoiceRecognizerClient onRecognitionFailure");
            exitActivity();
        }

        public void onRecognitionError(String err) {
            if (false) Log.d(TAG, "ChoiceRecognizerClient onRecognitionError");
            mHandler.post(new ErrorRunnable(R.string.recognition_error));
            exitActivity();
        }

        public void onMicrophoneStart(InputStream mic) {
            if (false) Log.d(TAG, "ChoiceRecognizerClient onMicrophoneStart");
        }
    }

    private void speakChoices() {
        if (false) Log.d(TAG, "speakChoices");
        mState = SPEAKING_CHOICES;

        String sentenceSpoken = spaceOutDigits(
                mAvailableChoices[0].getStringExtra(
                    RecognizerEngine.SENTENCE_EXTRA));

        // When we have multiple choices, they will be of the form
        // "call jack jones at home", "call jack jones on mobile".
        // Speak the entire first sentence, then the last word from each
        // of the remaining sentences.  This will come out to something
        // like "call jack jones at home mobile or work".
        StringBuilder builder = new StringBuilder();
        builder.append(sentenceSpoken);

        int count = mAvailableChoices.length;
        for (int i=1; i < count; i++) {
            if (i == count-1) {
                builder.append(" or ");
            } else {
                builder.append(" ");
            }
            String tmpSentence = mAvailableChoices[i].getStringExtra(
                    RecognizerEngine.SENTENCE_EXTRA);
            String[] words = tmpSentence.trim().split(" ");
            builder.append(words[words.length-1]);
        }
        mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                CHOICES_UTTERANCE);
        mTts.speak(builder.toString(),
            TextToSpeech.QUEUE_ADD,
            mTtsParams);
    }


    private static String spaceOutDigits(String sentenceDisplay) {
        // if we have a sentence of the form "dial 123 456 7890",
        // we need to insert a space between each digit, otherwise
        // the TTS engine will say "dial one hundred twenty three...."
        // When there already is a space, we also insert a comma,
        // so that it pauses between sections.  For the displayable
        // sentence "dial 123 456 7890" it will speak
        // "dial 1 2 3, 4 5 6, 7 8 9 0"
        char buffer[] = sentenceDisplay.toCharArray();
        StringBuilder builder = new StringBuilder();
        boolean buildingNumber = false;
        int l = sentenceDisplay.length();
        for (int index = 0; index < l; index++) {
            char c = buffer[index];
            if (Character.isDigit(c)) {
                if (buildingNumber) {
                    builder.append(" ");
                }
                buildingNumber = true;
                builder.append(c);
            } else if (c == ' ') {
                if (buildingNumber) {
                    builder.append(",");
                } else {
                    builder.append(" ");
                }
            } else {
                buildingNumber = false;
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private void startActivityHelp(Intent intent) {
        startActivity(intent);
    }

    private void listenForCommand() {
        if (false) Log.d(TAG, ""
                + "Command(): MICROPHONE_EXTRA: "+getArg(MICROPHONE_EXTRA)+
                ", CONTACTS_EXTRA: "+getArg(CONTACTS_EXTRA));

        mState = WAITING_FOR_COMMAND;
        mRecognizerThread = new Thread() {
            public void run() {
                mCommandEngine.recognize(mCommandClient,
                        VoiceDialerActivity.this,
                        newFile(getArg(MICROPHONE_EXTRA)),
                        mSampleRate);
            }
        };
        mRecognizerThread.start();
    }

    private void listenForChoice() {
        if (false) Log.d(TAG, "listenForChoice(): MICROPHONE_EXTRA: " +
                getArg(MICROPHONE_EXTRA));

        mState = WAITING_FOR_CHOICE;
        mRecognizerThread = new Thread() {
            public void run() {
                mPhoneTypeChoiceEngine.recognize(mChoiceClient,
                        VoiceDialerActivity.this,
                        newFile(getArg(MICROPHONE_EXTRA)), mSampleRate);
            }
        };
        mRecognizerThread.start();
    }

    private void exitActivity() {
        synchronized(this) {
            if (mState != EXITING) {
                if (false) Log.d(TAG, "exitActivity");
                mState = SPEAKING_GOODBYE;
                if (mUsingBluetooth) {
                    mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                            GOODBYE_UTTERANCE);
                    mTts.speak(getString(R.string.goodbye_tts),
                        TextToSpeech.QUEUE_FLUSH,
                        mTtsParams);
                    // Normally, the activity will finish() after the
                    // utterance completes.  As a fallback in case the utterance
                    // does not complete, post a delayed runnable finish the
                    // activity.
                    mFallbackRunnable = new OnTtsCompletionRunnable(true);
                    mHandler.postDelayed(mFallbackRunnable, MAX_TTS_DELAY);
                } else {
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, EXIT_DELAY);
                }
            }
        }
    }

    private String getArg(String name) {
        if (name == null) return null;
        String arg = getIntent().getStringExtra(name);
        if (arg != null) return arg;
        arg = SystemProperties.get("app.voicedialer." + name);
        return arg != null && arg.length() > 0 ? arg : null;
    }

    private static File newFile(String name) {
        return name != null ? new File(name) : null;
    }

    private int playSound(int toneType) {
        int msecDelay = 1;

        // use the MediaPlayer to prompt the user
        if (mToneGenerator != null) {
            mToneGenerator.startTone(toneType);
            msecDelay = StrictMath.max(msecDelay, 300);
        }
        // use the Vibrator to prompt the user
        if (mAudioManager != null &&
                mAudioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_RINGER)) {
            final int VIBRATOR_TIME = 150;
            final int VIBRATOR_GUARD_TIME = 150;
            Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATOR_TIME);
            msecDelay = StrictMath.max(msecDelay,
                    VIBRATOR_TIME + VIBRATOR_GUARD_TIME);
        }


        return msecDelay;
    }

    protected void onDestroy() {
        synchronized(this) {
            mState = EXITING;
        }

        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }

        // set the volume back to the level it was before we started.
        mAudioManager.setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO,
                                      mBluetoothVoiceVolume, 0);
        mAudioManager.abandonAudioFocus(null);

        // shut down bluetooth, if it exists
        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.stopVoiceRecognition(mBluetoothDevice);
            mAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
            mBluetoothHeadset = null;
        }

        // shut down recognizer and wait for the thread to complete
        if (mRecognizerThread !=  null) {
            mRecognizerThread.interrupt();
            try {
                mRecognizerThread.join();
            } catch (InterruptedException e) {
                if (false) Log.d(TAG, "onStop mRecognizerThread.join exception " + e);
            }
            mRecognizerThread = null;
        }

        // clean up UI
        mHandler.removeCallbacks(mMicFlasher);
        mHandler.removeMessages(0);

        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
            mTts = null;
        }
        unregisterReceiver(mReceiver);

        super.onDestroy();

        releaseWakeLock();
    }

    private void acquireWakeLock(Context context) {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                       "VoiceDialer");
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private Runnable mMicFlasher = new Runnable() {
        int visible = View.VISIBLE;

        public void run() {
            findViewById(R.id.microphone_view).setVisibility(visible);
            findViewById(R.id.state).setVisibility(visible);
            visible = visible == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
            mHandler.postDelayed(this, 750);
        }
    };
}
