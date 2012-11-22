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

package com.android.voicedialer;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.srec.WaveHeader;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class logs the inputs and results of a recognition session to
 * the files listed below, which reside in
 * /data/data/com.android.voicedialer/app_logdir.
 * The files have the date encoded in the  name so that they will sort in
 * time order.  The newest RecognizerLogger.MAX_FILES are kept,
 * and the rest deleted to limit space used in the file system.
 * <ul>
 * <li> datename.wav - what the microphone heard.
 * <li> datename.log - contact list, results, errors, etc.
 * </ul>
 */
public class RecognizerLogger {

    private static final String TAG = "RecognizerLogger";
    
    private static final String LOGDIR = "logdir";
    private static final String ENABLED = "enabled";
    
    private static final int MAX_FILES = 20;
    
    private final String mDatedPath;
    private final BufferedWriter mWriter;
    
    /**
     * Determine if logging is enabled.  If the
     * @param context needed to reference the logging directory.
     * @return true if logging is enabled, determined by the 'enabled' file.
     */
    public static boolean isEnabled(Context context) {
        File dir = context.getDir(LOGDIR, 0);
        File enabled = new File(dir, ENABLED);
        return enabled.exists();
    }
    
    /**
     * Enable logging.
     * @param context needed to reference the logging directory.
     */
    public static void enable(Context context) {
        try {
            File dir = context.getDir(LOGDIR, 0);
            File enabled = new File(dir, ENABLED);
            enabled.createNewFile();
        }
        catch (IOException e) {
            Log.e(TAG, "enableLogging " + e);
        }
    }
    
    /**
     * Disable logging.
     * @param context needed to reference the logging directory.
     */
    public static void disable(Context context) {
        try {
            File dir = context.getDir(LOGDIR, 0);
            File enabled = new File(dir, ENABLED);
            enabled.delete();
        }
        catch (SecurityException e) {
            Log.e(TAG, "disableLogging " + e);
        }
    }

    /**
     * Constructor
     * @param dataDir directory to contain the log files.
     */
    public RecognizerLogger(Context context) throws IOException {
        if (false) Log.d(TAG, "RecognizerLogger");
        
        // generate new root filename
        File dir = context.getDir(LOGDIR, 0);
        mDatedPath = dir.toString() + File.separator + "log_" +
                DateFormat.format("yyyy_MM_dd_kk_mm_ss",
                        System.currentTimeMillis());
        
        // delete oldest files
        deleteOldest(".wav");
        deleteOldest(".log");
        
        // generate new text output log file
        mWriter = new BufferedWriter(new FileWriter(mDatedPath + ".log"), 8192);
        mWriter.write(Build.FINGERPRINT);
        mWriter.newLine();
    }
    
    /**
     * Write a line into the text log file.
     */
    public void logLine(String msg) {
        try {
            mWriter.write(msg);
            mWriter.newLine();
        }
        catch (IOException e) {
            Log.e(TAG, "logLine exception: " + e);
        }
    }
    
    /**
     * Write a header for the NBest lines into the text log file.
     */
    public void logNbestHeader() {
        logLine("Nbest *****************");
    }
    
    /**
     * Write the list of contacts into the text log file.
     * @param contacts
     */
    public void logContacts(List<VoiceContact> contacts) {
        logLine("Contacts *****************");
        for (VoiceContact vc : contacts) logLine(vc.toString());
        try {
            mWriter.flush();
        }
        catch (IOException e) {
            Log.e(TAG, "logContacts exception: " + e);
        }
    }
    
    /**
     * Write a list of Intents into the text log file.
     * @param intents
     */
    public void logIntents(ArrayList<Intent> intents) {
        logLine("Intents *********************");
        StringBuffer sb = new StringBuffer();
        for (Intent intent : intents) {
            logLine(intent.toString() + " " + RecognizerEngine.SENTENCE_EXTRA + "=" +
                    intent.getStringExtra(RecognizerEngine.SENTENCE_EXTRA));
        }
        try {
            mWriter.flush();
        }
        catch (IOException e) {
            Log.e(TAG, "logIntents exception: " + e);
        }
    }
    
    /**
     * Close the text log file.
     * @throws IOException
     */
    public void close() throws IOException {
        mWriter.close();
    }

    /**
     * Delete oldest files with a given suffix, if more than MAX_FILES.
     * @param suffix delete oldest files with this suffix.
     */
    private void deleteOldest(final String suffix) {
        FileFilter ff = new FileFilter() {
            public boolean accept(File f) {
                String name = f.getName();
                return name.startsWith("log_") && name.endsWith(suffix);
            }
        };
        File[] files = (new File(mDatedPath)).getParentFile().listFiles(ff);
        Arrays.sort(files);

        for (int i = 0; i < files.length - MAX_FILES; i++) {
            files[i].delete();            
        }
    }

    /**
     * InputStream wrapper which will log the contents to a WAV file.
     * @param inputStream
     * @param sampleRate
     * @return
     */
    public InputStream logInputStream(final InputStream inputStream, final int sampleRate) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(sampleRate * 2 * 20);
        
        return new InputStream() {

            public int available() throws IOException {
                return inputStream.available();
            }

            public int read(byte[] b, int offset, int length) throws IOException {
                int rtn = inputStream.read(b, offset, length);
                if (rtn > 0) baos.write(b, offset, rtn);
                return rtn;
            }

            public int read(byte[] b) throws IOException {
                int rtn = inputStream.read(b);
                if (rtn > 0) baos.write(b, 0, rtn);
                return rtn;
            }

            public int read() throws IOException {
                int rtn = inputStream.read();
                if (rtn > 0) baos.write(rtn);
                return rtn;
            }

            public long skip(long n) throws IOException {
                throw new UnsupportedOperationException();
            }

            public void close() throws IOException {
                try {
                    OutputStream out = new FileOutputStream(mDatedPath + ".wav");
                    try {
                        byte[] pcm = baos.toByteArray();
                        WaveHeader hdr = new WaveHeader(WaveHeader.FORMAT_PCM,
                                (short)1, sampleRate, (short)16, pcm.length);
                        hdr.write(out);
                        out.write(pcm);
                    }
                    finally {
                        out.close();
                    }
                }
                finally {
                    inputStream.close();
                    baos.close();
                }
            }
        };
    }

}
