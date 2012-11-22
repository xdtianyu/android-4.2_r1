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
package com.example.plasma.llvm;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import android.content.res.Resources;

public class Plasma extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        view = new PlasmaView(this);
        setContentView(view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_mode:
                view.switchMode();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private PlasmaView view;

    /* load our native library */
    static {
        System.loadLibrary("plasma");
    }
}

class PlasmaView extends View {
    private Bitmap mBitmap;
    private long mStartTime;

    /* implementend by libplasma.so */
    private static native boolean gdk();
    private static native int nativeRenderPlasma(Bitmap  bitmap, long time_ms, byte[] script, int scriptLength, boolean useLLVM);

    private byte[] pgm;
    private int pgmLength;

    private boolean llvm_mode = false;
    private Paint paint = null;

    public void switchMode() {
        if (gdk())
            llvm_mode = !llvm_mode;
    }

    public PlasmaView(Context context) {
        super(context);

        llvm_mode = gdk();

        mStartTime = System.currentTimeMillis();
        if (llvm_mode)
        {
            InputStream is = null;
            is = getResources().openRawResource(R.raw.libplasma_portable);
            try {
                try {
                    pgm = new byte[1024];
                    pgmLength = 0;
                    while(true) {
                        int bytesLeft = pgm.length - pgmLength;
                        if (bytesLeft == 0) {
                            byte[] buf2 = new byte[pgm.length * 2];
                            System.arraycopy(pgm, 0, buf2, 0, pgm.length);
                            pgm = buf2;
                            bytesLeft = pgm.length - pgmLength;
                        }
                        int bytesRead = is.read(pgm, pgmLength, bytesLeft);
                        if (bytesRead <= 0) {
                            break;
                        }
                        pgmLength += bytesRead;
                    }
                } finally {
                    is.close();
                }
            } catch(IOException e) {
                throw new Resources.NotFoundException();
            }
        }

        paint = new Paint();
        paint.setTextSize(40);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (mBitmap == null || mBitmap.getWidth() != getWidth() || mBitmap.getHeight() != getHeight())
            mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);

        int frameRate = nativeRenderPlasma(mBitmap, System.currentTimeMillis() - mStartTime, pgm, pgmLength, llvm_mode);
        canvas.drawBitmap(mBitmap, 0, 0, null);
        canvas.drawText((llvm_mode ? "LLVM/GDK" : "Native") + " Frame: " + Integer.toString(frameRate), 100, 100, paint);

        // force a redraw, with a different time-based pattern.
        invalidate();
    }
}
