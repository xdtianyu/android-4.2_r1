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

package android.os.cts;


import android.content.Context;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ParcelFileDescriptorTest extends AndroidTestCase {
    private static final long DURATION = 100l;

    private TestThread mTestThread;

    public void testConstructorAndOpen() throws Exception {
        ParcelFileDescriptor tempFile = makeParcelFileDescriptor(getContext());

        ParcelFileDescriptor pfd = new ParcelFileDescriptor(tempFile);
        AutoCloseInputStream in = new AutoCloseInputStream(pfd);
        try {
            // read the data that was wrote previously
            assertEquals(0, in.read());
            assertEquals(1, in.read());
            assertEquals(2, in.read());
            assertEquals(3, in.read());
        } finally {
            in.close();
        }
    }

    public void testFromSocket() throws Throwable {
        final int PORT = 12222;
        final int DATA = 1;

        mTestThread = new TestThread(new Runnable() {
            public void run() {
                try {
                    ServerSocket ss;
                    ss = new ServerSocket(PORT);
                    Socket sSocket = ss.accept();
                    OutputStream out = sSocket.getOutputStream();
                    out.write(DATA);
                    Thread.sleep(DURATION);
                    out.close();
                } catch (Exception e) {
                    mTestThread.setThrowable(e);
                }
            }
        });
        mTestThread.start();

        Thread.sleep(DURATION);
        Socket socket;
        socket = new Socket(InetAddress.getLocalHost(), PORT);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
        AutoCloseInputStream in = new AutoCloseInputStream(pfd);
        assertEquals(DATA, in.read());
        in.close();
        socket.close();
        pfd.close();

        mTestThread.joinAndCheck(DURATION * 2);
    }

    public void testFromData() throws IOException {
        assertNull(ParcelFileDescriptor.fromData(null, null));
        byte[] data = new byte[] { 0 };
        assertFileDescriptorContent(data, ParcelFileDescriptor.fromData(data, null));
        data = new byte[] { 0, 1, 2, 3 };
        assertFileDescriptorContent(data, ParcelFileDescriptor.fromData(data, null));
        data = new byte[0];
        assertFileDescriptorContent(data, ParcelFileDescriptor.fromData(data, null));

        // Check that modifying the data does not modify the data in the FD
        data = new byte[] { 0, 1, 2, 3 };
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromData(data, null);
        data[1] = 42;
        assertFileDescriptorContent(new byte[] { 0, 1, 2, 3 }, pfd);
    }

    private static void assertFileDescriptorContent(byte[] expected, ParcelFileDescriptor fd)
        throws IOException {
        assertInputStreamContent(expected, new ParcelFileDescriptor.AutoCloseInputStream(fd));
    }

    private static void assertInputStreamContent(byte[] expected, InputStream is)
            throws IOException {
        try {
            byte[] observed = new byte[expected.length];
            int count = is.read(observed);
            assertEquals(expected.length, count);
            assertEquals(-1, is.read());
            MoreAsserts.assertEquals(expected, observed);
        } finally {
            is.close();
        }
    }

    public void testFromDataSkip() throws IOException {
        byte[] data = new byte[] { 40, 41, 42, 43, 44, 45, 46 };
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromData(data, null);
        assertNotNull(pfd);
        FileDescriptor fd = pfd.getFileDescriptor();
        assertNotNull(fd);
        assertTrue(fd.valid());
        FileInputStream is = new FileInputStream(fd);
        try {
            assertEquals(1, is.skip(1));
            assertEquals(41, is.read());
            assertEquals(42, is.read());
            assertEquals(2, is.skip(2));
            assertEquals(45, is.read());
            assertEquals(46, is.read());
            assertEquals(-1, is.read());
        } finally {
            is.close();
        }
    }

    public void testToString() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertNotNull(pfd.toString());
    }

    public void testWriteToParcel() throws Exception {
        ParcelFileDescriptor pf = makeParcelFileDescriptor(getContext());

        Parcel pl = Parcel.obtain();
        pf.writeToParcel(pl, ParcelFileDescriptor.PARCELABLE_WRITE_RETURN_VALUE);
        pl.setDataPosition(0);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.CREATOR.createFromParcel(pl);
        AutoCloseInputStream in = new AutoCloseInputStream(pfd);
        try {
            // read the data that was wrote previously
            assertEquals(0, in.read());
            assertEquals(1, in.read());
            assertEquals(2, in.read());
            assertEquals(3, in.read());
        } finally {
            in.close();
        }
    }

    public void testClose() throws Exception {
        ParcelFileDescriptor pf = makeParcelFileDescriptor(getContext());
        AutoCloseInputStream in1 = new AutoCloseInputStream(pf);
        try {
            assertEquals(0, in1.read());
        } finally {
            in1.close();
        }

        pf.close();

        AutoCloseInputStream in2 = new AutoCloseInputStream(pf);
        try {
            assertEquals(0, in2.read());
            fail("Failed to throw exception.");
        } catch (Exception e) {
            // expected
        } finally {
            in2.close();
        }
    }

    public void testGetStatSize() throws Exception {
        ParcelFileDescriptor pf = makeParcelFileDescriptor(getContext());
        assertTrue(pf.getStatSize() >= 0);
    }

    public void testGetFileDescriptor() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertNotNull(pfd.getFileDescriptor());

        ParcelFileDescriptor p = new ParcelFileDescriptor(pfd);
        assertSame(pfd.getFileDescriptor(), p.getFileDescriptor());
    }

    public void testDescribeContents() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertTrue((Parcelable.CONTENTS_FILE_DESCRIPTOR & pfd.describeContents()) != 0);
    }

    static ParcelFileDescriptor makeParcelFileDescriptor(Context con) throws Exception {
        final String fileName = "testParcelFileDescriptor";

        FileOutputStream fout = null;

        fout = con.openFileOutput(fileName, Context.MODE_WORLD_WRITEABLE);

        try {
            fout.write(new byte[] { 0x0, 0x1, 0x2, 0x3 });
        } finally {
            fout.close();
        }

        File dir = con.getFilesDir();
        File file = new File(dir, fileName);
        ParcelFileDescriptor pf = null;

        pf = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);

        return pf;
    }
}
