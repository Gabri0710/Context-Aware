package com.example.geo_fencing_basedemergencyadvertising;

import static android.content.Intent.getIntent;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class AlertReceiver extends BroadcastReceiver {
    private final String alertChannelId; // Id del canale delle notifiche
    private int notificationIdCounter = 0; // Contatore id notifiche, serve per avere id univoci

    public AlertReceiver(String channelId){
        this.alertChannelId = channelId;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ALERT RECEIVER", "RICEVUTO NUOVO INTENT");
        String notificationText = "TESTO BASE";
        int priorityLevel = NotificationCompat.PRIORITY_DEFAULT;

        Bundle extras = intent.getExtras();
        if (extras != null) {
            int priority = extras.getInt("priority");

            switch (priority){
                case 1:
                    notificationText = "SEI DENTRO IL GEOFENCE";
                    priorityLevel = NotificationCompat.PRIORITY_MAX;
                    break;
                case 2:
                    notificationText = "SEI IN UN'AREA A DISTANZA DI 1 KM DAL GEOFENCE";
                    priorityLevel = NotificationCompat.PRIORITY_HIGH;
                    break;
                case 3:
                    notificationText = "SEI IN UN'AREA A DISTANZA TRA 1 e 2 KM DAL GEOFENCE";
                    // priority level = DEFAULT
                    break;
            }
        }

        // Creazione della notifica tramite builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, alertChannelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("NUOVO GEOFENCE ALERT")
                .setContentText(notificationText)
                .setPriority(priorityLevel);

        Log.d("NOTIFICA", notificationText);

        // Invio della notifica tramite Notification Manager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(getUniqueNotificationId(), builder.build());

    }

    // Creazione id univoco della notifica
    private int getUniqueNotificationId() {
        return notificationIdCounter++;
    }

}
