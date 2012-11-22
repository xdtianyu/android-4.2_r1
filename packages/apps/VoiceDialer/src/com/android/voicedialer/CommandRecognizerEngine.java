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


import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.speech.srec.Recognizer;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
/**
 * This is a RecognizerEngine that processes commands to make phone calls and
 * open applications.
 * <ul>
 * <li>setupGrammar
 * <li>Scans contacts and determine if the Grammar g2g file is stale.
 * <li>If so, create and rebuild the Grammar,
 * <li>Else create and load the Grammar from the file.
 * <li>onRecognitionSuccess is called when we get results from the recognizer,
 * it will process the results, which will pass a list of intents to
 * the {@RecognizerClient}.  It will accept the following types of commands:
 * "call" a particular contact
 * "dial a particular number
 * "open" a particular application
 * "redial" the last number called
 * "voicemail" to call voicemail
 * <li>Pass a list of {@link Intent} corresponding to the recognition results
 * to the {@link RecognizerClient}, which notifies the user.
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
public class CommandRecognizerEngine extends RecognizerEngine {

    private static final String OPEN_ENTRIES = "openentries.txt";
    public static final String PHONE_TYPE_EXTRA = "phone_type";
    private static final int MINIMUM_CONFIDENCE = 100;
    private File mContactsFile;
    private boolean mMinimizeResults;
    private boolean mAllowOpenEntries;
    private HashMap<String,String> mOpenEntries;

    /**
     * Constructor.
     */
    public CommandRecognizerEngine() {
        mContactsFile = null;
        mMinimizeResults = false;
        mAllowOpenEntries = true;
    }

    public void setContactsFile(File contactsFile) {
        if (contactsFile != mContactsFile) {
            mContactsFile = contactsFile;
            // if we change the contacts file, then we need to recreate the grammar.
            if (mSrecGrammar != null) {
                mSrecGrammar.destroy();
                mSrecGrammar = null;
                mOpenEntries = null;
            }
        }
    }

    public void setMinimizeResults(boolean minimizeResults) {
        mMinimizeResults = minimizeResults;
    }

    public void setAllowOpenEntries(boolean allowOpenEntries) {
        if (mAllowOpenEntries != allowOpenEntries) {
            // if we change this setting, then we need to recreate the grammar.
            if (mSrecGrammar != null) {
                mSrecGrammar.destroy();
                mSrecGrammar = null;
                mOpenEntries = null;
            }
        }
        mAllowOpenEntries = allowOpenEntries;
    }

    protected void setupGrammar() throws IOException, InterruptedException {
        // fetch the contact list
        if (false) Log.d(TAG, "start getVoiceContacts");
        if (false) Log.d(TAG, "contactsFile is " + (mContactsFile == null ?
            "null" : "not null"));
        List<VoiceContact> contacts = mContactsFile != null ?
                VoiceContact.getVoiceContactsFromFile(mContactsFile) :
                VoiceContact.getVoiceContacts(mActivity);

        // log contacts if requested
        if (mLogger != null) mLogger.logContacts(contacts);
        // generate g2g grammar file name
        File g2g = mActivity.getFileStreamPath("voicedialer." +
                Integer.toHexString(contacts.hashCode()) + ".g2g");

        // rebuild g2g file if current one is out of date
        if (!g2g.exists()) {
            // clean up existing Grammar and old file
            deleteAllG2GFiles(mActivity);
            if (mSrecGrammar != null) {
                mSrecGrammar.destroy();
                mSrecGrammar = null;
            }

            // load the empty Grammar
            if (false) Log.d(TAG, "start new Grammar");
            mSrecGrammar = mSrec.new Grammar(SREC_DIR + "/grammars/VoiceDialer.g2g");
            mSrecGrammar.setupRecognizer();

            // reset slots
            if (false) Log.d(TAG, "start grammar.resetAllSlots");
            mSrecGrammar.resetAllSlots();

            // add names to the grammar
            addNameEntriesToGrammar(contacts);

            if (mAllowOpenEntries) {
                // add open entries to the grammar
                addOpenEntriesToGrammar();
            }

            // compile the grammar
            if (false) Log.d(TAG, "start grammar.compile");
            mSrecGrammar.compile();

            // update g2g file
            if (false) Log.d(TAG, "start grammar.save " + g2g.getPath());
            g2g.getParentFile().mkdirs();
            mSrecGrammar.save(g2g.getPath());
        }

        // g2g file exists, but is not loaded
        else if (mSrecGrammar == null) {
            if (false) Log.d(TAG, "start new Grammar loading " + g2g);
            mSrecGrammar = mSrec.new Grammar(g2g.getPath());
            mSrecGrammar.setupRecognizer();
        }
        if (mOpenEntries == null && mAllowOpenEntries) {
            // make sure to load the openEntries mapping table.
            loadOpenEntriesTable();
        }

    }

    /**
     * Number of phone ids appended to a grammer in {@link #addNameEntriesToGrammar(List)}.
     */
    private static final int PHONE_ID_COUNT = 7;

    /**
     * Add a list of names to the grammar
     * @param contacts list of VoiceContacts to be added.
     */
    private void addNameEntriesToGrammar(List<VoiceContact> contacts)
            throws InterruptedException {
        if (false) Log.d(TAG, "addNameEntriesToGrammar " + contacts.size());

        HashSet<String> entries = new HashSet<String>();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (VoiceContact contact : contacts) {
            if (Thread.interrupted()) throw new InterruptedException();
            String name = scrubName(contact.mName);
            if (name.length() == 0 || !entries.add(name)) continue;
            sb.setLength(0);
            // The number of ids appended here must be same as PHONE_ID_COUNT.
            sb.append("V='");
            sb.append(contact.mContactId).append(' ');
            sb.append(contact.mPrimaryId).append(' ');
            sb.append(contact.mHomeId).append(' ');
            sb.append(contact.mMobileId).append(' ');
            sb.append(contact.mWorkId).append(' ');
            sb.append(contact.mOtherId).append(' ');
            sb.append(contact.mFallbackId);
            sb.append("'");
            try {
                mSrecGrammar.addWordToSlot("@Names", name, null, 1, sb.toString());
            } catch (Exception e) {
                Log.e(TAG, "Cannot load all contacts to voice recognizer, loaded " +
                        count, e);
                break;
            }

            count++;
        }
    }

    /**
     * add a list of application labels to the 'open x' grammar
     */
    private void loadOpenEntriesTable() throws InterruptedException, IOException {
        if (false) Log.d(TAG, "addOpenEntriesToGrammar");

        // fill this
        File oe = mActivity.getFileStreamPath(OPEN_ENTRIES);

        // build and write list of entries
        if (!oe.exists()) {
            mOpenEntries = new HashMap<String, String>();

            // build a list of 'open' entries
            PackageManager pm = mActivity.getPackageManager();
            List<ResolveInfo> riList = pm.queryIntentActivities(
                            new Intent(Intent.ACTION_MAIN).
                            addCategory("android.intent.category.VOICE_LAUNCH"),
                            PackageManager.GET_ACTIVITIES);
            if (Thread.interrupted()) throw new InterruptedException();
            riList.addAll(pm.queryIntentActivities(
                            new Intent(Intent.ACTION_MAIN).
                            addCategory("android.intent.category.LAUNCHER"),
                            PackageManager.GET_ACTIVITIES));
            String voiceDialerClassName = mActivity.getComponentName().getClassName();

            // scan list, adding complete phrases, as well as individual words
            for (ResolveInfo ri : riList) {
                if (Thread.interrupted()) throw new InterruptedException();

                // skip self
                if (voiceDialerClassName.equals(ri.activityInfo.name)) continue;

                // fetch a scrubbed window label
                String label = scrubName(ri.loadLabel(pm).toString());
                if (label.length() == 0) continue;

                // insert it into the result list
                addClassName(mOpenEntries, label,
                        ri.activityInfo.packageName, ri.activityInfo.name);

                // split it into individual words, and insert them
                String[] words = label.split(" ");
                if (words.length > 1) {
                    for (String word : words) {
                        word = word.trim();
                        // words must be three characters long, or two if capitalized
                        int len = word.length();
                        if (len <= 1) continue;
                        if (len == 2 && !(Character.isUpperCase(word.charAt(0)) &&
                                        Character.isUpperCase(word.charAt(1)))) continue;
                        if ("and".equalsIgnoreCase(word) ||
                                "the".equalsIgnoreCase(word)) continue;
                        // add the word
                        addClassName(mOpenEntries, word,
                                ri.activityInfo.packageName, ri.activityInfo.name);
                    }
                }
            }

            // write list
            if (false) Log.d(TAG, "addOpenEntriesToGrammar writing " + oe);
            try {
                 FileOutputStream fos = new FileOutputStream(oe);
                 try {
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(mOpenEntries);
                    oos.close();
                } finally {
                    fos.close();
                }
            } catch (IOException ioe) {
                deleteCachedGrammarFiles(mActivity);
                throw ioe;
            }
        }

        // read the list
        else {
            if (false) Log.d(TAG, "addOpenEntriesToGrammar reading " + oe);
            try {
                FileInputStream fis = new FileInputStream(oe);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    mOpenEntries = (HashMap<String, String>)ois.readObject();
                    ois.close();
                } finally {
                    fis.close();
                }
            } catch (Exception e) {
                deleteCachedGrammarFiles(mActivity);
                throw new IOException(e.toString());
            }
        }
    }

    private void addOpenEntriesToGrammar() throws InterruptedException, IOException {
        // load up our open entries table
        loadOpenEntriesTable();

        // add list of 'open' entries to the grammar
        for (String label : mOpenEntries.keySet()) {
            if (Thread.interrupted()) throw new InterruptedException();
            String entry = mOpenEntries.get(label);
            // don't add if too many results
            int count = 0;
            for (int i = 0; 0 != (i = entry.indexOf(' ', i) + 1); count++) ;
            if (count > RESULT_LIMIT) continue;
            // add the word to the grammar
            // See Bug: 2457238.
            // We used to store the entire list of components into the grammar.
            // Unfortuantely, the recognizer has a fixed limit on the length of
            // the "semantic" string, which is easy to overflow.  So now,
            // the we store our own mapping table between words and component
            // names, and the entries in the grammar have the same value
            // for literal and semantic.
            mSrecGrammar.addWordToSlot("@Opens", label, null, 1, "V='" + label + "'");
        }
    }

    /**
     * Add a className to a hash table of class name lists.
     * @param openEntries HashMap of lists of class names.
     * @param label a label or word corresponding to the list of classes.
     * @param className class name to add
     */
    private static void addClassName(HashMap<String,String> openEntries,
            String label, String packageName, String className) {
        String component = packageName + "/" + className;
        String labelLowerCase = label.toLowerCase();
        String classList = openEntries.get(labelLowerCase);

        // first item in the list
        if (classList == null) {
            openEntries.put(labelLowerCase, component);
            return;
        }
        // already in list
        int index = classList.indexOf(component);
        int after = index + component.length();
        if (index != -1 && (index == 0 || classList.charAt(index - 1) == ' ') &&
                (after == classList.length() || classList.charAt(after) == ' ')) return;

        // add it to the end
        openEntries.put(labelLowerCase, classList + ' ' + component);
    }

    // map letters in Latin1 Supplement to basic ascii
    // from http://en.wikipedia.org/wiki/Latin-1_Supplement_unicode_block
    // not all letters map well, including Eth and Thorn
    // TODO: this should really be all handled in the pronunciation engine
    private final static char[] mLatin1Letters =
            "AAAAAAACEEEEIIIIDNOOOOO OUUUUYDsaaaaaaaceeeeiiiidnooooo ouuuuydy".
            toCharArray();
    private final static int mLatin1Base = 0x00c0;

    /**
     * Reformat a raw name from the contact list into a form a
     * {@link Recognizer.Grammar} can digest.
     * @param name the raw name.
     * @return the reformatted name.
     */
    private static String scrubName(String name) {
        // replace '&' with ' and '
        name = name.replace("&", " and ");

        // replace '@' with ' at '
        name = name.replace("@", " at ");

        // remove '(...)'
        while (true) {
            int i = name.indexOf('(');
            if (i == -1) break;
            int j = name.indexOf(')', i);
            if (j == -1) break;
            name = name.substring(0, i) + " " + name.substring(j + 1);
        }

        // map letters of Latin1 Supplement to basic ascii
        char[] nm = null;
        for (int i = name.length() - 1; i >= 0; i--) {
            char ch = name.charAt(i);
            if (ch < ' ' || '~' < ch) {
                if (nm == null) nm = name.toCharArray();
                nm[i] = mLatin1Base <= ch && ch < mLatin1Base + mLatin1Letters.length ?
                    mLatin1Letters[ch - mLatin1Base] : ' ';
            }
        }
        if (nm != null) {
            name = new String(nm);
        }

        // if '.' followed by alnum, replace with ' dot '
        while (true) {
            int i = name.indexOf('.');
            if (i == -1 ||
                    i + 1 >= name.length() ||
                    !Character.isLetterOrDigit(name.charAt(i + 1))) break;
            name = name.substring(0, i) + " dot " + name.substring(i + 1);
        }

        // trim
        name = name.trim();

        // ensure at least one alphanumeric character, or the pron engine will fail
        for (int i = name.length() - 1; true; i--) {
            if (i < 0) return "";
            char ch = name.charAt(i);
            if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9')) {
                break;
            }
        }

        return name;
    }

    /**
     * Delete all g2g files in the directory indicated by {@link File},
     * which is typically /data/data/com.android.voicedialer/files.
     * There should only be one g2g file at any one time, with a hashcode
     * embedded in it's name, but if stale ones are present, this will delete
     * them all.
     * @param context fetch directory for the stuffed and compiled g2g file.
     */
    private static void deleteAllG2GFiles(Context context) {
        FileFilter ff = new FileFilter() {
            public boolean accept(File f) {
                String name = f.getName();
                return name.endsWith(".g2g");
            }
        };
        File[] files = context.getFilesDir().listFiles(ff);
        if (files != null) {
            for (File file : files) {
                if (false) Log.d(TAG, "deleteAllG2GFiles " + file);
                file.delete();
            }
        }
    }

    /**
     * Delete G2G and OpenEntries files, to force regeneration of the g2g file
     * from scratch.
     * @param context fetch directory for file.
     */
    public static void deleteCachedGrammarFiles(Context context) {
        deleteAllG2GFiles(context);
        File oe = context.getFileStreamPath(OPEN_ENTRIES);
        if (false) Log.v(TAG, "deleteCachedGrammarFiles " + oe);
        if (oe.exists()) oe.delete();
    }

    // NANP number formats
    private final static String mNanpFormats =
        "xxx xxx xxxx\n" +
        "xxx xxxx\n" +
        "x11\n";

    // a list of country codes
    private final static String mPlusFormats =

        ////////////////////////////////////////////////////////////
        // zone 1: nanp (north american numbering plan), us, canada, caribbean
        ////////////////////////////////////////////////////////////

        "+1 xxx xxx xxxx\n" +         // nanp

        ////////////////////////////////////////////////////////////
        // zone 2: africa, some atlantic and indian ocean islands
        ////////////////////////////////////////////////////////////

        "+20 x xxx xxxx\n" +          // Egypt
        "+20 1x xxx xxxx\n" +         // Egypt
        "+20 xx xxx xxxx\n" +         // Egypt
        "+20 xxx xxx xxxx\n" +        // Egypt

        "+212 xxxx xxxx\n" +          // Morocco

        "+213 xx xx xx xx\n" +        // Algeria
        "+213 xx xxx xxxx\n" +        // Algeria

        "+216 xx xxx xxx\n" +         // Tunisia

        "+218 xx xxx xxx\n" +         // Libya

        "+22x \n" +
        "+23x \n" +
        "+24x \n" +
        "+25x \n" +
        "+26x \n" +

        "+27 xx xxx xxxx\n" +         // South africa

        "+290 x xxx\n" +              // Saint Helena, Tristan da Cunha

        "+291 x xxx xxx\n" +          // Eritrea

        "+297 xxx xxxx\n" +           // Aruba

        "+298 xxx xxx\n" +            // Faroe Islands

        "+299 xxx xxx\n" +            // Greenland

        ////////////////////////////////////////////////////////////
        // zone 3: europe, southern and small countries
        ////////////////////////////////////////////////////////////

        "+30 xxx xxx xxxx\n" +        // Greece

        "+31 6 xxxx xxxx\n" +         // Netherlands
        "+31 xx xxx xxxx\n" +         // Netherlands
        "+31 xxx xx xxxx\n" +         // Netherlands

        "+32 2 xxx xx xx\n" +         // Belgium
        "+32 3 xxx xx xx\n" +         // Belgium
        "+32 4xx xx xx xx\n" +        // Belgium
        "+32 9 xxx xx xx\n" +         // Belgium
        "+32 xx xx xx xx\n" +         // Belgium

        "+33 xxx xxx xxx\n" +         // France

        "+34 xxx xxx xxx\n" +        // Spain

        "+351 3xx xxx xxx\n" +       // Portugal
        "+351 7xx xxx xxx\n" +       // Portugal
        "+351 8xx xxx xxx\n" +       // Portugal
        "+351 xx xxx xxxx\n" +       // Portugal

        "+352 xx xxxx\n" +           // Luxembourg
        "+352 6x1 xxx xxx\n" +       // Luxembourg
        "+352 \n" +                  // Luxembourg

        "+353 xxx xxxx\n" +          // Ireland
        "+353 xxxx xxxx\n" +         // Ireland
        "+353 xx xxx xxxx\n" +       // Ireland

        "+354 3xx xxx xxx\n" +       // Iceland
        "+354 xxx xxxx\n" +          // Iceland

        "+355 6x xxx xxxx\n" +       // Albania
        "+355 xxx xxxx\n" +          // Albania

        "+356 xx xx xx xx\n" +       // Malta

        "+357 xx xx xx xx\n" +       // Cyprus

        "+358 \n" +                  // Finland

        "+359 \n" +                  // Bulgaria

        "+36 1 xxx xxxx\n" +         // Hungary
        "+36 20 xxx xxxx\n" +        // Hungary
        "+36 21 xxx xxxx\n" +        // Hungary
        "+36 30 xxx xxxx\n" +        // Hungary
        "+36 70 xxx xxxx\n" +        // Hungary
        "+36 71 xxx xxxx\n" +        // Hungary
        "+36 xx xxx xxx\n" +         // Hungary

        "+370 6x xxx xxx\n" +        // Lithuania
        "+370 xxx xx xxx\n" +        // Lithuania

        "+371 xxxx xxxx\n" +         // Latvia

        "+372 5 xxx xxxx\n" +        // Estonia
        "+372 xxx xxxx\n" +          // Estonia

        "+373 6xx xx xxx\n" +        // Moldova
        "+373 7xx xx xxx\n" +        // Moldova
        "+373 xxx xxxxx\n" +         // Moldova

        "+374 xx xxx xxx\n" +        // Armenia

        "+375 xx xxx xxxx\n" +       // Belarus

        "+376 xx xx xx\n" +          // Andorra

        "+377 xxxx xxxx\n" +         // Monaco

        "+378 xxx xxx xxxx\n" +      // San Marino

        "+380 xxx xx xx xx\n" +      // Ukraine

        "+381 xx xxx xxxx\n" +       // Serbia

        "+382 xx xxx xxxx\n" +       // Montenegro

        "+385 xx xxx xxxx\n" +       // Croatia

        "+386 x xxx xxxx\n" +        // Slovenia

        "+387 xx xx xx xx\n" +       // Bosnia and herzegovina

        "+389 2 xxx xx xx\n" +       // Macedonia
        "+389 xx xx xx xx\n" +       // Macedonia

        "+39 xxx xxx xxx\n" +        // Italy
        "+39 3xx xxx xxxx\n" +       // Italy
        "+39 xx xxxx xxxx\n" +       // Italy

        ////////////////////////////////////////////////////////////
        // zone 4: europe, northern countries
        ////////////////////////////////////////////////////////////

        "+40 xxx xxx xxx\n" +        // Romania

        "+41 xx xxx xx xx\n" +       // Switzerland

        "+420 xxx xxx xxx\n" +       // Czech republic

        "+421 xxx xxx xxx\n" +       // Slovakia

        "+421 xxx xxx xxxx\n" +      // Liechtenstein

        "+43 \n" +                   // Austria

        "+44 xxx xxx xxxx\n" +       // UK

        "+45 xx xx xx xx\n" +        // Denmark

        "+46 \n" +                   // Sweden

        "+47 xxxx xxxx\n" +          // Norway

        "+48 xx xxx xxxx\n" +        // Poland

        "+49 1xx xxxx xxx\n" +       // Germany
        "+49 1xx xxxx xxxx\n" +      // Germany
        "+49 \n" +                   // Germany

        ////////////////////////////////////////////////////////////
        // zone 5: latin america
        ////////////////////////////////////////////////////////////

        "+50x \n" +

        "+51 9xx xxx xxx\n" +        // Peru
        "+51 1 xxx xxxx\n" +         // Peru
        "+51 xx xx xxxx\n" +         // Peru

        "+52 1 xxx xxx xxxx\n" +     // Mexico
        "+52 xxx xxx xxxx\n" +       // Mexico

        "+53 xxxx xxxx\n" +          // Cuba

        "+54 9 11 xxxx xxxx\n" +     // Argentina
        "+54 9 xxx xxx xxxx\n" +     // Argentina
        "+54 11 xxxx xxxx\n" +       // Argentina
        "+54 xxx xxx xxxx\n" +       // Argentina

        "+55 xx xxxx xxxx\n" +       // Brazil

        "+56 2 xxxxxx\n" +           // Chile
        "+56 9 xxxx xxxx\n" +        // Chile
        "+56 xx xxxxxx\n" +          // Chile
        "+56 xx xxxxxxx\n" +         // Chile

        "+57 x xxx xxxx\n" +         // Columbia
        "+57 3xx xxx xxxx\n" +       // Columbia

        "+58 xxx xxx xxxx\n" +       // Venezuela

        "+59x \n" +

        ////////////////////////////////////////////////////////////
        // zone 6: southeast asia and oceania
        ////////////////////////////////////////////////////////////

        // TODO is this right?
        "+60 3 xxxx xxxx\n" +        // Malaysia
        "+60 8x xxxxxx\n" +          // Malaysia
        "+60 x xxx xxxx\n" +         // Malaysia
        "+60 14 x xxx xxxx\n" +      // Malaysia
        "+60 1x xxx xxxx\n" +        // Malaysia
        "+60 x xxxx xxxx\n" +        // Malaysia
        "+60 \n" +                   // Malaysia

        "+61 4xx xxx xxx\n" +        // Australia
        "+61 x xxxx xxxx\n" +        // Australia

        // TODO: is this right?
        "+62 8xx xxxx xxxx\n" +      // Indonesia
        "+62 21 xxxxx\n" +           // Indonesia
        "+62 xx xxxxxx\n" +          // Indonesia
        "+62 xx xxx xxxx\n" +        // Indonesia
        "+62 xx xxxx xxxx\n" +       // Indonesia

        "+63 2 xxx xxxx\n" +         // Phillipines
        "+63 xx xxx xxxx\n" +        // Phillipines
        "+63 9xx xxx xxxx\n" +       // Phillipines

        // TODO: is this right?
        "+64 2 xxx xxxx\n" +         // New Zealand
        "+64 2 xxx xxxx x\n" +       // New Zealand
        "+64 2 xxx xxxx xx\n" +      // New Zealand
        "+64 x xxx xxxx\n" +         // New Zealand

        "+65 xxxx xxxx\n" +          // Singapore

        "+66 8 xxxx xxxx\n" +        // Thailand
        "+66 2 xxx xxxx\n" +         // Thailand
        "+66 xx xx xxxx\n" +         // Thailand

        "+67x \n" +
        "+68x \n" +

        "+690 x xxx\n" +             // Tokelau

        "+691 xxx xxxx\n" +          // Micronesia

        "+692 xxx xxxx\n" +          // marshall Islands

        ////////////////////////////////////////////////////////////
        // zone 7: russia and kazakstan
        ////////////////////////////////////////////////////////////

        "+7 6xx xx xxxxx\n" +        // Kazakstan
        "+7 7xx 2 xxxxxx\n" +        // Kazakstan
        "+7 7xx xx xxxxx\n" +        // Kazakstan

        "+7 xxx xxx xx xx\n" +       // Russia

        ////////////////////////////////////////////////////////////
        // zone 8: east asia
        ////////////////////////////////////////////////////////////

        "+81 3 xxxx xxxx\n" +        // Japan
        "+81 6 xxxx xxxx\n" +        // Japan
        "+81 xx xxx xxxx\n" +        // Japan
        "+81 x0 xxxx xxxx\n" +       // Japan

        "+82 2 xxx xxxx\n" +         // South korea
        "+82 2 xxxx xxxx\n" +        // South korea
        "+82 xx xxxx xxxx\n" +       // South korea
        "+82 xx xxx xxxx\n" +        // South korea

        "+84 4 xxxx xxxx\n" +        // Vietnam
        "+84 xx xxxx xxx\n" +        // Vietnam
        "+84 xx xxxx xxxx\n" +       // Vietnam

        "+850 \n" +                  // North Korea

        "+852 xxxx xxxx\n" +         // Hong Kong

        "+853 xxxx xxxx\n" +         // Macau

        "+855 1x xxx xxx\n" +        // Cambodia
        "+855 9x xxx xxx\n" +        // Cambodia
        "+855 xx xx xx xx\n" +       // Cambodia

        "+856 20 x xxx xxx\n" +      // Laos
        "+856 xx xxx xxx\n" +        // Laos

        "+852 xxxx xxxx\n" +         // Hong kong

        "+86 10 xxxx xxxx\n" +       // China
        "+86 2x xxxx xxxx\n" +       // China
        "+86 xxx xxx xxxx\n" +       // China
        "+86 xxx xxxx xxxx\n" +      // China

        "+880 xx xxxx xxxx\n" +      // Bangladesh

        "+886 \n" +                  // Taiwan

        ////////////////////////////////////////////////////////////
        // zone 9: south asia, west asia, central asia, middle east
        ////////////////////////////////////////////////////////////

        "+90 xxx xxx xxxx\n" +       // Turkey

        "+91 9x xx xxxxxx\n" +       // India
        "+91 xx xxxx xxxx\n" +       // India

        "+92 xx xxx xxxx\n" +        // Pakistan
        "+92 3xx xxx xxxx\n" +       // Pakistan

        "+93 70 xxx xxx\n" +         // Afghanistan
        "+93 xx xxx xxxx\n" +        // Afghanistan

        "+94 xx xxx xxxx\n" +        // Sri Lanka

        "+95 1 xxx xxx\n" +          // Burma
        "+95 2 xxx xxx\n" +          // Burma
        "+95 xx xxxxx\n" +           // Burma
        "+95 9 xxx xxxx\n" +         // Burma

        "+960 xxx xxxx\n" +          // Maldives

        "+961 x xxx xxx\n" +         // Lebanon
        "+961 xx xxx xxx\n" +        // Lebanon

        "+962 7 xxxx xxxx\n" +       // Jordan
        "+962 x xxx xxxx\n" +        // Jordan

        "+963 11 xxx xxxx\n" +       // Syria
        "+963 xx xxx xxx\n" +        // Syria

        "+964 \n" +                  // Iraq

        "+965 xxxx xxxx\n" +         // Kuwait

        "+966 5x xxx xxxx\n" +       // Saudi Arabia
        "+966 x xxx xxxx\n" +        // Saudi Arabia

        "+967 7xx xxx xxx\n" +       // Yemen
        "+967 x xxx xxx\n" +         // Yemen

        "+968 xxxx xxxx\n" +         // Oman

        "+970 5x xxx xxxx\n" +       // Palestinian Authority
        "+970 x xxx xxxx\n" +        // Palestinian Authority

        "+971 5x xxx xxxx\n" +       // United Arab Emirates
        "+971 x xxx xxxx\n" +        // United Arab Emirates

        "+972 5x xxx xxxx\n" +       // Israel
        "+972 x xxx xxxx\n" +        // Israel

        "+973 xxxx xxxx\n" +         // Bahrain

        "+974 xxx xxxx\n" +          // Qatar

        "+975 1x xxx xxx\n" +        // Bhutan
        "+975 x xxx xxx\n" +         // Bhutan

        "+976 \n" +                  // Mongolia

        "+977 xxxx xxxx\n" +         // Nepal
        "+977 98 xxxx xxxx\n" +      // Nepal

        "+98 xxx xxx xxxx\n" +       // Iran

        "+992 xxx xxx xxx\n" +       // Tajikistan

        "+993 xxxx xxxx\n" +         // Turkmenistan

        "+994 xx xxx xxxx\n" +       // Azerbaijan
        "+994 xxx xxxxx\n" +         // Azerbaijan

        "+995 xx xxx xxx\n" +        // Georgia

        "+996 xxx xxx xxx\n" +       // Kyrgyzstan

        "+998 xx xxx xxxx\n";        // Uzbekistan


    // TODO: need to handle variable number notation
    private static String formatNumber(String formats, String number) {
        number = number.trim();
        final int nlen = number.length();
        final int formatslen = formats.length();
        StringBuffer sb = new StringBuffer();

        // loop over country codes
        for (int f = 0; f < formatslen; ) {
            sb.setLength(0);
            int n = 0;

            // loop over letters of pattern
            while (true) {
                final char fch = formats.charAt(f);
                if (fch == '\n' && n >= nlen) return sb.toString();
                if (fch == '\n' || n >= nlen) break;
                final char nch = number.charAt(n);
                // pattern matches number
                if (fch == nch || (fch == 'x' && Character.isDigit(nch))) {
                    f++;
                    n++;
                    sb.append(nch);
                }
                // don't match ' ' in pattern, but insert into result
                else if (fch == ' ') {
                    f++;
                    sb.append(' ');
                    // ' ' at end -> match all the rest
                    if (formats.charAt(f) == '\n') {
                        return sb.append(number, n, nlen).toString();
                    }
                }
                // match failed
                else break;
            }

            // step to the next pattern
            f = formats.indexOf('\n', f) + 1;
            if (f == 0) break;
        }

        return null;
    }

    /**
     * Format a phone number string.
     * At some point, PhoneNumberUtils.formatNumber will handle this.
     * @param num phone number string.
     * @return formatted phone number string.
     */
    private static String formatNumber(String num) {
        String fmt = null;

        fmt = formatNumber(mPlusFormats, num);
        if (fmt != null) return fmt;

        fmt = formatNumber(mNanpFormats, num);
        if (fmt != null) return fmt;

        return null;
    }

    /**
     * Called when recognition succeeds.  It receives a list
     * of results, builds a corresponding list of Intents, and
     * passes them to the {@link RecognizerClient}, which selects and
     * performs a corresponding action.
     * @param recognizerClient the client that will be sent the results
     */
    protected  void onRecognitionSuccess(RecognizerClient recognizerClient)
            throws InterruptedException {
        if (false) Log.d(TAG, "onRecognitionSuccess");

        if (mLogger != null) mLogger.logNbestHeader();

        ArrayList<Intent> intents = new ArrayList<Intent>();

        int highestConfidence = 0;
        int examineLimit = RESULT_LIMIT;
        if (mMinimizeResults) {
            examineLimit = 1;
        }
        for (int result = 0; result < mSrec.getResultCount() &&
                intents.size() < examineLimit; result++) {

            // parse the semanticMeaning string and build an Intent
            String conf = mSrec.getResult(result, Recognizer.KEY_CONFIDENCE);
            String literal = mSrec.getResult(result, Recognizer.KEY_LITERAL);
            String semantic = mSrec.getResult(result, Recognizer.KEY_MEANING);
            String msg = "conf=" + conf + " lit=" + literal + " sem=" + semantic;
            if (false) Log.d(TAG, msg);
            int confInt = Integer.parseInt(conf);
            if (highestConfidence < confInt) highestConfidence = confInt;
            if (confInt < MINIMUM_CONFIDENCE || confInt * 2 < highestConfidence) {
                if (false) Log.d(TAG, "confidence too low, dropping");
                break;
            }
            if (mLogger != null) mLogger.logLine(msg);
            String[] commands = semantic.trim().split(" ");

            // DIAL 650 867 5309
            // DIAL 867 5309
            // DIAL 911
            if ("DIAL".equalsIgnoreCase(commands[0])) {
                Uri uri = Uri.fromParts("tel", commands[1], null);
                String num =  formatNumber(commands[1]);
                if (num != null) {
                    addCallIntent(intents, uri,
                            literal.split(" ")[0].trim() + " " + num, "", 0);
                }
            }

            // CALL JACK JONES
            // commands should become ["CALL", id, id, ..] reflecting addNameEntriesToGrammar().
            else if ("CALL".equalsIgnoreCase(commands[0])
                    && commands.length >= PHONE_ID_COUNT + 1) {
                // parse the ids
                long contactId = Long.parseLong(commands[1]); // people table
                long primaryId   = Long.parseLong(commands[2]); // phones table
                long homeId    = Long.parseLong(commands[3]); // phones table
                long mobileId  = Long.parseLong(commands[4]); // phones table
                long workId    = Long.parseLong(commands[5]); // phones table
                long otherId   = Long.parseLong(commands[6]); // phones table
                long fallbackId = Long.parseLong(commands[7]); // phones table
                Resources res  = mActivity.getResources();

                int count = 0;

                //
                // generate the best entry corresponding to what was said
                //

                // 'CALL JACK JONES AT HOME|MOBILE|WORK|OTHER'
                if (commands.length == PHONE_ID_COUNT + 2) {
                    // The last command should imply the type of the phone number.
                    final String spokenPhoneIdCommand = commands[PHONE_ID_COUNT + 1];
                    long spokenPhoneId =
                            "H".equalsIgnoreCase(spokenPhoneIdCommand) ? homeId :
                            "M".equalsIgnoreCase(spokenPhoneIdCommand) ? mobileId :
                            "W".equalsIgnoreCase(spokenPhoneIdCommand) ? workId :
                            "O".equalsIgnoreCase(spokenPhoneIdCommand) ? otherId :
                             VoiceContact.ID_UNDEFINED;
                    if (spokenPhoneId != VoiceContact.ID_UNDEFINED) {
                        addCallIntent(intents, ContentUris.withAppendedId(
                                Phone.CONTENT_URI, spokenPhoneId),
                                literal, spokenPhoneIdCommand, 0);
                        count++;
                    }
                }

                // 'CALL JACK JONES', with valid default phoneId
                else if (commands.length == PHONE_ID_COUNT + 1) {
                    String phoneType = null;
                    CharSequence phoneIdMsg = null;
                    if (primaryId == VoiceContact.ID_UNDEFINED) {
                        phoneType = null;
                        phoneIdMsg = null;
                    } else if (primaryId == homeId) {
                        phoneType = "H";
                        phoneIdMsg = res.getText(R.string.at_home);
                    } else if (primaryId == mobileId) {
                        phoneType = "M";
                        phoneIdMsg = res.getText(R.string.on_mobile);
                    } else if (primaryId == workId) {
                        phoneType = "W";
                        phoneIdMsg = res.getText(R.string.at_work);
                    } else if (primaryId == otherId) {
                        phoneType = "O";
                        phoneIdMsg = res.getText(R.string.at_other);
                    }
                    if (phoneIdMsg != null) {
                        addCallIntent(intents, ContentUris.withAppendedId(
                                Phone.CONTENT_URI, primaryId),
                                literal + phoneIdMsg, phoneType, 0);
                        count++;
                    }
                }

                if (count == 0 || !mMinimizeResults) {
                    //
                    // generate all other entries for this person
                    //

                    // trim last two words, ie 'at home', etc
                    String lit = literal;
                    if (commands.length == PHONE_ID_COUNT + 2) {
                        String[] words = literal.trim().split(" ");
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < words.length - 2; i++) {
                            if (i != 0) {
                                sb.append(' ');
                            }
                            sb.append(words[i]);
                        }
                        lit = sb.toString();
                    }

                    //  add 'CALL JACK JONES at home' using phoneId
                    if (homeId != VoiceContact.ID_UNDEFINED) {
                        addCallIntent(intents, ContentUris.withAppendedId(
                                Phone.CONTENT_URI, homeId),
                                lit + res.getText(R.string.at_home), "H",  0);
                        count++;
                    }

                    //  add 'CALL JACK JONES on mobile' using mobileId
                    if (mobileId != VoiceContact.ID_UNDEFINED) {
                        addCallIntent(intents, ContentUris.withAppendedId(
                                Phone.CONTENT_URI, mobileId),
                                lit + res.getText(R.string.on_mobile), "M", 0);
                        count++;
                    }

                    //  add 'CALL JACK JONES at work' using workId
                    if (workId != VoiceContact.ID_UNDEFINED) {
                        addCallIntent(intents, ContentUris.withAppendedId(
                                Phone.CONTENT_URI, workId),
                                lit + res.getText(R.string.at_work), "W", 0);
                        count++;
                    }

                    //  add 'CALL JACK JONES at other' using otherId
                    if (otherId != VoiceContact.ID_UNDEFINED) {
                        addCallIntent(intents, ContentUris.withAppendedId(
                                Phone.CONTENT_URI, otherId),
                                lit + res.getText(R.string.at_other), "O", 0);
                        count++;
                    }

                    if (fallbackId != VoiceContact.ID_UNDEFINED) {
                        addCallIntent(intents, ContentUris.withAppendedId(
                                Phone.CONTENT_URI, fallbackId),
                                lit, "", 0);
                        count++;
                    }
                }
            }

            else if ("X".equalsIgnoreCase(commands[0])) {
                Intent intent = new Intent(RecognizerEngine.ACTION_RECOGNIZER_RESULT, null);
                intent.putExtra(RecognizerEngine.SENTENCE_EXTRA, literal);
                intent.putExtra(RecognizerEngine.SEMANTIC_EXTRA, semantic);
                addIntent(intents, intent);
            }

            // "CALL VoiceMail"
            else if ("voicemail".equalsIgnoreCase(commands[0]) && commands.length == 1) {
                addCallIntent(intents, Uri.fromParts("voicemail", "x", null),
                        literal, "", Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            }

            // "REDIAL"
            else if ("redial".equalsIgnoreCase(commands[0]) && commands.length == 1) {
                String number = VoiceContact.redialNumber(mActivity);
                if (number != null) {
                    addCallIntent(intents, Uri.fromParts("tel", number, null),
                            literal, "", Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                }
            }

            // "Intent ..."
            else if ("Intent".equalsIgnoreCase(commands[0])) {
                for (int i = 1; i < commands.length; i++) {
                    try {
                        Intent intent = Intent.getIntent(commands[i]);
                        if (intent.getStringExtra(SENTENCE_EXTRA) == null) {
                            intent.putExtra(SENTENCE_EXTRA, literal);
                        }
                        addIntent(intents, intent);
                    } catch (URISyntaxException e) {
                        if (false) {
                            Log.d(TAG, "onRecognitionSuccess: poorly " +
                                    "formed URI in grammar" + e);
                        }
                    }
                }
            }

            // "OPEN ..."
            else if ("OPEN".equalsIgnoreCase(commands[0]) && mAllowOpenEntries) {
                PackageManager pm = mActivity.getPackageManager();
                if (commands.length > 1 & mOpenEntries != null) {
                    // the semantic value is equal to the literal in this case.
                    // We have to do the mapping from this text to the
                    // componentname ourselves.  See Bug: 2457238.
                    // The problem is that the list of all componentnames
                    // can be pretty large and overflow the limit that
                    // the recognizer has.
                    String meaning = mOpenEntries.get(commands[1]);
                    String[] components = meaning.trim().split(" ");
                    for (int i=0; i < components.length; i++) {
                        String component = components[i];
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory("android.intent.category.VOICE_LAUNCH");
                        String packageName = component.substring(
                                0, component.lastIndexOf('/'));
                        String className = component.substring(
                                component.lastIndexOf('/')+1, component.length());
                        intent.setClassName(packageName, className);
                        List<ResolveInfo> riList = pm.queryIntentActivities(intent, 0);
                        for (ResolveInfo ri : riList) {
                            String label = ri.loadLabel(pm).toString();
                            intent = new Intent(Intent.ACTION_MAIN);
                            intent.addCategory("android.intent.category.VOICE_LAUNCH");
                            intent.setClassName(packageName, className);
                            intent.putExtra(SENTENCE_EXTRA, literal.split(" ")[0] + " " + label);
                            addIntent(intents, intent);
                        }
                    }
                }
            }

            // can't parse result
            else {
                if (false) Log.d(TAG, "onRecognitionSuccess: parse error");
            }
        }

        // log if requested
        if (mLogger != null) mLogger.logIntents(intents);

        // bail out if cancelled
        if (Thread.interrupted()) throw new InterruptedException();

        if (intents.size() == 0) {
            // TODO: strip HOME|MOBILE|WORK and try default here?
            recognizerClient.onRecognitionFailure("No Intents generated");
        }
        else {
            recognizerClient.onRecognitionSuccess(
                    intents.toArray(new Intent[intents.size()]));
        }
    }

    // only add if different
    private static void addCallIntent(ArrayList<Intent> intents, Uri uri, String literal,
            String phoneType, int flags) {
        Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, uri)
                .setFlags(flags)
                .putExtra(SENTENCE_EXTRA, literal)
                .putExtra(PHONE_TYPE_EXTRA, phoneType);
        addIntent(intents, intent);
    }
}
