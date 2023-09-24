package com.example.geo_fencing_basedemergencyadvertising;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;


import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    // Launcher per la richiesta di autorizzazione. Nelle nuove versioni di android bisogna richiederla anche da codice e non solo nel manifest
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private ActivityResultLauncher<String> locationPermissionLauncher;

    private ActivityResultLauncher<String> requestPermissionNotificationLauncher;


    //definisco client riconoscimento attività
    private ActivityRecognitionClient activityRecognitionClient;

    //definisco PendingIntent per ricevere i risultati del riconoscimento
    private PendingIntent pendingIntent;

    //definisco le attività alla quale sono interessato: WALKING e IN_VEHICLE
    private static final int WALKING = 1;
    private static final int IN_VEHICLE = 2;

    //definisco variabile che memorizzerà l'attività riconosciuta
    private int recognizedActivity;

    //definisco oggetti che mi servono per l'ottenimento della posizione
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private NotificationChannel alertChannel;
    private AlertReceiver alertReceiver;


    //definisco oggetto dove manderemo i risultati dell'attività riconosciuta, con relativa logica nel cambio attività
    private BroadcastReceiver activityRecognitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("ACTION_ACTIVITY_RECOGNITION_RESULT")) {
                String activityMessage = intent.getStringExtra("ACTIVITY_MESSAGE");
                if(activityMessage.equals("In macchina")){
                    recognizedActivity = IN_VEHICLE;
                }
                else if(activityMessage.equals("Camminare")||activityMessage.equals("A piedi")){
                    recognizedActivity = WALKING;
                }

                //if da eliminare successivamente. Utile adesso solo per test.
                //Successivamente la variabile recognizedActivity ci servirà solo quando dovremo inviare l'attività rilevata al backend
                if(recognizedActivity==1){
                    Toast.makeText(MainActivity.this, "walking", Toast.LENGTH_SHORT).show();
                    Log.d("Attività: ", "a piedi");
                }
                else{
                    Toast.makeText(MainActivity.this, "in macchina", Toast.LENGTH_SHORT).show();
                    Log.d("Attività: ", "in macchina");
                }

            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creazione del canale delle notifiche per gli alert
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             /* Verifica se il dispositivo Android in esecuzione ha una versione maggiore o uguale
                a Android 8.0 (API 26) perché i canali delle notifiche sono supportati solo in questa versione in poi */
            String channelId = "alertChannelId";
            CharSequence channelName = "ALERT CHANNEL";
            String channelDescription = "Canale utile per la ricezione delle notifiche di nuovi alert";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            alertChannel = new NotificationChannel(channelId, channelName, importance);
            alertChannel.setDescription(channelDescription);

            // Ottieni il NotificationManager
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Crea il canale
            notificationManager.createNotificationChannel(alertChannel);

            // Creo il receiver degli alert e setto l'ìd del canale
            alertReceiver = new AlertReceiver(alertChannel.getId());
        } else {
            throw new RuntimeException("SDK VERSION BELOW 26");
        }

        // Inizializzo il launcher per la richiesta di autorizzazione. Viene richiamato se non abbiamo concesso l'autorizzazione per il riconoscimento delle attività
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // L'utente ha concesso l'autorizzazione ACTIVITY_RECOGNITION
                        // Si può procedere con il riconoscimento dell'attività
                        Log.d("AUTORIZZAZIONE ActivityRecognition", "Concessa");
                    } else {
                        // L'utente ha negato l'autorizzazione ACTIVITY_RECOGNITION
                        // Da gestire di conseguenza (ad esempio, informare l'utente o chiudere l'app)
                        Log.d("AUTORIZZAZIONE ActivityRecognition", "Negata");
                    }
                });

        locationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // L'utente ha concesso l'autorizzazione ACCESS_FINE_LOCATION
                        // Puoi procedere con le operazioni relative alla posizione
                        Log.d("AUTORIZZAZIONE Location", "Concessa");

                    } else {
                        // L'utente ha negato l'autorizzazione ACCESS_FINE_LOCATION
                        // Da gestire di conseguenza (ad esempio, informare l'utente o chiudere l'app)
                        Log.d("AUTORIZZAZIONE Location", "Negata");
                    }
                }
        );


        //controllo se stiamo usando l'app in device con nuove versioni di android (bisogna richiedere i permessi via codice se così)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //controllo se non ho fornito l'autorizzazione precedentemente
            if (ContextCompat.checkSelfPermission(this, "android.permission.ACTIVITY_RECOGNITION")
                    != PackageManager.PERMISSION_GRANTED) {
                //in caso, richiamo il launcher per la richiesta di autorizzazione
                requestPermissionLauncher.launch("android.permission.ACTIVITY_RECOGNITION");
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){

                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        // Inizializzo il launcher per la richiesta di autorizzazione. Viene richiamato se non abbiamo concesso l'autorizzazione per il riconoscimento delle attività
        requestPermissionNotificationLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // L'utente ha concesso l'autorizzazione POST_NOTIFICATIONS
                        // Si può procedere con il riconoscimento dell'attività
                        Log.d("AUTORIZZAZIONE", "Concessa");
                        Toast.makeText(getApplicationContext(), "AUTORIZZAZIONE NOTIFICHE concessa", Toast.LENGTH_SHORT).show();
                    } else {
                        // L'utente ha negato l'autorizzazione ACTIVITY_RECOGNITION
                        // Da gestire di conseguenza (ad esempio, informare l'utente o chiudere l'app)
                        Toast.makeText(getApplicationContext(), "AUTORIZZAZIONE NOTIFICHE negata", Toast.LENGTH_SHORT).show();
                    }
                });

        if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            // Se il permesso non è stato concesso, richiedilo all'utente
            requestPermissionNotificationLauncher.launch("android.permission.POST_NOTIFICATIONS");
        }

        //inizializzo activityRecognition e pendingIntent
        activityRecognitionClient = ActivityRecognition.getClient(this);
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        pendingIntent = PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE
        );


        // Inizializzo il FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurazione delle richieste di posizione
        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000); // Intervallo di aggiornamento della posizione in millisecondi

        Toast.makeText(getApplicationContext(), "START", Toast.LENGTH_SHORT).show();

        //inizializzo di default l'attività iniziale come walking
        //recognizedActivity = WALKING;
        recognizedActivity = IN_VEHICLE;

        //inizializzo l'intentFilter per i risultati dell'Activity Recognition
        IntentFilter intentFilter = new IntentFilter("ACTION_ACTIVITY_RECOGNITION_RESULT");
        //setto il receiver per ottenere i risultati della misurazione
        registerReceiver(activityRecognitionReceiver, intentFilter);

        // Richiedi le attività rilevate
        activityRecognitionClient.requestActivityUpdates(1000, pendingIntent);

        //inizializzo l'intentFilter per i risultati dell'Activity Recognition
        IntentFilter alertIntentFilter = new IntentFilter("ACTION_NEW_ALERT_NOTIFICATION");
        //setto il receiver per ottenere i risultati della misurazione
        registerReceiver(alertReceiver, alertIntentFilter);

        // Prova di invio di una notifica
        Intent alertIntent = new Intent("ACTION_NEW_ALERT_NOTIFICATION");
        sendBroadcast(alertIntent);

    }
}