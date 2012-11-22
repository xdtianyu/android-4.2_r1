package com.example.hellollvm;

import android.app.Activity;
import android.widget.TextView;
import android.os.Bundle;

public class HelloLLVM extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        TextView  tv = new TextView(this);
        test_func();
        tv.setText("HelloLLVM!");
        setContentView(tv);
    }

    public native void test_func();

    static {
        System.loadLibrary("hello_llvm");
    }
}
