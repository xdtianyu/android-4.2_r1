package org.gradle.sample;

import android.app.Activity;
import android.view.View;
import android.content.Intent;
import android.os.Bundle;

import android.annotation.TargetApi;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @TargetApi(10)
    public void sendMessage(View view) {
        Intent intent = new Intent(this, ShowPeopleActivity.class);
        startActivity(intent);
    }
}
