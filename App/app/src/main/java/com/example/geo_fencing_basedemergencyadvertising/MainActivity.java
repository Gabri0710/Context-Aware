package com.example.geo_fencing_basedemergencyadvertising;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    //definisco client riconoscimento attività
    private ActivityRecognitionClient activityRecognitionClient;

    //definisco PendingIntent per ricevere i risultati del riconoscimento
    private PendingIntent pendingIntent;

    // Launcher per la richiesta di autorizzazione. Nelle nuove versioni di android bisogna richiederla anche da codice e non solo nel manifest
    private ActivityResultLauncher<String> requestPermissionLauncher;


    private BroadcastReceiver activityRecognitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("ACTION_ACTIVITY_RECOGNITION_RESULT")) {
                String activityMessage = intent.getStringExtra("ACTIVITY_MESSAGE");
                Toast.makeText(MainActivity.this, activityMessage, Toast.LENGTH_SHORT).show();
                Log.d("RICEZIONE", "confermata");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inizializzo il launcher per la richiesta di autorizzazione. Viene richiamato se non abbiamo concesso l'autorizzazione per il riconoscimento delle attività
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // L'utente ha concesso l'autorizzazione ACTIVITY_RECOGNITION
                        // Si può procedere con il riconoscimento dell'attività
                        Log.d("AUTORIZZAZIONE", "Concessa");
                        Toast.makeText(getApplicationContext(), "AUTORIZZAZIONE concessa", Toast.LENGTH_SHORT).show();
                    } else {
                        // L'utente ha negato l'autorizzazione ACTIVITY_RECOGNITION
                        // Da gestire di conseguenza (ad esempio, informare l'utente o chiudere l'app)
                        Toast.makeText(getApplicationContext(), "AUTORIZZAZIONE negata", Toast.LENGTH_SHORT).show();
                    }
                });


        //controllo se stiamo usando l'app in device con nuove versioni di android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //controllo se non ho fornito l'autorizzazione precedentemente
            if (ContextCompat.checkSelfPermission(this, "android.permission.ACTIVITY_RECOGNITION")
                    != PackageManager.PERMISSION_GRANTED) {
                //in caso, richiamo il launcher per la richiesta di autorizzazione
                requestPermissionLauncher.launch("android.permission.ACTIVITY_RECOGNITION");
            }
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

        Toast.makeText(getApplicationContext(), "START", Toast.LENGTH_SHORT).show();
        // Richiedi le attività rilevate
        activityRecognitionClient.requestActivityUpdates(1000, pendingIntent);
    }
}