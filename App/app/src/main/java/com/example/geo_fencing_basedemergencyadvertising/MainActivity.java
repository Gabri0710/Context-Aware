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
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_GPS = 123;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 124;

    // Launcher per la richiesta di autorizzazione.
    // Nelle nuove versioni di android bisogna richiederla anche da codice e non solo nel manifest
    private static ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    // definisco client riconoscimento attività
    private ActivityRecognitionClient activityRecognitionClient;

    // definisco variabile che memorizzerà l'attività riconosciuta
    private int recognizedActivity;

    // definisco le attività alla quale sono interessato: WALKING e IN_VEHICLE
    private static final int WALKING = 1;
    private static final int IN_VEHICLE = 2;

    // definisco PendingIntent per ricevere i risultati del riconoscimento
    private PendingIntent pendingIntent;

    // definisco oggetti che mi servono per l'ottenimento della posizione
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    // definisco manager per la gestione di attivare il gps se è spento
    private LocationManagerImpl locationManagerImpl;

    // definisco nuovo canale di notifiche, per gli alert che arriveranno dal backend
    private NotificationChannel alertChannel;

    // definisco un broadcast receiver personalizzato per ricevere gli intent degli alert
    // dai questi intent il broadcast receiver gestirà la creazione e l'invio di notifiche
    private AlertReceiver alertReceiver;

    private Location location;
    private MapView mapView;
    private ItemizedIconOverlay<OverlayItem> itemizedIconOverlay;
    private MyLocationNewOverlay myLocationOverlay;

    // intero che mi serve nell'aggiornamento della mappa per capire se centrarla o no
    private int center;

    // interfaccia per invio dati a backend
    private SendUserDataService sendUserDataService;

    private UserData userData;

    FirebaseDatabase database;
    DatabaseReference myRef;

    private String username = "USER-TEST4";

    //Hashmap che contiene associazione chiave valore dove chiave=id_geofence valore=punti del geofence
    Map<String, Polygon> geofence = new HashMap<String, Polygon>();



    // definisco oggetto dove manderemo i risultati dell'attività riconosciuta, con relativa logica nel cambio attività
    private final BroadcastReceiver activityRecognitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("ACTION_ACTIVITY_RECOGNITION_RESULT")) {
                String activityMessage = intent.getStringExtra("ACTIVITY_MESSAGE");
                if (activityMessage.equals("In macchina")) {
                    recognizedActivity = IN_VEHICLE;
                } else if (activityMessage.equals("Camminare") || activityMessage.equals("A piedi")) {
                    recognizedActivity = WALKING;
                }

                //TODO: if da eliminare successivamente. Utile adesso solo per test.
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

        initAlertNotificationChannel();

        initRequestPermissionsLauncher();

        checkAllPermissions();

        database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReferenceFromUrl("https://geo-fencing-based-emergency-default-rtdb.europe-west1.firebasedatabase.app/notifiche");


        Log.d("Firebase Reference", "Percorso della referenza: " + myRef.toString());

        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                // Questo metodo viene chiamato quando viene aggiunto un nuovo figlio al nodo "notifiche"
                // Puoi gestire qui la notifica o l'azione da intraprendere quando viene aggiunto un nuovo valore.

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                //INSERITO QUI PERCHE PRIMA VIENE INSERITO L'IDENTIFICATIVO E POI VIENE AGGIORNATO CON
                //TESTO, LATITUDINE E LONGITUDINE. DA GESTIRE QUI SE NO VENGONO VISTI COME null

                String identificativo = dataSnapshot.getKey();
                String testo = dataSnapshot.child("testo").getValue(String.class);

                //classe di utilità fornita da Firebase SDK per Java per aiutare nella deserializzazione dei dati da Firebase Realtime Database.
                //È utilizzato quando si desidera deserializzare dati generici, come liste ecc, perché firebase non riconosce automaticamente il tipo di dati
                //quindi, lo specifichiamo e lo passiamo successivamente a getValue(). In particolare, specifichiamo che stiamo ricevendo una lista di liste di double
                GenericTypeIndicator<ArrayList<ArrayList<Double>>> t = new GenericTypeIndicator<ArrayList<ArrayList<Double>>>() {};

                //array di coordinate. Per ogni punto, in posizione i latitudine, i+1 longitudine
                ArrayList<ArrayList<Double>> coordinateList = dataSnapshot.child("coordinate").getValue(t);

                drawGeofence(identificativo, coordinateList);

                GenericTypeIndicator<ArrayList<String>> t1 = new GenericTypeIndicator<ArrayList<String>>() {};

                //Lista di utenti nel geofence
                ArrayList<String> users_in_geofenceList = dataSnapshot.child("in_geofence").getValue(t1);
                //Lista di utenti a 1km dal geofence
                ArrayList<String> users_in_1kmList = dataSnapshot.child("in_1km").getValue(t1);
                //Lista di utenti tra 1 e 2 km
                ArrayList<String> users_between_1_2km = dataSnapshot.child("between_1_2km").getValue(t1);

                if (users_in_geofenceList.contains(username)) {
                    // L'utente è presente nella lista
                    Log.d("ALLARME", "SEI DENTRO IL GEOFENCE: " + testo );
                    for (int i=0;i<coordinateList.size();i++){
                        ArrayList<Double> coppiaCoordinate = coordinateList.get(i);
                        Double latitudine = coppiaCoordinate.get(0);
                        Double longitudine = coppiaCoordinate.get(1);
                        Log.d("Coordinate allarme punto " + Integer.toString(i), "Latitudine: " + latitudine + ", Longitudine: " + longitudine);
                        Intent alertIntent = new Intent("ACTION_NEW_ALERT_NOTIFICATION");
                        alertIntent.putExtra("priority", 1);
                        sendBroadcast(alertIntent);
                    }
                }
                else if(users_in_1kmList.contains(username)){
                    Log.d("ALLARME", "SEI A 1km DAL GEOFENCE: " + testo );
                    for (int i=0;i<coordinateList.size();i++){
                        ArrayList<Double> coppiaCoordinate = coordinateList.get(i);
                        Double latitudine = coppiaCoordinate.get(0);
                        Double longitudine = coppiaCoordinate.get(1);
                        Log.d("Coordinate allarme punto " + Integer.toString(i), "Latitudine: " + latitudine + ", Longitudine: " + longitudine);
                        Intent alertIntent = new Intent("ACTION_NEW_ALERT_NOTIFICATION");
                        alertIntent.putExtra("priority", 2);
                        sendBroadcast(alertIntent);
                    }
                }
//                else if(users_between_1_2km.contains(username)){
//                    Log.d("ALLARME", "SEI TRA 1 e 2km DAL GEOFENCE: " + testo );
//                    for (int i=0;i<coordinateList.size();i++){
//                        ArrayList<Double> coppiaCoordinate = coordinateList.get(i);
//                        Double latitudine = coppiaCoordinate.get(0);
//                        Double longitudine = coppiaCoordinate.get(1);
//                        Log.d("Coordinate allarme punto " + Integer.toString(i), "Latitudine: " + latitudine + ", Longitudine: " + longitudine);
//                        Intent alertIntent = new Intent("ACTION_NEW_ALERT_NOTIFICATION");
//                        alertIntent.putExtra("priority", 3);
//                        sendBroadcast(alertIntent);
//                    }
//                }




            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Questo metodo viene chiamato quando un figlio viene rimosso dal nodo "notifiche"
                String identificativo = dataSnapshot.getKey();
                deleteGeofence(identificativo);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Questo metodo viene chiamato quando un figlio nel nodo "notifiche" viene spostato
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Gestisci eventuali errore
                Log.d("ALLARME", "ERRORE: " + databaseError.getMessage());
            }
        });


        // Verifica se il GPS è attualmente disattivato
        locationManagerImpl = new LocationManagerImpl(this); // Passa il contesto dell'app
        locationManagerImpl.checkAndRequestLocationUpdates(); // ascolta aggiornamenti

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

        // inizializzazione mapView
        initMapView();

        //ROBA PER INVIO DATI A BACKEND

        //url del localhost da emulatore. Se da telefono vero sostituire con 127.0.0.1:5000
        // String BASE_URL = "http://10.0.2.2:5000";
        String BASE_URL = "http://192.168.1.189:5000";
        // Inizializza Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Crea un'istanza dell'interfaccia ApiService
        sendUserDataService = retrofit.create(SendUserDataService.class);

        userData = new UserData();


        //drawGeofence();

        //richiedo aggiornamenti posizione
        requestLocationUpdates();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (locationManagerImpl.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Il GPS è stato abilitato, puoi chiudere il dialog qui se è ancora aperto
            if (locationManagerImpl.getLocationDialog() != null && locationManagerImpl.getLocationDialog().isShowing()) {
                locationManagerImpl.getLocationDialog().dismiss();
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            // Il codice di richiesta corrisponde alle impostazioni di localizzazione
            if (locationManagerImpl.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Il GPS è stato abilitato, puoi chiudere il dialog qui se è ancora aperto
                if (locationManagerImpl.getLocationDialog() != null && locationManagerImpl.getLocationDialog().isShowing()) {
                    locationManagerImpl.getLocationDialog().dismiss();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Esegui le operazioni necessarie per rilevare la chiusura definitiva dell'app
        // Questo metodo verrà chiamato quando l'Activity viene distrutta
    }

    /**
     * Metodo usato per creare ed inizializzare il canale delle notifiche per gli alert
     * che verranno inviati dal server
     * */
    private void initAlertNotificationChannel() {
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
            throw new RuntimeException("PROBABLY SDK VERSION BELOW 26");
        }
    }

    /**
     * Metodo usato per inizializzare il launcher usato per le varie richieste di permessi
     * */
    private void initRequestPermissionsLauncher() {
        // Inizializzo il launcher per la richiesta di autorizzazione.
        // Viene richiamato se non abbiamo concesso l'autorizzazione per il riconoscimento delle attività
        requestPermissionsLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        permissions -> {
                            boolean allPermissionsGranted = true;
                            for (String permission : permissions.keySet()) {
                                if (!permissions.get(permission)) {
                                    allPermissionsGranted = false;
                                    break;
                                }
                            }

                            if (allPermissionsGranted) {
                                // Tutti i permessi sono stati concessi, continua con il funzionamento dell'app
                                Log.d("AUTORIZZAZIONI", "CONCESSE");
                            } else {
                                // Almeno uno dei permessi è stato negato, puoi informare l'utente o gestire di conseguenza
                                Log.d("AUTORIZZAZIONI", "NON (TUTTE O PARTE DI ESSE) CONCESSE");
                            }
                        });
    }

    /**
     * Metodo utilizzato per verificare la richiesta che tutti i permessi siano stati concessi.
     * Viene verificato dapprima se si usa l'app su un dispositivo con Android 10 o superiore:
     * in caso positivo allora viene controllato che tutti i permessi necessari siano garantiti;
     * se non sono garantiti, vengono richiesti.
     * */
    private void checkAllPermissions() {

        // Controllo se stiamo usando l'app su un dispositivo con Android 10 o superiore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String[] permissionsToCheck = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    "android.permission.ACTIVITY_RECOGNITION",
                    "android.permission.POST_NOTIFICATIONS",
            };

            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissionsToCheck) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
            if(permissionsToRequest.size() > 0){
                String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
                requestPermissionsLauncher.launch(permissionsArray);
            }
        }

    }

    /**
     * Metodo usato per creare, inizializzare e visualizzare la MapView nell'applicazione.
     * */
    private void initMapView() {
        // Ottieni la view della mappa dal layout XML
        mapView = findViewById(R.id.mapView);

        // Abilita il provider di posizione GPS
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Imposto la posizione a una iniziale fittizia per evitare valori null iniziali e lo setto come centro della mappa
        GeoPoint startPoint = new GeoPoint(41.8902, 12.4922);
        mapView.getController().setCenter(startPoint);
        mapView.getController().setZoom(20);

        // Aggiungi un overlay di posizione
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getApplicationContext()), mapView);
        myLocationOverlay.enableMyLocation();                     //mette l'omino sulla mappa
        mapView.getOverlays().add(myLocationOverlay);


        // Creo un overlay dei marcatori con il marker predefinito di OSM
        itemizedIconOverlay = new ItemizedIconOverlay<>(new ArrayList<>(),
                getResources().getDrawable(org.osmdroid.library.R.drawable.marker_default), null, getApplicationContext());
        mapView.getOverlays().add(itemizedIconOverlay);

        //flag che uso per centrare la vista, mi serve successivamente, momentaneo
        center = 1;

    }


    /**
     * Metodo per inviare la posizione al backend Flask
     * */
    private void sendLocationToBackend(double latitude, double longitude) {

        userData.setData(latitude, longitude, recognizedActivity);
        // Effettuo la richiesta HTTP POST. uso l'istanza dell'interfaccia SendPositionService (dove è gestito la richiesta POST)
        Call<Void> call = sendUserDataService.uploadData(userData);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("SEND LOCATION TO BACKEND,", "success");
                    // La posizione è stata inviata con successo
                    // Puoi gestire la risposta del backend qui se necessario
                } else {
                    Log.d("SEND LOCATION TO BACKEND,", "failure");
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

    /**
     * Metodo per richiedere gli aggiornamenti della posizione
     */
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
                    //OverlayItem myLocationMarker = new OverlayItem("La mia posizione", "Descrizione della posizione", currentLocation);

                    // Aggiungi il marker all'overlay dei marcatori
                    //itemizedIconOverlay.addItem(myLocationMarker);

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

    /*
    private void drawGeofence(ArrayList<ArrayList<Double>> points){
        ArrayList<GeoPoint> polygonPoints = new ArrayList<>();
        for (int i=0;i<points.size(); i++){
            ArrayList<Double> coppiaCoordinate = points.get(i);
            Double latitudine = coppiaCoordinate.get(0);
            Double longitudine = coppiaCoordinate.get(1);
            polygonPoints.add(new GeoPoint(latitudine, longitudine));
        }
        //inserisco di nuovo ultimo punto per chiudere il geofence
        ArrayList<Double> coppiaCoordinate = points.get(0);
        Double latitudine = coppiaCoordinate.get(0);
        Double longitudine = coppiaCoordinate.get(1);
        polygonPoints.add(new GeoPoint(latitudine, longitudine));

        /*
        polygonPoints.add(new GeoPoint(44.493760, 11.343032)); // Vertice 1
        polygonPoints.add(new GeoPoint(44.493760, 11.343234)); // Vertice 2
        polygonPoints.add(new GeoPoint(44.493911, 11.343437)); // Vertice 3
        polygonPoints.add(new GeoPoint(44.494072, 11.343437)); // Vertice 4
        polygonPoints.add(new GeoPoint(44.494222, 11.343234)); // Vertice 5
        polygonPoints.add(new GeoPoint(44.494222, 11.343032)); // Vertice 6
        polygonPoints.add(new GeoPoint(44.493760, 11.343032)); // Torna al Vertice 1 per chiudere il poligono
        */


    /*
        // Creazione del poligono
        Polygon polygon = new Polygon();
        polygon.setPoints(polygonPoints);
        polygon.setFillColor(0x22FF0000); // Colore di riempimento con alpha
        polygon.setStrokeColor(Color.RED); // Colore del bordo
        polygon.setStrokeWidth(2); // Larghezza del bordo

        mapView.getOverlayManager().add(polygon);
    }
    */

    private void drawGeofence(String identificativo, ArrayList<ArrayList<Double>> points){
        ArrayList<GeoPoint> polygonPoints = new ArrayList<>();
        for (int i=0;i<points.size(); i++){
            ArrayList<Double> coppiaCoordinate = points.get(i);
            Double latitudine = coppiaCoordinate.get(0);
            Double longitudine = coppiaCoordinate.get(1);
            polygonPoints.add(new GeoPoint(latitudine, longitudine));
        }
        //inserisco di nuovo ultimo punto per chiudere il geofence
        ArrayList<Double> coppiaCoordinate = points.get(0);
        Double latitudine = coppiaCoordinate.get(0);
        Double longitudine = coppiaCoordinate.get(1);
        polygonPoints.add(new GeoPoint(latitudine, longitudine));


        /*
        polygonPoints.add(new GeoPoint(44.493760, 11.343032)); // Vertice 1
        polygonPoints.add(new GeoPoint(44.493760, 11.343234)); // Vertice 2
        polygonPoints.add(new GeoPoint(44.493911, 11.343437)); // Vertice 3
        polygonPoints.add(new GeoPoint(44.494072, 11.343437)); // Vertice 4
        polygonPoints.add(new GeoPoint(44.494222, 11.343234)); // Vertice 5
        polygonPoints.add(new GeoPoint(44.494222, 11.343032)); // Vertice 6
        polygonPoints.add(new GeoPoint(44.493760, 11.343032)); // Torna al Vertice 1 per chiudere il poligono
        */



        // Creazione del poligono
        Polygon polygon = new Polygon();
        polygon.setPoints(polygonPoints);
        polygon.setFillColor(0x22FF0000); // Colore di riempimento con alpha
        polygon.setStrokeColor(Color.RED); // Colore del bordo
        polygon.setStrokeWidth(2); // Larghezza del bordo

        geofence.put(identificativo, polygon);

        mapView.getOverlayManager().add(polygon);
    }


    private void deleteGeofence(String identificativo){
        Polygon polygon = geofence.get(identificativo);
        mapView.getOverlayManager().remove(polygon);

        geofence.remove(identificativo);
    }

}
