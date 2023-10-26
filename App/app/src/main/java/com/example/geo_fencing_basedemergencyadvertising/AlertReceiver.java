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
    private TextToSpeech textToSpeech;              //oggetto per notifiche audio

    public AlertReceiver(Context context, String channelId){
        this.alertChannelId = channelId;
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if (status != TextToSpeech.ERROR) {
                    // Configuro la lingua della voce
                    textToSpeech.setLanguage(Locale.ITALIAN);
                }
            }
        });
    }

    //metodo che viene chiamato quando ricevo un nuovo intent
    @Override
    public void onReceive(Context context, Intent intent) {
        // definisco testo completo della notifica
        String notificationText = "";

        //definisco priorità
        int priorityLevel = NotificationCompat.PRIORITY_DEFAULT;

        //definisco messaggio di allerta
        String alertText = "";

        //definisco coordinate
        String coordinate="";

        //inizializzo recognizedActivity
        int recognizedActivity = 0;

        if (intent.hasExtra("priority") && intent.hasExtra("recognizedActivity")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                //prendo gli extra inseriti dalla mainActivity per creare l'allarme
                int priority = extras.getInt("priority");
                recognizedActivity = extras.getInt("recognizedActivity");
                alertText = extras.getString("alertText");
                coordinate = extras.getString("coordinate");

                //creo l'allarme in base a dove mi trovo (dentro il geofence, a 1km, tra 1-2km)
                switch (priority) {
                    case 1:
                        notificationText = "ALLARME: " + alertText + " con coordinate: " + coordinate;
                        priorityLevel = NotificationCompat.PRIORITY_MAX;
                        break;
                    case 2:
                        notificationText = "ALLARME: " + alertText + " con coordinate: " + coordinate;
                        priorityLevel = NotificationCompat.PRIORITY_HIGH;
                        break;
                    case 3:
                        notificationText = "ALLARME: " + alertText + " con coordinate: " + coordinate;
                        // priority level = DEFAULT
                        break;
                }
            }
        }


        // Creazione della notifica tramite builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, alertChannelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("ALLERTA")
                .setContentText(notificationText)
                .setPriority(priorityLevel);


        // Invio della notifica tramite Notification Manager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(getUniqueNotificationId(), builder.build());

        if (recognizedActivity == 2) {                  // IN VEHICLE
            //Riproduco notifica audio
            textToSpeech.speak(notificationText, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // Creazione id univoco della notifica
    private int getUniqueNotificationId() {
        return notificationIdCounter++;
    }

}
