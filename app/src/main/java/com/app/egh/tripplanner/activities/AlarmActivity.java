package com.app.egh.tripplanner.activities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.app.egh.tripplanner.R;
import com.app.egh.tripplanner.data.model.Adapter;
import com.app.egh.tripplanner.data.model.NoteHeadService;
import com.app.egh.tripplanner.data.model.Trip;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.javiersantos.materialstyleddialogs.enums.Style;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    final static String TAG = "powerLock";
    Ringtone r;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        this.setFinishOnTouchOutside(false);


       setContentView(R.layout.activity_alarm);

       Intent intent = getIntent();
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        if (alert == null) {
            // alert is null, using backup
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // I can't see this ever being null (as always have a default notification)
            // but just incase
            if (alert == null) {
                // alert backup is null, using 2nd backup
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }

         r = RingtoneManager.getRingtone(getApplicationContext(), alert);
        r.play();

        int tripId = intent.getIntExtra("tripReminder",-1);
        Adapter adb = new Adapter(this);
     //  final Trip currentTrip = (Trip) intent.getSerializableExtra("tripReminder");
        final Trip currentTrip = adb.getTrip(tripId);
        new MaterialStyledDialog.Builder(this)
                .setTitle(currentTrip.getTrip_name())
                .setDescription("Do you want to start your trip now?")
                .setPositiveText("Start")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                        r.stop();
                        Log.i("MaterialStyledDialogs", "Do something!");
                        Toast.makeText(getApplicationContext(),"start trip" + currentTrip.getTrip_id(),Toast.LENGTH_SHORT).show();

                        Trip tripData = currentTrip;
                        //int itemPosition = recyclerView.getChildLayoutPosition(v);
                        //Trip tripData = tripDataList.get(itemPosition);
                        // Trip tripData = tripDataList.get(0);
                        tripData.setStarted(true);
                        //String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?saddr=%f,%f(%s)&daddr=%f,%f (%s)", tripData.getStart_lat(), tripData.getStart_long(), tripData.getStart_name(),  tripData.getEnd_lat() , tripData.getEnd_long(), tripData.getEnd_name());
                        if(tripData.isRoundtrip()){
                            tripData.setStarted(false);
                            tripData.setRoundtrip(false);
                            // add 2 hours
                            final long millisToAdd = 7_200_000; //two hours
                            Date d = tripData.getDate_time();
                            d.setTime(d.getTime() + millisToAdd);
                            tripData.setDate_time(d);
                            // swip lat long
                            double temp_lat = tripData.getStart_lat();
                            double temp_long = tripData.getStart_long();
                            String temp_name = tripData.getStart_name();

                            tripData.setStart_name(tripData.getEnd_name());
                            tripData.setStart_lat(tripData.getEnd_lat());
                            tripData.setStart_long(tripData.getEnd_long());

                            tripData.setEnd_name(temp_name);
                            tripData.setEnd_lat(temp_lat);
                            tripData.setEnd_long(temp_long);

                        }
                        Adapter dbAdapter = new Adapter(AlarmActivity.this);

                        dbAdapter.updateTrip(tripData);

                        //Toast.makeText( context, "Start trip activity", Toast.LENGTH_LONG).show();

                        /*Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);*/

                        // new code for notes
                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(AlarmActivity.this)) {
                            Intent intent_permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            intent_permission.putExtra("trip",currentTrip);
                            startActivityForResult(intent_permission, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                        } else {
                            initializeView(currentTrip);
                        }

                    }})
                .setStyle(Style.HEADER_WITH_TITLE)
                .setHeaderColor(R.color.colorAccent)
                .setNegativeText("Cancel")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                        r.stop();
                        Log.i("MaterialStyledDialogs", "Do something!");
                        Toast.makeText(getApplicationContext(),"cancel",Toast.LENGTH_SHORT).show();

                    }})
                .setNeutralText("Later")
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                        r.stop();
                        Log.i("MaterialStyledDialogs", "Do something!");
                        Toast.makeText(getApplicationContext(),"postpone trip",Toast.LENGTH_SHORT).show();

                        showNotification(AlarmActivity.this,currentTrip.getTrip_id(), currentTrip.getTrip_name(),"Want to start the Trip?", currentTrip);


                    }})
                .show();
    }
    private void showNotification(Context mContext, int notificationId, String title, String content, Trip trip) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext(), "notify_001");

        Intent intent = new Intent(mContext, AlarmActivity.class);
        if (trip != null) {
           // intent.putExtra("tripReminder", trip);
            intent.putExtra("tripReminder",trip.getTrip_id());
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pendingIntent)
                // .setSmallIcon(R.drawable.app_logo_notification)
                .setSmallIcon(R.mipmap.trip_icon)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setOngoing(true)

                //  .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.app_logo));
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.trip_icon));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mBuilder.setPriority(Notification.PRIORITY_MAX);
        }

        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("notify_001",
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        if (mNotificationManager != null) {
            mNotificationManager.notify(notificationId, mBuilder.build());
        }
    }
