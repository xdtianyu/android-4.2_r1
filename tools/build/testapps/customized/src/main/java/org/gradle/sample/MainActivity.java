package org.gradle.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String message = "People:";

        Iterable<Person> people = new People();
        for (Person person : people) {
            message += "\n * ";
            message += person.getName();
        }

        TextView textView = (TextView)findViewById(R.id.people);
        textView.setTextSize(20);
        textView.setText(message);
    }
}
