/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */

//Contributors: Jonathan Cox, Bogdan Onoiu, Jerry Tian
// Greatly simplified for Google, Inc. by Marc Blank

package com.android.exchange.adapter;

import android.content.ContentValues;
import android.util.Log;

import com.android.exchange.Eas;
import com.android.exchange.utility.FileLogger;
import com.google.common.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Serializer {
    private static final String TAG = "Serializer";
    private static final int BUFFER_SIZE = 16*1024;
    private static final int NOT_PENDING = -1;

    private final OutputStream mOutput;
    private int mPendingTag = NOT_PENDING;
    private int mDepth;
    private String[] mNameStack = new String[20];
    private int mTagPage = 0;
    private boolean mLogging = Log.isLoggable(TAG, Log.VERBOSE);

    public Serializer() throws IOException {
        this(new ByteArrayOutputStream(), true);
    }

    public Serializer(OutputStream os) throws IOException {
        this(os, true);
    }

    @VisibleForTesting
    public Serializer(boolean startDocument) throws IOException {
        this(new ByteArrayOutputStream(), startDocument);
    }

    /**
     * Base constructor
     * @param outputStream the stream we're serializing to
     * @param startDocument whether or not to start a document
     * @param _logging whether or not to log our output
     * @throws IOException
     */
    public Serializer(OutputStream outputStream, boolean startDocument) throws IOException {
        super();
        mOutput = outputStream;
        if (startDocument) {
            startDocument();
        } else {
            mOutput.write(0);
        }
    }

    void log(String str) {
        int cr = str.indexOf('\n');
        if (cr > 0) {
            str = str.substring(0, cr);
        }
        Log.v(TAG, str);
        if (Eas.FILE_LOG) {
            FileLogger.log(TAG, str);
        }
    }

    public void done() throws IOException {
        if (mDepth != 0) {
            throw new IOException("Done received with unclosed tags");
        }
        mOutput.flush();
    }

    public void startDocument() throws IOException{
        mOutput.write(0x03); // version 1.3
        mOutput.write(0x01); // unknown or missing public identifier
        mOutput.write(106);  // UTF-8
        mOutput.write(0);    // 0 length string array
    }

    public void checkPendingTag(boolean degenerated) throws IOException {
        if (mPendingTag == NOT_PENDING)
            return;

        int page = mPendingTag >> Tags.PAGE_SHIFT;
        int tag = mPendingTag & Tags.PAGE_MASK;
        if (page != mTagPage) {
            mTagPage = page;
            mOutput.write(Wbxml.SWITCH_PAGE);
            mOutput.write(page);
        }

        mOutput.write(degenerated ? tag : tag | Wbxml.WITH_CONTENT);
        if (mLogging) {
            String name = Tags.pages[page][tag - 5];
            mNameStack[mDepth] = name;
            log("<" + name + '>');
        }
        mPendingTag = NOT_PENDING;
    }

    public Serializer start(int tag) throws IOException {
        checkPendingTag(false);
        mPendingTag = tag;
        mDepth++;
        return this;
    }

    public Serializer end() throws IOException {
        if (mPendingTag >= 0) {
            checkPendingTag(true);
        } else {
            mOutput.write(Wbxml.END);
            if (mLogging) {
                log("</" + mNameStack[mDepth] + '>');
            }
        }
        mDepth--;
        return this;
    }

    public Serializer tag(int t) throws IOException {
        start(t);
        end();
        return this;
    }

    public Serializer data(int tag, String value) throws IOException {
        if (value == null) {
            Log.e(TAG, "Writing null data for tag: " + tag);
        }
        start(tag);
        text(value);
        end();
        return this;
    }

    public Serializer text(String text) throws IOException {
        if (text == null) {
            Log.e(TAG, "Writing null text for pending tag: " + mPendingTag);
        }
        checkPendingTag(false);
        mOutput.write(Wbxml.STR_I);
        writeLiteralString(mOutput, text);
        if (mLogging) {
            log(text);
        }
        return this;
    }

    public Serializer opaque(InputStream is, int length) throws IOException {
        checkPendingTag(false);
        mOutput.write(Wbxml.OPAQUE);
        writeInteger(mOutput, length);
        if (mLogging) {
            log("Opaque, length: " + length);
        }
        // Now write out the opaque data in batches
        byte[] buffer = new byte[BUFFER_SIZE];
        while (length > 0) {
            int bytesRead = is.read(buffer, 0, (int)Math.min(BUFFER_SIZE, length));
            if (bytesRead == -1) {
                break;
            }
            mOutput.write(buffer, 0, bytesRead);
            length -= bytesRead;
        }
        return this;
    }

    public Serializer opaqueWithoutData(int length) throws IOException {
        checkPendingTag(false);
        mOutput.write(Wbxml.OPAQUE);
        writeInteger(mOutput, length);
        return this;
    }

    void writeInteger(OutputStream out, int i) throws IOException {
        byte[] buf = new byte[5];
        int idx = 0;

        do {
            buf[idx++] = (byte) (i & 0x7f);
            i = i >> 7;
        } while (i != 0);

        while (idx > 1) {
            out.write(buf[--idx] | 0x80);
        }
        out.write(buf[0]);
        if (mLogging) {
            log(Integer.toString(i));
        }
    }

    void writeLiteralString(OutputStream out, String s) throws IOException {
        byte[] data = s.getBytes("UTF-8");
        out.write(data);
        out.write(0);
    }

    void writeStringValue (ContentValues cv, String key, int tag) throws IOException {
        String value = cv.getAsString(key);
        if (value != null && value.length() > 0) {
            data(tag, value);
        } else {
            tag(tag);
        }
    }

    @Override
    public String toString() {
        if (mOutput instanceof ByteArrayOutputStream) {
            return ((ByteArrayOutputStream)mOutput).toString();
        }
        throw new IllegalStateException();
    }

    public byte[] toByteArray() {
        if (mOutput instanceof ByteArrayOutputStream) {
            return ((ByteArrayOutputStream)mOutput).toByteArray();
        }
        throw new IllegalStateException();
    }

}
