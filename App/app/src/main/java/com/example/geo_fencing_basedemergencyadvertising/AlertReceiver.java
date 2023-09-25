package com.example.geo_fencing_basedemergencyadvertising;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

        // Creazione della notifica tramite builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, alertChannelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("NUOVO ALERT")
                .setContentText("Entra nell'app per visualizzare la mappa!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Invio della notifica tramite Notification Manager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(getUniqueNotificationId(), builder.build());

    }

    // Creazione id univoco della notifica
    private int getUniqueNotificationId() {
        return notificationIdCounter++;
    }

}
