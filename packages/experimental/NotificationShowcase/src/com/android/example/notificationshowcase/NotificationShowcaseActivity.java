// dummy notifications for demos
// for anandx@google.com by dsandler@google.com

package com.android.example.notificationshowcase;

import java.util.ArrayList;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class NotificationShowcaseActivity extends Activity {
    private static final String TAG = "NotificationShowcase";
    
    private static final int NOTIFICATION_ID = 31338;

    private static int bigtextId;
    private static int uploadId;

    private static final boolean FIRE_AND_FORGET = true;
    
    public static class ToastFeedbackActivity extends Activity {
        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
        }
        
        @Override
        public void onResume() {
            super.onResume();
            Intent i = getIntent();
            Log.v(TAG, "clicked a thing! intent=" + i.toString());
            if (i.hasExtra("text")) {
                final String text = i.getStringExtra("text");
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    public static class UpdateService extends Service {
        @Override
            public IBinder onBind(Intent intent) {
            Log.v(TAG, "onbind");
            return null;
        }
        
        @Override
        public void onStart(Intent i, int startId) {
            super.onStart(i, startId);
            try {
                // allow the user close the shade, if they want to test that.
                Thread.sleep(3000);
            } catch (Exception e) {
            }
            Log.v(TAG, "clicked a thing! intent=" + i.toString());
            if (i.hasExtra("id") && i.hasExtra("when")) {
                final int id = i.getIntExtra("id", 0);
                if (id == bigtextId) {
                    NotificationManager noMa =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    final int update = i.getIntExtra("update", 0);
                    final long when = i.getLongExtra("when", 0L);
                    Log.v(TAG, "id: " + id + " when: " + when + " update: " + update);
                    noMa.notify(NOTIFICATION_ID + id,
                                makeBigTextNotification(this, update, id, when));
                }
            } else {
                Log.v(TAG, "id extra was " + (i.hasExtra("id") ? "present" : "missing"));
                Log.v(TAG, "when extra was " + (i.hasExtra("when") ? "present"  : "missing"));
            }
        }
    }
    
    public static class ProgressService extends Service {
        @Override
            public IBinder onBind(Intent intent) {
            Log.v(TAG, "onbind");
            return null;
        }
        
        @Override
        public void onStart(Intent i, int startId) {
            super.onStart(i, startId);
            if (i.hasExtra("id") && i.hasExtra("when") && i.hasExtra("progress")) {
                final int id = i.getIntExtra("id", 0);
                if (id == uploadId) {
                    final long when = i.getLongExtra("when", 0L);
                    int progress = i.getIntExtra("progress", 0);
                    NotificationManager noMa = (NotificationManager)
                            getSystemService(Context.NOTIFICATION_SERVICE);
                    while (progress <= 100) {
                        try {
                            // allow the user close the shade, if they want to test that.
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                        Log.v(TAG, "id: " + id + " when: " + when + " progress: " + progress);
                        noMa.notify(NOTIFICATION_ID + id,
                                    makeUploadNotification(this, progress, id, when));
                        progress+=10;
                    }
                }
            } else {
                Log.v(TAG, "id extra " + (i.hasExtra("id") ? "present" : "missing"));
                Log.v(TAG, "when extra " + (i.hasExtra("when") ? "present"  : "missing"));
                Log.v(TAG, "progress extra " + (i.hasExtra("progress") ? "present"  : "missing"));
            }
        }
    }
    
    private ArrayList<Notification> mNotifications = new ArrayList<Notification>();
    NotificationManager mNoMa;

    static int mLargeIconWidth, mLargeIconHeight;
    private static Bitmap getBitmap(Context context, int resId) {
        Drawable d = context.getResources().getDrawable(resId);
        Bitmap b = Bitmap.createBitmap(mLargeIconWidth, mLargeIconHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, mLargeIconWidth, mLargeIconHeight);
        d.draw(c);
        return b;
    }
    
    private static PendingIntent makeToastIntent(Context context, String s) {
        Intent toastIntent = new Intent(context, ToastFeedbackActivity.class);
        toastIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        toastIntent.putExtra("text", s);
        PendingIntent pi = PendingIntent.getActivity(
                context, 58, toastIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        return pi;
    }
    
    private static PendingIntent makeEmailIntent(Context context, String who) {
        final Intent intent = new Intent(android.content.Intent.ACTION_SENDTO,
                Uri.parse("mailto:" + who));
        return PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }
    
    // this is a service, it will only close the notification shade if used as a contentIntent.
    private static int updateId = 3000;
    private static PendingIntent makeUpdateIntent(Context context, int update, int id, long when) {
        Intent updateIntent = new Intent();
        updateIntent.setComponent(
                new ComponentName(context, UpdateService.class));
        updateIntent.putExtra("id", id);
        updateIntent.putExtra("when", when);
        updateIntent.putExtra("update", update);
        Log.v(TAG, "added id extra " + id);
        Log.v(TAG, "added when extra " + when);
        PendingIntent pi = PendingIntent.getService(
                context, updateId++, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }

    private static Notification makeBigTextNotification(Context context, int update, int id,
                                                        long when) {
        String addendum = update > 0 ? "(updated) " : "";
        String longSmsText = "Hey, looks like\nI'm getting kicked out of this conference" + 
                " room";
        if (update > 1) {
            longSmsText += ", so stay in the hangout and I'll rejoin in about 5-10 minutes" + 
                ". If you don't see me, assume I got pulled into another meeting. And" + 
                " now \u2026 I have to find my shoes.  Four score and seven years "+
                "ago our fathers brought forth on this continent, a new nation, conceived "+
                "in Liberty, and dedicated to the proposition that all men are created "+
                "equal. Now we are engaged in a great civil war, testing whether that "+
                "nation, or any nation so conceived and so dedicated, can long "+
                "endure. We are met on a great battle-field of that war. We have come "+
                "to dedicate a portion of that field, as a final resting place for "+
                "those who here gave their lives that that nation might live. It is "+
                "altogether fitting and proper that we should do this.But, in a larger "+
                "sense, we can not dedicate -- we can not consecrate -- we can not "+
                "hallow -- this ground.The brave men, living and dead, who struggled "+
                "here, have consecrated it, far above our poor power to add or detract."+
                " The world will little note, nor long remember what we say here, but "+
                "it can never forget what they did here. It is for us the living, rather,"+
                " to be dedicated here to the unfinished work which they who fought "+
                "here have thus far so nobly advanced.It is rather for us to be here "+
                "dedicated to the great task remaining before us -- that from these "+
                "honored dead we take increased devotion to that cause for which they "+
                "gave the last full measure of devotion -- that we here highly resolve "+
                "that these dead shall not have died in vain -- that this nation, under "+
                "God, shall have a new birth of freedom -- and that government of "+
                "the people, by the people, for the people, shall not perish from the earth.";
        }
        if (update > 2) {
            when = System.currentTimeMillis();
        }
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(addendum + longSmsText);
        NotificationCompat.Builder bigTextNotification = new NotificationCompat.Builder(context)
                .setContentTitle(addendum + "Mike Cleron")
                .setContentIntent(makeToastIntent(context, "Clicked on bigText"))
                .setContentText(addendum + longSmsText)
                .setTicker(addendum + "Mike Cleron: " + longSmsText)
                .setWhen(when)
                .setLargeIcon(getBitmap(context, R.drawable.bucket))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_media_next,
                           "update: " + update,
                           makeUpdateIntent(context, update+1, id, when))
                .setSmallIcon(R.drawable.stat_notify_talk_text)
                .setStyle(bigTextStyle);
        return bigTextNotification.build();
    }
    
    // this is a service, it will only close the notification shade if used as a contentIntent.
    private static void startProgressUpdater(Context context, int progress, int id, long when) {
        Intent progressIntent = new Intent();
        progressIntent.setComponent(new ComponentName(context, ProgressService.class));
        progressIntent.putExtra("id", id);
        progressIntent.putExtra("when", when);
        progressIntent.putExtra("progress", progress);
        context.startService(progressIntent);
    }

    private static Notification makeUploadNotification(Context context, int progress, int id,
                                                       long when) {
        NotificationCompat.Builder uploadNotification = new NotificationCompat.Builder(context)
                .setContentTitle("File Upload")
                .setContentText("foo.txt")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(makeToastIntent(context, "Clicked on Upload"))
                .setWhen(when)
                .setSmallIcon(R.drawable.ic_menu_upload)
                .setProgress(100, Math.min(progress, 100), false);
        return uploadNotification.build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mLargeIconWidth = (int) getResources().getDimension(R.dimen.notification_large_icon_width);
        mLargeIconHeight = (int) getResources().getDimension(R.dimen.notification_large_icon_height);
        
        mNoMa = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        bigtextId = mNotifications.size();
        mNotifications.add(makeBigTextNotification(this, 0, bigtextId,
                                                   System.currentTimeMillis()));
        
        uploadId = mNotifications.size();
        long uploadWhen = System.currentTimeMillis();
        mNotifications.add(makeUploadNotification(this, 10, uploadId, uploadWhen));

        mNotifications.add(new NotificationCompat.Builder(this)
        .setContentTitle("Incoming call")
        .setContentText("Matias Duarte")
        .setLargeIcon(getBitmap(this, R.drawable.matias_hed))
        .setSmallIcon(R.drawable.stat_sys_phone_call)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setContentIntent(makeToastIntent(this, "Clicked on Matias"))
        .addAction(R.drawable.ic_dial_action_call, "Answer", makeToastIntent(this, "call answered"))
        .addAction(R.drawable.ic_end_call, "Ignore", makeToastIntent(this, "call ignored"))
        .setAutoCancel(true)
        .build());

        mNotifications.add(new NotificationCompat.Builder(this)
        .setContentTitle("Stopwatch PRO")
        .setContentText("Counting up")
        .setContentIntent(makeToastIntent(this, "Clicked on Stopwatch"))
        .setSmallIcon(R.drawable.stat_notify_alarm)
        .setUsesChronometer(true)
        .build());

        mNotifications.add(new NotificationCompat.Builder(this)
            .setContentTitle("J Planning")
            .setContentText("The Botcave")
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.stat_notify_calendar)
            .setContentIntent(makeToastIntent(this, "tapped in the calendar event"))
            .setContentInfo("7PM")
            .addAction(R.drawable.stat_notify_snooze, "+10 min",
                       makeToastIntent(this, "snoozed 10 min"))
            .addAction(R.drawable.stat_notify_snooze_longer, "+1 hour",
                       makeToastIntent(this, "snoozed 1 hr"))
            .addAction(R.drawable.stat_notify_email, "Email",
                       makeEmailIntent(this, 
                               "gabec@example.com,mcleron@example.com,dsandler@example.com"))
            .build());

        BitmapDrawable d =
                (BitmapDrawable) getResources().getDrawable(R.drawable.romainguy_rockaway);
        mNotifications.add(new NotificationCompat.BigPictureStyle(
                new NotificationCompat.Builder(this)
                    .setContentTitle("Romain Guy")
                    .setContentText("I was lucky to find a Canon 5D Mk III at a local Bay Area store last "
                            + "week but I had not been able to try it in the field until tonight. After a "
                            + "few days of rain the sky finally cleared up. Rockaway Beach did not disappoint "
                            + "and I was finally able to see what my new camera feels like when shooting "
                            + "landscapes.")
                    .setSmallIcon(R.drawable.ic_stat_gplus)
                    .setContentIntent(makeToastIntent(this, "Clicked on bigPicture"))
                    .setLargeIcon(getBitmap(this, R.drawable.romainguy_hed))
                    .addAction(R.drawable.add, "Add to Gallery",
                               makeToastIntent(this, "added! (just kidding)"))
                    .setSubText("talk rocks!")
                )
                .bigPicture(d.getBitmap())
                .build());

        // Note: this may conflict with real email notifications
        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        SpannableString line1 = new SpannableString("Alice: hey there!");
        line1.setSpan(bold, 0, 5, 0);
        SpannableString line2 = new SpannableString("Bob: hi there!");
        line2.setSpan(bold, 0, 3, 0);
        SpannableString line3 = new SpannableString("Charlie: Iz IN UR EMAILZ!!");
        line3.setSpan(bold, 0, 7, 0);
        mNotifications.add(new NotificationCompat.InboxStyle(
                new NotificationCompat.Builder(this)
                .setContentTitle("24 new messages")
                .setContentText("You have mail!")
                .setSubText("test.hugo2@gmail.com")
                .setContentIntent(makeToastIntent(this, "Clicked on Email"))
                .setSmallIcon(R.drawable.stat_notify_email))
           .setSummaryText("+21 more")
           .addLine(line1)
           .addLine(line2)
           .addLine(line3)
           .build());

        mNotifications.add(new NotificationCompat.Builder(this)
        .setContentTitle("Twitter")
        .setContentText("New mentions")
        .setContentIntent(makeToastIntent(this, "Clicked on Twitter"))
        .setSmallIcon(R.drawable.twitter_icon)
        .setNumber(15)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build());

        if (FIRE_AND_FORGET) {
            doPost(null);
            startProgressUpdater(this, 10, uploadId, uploadWhen);
            finish();
        }
    }
    
    public void doPost(View v) {
        for (int i=0; i<mNotifications.size(); i++) {
            mNoMa.notify(NOTIFICATION_ID + i, mNotifications.get(i));
        }
    }
    
    public void doRemove(View v) {
        for (int i=0; i<mNotifications.size(); i++) {
            mNoMa.cancel(NOTIFICATION_ID + i);
        }
    }
}
