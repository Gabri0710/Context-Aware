package com.example.geo_fencing_basedemergencyadvertising;

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
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.geo_fencing_basedemergencyadvertising.SendPositionService;


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

    private Location location;

    private MapView mapView;
    private ItemizedIconOverlay<OverlayItem> itemizedIconOverlay;
    private MyLocationNewOverlay myLocationOverlay;

    //intero che mi serve nell'aggiornamento della mappa per capire se centrarla o no
    private int center;

    //interfaccia per invio dati a backend
    private SendPositionService sendPositionService;

    //definisco oggetto dove manderemo i risultati dell'attività riconosciuta, con relativa logica nel cambio attività
    private BroadcastReceiver activityRecognitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("ACTION_ACTIVITY_RECOGNITION_RESULT")) {
                String activityMessage = intent.getStringExtra("ACTIVITY_MESSAGE");
                if (activityMessage.equals("In macchina")) {
                    recognizedActivity = IN_VEHICLE;
                } else if (activityMessage.equals("Camminare") || activityMessage.equals("A piedi")) {
                    recognizedActivity = WALKING;
                }

                //if da eliminare successivamente. Utile adesso solo per test.
                //Successivamente la variabile recognizedActivity ci servirà solo quando dovremo inviare l'attività rilevata al backend
                if (recognizedActivity == 1) {
                    Toast.makeText(MainActivity.this, "walking", Toast.LENGTH_SHORT).show();
                    Log.d("Attività: ", "a piedi");
                } else {
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

        // Inizializzo il launcher per la richiesta di autorizzazione POST_NOTIFICATIONS
        requestPermissionNotificationLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // L'utente ha concesso l'autorizzazione POST_NOTIFICATIONS
                        // Si può procedere con le operazioni relative alle notifiche
                        Log.d("AUTORIZZAZIONE NOTIFICHE", "Concessa");
                        Toast.makeText(getApplicationContext(), "AUTORIZZAZIONE NOTIFICHE concessa", Toast.LENGTH_SHORT).show();
                    } else {
                        // L'utente ha negato l'autorizzazione POST_NOTIFICATIONS
                        // Da gestire di conseguenza (ad esempio, informare l'utente o chiudere l'app)
                        Toast.makeText(getApplicationContext(), "AUTORIZZAZIONE NOTIFICHE negata", Toast.LENGTH_SHORT).show();
                    }
                });

        // Controlla se il permesso POST_NOTIFICATIONS non è stato concesso e richiedilo
        if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissionNotificationLauncher.launch("android.permission.POST_NOTIFICATIONS");
        }

        // Controllo se stiamo usando l'app su un dispositivo con Android 10 o superiore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Controllo se non ho fornito l'autorizzazione ACTIVITY_RECOGNITION precedentemente
            if (ContextCompat.checkSelfPermission(this, "android.permission.ACTIVITY_RECOGNITION") != PackageManager.PERMISSION_GRANTED) {
                // In caso contrario, richiamo il launcher per la richiesta di autorizzazione ACTIVITY_RECOGNITION
                requestPermissionLauncher.launch("android.permission.ACTIVITY_RECOGNITION");
            }


            // Controllo se non ho fornito l'autorizzazione ACCESS_FINE_LOCATION precedentemente
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // In caso contrario, richiamo il launcher per la richiesta di autorizzazione ACCESS_FINE_LOCATION
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        // Inizializzo activityRecognition e pendingIntent
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
                .setInterval(15000); // Intervallo di aggiornamento della posizione in millisecondi

        Toast.makeText(getApplicationContext(), "START", Toast.LENGTH_SHORT).show();

        //inizializzo di default l'attività iniziale come IN_VEHICLE
        recognizedActivity = IN_VEHICLE;

        // Inizializzo l'intentFilter per i risultati dell'Activity Recognition
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



        // Inizializza la configurazione di OpenStreetMap
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("OpenStreetMap", MODE_PRIVATE));

        // Ottieni la view della mappa dal layout XML
        mapView = findViewById(R.id.mapView);

        // Abilita il provider di posizione GPS
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Imposto la posizione a una iniziale fittizia per evitare valori null iniziali e lo setto come centro della mappa
        GeoPoint startPoint = new GeoPoint(41.8902, 12.4922);
        mapView.getController().setCenter(startPoint);
        mapView.getController().setZoom(15);

        // Aggiungi un overlay di posizione
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getApplicationContext()), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);


        // Creo un overlay dei marcatori con il marker predefinito di OSM
        itemizedIconOverlay = new ItemizedIconOverlay<>(new ArrayList<>(),
                getResources().getDrawable(org.osmdroid.library.R.drawable.marker_default), null, getApplicationContext());
        mapView.getOverlays().add(itemizedIconOverlay);

        //flag che uso per centrare la vista, mi serve successivamente, momentaneo
        center = 1;




        //ROBA PER INVIO DATI A BACKEND
        //localhost, viene indicato così, sto provando a inviare informazioni a un backend
        String BASE_URL = "https://10.0.2.2:5000";
        // Inizializza Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Crea un'istanza dell'interfaccia ApiService
        sendPositionService = retrofit.create(SendPositionService.class);


        //richiedo aggiornamenti posizione
        requestLocationUpdates();


    }


    // Metodo per inviare la posizione al backend Flask
    private void sendLocationToBackend(double latitude, double longitude) {


        // Effettuo la richiesta HTTP POST. uso l'istanza dell'interfaccia SendPositionService (dove è gestito la richiesta POST)
        Call<Void> call = sendPositionService.uploadLocation(latitude, longitude);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("S,", "success");
                    // La posizione è stata inviata con successo
                    // Puoi gestire la risposta del backend qui se necessario
                } else {
                    Log.d("S,", "failure");
                    // Gestisci un errore nella risposta del backend (es. codice di errore HTTP)
                    // Puoi mostrare un messaggio di errore all'utente o registrare l'errore
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                //Log.e("Retrofit", "Errore durante la richiesta HTTP", t);
                //t.printStackTrace();
                // Gestisci un errore nella richiesta HTTP (es. problema di connessione)
                // Puoi mostrare un messaggio di errore all'utente o registrare l'errore
            }
        });
    }

    // Metodo per richiedere gli aggiornamenti della posizione
    private void requestLocationUpdates() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    location = locationResult.getLastLocation();
                    //ottengo la posizione e faccio qualcosa
                    //Log.d("POSIZIONE", "Lat: " + location.getLatitude() + ", Long: " + location.getLongitude());

                    //invio la posizione al backend (prova). Chiamo il metodo sendLocationToBackend
                    sendLocationToBackend(location.getLatitude(), location.getLongitude());
                    Log.d("Invio", "effettuato");

                    //creo la currentlocation (per la mappa)
                    GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                    Log.d("POSIZIONE", "Lat: " + currentLocation.getLatitude() + ", Long: " + currentLocation.getLongitude());

                    //uso questo flag per capire se è la prima volta che richiediamo l'aggiornamento della posizione
                    //se sì, aggiorno la posizione a quella rilevata (ho dovuto impostare un valore qualsiasi per l'inizializzazione per
                    //evitare un riferimento null alla location). Se center è uguale a 1, ovvero è la prima volta che entro, aggiorno la posizione
                    //e centro la mappa sulla posizione rilevata. Successivamente lo incremento perché se no la mappa verrebbe centrata a ogni
                    //chiamata di questo metodo per l'aggiornamento della posizione e non mi permetterebbe di muovermi per la mappa
                    if(center==1){
                        mapView.getController().setCenter(currentLocation);
                    }

                    center+=1;


                    // Creo un oggetto OverlayItem (marker) con le coordinate rilevate
                    OverlayItem myLocationMarker = new OverlayItem("La mia posizione", "Descrizione della posizione", currentLocation);

                    // Aggiungi il marker all'overlay dei marcatori
                    itemizedIconOverlay.addItem(myLocationMarker);

                    // Aggiorna la mappa
                    mapView.invalidate();
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //Richiamo requestLocationUpdates. Vuole per forza sto if sopra qui, è messo anche prima ma non gli va bene, poi vedrò perché
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);



    }
}
