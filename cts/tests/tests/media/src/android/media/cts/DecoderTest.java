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

package android.media.cts;

import com.android.cts.media.R;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

public class DecoderTest extends AndroidTestCase {
    private static final String TAG = "DecoderTest";

    private Resources mResources;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        mResources = mContext.getResources();
    }

    // The allowed errors in the following tests are the actual maximum measured
    // errors with the standard decoders, plus 10%.
    // This should allow for some variation in decoders, while still detecting
    // phase and delay errors, channel swap, etc.
    public void testDecodeMp3Lame() throws Exception {
        decode(R.raw.sinesweepmp3lame, R.raw.sinesweepraw, 804.f);
    }
    public void testDecodeMp3Smpb() throws Exception {
        decode(R.raw.sinesweepmp3smpb, R.raw.sinesweepraw, 413.f);
    }
    public void testDecodeM4a() throws Exception {
        decode(R.raw.sinesweepm4a, R.raw.sinesweepraw, 124.f);
    }
    public void testDecodeOgg() throws Exception {
        decode(R.raw.sinesweepogg, R.raw.sinesweepraw, 168.f);
    }
    public void testDecodeWav() throws Exception {
        decode(R.raw.sinesweepwav, R.raw.sinesweepraw, 0.0f);
    }
    public void testDecodeFlac() throws Exception {
        decode(R.raw.sinesweepflac, R.raw.sinesweepraw, 0.0f);
    }

    /**
     * @param testinput the file to decode
     * @param master the "golden master" to compared against
     * @param maxerror the maximum allowed root mean squared error
     * @throws IOException
     */
    private void decode(int testinput, int master, float maxerror) throws IOException {

        // read master file into memory
        AssetFileDescriptor masterFd = mResources.openRawResourceFd(master);
        long masterLength = masterFd.getLength();
        short[] masterBuffer = new short[(int) (masterLength / 2)];
        InputStream is = masterFd.createInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        for (int i = 0; i < masterBuffer.length; i++) {
            int lo = bis.read();
            int hi = bis.read();
            if (hi >= 128) {
                hi -= 256;
            }
            int sample = hi * 256 + lo;
            masterBuffer[i] = (short) sample;
        }
        bis.close();

        AssetFileDescriptor testFd = mResources.openRawResourceFd(testinput);

        MediaExtractor extractor;
        MediaCodec codec;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        extractor = new MediaExtractor();
        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                testFd.getLength());

        assertEquals("wrong number of tracks", 1, extractor.getTrackCount());
        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        assertTrue("not an audio file", mime.startsWith("audio/"));

        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        extractor.selectTrack(0);

        // start decoding
        int numBytesDecoded = 0;
        int maxdelta = 0;
        long totalErrorSquared = 0;
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize =
                        extractor.readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0) {
                        Log.d(TAG, "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }

                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
            }

            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {
                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                // check the waveform matches within the specified max error
                for (int i = 0; i < info.size; i += 2) {
                    short sample = buf.getShort(i);
                    int idx = (numBytesDecoded + i) / 2;
                    assertTrue("decoder returned too much data", idx < masterBuffer.length);
                    short mastersample = masterBuffer[idx];
                    int d = sample - mastersample;
                    totalErrorSquared += d * d;
                }

                numBytesDecoded += info.size;

                codec.releaseOutputBuffer(outputBufIndex, false /* render */);

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();

                Log.d(TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();

                Log.d(TAG, "output format has changed to " + oformat);
            }
        }

        codec.stop();
        codec.release();
        testFd.close();
        masterFd.close();

        assertEquals("wrong data size", masterLength, numBytesDecoded);
        long avgErrorSquared = (totalErrorSquared / (numBytesDecoded / 2));
        double rmse = Math.sqrt(avgErrorSquared); 
        assertTrue("decoding error too big: " + rmse, rmse <= maxerror);
    }

}

