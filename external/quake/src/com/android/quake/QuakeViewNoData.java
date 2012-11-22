/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.quake;

import android.view.View;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Canvas;

public class QuakeViewNoData extends View {
    public QuakeViewNoData(Context context, int reason)
    {
        super(context);
        mReason = reason;
    }

    public static final int E_NODATA = 1;
    public static final int E_INITFAILED = 2;

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(0xffffffff);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(0xff000000);
        switch(mReason)
        {
            case E_NODATA:
                canvas.drawText("Missing data files. Looking for one of:",
                        10.0f, 20.0f, paint);
                canvas.drawText("/sdcard/data/quake/id1/pak0.pak",
                        10.0f, 35.0f, paint);
                canvas.drawText("/data/quake/id1/pak0.pak",
                        10.0f, 50.0f, paint);
                canvas.drawText("Please copy a pak file to the device and reboot.",
                        10.0f, 65.0f, paint);
                break;
            case E_INITFAILED:
                canvas.drawText("Quake C library initialization failed.",
                        10.0f, 20.0f, paint);
                canvas.drawText("Try stopping and restarting the simulator.",
                        10.0f, 35.0f, paint);
                break;
        }
    }

    private int mReason;

}
