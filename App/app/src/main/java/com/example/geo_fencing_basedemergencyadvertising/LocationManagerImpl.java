package com.example.geo_fencing_basedemergencyadvertising;

import static androidx.core.app.ActivityCompat.finishAffinity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

/**
 * Classe utilizzata per l'implementazione di un location manager personalizzato
 * In questo modo ogni qualvolta l'utente disattivi la localizzazione
 * - durante l'utilizzo dell'applicazione - verrà mostrato una finestra di dialogo
 * e verrà reindirizzato alle impostazioni di localizzazione del dispositivo
 * */
public class LocationManagerImpl {

    private LocationManager locationManager;
    private Context context;
    private AlertDialog locationDialog;

    public LocationManagerImpl(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public AlertDialog getLocationDialog() {
        return locationDialog;
    }


    // Metodo per controllare lo stato del GPS e richiedere l'attivazione se è disattivato
    @SuppressLint("MissingPermission")
    public void checkAndRequestLocationUpdates() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Il GPS è disattivato, mostro una finestra di dialogo informativa
            showEnableLocationDialog();
        }


        // Implementa il LocationListener per gestire gli aggiornamenti sulla posizione
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                // La localizzazione è stata abilitata
            }

            @Override
            public void onProviderDisabled(String provider) {
                // La localizzazione è stata disabilitata, mostro una finestra di dialogo informativa
                showEnableLocationDialog();
            }
        };

        //richiedo aggiornamenti posizione
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    }

    // Metodo che mostra una finestra di dialogo per richiedere all'utente di attivare il GPS
    private void showEnableLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Attiva il GPS");
        builder.setMessage("Per utilizzare questa app, devi attivare il GPS. Vuoi andare alle impostazioni?");
        builder.setPositiveButton("Sì", (dialog, which) -> {
            // L'utente ha scelto "Sì", chiudi la finestra di dialogo e apri le impostazioni di localizzazione
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            context.startActivity(intent);
            dialog.dismiss();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            // L'utente ha scelto "No", chiudi la finestra di dialogo
            dialog.dismiss();

            // chiudi l'app
            finishAffinity((Activity) context);
        });

        // Assegno la finestra di dialogo a locationDialog e lo mostra
        locationDialog = builder.create();
        locationDialog.show();
    }

}