/* new MaterialStyledDialog.Builder(this)
         .setTitle(currentTrip.getTrip_name())
            .setDescription("Do you want to start your trip now?")
                .setPositiveText("Ok")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            finish();
            Log.i("MaterialStyledDialogs", "Do something!");
            Toast.makeText(getApplicationContext(),"start trip" + currentTrip.getTrip_id(),Toast.LENGTH_SHORT).show();

            Trip tripData = currentTrip;
            //int itemPosition = recyclerView.getChildLayoutPosition(v);
            //Trip tripData = tripDataList.get(itemPosition);
            // Trip tripData = tripDataList.get(0);
            tripData.setStarted(true);
            String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?saddr=%f,%f(%s)&daddr=%f,%f (%s)", tripData.getStart_lat(), tripData.getStart_long(), tripData.getStart_name(),  tripData.getEnd_lat() , tripData.getEnd_long(), tripData.getEnd_name());
            if(tripData.isRoundtrip()){
                tripData.setStarted(false);
                tripData.setRoundtrip(false);
                // add 2 hours
                final long millisToAdd = 7_200_000; //two hours
                Date d = tripData.getDate_time();
                d.setTime(d.getTime() + millisToAdd);
                tripData.setDate_time(d);
                // swip lat long
                double temp_lat = tripData.getStart_lat();
                double temp_long = tripData.getStart_long();
                String temp_name = tripData.getStart_name();

                tripData.setStart_name(tripData.getEnd_name());
                tripData.setStart_lat(tripData.getEnd_lat());
                tripData.setStart_long(tripData.getEnd_long());

                tripData.setEnd_name(temp_name);
                tripData.setEnd_lat(temp_lat);
                tripData.setEnd_long(temp_long);

            }
            Adapter dbAdapter = new Adapter(AlarmActivity.this);

            dbAdapter.updateTrip(tripData);

            //Toast.makeText( context, "Start trip activity", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        }})
            .setStyle(Style.HEADER_WITH_TITLE)
                .setHeaderColor(R.color.colorAccent)


            .show();
            */

    private void initializeView(Trip trip) {
        //List<String> notes = new ArrayList<>();
        //notes = trip.getNotes();
        Intent intent = new Intent(AlarmActivity.this, NoteHeadService.class);
        //Bundle b = new Bundle();
        //b.putSerializable("trip", trip);
        intent.putExtra("trip", trip.getTrip_id());
        finish();
        startService(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

            //Check if the permission is granted or not.
            // Settings activity never returns proper value so instead check with following method
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    initializeView((Trip) data.getSerializableExtra("trip"));
                } else { //Permission is not available
                    Toast.makeText(this,
                            "Draw over other app permission not available. Closing the application",
                            Toast.LENGTH_SHORT).show();

                    finish();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
