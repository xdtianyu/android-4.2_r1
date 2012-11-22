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

package com.android.testingcamera;

import android.app.Activity;
import android.app.FragmentManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple test application for the camera API.
 *
 * The goal of this application is to allow all camera API features to be
 * exercised, and all information provided by the API to be shown.
 */
public class TestingCamera extends Activity implements SurfaceHolder.Callback {

    /** UI elements */
    private SurfaceView mPreviewView;
    private SurfaceHolder mPreviewHolder;

    private Spinner mCameraSpinner;
    private Button mInfoButton;
    private Spinner mPreviewSizeSpinner;
    private ToggleButton mPreviewToggle;
    private Spinner mAutofocusModeSpinner;
    private Button mAutofocusButton;
    private Button mCancelAutofocusButton;
    private Spinner mSnapshotSizeSpinner;
    private Button  mTakePictureButton;
    private Spinner mCamcorderProfileSpinner;
    private ToggleButton mRecordToggle;

    private TextView mLogView;

    private Set<View> mPreviewOnlyControls = new HashSet<View>();

    /** Camera state */
    private int mCameraId = 0;
    private Camera mCamera;
    private Camera.Parameters mParams;
    private List<Camera.Size> mPreviewSizes;
    private int mPreviewSize = 0;
    private List<String> mAfModes;
    private int mAfMode = 0;
    private List<Camera.Size> mSnapshotSizes;
    private int mSnapshotSize = 0;
    private List<CamcorderProfile> mCamcorderProfiles;
    private int mCamcorderProfile = 0;

    private MediaRecorder mRecorder;
    private File mRecordingFile;

    private static final int CAMERA_UNINITIALIZED = 0;
    private static final int CAMERA_OPEN = 1;
    private static final int CAMERA_PREVIEW = 2;
    private static final int CAMERA_TAKE_PICTURE = 3;
    private static final int CAMERA_RECORD = 4;
    private int mState = CAMERA_UNINITIALIZED;

    /** Misc variables */

    private static final String TAG = "TestingCamera";

    /** Activity lifecycle */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mPreviewView = (SurfaceView)findViewById(R.id.preview);
        mPreviewView.getHolder().addCallback(this);

        mCameraSpinner = (Spinner) findViewById(R.id.camera_spinner);
        mCameraSpinner.setOnItemSelectedListener(mCameraSpinnerListener);

        mInfoButton = (Button) findViewById(R.id.info_button);
        mInfoButton.setOnClickListener(mInfoButtonListener);

        mPreviewSizeSpinner = (Spinner) findViewById(R.id.preview_size_spinner);
        mPreviewSizeSpinner.setOnItemSelectedListener(mPreviewSizeListener);

        mPreviewToggle = (ToggleButton) findViewById(R.id.start_preview);
        mPreviewToggle.setOnClickListener(mPreviewToggleListener);

        mAutofocusModeSpinner = (Spinner) findViewById(R.id.af_mode_spinner);
        mAutofocusModeSpinner.setOnItemSelectedListener(mAutofocusModeListener);

        mAutofocusButton = (Button) findViewById(R.id.af_button);
        mAutofocusButton.setOnClickListener(mAutofocusButtonListener);
        mPreviewOnlyControls.add(mAutofocusButton);

        mCancelAutofocusButton = (Button) findViewById(R.id.af_cancel_button);
        mCancelAutofocusButton.setOnClickListener(mCancelAutofocusButtonListener);
        mPreviewOnlyControls.add(mCancelAutofocusButton);

        mSnapshotSizeSpinner = (Spinner) findViewById(R.id.snapshot_size_spinner);
        mSnapshotSizeSpinner.setOnItemSelectedListener(mSnapshotSizeListener);

        mTakePictureButton = (Button) findViewById(R.id.take_picture);
        mTakePictureButton.setOnClickListener(mTakePictureListener);
        mPreviewOnlyControls.add(mTakePictureButton);

        mCamcorderProfileSpinner = (Spinner) findViewById(R.id.camcorder_profile_spinner);
        mCamcorderProfileSpinner.setOnItemSelectedListener(mCamcorderProfileListener);

        mRecordToggle = (ToggleButton) findViewById(R.id.start_record);
        mRecordToggle.setOnClickListener(mRecordToggleListener);
        mPreviewOnlyControls.add(mRecordToggle);

        mLogView = (TextView) findViewById(R.id.log);
        mLogView.setMovementMethod(new ScrollingMovementMethod());

        int numCameras = Camera.getNumberOfCameras();
        String[] cameraNames = new String[numCameras];
        for (int i = 0; i < numCameras; i++) {
            cameraNames[i] = "Camera " + i;
        }

