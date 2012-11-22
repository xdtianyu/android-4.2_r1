/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.media.MediaPlayer;
import android.webkit.cts.CtsTestServer;


/**
 * Tests of MediaPlayer streaming capabilities.
 */
public class StreamingMediaPlayerTest extends MediaPlayerTestBase {
    private CtsTestServer mServer;

/* RTSP tests are more flaky and vulnerable to network condition.
   Disable until better solution is available
    // Streaming RTSP video from YouTube
    public void testRTSP_H263_AMR_Video1() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0x271de9756065677e"
                + "&fmt=13&user=android-device-test", 176, 144);
    }
    public void testRTSP_H263_AMR_Video2() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0xc80658495af60617"
                + "&fmt=13&user=android-device-test", 176, 144);
    }

    public void testRTSP_MPEG4SP_AAC_Video1() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0x271de9756065677e"
                + "&fmt=17&user=android-device-test", 176, 144);
    }
    public void testRTSP_MPEG4SP_AAC_Video2() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0xc80658495af60617"
                + "&fmt=17&user=android-device-test", 176, 144);
    }

    public void testRTSP_H264Base_AAC_Video1() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0x271de9756065677e"
                + "&fmt=18&user=android-device-test", 480, 270);
    }
    public void testRTSP_H264Base_AAC_Video2() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0xc80658495af60617"
                + "&fmt=18&user=android-device-test", 480, 270);
    }
*/
    // Streaming HTTP video from YouTube
    public void testHTTP_H263_AMR_Video1() throws Exception {
        playVideoTest("http://v20.lscache8.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=13&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=372FA4C532AA49D14EAF049BCDA66460EEE161E9"
                + ".6D8BF096B73B7A68A7032CA8685053CFB498D30A"
                + "&key=test_key1&user=android-device-test", 176, 144);
    }
    public void testHTTP_H263_AMR_Video2() throws Exception {
        playVideoTest("http://v20.lscache8.c.youtube.com/videoplayback?id=c80658495af60617"
                + "&itag=13&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=191FCD5C4B7400065C20845D7AC2B437B1291F26"
                + ".66F8B8D7EFF7F144141AC67E8E35E078468CE6FB"
                + "&key=test_key1&user=android-device-test", 176, 144);
    }

    public void testHTTP_MPEG4SP_AAC_Video1() throws Exception {
        playVideoTest("http://v20.lscache8.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=17&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=3DCD3F79E045F95B6AF661765F046FB0440FF016"
                + ".06A42661B3AF6BAF046F012549CC9BA34EBC80A9"
                + "&key=test_key1&user=android-device-test", 176, 144);
    }
    public void testHTTP_MPEG4SP_AAC_Video2() throws Exception {
        playVideoTest("http://v20.lscache8.c.youtube.com/videoplayback?id=c80658495af60617"
                + "&itag=17&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=242B7AEF3AB38519F593203FDEF420E2A585DA6E"
                + ".4A57C03AF6859FE4694CD69C3225E386373A98B0"
                + "&key=test_key1&user=android-device-test", 176, 144);
    }

    public void testHTTP_H264Base_AAC_Video1() throws Exception {
        playVideoTest("http://v20.lscache8.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=18&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=1219C2B07AF0638C27916307A6093C0E43CB894E"
                + ".126B6B916BD57157782738AA7C03E59F21DBC168"
                + "&key=test_key1&user=android-device-test", 480, 270);
    }
    public void testHTTP_H264Base_AAC_Video2() throws Exception {
        playVideoTest("http://v20.lscache8.c.youtube.com/videoplayback?id=c80658495af60617"
                + "&itag=18&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&sparams=ip,ipbits,expire,ip,ipbits,expire,id,itag"
                + "&signature=61674BC069F2C812A18C390DE785CECD296228C7"
                + ".11F5FCE823FB8FA31269A06A483C2F9B2C22F357"
                + "&key=test_key1&user=android-device-test", 480, 270);
    }

    // Streaming HLS video from YouTube
    public void testHLS() throws Exception {
        // Play stream for 60 seconds
        playLiveVideoTest("http://www.youtube.com/api/manifest/hls/ns/yt-live/id/UeHRu5LFHaU"
                + "?ip=0.0.0.0&ipbits=0&expire=19000000000&sparams=ip,ipbits,expire&signature"
                + "=313BE90526F2D815EB207156E1460C7E8EEC2503.799EE7B8B7CE3F2957060DB27C216077"
                + "0303EBD2&key=test_key1&user=android-device-test&m3u8=1", 60 * 1000);
    }

    // Streaming audio from local HTTP server
    public void testPlayMp3Stream1() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", false, false);
    }
    public void testPlayMp3Stream2() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", false, false);
    }
    public void testPlayMp3StreamRedirect() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", true, false);
    }
    public void testPlayMp3StreamNoLength() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.mp3", false, true);
    }
    public void testPlayOggStream() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", false, false);
    }
    public void testPlayOggStreamRedirect() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", true, false);
    }
    public void testPlayOggStreamNoLength() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", false, true);
    }

    private void localHttpAudioStreamTest(final String name, boolean redirect, boolean nolength)
            throws Throwable {
        mServer = new CtsTestServer(mContext);
        try {
            String stream_url = null;
            if (redirect) {
                // Stagefright doesn't have a limit, but we can't test support of infinite redirects
                // Up to 4 redirects seems reasonable though.
                stream_url = mServer.getRedirectingAssetUrl(name, 4);
            } else {
                stream_url = mServer.getAssetUrl(name);
            }
            if (nolength) {
                stream_url = stream_url + "?" + CtsTestServer.NOLENGTH_POSTFIX;
            }

            mMediaPlayer.setDataSource(stream_url);

            mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
            mMediaPlayer.setScreenOnWhilePlaying(true);

            mOnBufferingUpdateCalled.reset();
            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    mOnBufferingUpdateCalled.signal();
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    fail("Media player had error " + what + " playing " + name);
                    return true;
                }
            });

            assertFalse(mOnBufferingUpdateCalled.isSignalled());
            mMediaPlayer.prepare();

            if (nolength) {
                mMediaPlayer.start();
                Thread.sleep(LONG_SLEEP_TIME);
                assertFalse(mMediaPlayer.isPlaying());
            } else {
                mOnBufferingUpdateCalled.waitForSignal();
                mMediaPlayer.start();
                Thread.sleep(SLEEP_TIME);
            }
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        } finally {
            mServer.shutdown();
        }
    }
}
