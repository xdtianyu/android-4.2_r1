/* Copyright (C) 2011 The Android Open Source Project.
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

package com.android.exchange.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Parse the result of an ItemOperations command; we use this to load attachments in EAS 14.0
 */
public class ItemOperationsParser extends Parser {
    private final AttachmentLoader mAttachmentLoader;
    private int mStatusCode = 0;
    private final OutputStream mAttachmentOutputStream;
    private final int mAttachmentSize;

    public ItemOperationsParser(AttachmentLoader loader, InputStream in, OutputStream out, int size)
            throws IOException {
        super(in);
        mAttachmentLoader = loader;
        mAttachmentOutputStream = out;
        mAttachmentSize = size;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    private void parseProperties() throws IOException {
        while (nextTag(Tags.ITEMS_PROPERTIES) != END) {
            if (tag == Tags.ITEMS_DATA) {
                // Wrap the input stream in our custom base64 input stream
                Base64InputStream bis = new Base64InputStream(getInput());
                // Read the attachment
                mAttachmentLoader.readChunked(bis, mAttachmentOutputStream, mAttachmentSize);
            } else {
                skipTag();
            }
        }
    }

    private void parseFetch() throws IOException {
        while (nextTag(Tags.ITEMS_FETCH) != END) {
            if (tag == Tags.ITEMS_PROPERTIES) {
                parseProperties();
            } else {
                skipTag();
            }
        }
    }

    private void parseResponse() throws IOException {
        while (nextTag(Tags.ITEMS_RESPONSE) != END) {
            if (tag == Tags.ITEMS_FETCH) {
                parseFetch();
            } else {
                skipTag();
            }
        }
    }

    @Override
    public boolean parse() throws IOException {
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.ITEMS_ITEMS) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.ITEMS_STATUS) {
                // Save the status code
                mStatusCode = getValueInt();
            } else if (tag == Tags.ITEMS_RESPONSE) {
                parseResponse();
            } else {
                skipTag();
            }
        }
        return res;
    }
}