        mCameraSpinner.setAdapter(
                new ArrayAdapter<String>(this,
                        R.layout.spinner_item, cameraNames));
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume: Setting up camera");
        mPreviewHolder = null;
        setUpCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        log("onPause: Releasing camera");
        mCamera.release();
        mState = CAMERA_UNINITIALIZED;
    }

    /** SurfaceHolder.Callback methods */
    public void surfaceChanged(SurfaceHolder holder,
            int format,
            int width,
            int height) {
        if (mPreviewHolder != null) return;

        log("Surface holder available: " + width + " x " + height);
        mPreviewHolder = holder;
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            logE("Unable to set up preview!");
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mPreviewHolder = null;
    }

    /** UI controls enable/disable */
    private void enablePreviewOnlyControls(boolean enabled) {
        for (View v : mPreviewOnlyControls) {
                v.setEnabled(enabled);
        }
    }

    /** UI listeners */

    private AdapterView.OnItemSelectedListener mCameraSpinnerListener =
                new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent,
                        View view, int pos, long id) {
            if (mCameraId != pos) {
                mCameraId = pos;
                setUpCamera();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private OnClickListener mInfoButtonListener = new OnClickListener() {
        public void onClick(View v) {
            FragmentManager fm = getFragmentManager();
            InfoDialogFragment infoDialog = new InfoDialogFragment();
            infoDialog.updateInfo(mCameraId, mCamera);
            infoDialog.show(fm, "info_dialog_fragment");
        }
    };

    private AdapterView.OnItemSelectedListener mPreviewSizeListener =
        new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent,
                View view, int pos, long id) {
            if (pos == mPreviewSize) return;
            if (mState == CAMERA_PREVIEW) {
                log("Stopping preview to switch resolutions");
                mCamera.stopPreview();
            }

            mPreviewSize = pos;
            int width = mPreviewSizes.get(mPreviewSize).width;
            int height = mPreviewSizes.get(mPreviewSize).height;
            mParams.setPreviewSize(width, height);

            log("Setting preview size to " + width + "x" + height);

            mCamera.setParameters(mParams);

            if (mState == CAMERA_PREVIEW) {
                log("Restarting preview");
                resizePreview(width, height);
                mCamera.startPreview();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private View.OnClickListener mPreviewToggleListener =
            new View.OnClickListener() {
        public void onClick(View v) {
            if (mState == CAMERA_TAKE_PICTURE) {
                logE("Can't change preview state while taking picture!");
                return;
            }
            if (mPreviewToggle.isChecked()) {
                log("Starting preview");
                resizePreview(mPreviewSizes.get(mPreviewSize).width,
                        mPreviewSizes.get(mPreviewSize).height);
                mCamera.startPreview();
                mState = CAMERA_PREVIEW;
                enablePreviewOnlyControls(true);
            } else {
                log("Stopping preview");
                mCamera.stopPreview();
                mState = CAMERA_OPEN;

                enablePreviewOnlyControls(false);
            }
        }
    };

    private OnItemSelectedListener mAutofocusModeListener =
                new OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent,
                        View view, int pos, long id) {
            if (pos == mAfMode) return;

            mAfMode = pos;
            String focusMode = mAfModes.get(mAfMode);
            log("Setting focus mode to " + focusMode);
            if (focusMode == Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE ||
                        focusMode == Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) {
                mCamera.setAutoFocusMoveCallback(mAutofocusMoveCallback);
            }
            mParams.setFocusMode(focusMode);

            mCamera.setParameters(mParams);
        }

        public void onNothingSelected(AdapterView<?> arg0) {

        }
    };

    private OnClickListener mAutofocusButtonListener =
            new View.OnClickListener() {
        public void onClick(View v) {
            log("Triggering autofocus");
            mCamera.autoFocus(mAutofocusCallback);
        }
    };

    private OnClickListener mCancelAutofocusButtonListener =
            new View.OnClickListener() {
        public void onClick(View v) {
            log("Cancelling autofocus");
            mCamera.cancelAutoFocus();
        }
    };

    private Camera.AutoFocusCallback mAutofocusCallback =
            new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            log("Autofocus completed: " + (success ? "success" : "failure") );
        }
    };

    private Camera.AutoFocusMoveCallback mAutofocusMoveCallback =
            new Camera.AutoFocusMoveCallback() {
        public void onAutoFocusMoving(boolean start, Camera camera) {
            log("Autofocus movement: " + (start ? "starting" : "stopped") );
        }
    };

    private AdapterView.OnItemSelectedListener mSnapshotSizeListener =
            new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent,
                View view, int pos, long id) {
            if (pos == mSnapshotSize) return;

            mSnapshotSize = pos;
            int width = mSnapshotSizes.get(mSnapshotSize).width;
            int height = mSnapshotSizes.get(mSnapshotSize).height;
            log("Setting snapshot size to " + width + " x " + height);

            mParams.setPictureSize(width, height);

            mCamera.setParameters(mParams);
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private View.OnClickListener mTakePictureListener =
            new View.OnClickListener() {
        public void onClick(View v) {
            log("Taking picture");
            if (mState == CAMERA_PREVIEW) {
                mState = CAMERA_TAKE_PICTURE;
                enablePreviewOnlyControls(false);
                mPreviewToggle.setChecked(false);

                mCamera.takePicture(mShutterCb, mRawCb, mPostviewCb, mJpegCb);
            } else {
                logE("Can't take picture while not running preview!");
            }
        }
    };

    private AdapterView.OnItemSelectedListener mCamcorderProfileListener =
                new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent,
                        View view, int pos, long id) {
            if (pos == mCamcorderProfile) return;

            log("Setting camcorder profile to " + ((TextView)view).getText());
            mCamcorderProfile = pos;
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private View.OnClickListener mRecordToggleListener =
            new View.OnClickListener() {
        public void onClick(View v) {
            mPreviewToggle.setEnabled(false);
            if (mState == CAMERA_PREVIEW) {
                startRecording();
            } else if (mState == CAMERA_RECORD) {
                stopRecording(false);
            } else {
                logE("Can't toggle recording in current state!");
            }
            mPreviewToggle.setEnabled(true);
        }
    };

    private Camera.ShutterCallback mShutterCb = new Camera.ShutterCallback() {
        public void onShutter() {
            log("Shutter callback received");
        }
    };

    private Camera.PictureCallback mRawCb = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            log("Raw callback received");
        }
    };

    private Camera.PictureCallback mPostviewCb = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            log("Postview callback received");
        }
    };

    private Camera.PictureCallback mJpegCb = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            log("JPEG picture callback received");
            FragmentManager fm = getFragmentManager();
            SnapshotDialogFragment snapshotDialog = new SnapshotDialogFragment();

            snapshotDialog.updateImage(data);
            snapshotDialog.show(fm, "snapshot_dialog_fragment");

            mPreviewToggle.setEnabled(true);

            mState = CAMERA_OPEN;
        }
    };

    // Internal methods

    void setUpCamera() {
        log("Setting up camera " + mCameraId);
        logIndent(1);
        if (mState >= CAMERA_OPEN) {
            log("Closing old camera");
            mCamera.release();
            mState = CAMERA_UNINITIALIZED;
        }
        log("Opening camera " + mCameraId);
        mCamera = Camera.open(mCameraId);
        mState = CAMERA_OPEN;

        mParams = mCamera.getParameters();

        // Set up preview size selection

        log("Configuring camera");
        logIndent(1);

        updatePreviewSizes(mParams);
        updateAfModes(mParams);
        updateSnapshotSizes(mParams);
        updateCamcorderProfile(mCameraId);

        // Update parameters based on above updates
        mCamera.setParameters(mParams);

        if (mPreviewHolder != null) {
            log("Setting preview display");
            try {
                mCamera.setPreviewDisplay(mPreviewHolder);
            } catch(IOException e) {
                Log.e(TAG, "Unable to set up preview!");
            }
        }

        logIndent(-1);

        mPreviewToggle.setEnabled(true);
        mPreviewToggle.setChecked(false);
        enablePreviewOnlyControls(false);

        int width = mPreviewSizes.get(mPreviewSize).width;
        int height = mPreviewSizes.get(mPreviewSize).height;
        resizePreview(width, height);
        if (mPreviewToggle.isChecked()) {
            log("Starting preview" );
            mCamera.startPreview();
            mState = CAMERA_PREVIEW;
        } else {
            mState = CAMERA_OPEN;
        }
        logIndent(-1);
    }

    private void updateAfModes(Parameters params) {
        mAfModes = params.getSupportedFocusModes();

        mAutofocusModeSpinner.setAdapter(
                new ArrayAdapter<String>(this, R.layout.spinner_item,
                        mAfModes.toArray(new String[0])));

        mAfMode = 0;

        params.setFocusMode(mAfModes.get(mAfMode));

        log("Setting AF mode to " + mAfModes.get(mAfMode));
    }

    private void updatePreviewSizes(Camera.Parameters params) {
        mPreviewSizes = params.getSupportedPreviewSizes();

        String[] availableSizeNames = new String[mPreviewSizes.size()];
        int i = 0;
        for (Camera.Size previewSize: mPreviewSizes) {
            availableSizeNames[i++] =
                Integer.toString(previewSize.width) + " x " +
                Integer.toString(previewSize.height);
        }
        mPreviewSizeSpinner.setAdapter(
                new ArrayAdapter<String>(
                        this, R.layout.spinner_item, availableSizeNames));

        mPreviewSize = 0;

        int width = mPreviewSizes.get(mPreviewSize).width;
        int height = mPreviewSizes.get(mPreviewSize).height;
        params.setPreviewSize(width, height);
        log("Setting preview size to " + width + " x " + height);
    }

    private void updateSnapshotSizes(Camera.Parameters params) {
        String[] availableSizeNames;
        mSnapshotSizes = params.getSupportedPictureSizes();

        availableSizeNames = new String[mSnapshotSizes.size()];
        int i = 0;
        for (Camera.Size snapshotSize : mSnapshotSizes) {
            availableSizeNames[i++] =
                Integer.toString(snapshotSize.width) + " x " +
                Integer.toString(snapshotSize.height);
        }
        mSnapshotSizeSpinner.setAdapter(
                new ArrayAdapter<String>(
                        this, R.layout.spinner_item, availableSizeNames));

        mSnapshotSize = 0;

        int snapshotWidth = mSnapshotSizes.get(mSnapshotSize).width;
        int snapshotHeight = mSnapshotSizes.get(mSnapshotSize).height;
        params.setPictureSize(snapshotWidth, snapshotHeight);
        log("Setting snapshot size to " + snapshotWidth + " x " + snapshotHeight);
    }

    private void updateCamcorderProfile(int cameraId) {
        // Have to query all of these individually,
        final int PROFILES[] = new int[] {
            CamcorderProfile.QUALITY_1080P,
            CamcorderProfile.QUALITY_480P,
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_CIF,
            CamcorderProfile.QUALITY_HIGH,
            CamcorderProfile.QUALITY_LOW,
            CamcorderProfile.QUALITY_QCIF,
            CamcorderProfile.QUALITY_QVGA,
            CamcorderProfile.QUALITY_TIME_LAPSE_1080P,
            CamcorderProfile.QUALITY_TIME_LAPSE_480P,
            CamcorderProfile.QUALITY_TIME_LAPSE_720P,
            CamcorderProfile.QUALITY_TIME_LAPSE_CIF,
            CamcorderProfile.QUALITY_TIME_LAPSE_HIGH,
            CamcorderProfile.QUALITY_TIME_LAPSE_LOW,
            CamcorderProfile.QUALITY_TIME_LAPSE_QCIF,
            CamcorderProfile.QUALITY_TIME_LAPSE_QVGA
        };

        final String PROFILE_NAMES[] = new String[] {
            "1080P",
            "480P",
            "720P",
            "CIF",
            "HIGH",
            "LOW",
            "QCIF",
            "QVGA",
            "TIME_LAPSE_1080P",
            "TIME_LAPSE_480P",
            "TIME_LAPSE_720P",
            "TIME_LAPSE_CIF",
            "TIME_LAPSE_HIGH",
            "TIME_LAPSE_LOW",
            "TIME_LAPSE_QCIF",
            "TIME_LAPSE_QVGA"
        };

        List<String> availableCamcorderProfileNames = new ArrayList<String>();
        mCamcorderProfiles = new ArrayList<CamcorderProfile>();

        for (int i = 0; i < PROFILES.length; i++) {
            if (CamcorderProfile.hasProfile(cameraId, PROFILES[i])) {
                availableCamcorderProfileNames.add(PROFILE_NAMES[i]);
                mCamcorderProfiles.add(CamcorderProfile.get(cameraId, PROFILES[i]));
            }
        }
        String[] nameArray = (String[])availableCamcorderProfileNames.toArray(new String[0]);
        mCamcorderProfileSpinner.setAdapter(
                new ArrayAdapter<String>(
                        this, R.layout.spinner_item, nameArray));

        mCamcorderProfile = 0;
        log("Setting camcorder profile to " + nameArray[mCamcorderProfile]);

    }

    void resizePreview(int width, int height) {
        if (mPreviewHolder != null) {
            int viewHeight = mPreviewView.getHeight();
            int viewWidth = (int)(((double)width)/height * viewHeight);

            mPreviewView.setLayoutParams(
                new LayoutParams(viewWidth, viewHeight));
        }

    }

    static final int MEDIA_TYPE_IMAGE = 0;
    static final int MEDIA_TYPE_VIDEO = 1;
    File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
                return null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_DCIM), "TestingCamera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                logE("Failed to create directory for pictures/video");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    void notifyMediaScannerOfFile(File newFile,
                final MediaScannerConnection.OnScanCompletedListener listener) {
        final Handler h = new Handler();
        MediaScannerConnection.scanFile(this,
                new String[] { newFile.toString() },
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(final String path, final Uri uri) {
                        h.post(new Runnable() {
                            public void run() {
                                log("MediaScanner notified: " +
                                        path + " -> " + uri);
                                if (listener != null)
                                    listener.onScanCompleted(path, uri);
                            }
                        });
                    }
                });
    }

    private void deleteFile(File badFile) {
        if (badFile.exists()) {
            boolean success = badFile.delete();
            if (success) log("Deleted file " + badFile.toString());
            else log("Unable to delete file " + badFile.toString());
        }
    }

    private void startRecording() {
        log("Starting recording");
        logIndent(1);
        log("Configuring MediaRecoder");
        mCamera.unlock();
        if (mRecorder != null) {
            mRecorder.release();
        }
        mRecorder = new MediaRecorder();
        mRecorder.setOnErrorListener(mRecordingErrorListener);
        mRecorder.setOnInfoListener(mRecordingInfoListener);
        mRecorder.setCamera(mCamera);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mRecorder.setProfile(mCamcorderProfiles.get(mCamcorderProfile));
        File outputFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        log("File name:" + outputFile.toString());
        mRecorder.setOutputFile(outputFile.toString());

        boolean ready = false;
        log("Preparing MediaRecorder");
        try {
            mRecorder.prepare();
            ready = true;
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            logE("Exception preparing MediaRecorder:\n" + writer.toString());
        }

        if (ready) {
            try {
                log("Starting MediaRecorder");
                mRecorder.start();
                mState = CAMERA_RECORD;
                log("Recording active");
                mRecordingFile = outputFile;
            } catch (Exception e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                logE("Exception starting MediaRecorder:\n" + writer.toString());
            }
        } else {
            mPreviewToggle.setChecked(false);
        }
        logIndent(-1);
    }

    private MediaRecorder.OnErrorListener mRecordingErrorListener =
            new MediaRecorder.OnErrorListener() {
        public void onError(MediaRecorder mr, int what, int extra) {
            logE("MediaRecorder reports error: " + what + ", extra "
                    + extra);
            if (mState == CAMERA_RECORD) {
                stopRecording(true);
            }
        }
    };

    private MediaRecorder.OnInfoListener mRecordingInfoListener =
            new MediaRecorder.OnInfoListener() {
        public void onInfo(MediaRecorder mr, int what, int extra) {
            log("MediaRecorder reports info: " + what + ", extra "
                    + extra);
        }
    };

    private void stopRecording(boolean error) {
        log("Stopping recording");
        if (mRecorder != null) {
            mRecorder.stop();
            mCamera.lock();
            mState = CAMERA_PREVIEW;
            if (!error) {
                notifyMediaScannerOfFile(mRecordingFile, null);
            } else {
                deleteFile(mRecordingFile);
            }
            mRecordingFile = null;
        } else {
            logE("Recorder is unexpectedly null!");
        }
    }

    private int mLogIndentLevel = 0;
    private String mLogIndent = "\t";
    /** Increment or decrement log indentation level */
    synchronized void logIndent(int delta) {
        mLogIndentLevel += delta;
        if (mLogIndentLevel < 0) mLogIndentLevel = 0;
        char[] mLogIndentArray = new char[mLogIndentLevel + 1];
        for (int i = -1; i < mLogIndentLevel; i++) {
            mLogIndentArray[i + 1] = '\t';
        }
        mLogIndent = new String(mLogIndentArray);
    }

    SimpleDateFormat mDateFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
    /** Log both to log text view and to device logcat */
    void log(String logLine) {
        Log.d(TAG, logLine);
        logAndScrollToBottom(logLine, mLogIndent);
    }

    void logE(String logLine) {
        Log.e(TAG, logLine);
        logAndScrollToBottom(logLine, mLogIndent + "!!! ");
    }

    synchronized private void logAndScrollToBottom(String logLine, String logIndent) {
        StringBuffer logEntry = new StringBuffer(32);
        logEntry.append("\n").append(mDateFormatter.format(new Date())).append(logIndent);
        logEntry.append(logLine);
        mLogView.append(logEntry);
        final Layout layout = mLogView.getLayout();
        if (layout != null){
            int scrollDelta = layout.getLineBottom(mLogView.getLineCount() - 1)
                - mLogView.getScrollY() - mLogView.getHeight();
            if(scrollDelta > 0) {
                mLogView.scrollBy(0, scrollDelta);
            }
        }
    }

}