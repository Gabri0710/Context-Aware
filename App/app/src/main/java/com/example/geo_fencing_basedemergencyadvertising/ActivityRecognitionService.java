package com.example.geo_fencing_basedemergencyadvertising;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionService extends IntentService {
    public ActivityRecognitionService() {
        super("ActivityRecognitionService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("Ciao", "ciao");
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            int activityType = mostProbableActivity.getType();
            int confidence = mostProbableActivity.getConfidence();

            String activityName = getActivityName(activityType);

            String message = "Attività rilevata: " + activityName + " (Confidenza: " + confidence + ")";
            Log.d("ActivityRecognition", message);

            //showActivityToast(message);

            sendActivityRecognitionResultToMainActivity(message);

            Log.d("Method", "called");
        }

    }


    private void sendActivityRecognitionResultToMainActivity(String message) {
        Intent intent = new Intent("ACTION_ACTIVITY_RECOGNITION_RESULT");
        intent.putExtra("ACTIVITY_MESSAGE", message);
        sendBroadcast(intent);
    }


    private void showActivityToast(String message) {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private String getActivityName(int activityType) {
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "In veicolo";
            case DetectedActivity.ON_BICYCLE:
                return "In bicicletta";
            case DetectedActivity.ON_FOOT:
                return "A piedi";
            case DetectedActivity.RUNNING:
                return "Correre";
            case DetectedActivity.STILL:
                return "Fermo";
            case DetectedActivity.TILTING:
                return "Inclinato";
            case DetectedActivity.UNKNOWN:
                return "Sconosciuto";
            case DetectedActivity.WALKING:
                return "Camminare";
            default:
                return "Sconosciuto";
        }
    }
}
