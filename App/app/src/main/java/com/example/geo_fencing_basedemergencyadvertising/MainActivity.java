package com.example.geo_fencing_basedemergencyadvertising;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private ActivityRecognitionClient activityRecognitionClient;   //client riconoscimento attività
    private PendingIntent pendingIntent;    //intent per ricevere i risultati del riconoscimento
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        activityRecognitionClient = ActivityRecognition.getClient(this);
//
//        Intent intent = new Intent(this, ActivityRecognitionService.class);
//        pendingIntent = PendingIntent.getService(
//                this,
//                0,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT
//        );
//
//        // Richiedi le attività rilevate
//        activityRecognitionClient.requestActivityUpdates(5000, pendingIntent);
//        Log.d("Test", "Test");
    }
}