package com.android.app.sgk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

//Class to receive message from notification
public class NotificationsListenerService extends GcmListenerService {

    String url;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        url = data.getString("key1","defValue");
        String message = data.getString("body", "defValue");
        String title = data.getString("title","defValue");
        Log.d("Message", url);

        //Store URL
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("URL", url);
        editor.apply();

        Intent notificationIntent = new Intent(getApplicationContext(), UserAction.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setLights(Color.GREEN, 1, 1)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setPriority(Notification.PRIORITY_HIGH)
                //.addAction(android.R.drawable.ic_menu_view, "Accept", contentIntent)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, mBuilder.build());
    }
}
