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
        if (ActivityRecognitionResult.hasResult(intent)) {
            //detecto l'attività rilevata
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            int activityType = mostProbableActivity.getType();
            int confidence = mostProbableActivity.getConfidence();

            String activityName = getActivityName(activityType);

            //lo invio alla mainActivity
            sendActivityRecognitionResultToMainActivity(activityName);
        }

    }


    private void sendActivityRecognitionResultToMainActivity(String message) {
        Intent intent = new Intent("ACTION_ACTIVITY_RECOGNITION_RESULT");
        intent.putExtra("ACTIVITY_MESSAGE", message);
        sendBroadcast(intent);
    }


    //associa l'activityType a una stringa che indica l'attività
    private String getActivityName(int activityType) {
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "In macchina";
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
