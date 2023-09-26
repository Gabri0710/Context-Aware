package com.example.geo_fencing_basedemergencyadvertising;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import static androidx.core.app.ActivityCompat.finishAffinity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

/**
 * Classe utilizzata per l'implementazione di un location manager personalizzato
 * In questo modo ogni qualvolta l'utente disattivi la localizzazione
 * - durante l'utilizzo dell'applicazione - verrà mostrato una finestra di dialogo
 * e verrà reindirizzato alle impostazioni di localizzazione del dispositivo
 * */
public class LocationManagerImpl {
    private LocationManager locationManager;
    private Context context;

    public LocationManagerImpl(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    // Metodo per controllare lo stato del GPS e richiedere l'attivazione se è disattivato
    public void checkAndRequestLocationUpdates() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Il GPS è disattivato
            // mostra una finestra di dialogo informativa
            showEnableLocationDialog();
        }

        // Registra il listener per ricevere aggiornamenti sulla posizione
        // Implementa il LocationListener per gestire gli aggiornamenti sulla posizione
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                // Gestisci l'aggiornamento sulla posizione
                // Do nothing
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Gestisci il cambiamento dello stato del provider
                // Do nothing
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Il provider di localizzazione è stato abilitato
                // Do nothing
            }

            @Override
            public void onProviderDisabled(String provider) {
                // Il provider di localizzazione è stato disabilitato
                // Il GPS è disattivato
                // mostra una finestra di dialogo informativa
                showEnableLocationDialog();
            }
        };

        // Richiedi aggiornamenti sulla posizione
        if (ActivityCompat.checkSelfPermission(this.context,
                ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this.context,
                ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    }

    // Mostra una finestra di dialogo per informare l'utente
    private void showEnableLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Attiva il GPS");
        builder.setMessage("Per utilizzare questa app, devi attivare il GPS. Vuoi andare alle impostazioni?");
        builder.setPositiveButton("Sì", (dialog, which) -> {
            // L'utente ha scelto "Sì"
            // chiudi la finestra di dialogo e apri le impostazioni di localizzazione
            dialog.dismiss();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            this.context.startActivity(intent);
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            // L'utente ha scelto "No", chiudi la finestra di dialogo
            dialog.dismiss();
            // chiudi l'app
            finishAffinity((Activity) this.context);
        });

        // Mostra la finestra di dialogo
        builder.create().show();
    }
}

