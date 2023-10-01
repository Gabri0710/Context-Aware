package com.example.geo_fencing_basedemergencyadvertising;

import static android.content.Intent.getIntent;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Locale;

public class AlertReceiver extends BroadcastReceiver {
    private final String alertChannelId; // Id del canale delle notifiche
    private int notificationIdCounter = 0; // Contatore id notifiche, serve per avere id univoci
    private TextToSpeech textToSpeech;
    public AlertReceiver(Context context, String channelId){
        this.alertChannelId = channelId;
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                Log.d("STATUS", Integer.toString(status));

                if (status != TextToSpeech.ERROR) {
                    // Configura la lingua della voce, se necessario
                    Log.d("HERE", "HERE");
                    textToSpeech.setLanguage(Locale.ITALIAN);


                }
            }
        });
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ALERT RECEIVER", "RICEVUTO NUOVO INTENT");
        String notificationText = "TESTO BASE";
        int priorityLevel = NotificationCompat.PRIORITY_DEFAULT;

        String alertText = "";
        String coordinate="";
        int recognizedActivity = 0;
        if (intent.hasExtra("priority") && intent.hasExtra("recognizedActivity")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int priority = extras.getInt("priority");
                recognizedActivity = extras.getInt("recognizedActivity");
                alertText = extras.getString("alertText");
                coordinate = extras.getString("coordinate");

                switch (priority) {
                    case 1:
                        notificationText = "SEI DENTRO IL GEOFENCE: " + alertText + " con coordinate: " + coordinate;
                        priorityLevel = NotificationCompat.PRIORITY_MAX;
                        break;
                    case 2:
                        notificationText = "SEI IN UN'AREA A DISTANZA DI 1 KM DAL GEOFENCE: " + alertText + " con coordinate: " + coordinate;
                        priorityLevel = NotificationCompat.PRIORITY_HIGH;
                        break;
                    case 3:
                        notificationText = "SEI IN UN'AREA A DISTANZA TRA 1 e 2 KM DAL GEOFENCE: " + alertText + " con coordinate: " + coordinate;
                        // priority level = DEFAULT
                        break;
                }
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

        if (recognizedActivity == 2) { // IN VEHICLE
            // Inizializzazione nel metodo onCreate o in un punto appropriato della tua Activity o Fragment
            // Per riprodurre il testo come voce
            textToSpeech.speak(notificationText, TextToSpeech.QUEUE_FLUSH, null, null);

        }
    }

    // Creazione id univoco della notifica
    private int getUniqueNotificationId() {
        return notificationIdCounter++;
    }

}
