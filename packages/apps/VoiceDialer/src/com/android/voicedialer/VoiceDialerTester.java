/*
 * Copyright (C) 2007 Google Inc.
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
import android.util.Log;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * This class represents a person who may be called via the VoiceDialer app.
 * The person has a name and a list of phones (home, mobile, work).
 */
public class VoiceDialerTester {
    private static final String TAG = "VoiceDialerTester";

    private final WavFile[] mWavFiles;
    private final File[] mWavDirs;

    // these indicate the current test
    private int mWavFile = -1; // -1 so it will step to 0 on first iteration

    private static class WavFile {
        final public File mFile;
        public int mRank;
        public int mTotal;
        public String mMessage;

        public WavFile(File file) {
            mFile = file;
        }
    }

    /**
     * Sweep directory of directories, listing all WAV files.
     */
    public VoiceDialerTester(File dir) {
        if (false) {
            Log.d(TAG, "VoiceDialerTester " + dir);
        }

        // keep a list of directories visited
        Vector<File> wavDirs = new Vector<File>();
        wavDirs.add(dir);

        // scan the directory tree
        Vector<File> wavFiles = new Vector<File>();
        for (int i = 0; i < wavDirs.size(); i++) {
            File d = wavDirs.get(i);
            for (File f : d.listFiles()) {
                if (f.isFile() && f.getName().endsWith(".wav")) {
                    wavFiles.add(f);
                }
                else if (f.isDirectory()) {
                    wavDirs.add(f);
                }
            }
        }

        // produce a sorted list of WavFiles
        File[] fa = wavFiles.toArray(new File[wavFiles.size()]);
        Arrays.sort(fa);
        mWavFiles = new WavFile[fa.length];
        for (int i = 0; i < mWavFiles.length; i++) {
            mWavFiles[i] = new WavFile(fa[i]);
        }

        // produce a sorted list of directories
        mWavDirs = wavDirs.toArray(new File[wavDirs.size()]);
        Arrays.sort(mWavDirs);
    }

    public File getWavFile() {
        return mWavFiles[mWavFile].mFile;
    }

    /**
     * Called by VoiceDialerActivity when a recognizer error occurs.
     */
    public void onRecognitionError(String msg) {
        WavFile wf = mWavFiles[mWavFile];
        wf.mRank = -1;
        wf.mTotal = -1;
        wf.mMessage = msg;
    }

    /**
     * Called by VoiceDialerActivity when a recognizer failure occurs.
     * @param msg Message to display.
     */
    public void onRecognitionFailure(String msg) {
        WavFile wf = mWavFiles[mWavFile];
        wf.mRank = 0;
        wf.mTotal = 0;
        wf.mMessage = msg;
    }

    /**
     * Called by VoiceDialerActivity when the recognizer succeeds.
     * @param intents Array of Intents corresponding to recognized sentences.
     */
    public void onRecognitionSuccess(Intent[] intents) {
        WavFile wf = mWavFiles[mWavFile];
        wf.mTotal = intents.length;
        String utter = wf.mFile.getName().toLowerCase().replace('_', ' ');
        utter = utter.substring(0, utter.indexOf('.')).trim();
        for (int i = 0; i < intents.length; i++) {
            String sentence =
                    intents[i].getStringExtra(RecognizerEngine.SENTENCE_EXTRA).
                    toLowerCase().trim();
            // note the first in case there are no matches
            if (i == 0) {
                wf.mMessage = sentence;
                if (intents.length > 1) wf.mMessage += ", etc";
            }
            // is this a match
            if (utter.equals(sentence)) {
                wf.mRank = i + 1;
                wf.mMessage = null;
                break;
            }
        }
    }

    /**
     * Called to step to the next WAV file in the test set.
     * @return true if successful, false if no more test files.
     */
    public boolean stepToNextTest() {
        mWavFile++;
        return mWavFile < mWavFiles.length;
    }

    private static final String REPORT_FMT = "%6s %6s %6s %6s %6s %6s %6s %s";
    private static final String REPORT_HDR = String.format(REPORT_FMT,
            "1/1", "1/N", "M/N", "0/N", "Fail", "Error", "Total", "");

    /**
     * Called when the test is complete to dump a summary.
     */
    public void report() {
        // report for each file
        Log.d(TAG, "List of all utterances tested");
        for (WavFile wf : mWavFiles) {
            Log.d(TAG, wf.mRank + "/" + wf.mTotal + "  " + wf.mFile +
                    (wf.mMessage != null ? "  " + wf.mMessage : ""));
        }

        // summary reports by file name
        reportSummaryForEachFileName();

        // summary reports by directory name
        reportSummaryForEachDir();

        // summary report for all files
        Log.d(TAG, "Summary of all utterances");
        Log.d(TAG, REPORT_HDR);
        reportSummary("Total", null);
    }

    private void reportSummaryForEachFileName() {
        Set<String> set = new HashSet<String>();
        for (WavFile wf : mWavFiles) {
            set.add(wf.mFile.getName());
        }
        String[] names = set.toArray(new String[set.size()]);
        Arrays.sort(names);

        Log.d(TAG, "Summary of utternaces by filename");
        Log.d(TAG, REPORT_HDR);
        for (final String fn : names) {
            reportSummary(fn,
                    new FileFilter() {
                        public boolean accept(File file) {
                            return fn.equals(file.getName());
                        }
            });
        }
    }

    private void reportSummaryForEachDir() {
        Set<String> set = new HashSet<String>();
        for (WavFile wf : mWavFiles) {
            set.add(wf.mFile.getParent());
        }
        String[] names = set.toArray(new String[set.size()]);
        Arrays.sort(names);

        Log.d(TAG, "Summary of utterances by directory");
        Log.d(TAG, REPORT_HDR);
        for (File dir : mWavDirs) {
            final String dn = dir.getPath();
            final String dn2 = dn + "/";
            reportSummary(dn,
                    new FileFilter() {
                        public boolean accept(File file) {
                            return file.getPath().startsWith(dn2);
                        }
            });
        }
    }

    private void reportSummary(String label, FileFilter filter) {
        if (!false) return;

        // log cumulative stats
        int total = 0;
        int count11 = 0;
        int count1N = 0;
        int countMN = 0;
        int count0N = 0;
        int countFail = 0;
        int countErrors = 0;

        for (WavFile wf : mWavFiles) {
            if (filter == null || filter.accept(wf.mFile)) {
                total++;
                if (wf.mRank == 1 && wf.mTotal == 1) count11++;
                if (wf.mRank == 1 && wf.mTotal >= 1) count1N++;
                if (wf.mRank >= 1 && wf.mTotal >= 1) countMN++;
                if (wf.mRank == 0 && wf.mTotal >= 1) count0N++;
                if (wf.mRank == 0 && wf.mTotal == 0) countFail++;
                if (wf.mRank == -1 && wf.mTotal == -1) countErrors++;
            }
        }

        String line = String.format(REPORT_FMT,
                countString(count11, total),
                countString(count1N, total),
                countString(countMN, total),
                countString(count0N, total),
                countString(countFail, total),
                countString(countErrors, total),
                "" + total,
                label);
        Log.d(TAG, line);
    }

    private static String countString(int count, int total) {
        return total > 0 ? "" + (100 * count / total) + "%" : "";
    }

}
