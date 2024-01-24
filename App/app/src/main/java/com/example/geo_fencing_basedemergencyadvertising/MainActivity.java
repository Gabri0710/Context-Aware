package com.example.geo_fencing_basedemergencyadvertising;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
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
import java.util.concurrent.CompletableFuture;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    //    private static final int REQUEST_ENABLE_GPS = 123;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 124;

    // Launcher per la richieste di autorizzazione.
    // Nelle nuove versioni di android bisogna richiederla anche da codice e non solo nel manifest
    private static ActivityResultLauncher<String[]> requestPermissionsLauncher;


    // definisco client riconoscimento attività
    private ActivityRecognitionClient activityRecognitionClient;

    // definisco PendingIntent per ricevere i risultati del riconoscimento dell'attività
    private PendingIntent pendingIntent;

    // definisco variabile che memorizzerà l'attività riconosciuta
    private int recognizedActivity;

    // definisco le attività alla quale sono interessato: WALKING e IN_VEHICLE
    private static final int WALKING = 1;
    private static final int IN_VEHICLE = 2;


    // definisco oggetti che mi servono per l'ottenimento della posizione
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    //posizione del dispositivo
    private Location location;

    // definisco manager per la gestione di attivazione del gps se spento
    private LocationManagerImpl locationManagerImpl;


    // definisco canale di notifiche
    private NotificationChannel alertChannel;

    // definisco un broadcast receiver personalizzato per ricevere gli intent degli alert
    // da questi intent il broadcast receiver gestirà la creazione e l'invio di notifiche
    private AlertReceiver alertReceiver;

    //definisco oggetti per la mappa
    private MapView mapView;
    private MapController mapController;
    private ScaleGestureDetector scaleGestureDetector;
    private ItemizedIconOverlay<OverlayItem> itemizedIconOverlay;
    private MyLocationNewOverlay myLocationOverlay;

    // flag per centrare la mappa solo all'avvio dell'app (non se l'utente sposta la mappa)
    private boolean isMapToCenter;
    // imageview usata come pulsante per centrare la mappa durante l'utilizzo dell'applicazione
    private ImageView centerMapButton;

    // interfaccia per invio dati a backend
    private SendUserDataService sendUserDataService;

    //oggetto che contiene i dati dell'utente (posizione + attività riconosciuta)
    private UserData userData;


    //Definisco database firebase e riferimenti a vari nodi del mio db firebase
    FirebaseDatabase database;
    DatabaseReference myRef4geofence;
    DatabaseReference myRef4user;

    //Definisco oggetti per autenticazione
    private FirebaseAuth mAuth;
    private Button authBtn;

    //username utente loggato
    private String username = "";

    //textview dove indico l'username dell'utente loggato
    private TextView usernameTextView;

    //Hashmap che contiene associazione chiave valore dove chiave=id_geofence - valore=oggetto CustomGeofence
    Map<String, CustomGeofence> geofence = new HashMap<String, CustomGeofence>();

    //ArrayList che contiene i geofence che influenzano l'utente
    ArrayList<ArrayList<String>> user_geofence = new ArrayList<>();

    //Oggetti che utilizzo per realizzare una sequenzialità degli eventi.
    CompletableFuture<Void> firstOperationCompleted = new CompletableFuture<>();
    CompletableFuture<Void> secondOperationCompleted = new CompletableFuture<>();
    CompletableFuture<Void> thirdOperationCompleted = new CompletableFuture<>();

    //url per connettersi a localhost da emulatore. Se da dispositivo fisico sostituire con indirizzo fornito da flask o con link ngrok se su docker
    String BASE_URL = "http://10.0.2.2:5001";
    //String BASE_URL = "http://192.168.1.189:5001";
    //String BASE_URL = "https://link_ngrok_qui";

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

                // Toast con attività riconosciuta (scopo didattico)
                if (recognizedActivity == 1) {
                    Toast.makeText(MainActivity.this, "walking", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(MainActivity.this, "car", Toast.LENGTH_SHORT).show();

                }

            }
        }
    };



    public MainActivity() {
    }

    /**
     * Metodo che aggiorna la textview con l'username dell'utente loggato.
     * In caso di logout reindirizza all'authActivity
     * */
    private void updateAuthBtn(FirebaseUser currentUser) {

        authBtn.setOnClickListener(null);
        this.username = currentUser.getUid();
        this.usernameTextView.setText(currentUser.getEmail());
        authBtn.setOnClickListener(view -> {            // se l'utente clicca il pulsante logout
            FirebaseAuth.getInstance().signOut();
            goToAuthActivity();
        });

    }

    //metodo che reindirizza all'auth activity
    private void goToAuthActivity(){
        startActivity(new Intent(this, AuthActivity.class));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //prendo vari riferimenti
        usernameTextView = findViewById(R.id.usernameTextView);
        authBtn = findViewById(R.id.authBtn);
        mAuth = FirebaseAuth.getInstance();
        centerMapButton = findViewById(R.id.centerMapButton);

        //ottengo l'utente con la quale ho loggato e lo assegno alla mia variabile privata
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateAuthBtn(currentUser);

        //inizializzo canale notifiche
        initAlertNotificationChannel();

        //inizializzo launcher per richiedere permessi
        initRequestPermissionsLauncher();

        //richiamo il dialog se ci sono permessi da concedere
        checkAllPermissions();

        //inizializzo db firebase
        database = FirebaseDatabase.getInstance();

        //inizializzo riferimento a nodo notifiche, dove vengono aggiunti i geofence
        myRef4geofence = database.getReferenceFromUrl("https://geo-fencing-based-emergency-default-rtdb.europe-west1.firebasedatabase.app/notifiche");

        //inizializzo riferimento a nodo utente, mi permette di notare cambiamenti all'intero nodo
        myRef4user = database.getReferenceFromUrl("https://geo-fencing-based-emergency-default-rtdb.europe-west1.firebasedatabase.app/user/"+username);


        // inizializzo locationManagerImpl
        locationManagerImpl = new LocationManagerImpl(this);


        // Inizializzo activityRecognition e pendingIntent associato ad essa
        activityRecognitionClient = ActivityRecognition.getClient(this);
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        pendingIntent = PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE
        );


        // Inizializzo il FusedLocationProviderClient (per ottenere la posizione del dispositivo)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurazione delle richieste di posizione
        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000); // Intervallo di aggiornamento della posizione in millisecondi


        //inizializzo di default l'attività iniziale come IN_VEHICLE (Scopo didattico)
        recognizedActivity = IN_VEHICLE;

        // Inizializzo l'intentFilter per i risultati dell'Activity Recognition
        IntentFilter intentFilter = new IntentFilter("ACTION_ACTIVITY_RECOGNITION_RESULT");

        // associo il broadcast receiver al filtro per ricevere i risultati della misurazione
        registerReceiver(activityRecognitionReceiver, intentFilter);

        // Richiedo le attività rilevate
        activityRecognitionClient.requestActivityUpdates(2000, pendingIntent);


        //inizializzo l'intentFilter per la ricezione delle notifiche di allerta
        IntentFilter alertIntentFilter = new IntentFilter("ACTION_NEW_ALERT_NOTIFICATION");
        // associo il broadcast receiver al filtro per la ricezione delle notifiche
        registerReceiver(alertReceiver, alertIntentFilter);

        // Inizializza la configurazione di OpenStreetMap
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("OpenStreetMap", MODE_PRIVATE));

        // inizializzazione mapView
        initMapView();

        // Inizializza Retrofit (per effettuare richieste HTTP)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Crea un'istanza che è pronta per essere utilizzata per effettuare chiamate di rete
        sendUserDataService = retrofit.create(SendUserDataService.class);

        // inizializzo userData
        userData = new UserData();


        //richiedo aggiornamenti posizione. Prima operazione serializzata (prima cosa da fare)
        requestLocationUpdates();


        //Viene richiamato se viene aggiunto o cancellato un geofence quando l'app non è in esecuzione.
        // Secondo metodo serializzato (viene eseguito dopo aver aggiornato la posizione quando viene aperta l'app)
        firstOperationCompleted.thenRun(() ->{
            myRef4geofence.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Verifico se ci sono dati nel nodo "notifiche"
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {                 //per ciascun figlio

                            // Ottengo le informazioni
                            String identificativo = childSnapshot.getKey();
                            String titolo = childSnapshot.child("titolo").getValue(String.class);
                            String allarme1 = childSnapshot.child("allarme1").getValue(String.class);
                            String allarme2 = childSnapshot.child("allarme2").getValue(String.class);
                            String allarme3 = childSnapshot.child("allarme3").getValue(String.class);



                            //classe necessaria a deserializzare dati generici come liste ecc, firebase non riconosce automaticamente il tipo di dato.
                            // Specifichiamo che stiamo ricevendo una lista di liste di double. Ogni lista indica una coordinata del punto del geofence.
                            GenericTypeIndicator<ArrayList<ArrayList<Double>>> t = new GenericTypeIndicator<ArrayList<ArrayList<Double>>>() {};

                            //Uso t per ottenere la mia lista di liste di double (coordinate)
                            ArrayList<ArrayList<Double>> coordinateList = childSnapshot.child("coordinate").getValue(t);

                            //richiamo drawGeofence che creerà l'oggetto e lo disegnerà
                            drawGeofence(identificativo, titolo, allarme1, allarme2, allarme3, coordinateList);
                        }
                        //indico che l'operazione è stata completata
                        secondOperationCompleted.complete(null);
                    } else {
                        // Se non ci sono nuove notifiche, indico comunque che l'operazione è stata completata
                        secondOperationCompleted.complete(null);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("Errore", databaseError.getMessage());
                }
            });
        });



        //terza operazione serializzata. Viene richiamata dopo l'aggiornamento dei geofence precedente.
        //Controlla se l'utente è coinvolto nel cambiamento dei geofence (se un geofence lo interessa).
        secondOperationCompleted.thenRun(() -> {
            myRef4user.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    //creo un array temporaneo per memorizzare i geofence che influenzano l'utente
                    ArrayList<ArrayList<String>> geofence_influence = new ArrayList<>();

                    //se ci sono geofence che influenzano l'utente
                    if (dataSnapshot.child("information").child("geofenceinfo").exists()) {
                        for (DataSnapshot childSnapshot : dataSnapshot.child("information").child("geofenceinfo").getChildren()) {          //per ogni geofence
                            // aggiungo id del geofence e stato dell'utente rispetto al geofence alla lista
                            ArrayList<String> coppia = new ArrayList<>();
                            coppia.add(childSnapshot.child("0").getValue(String.class));
                            coppia.add(childSnapshot.child("1").getValue(String.class));
                            geofence_influence.add(coppia);
                        }


                        for (ArrayList<String> elem : geofence_influence) {
                            // Ottengo id e stato
                            String idGeofence = elem.get(0);
                            String state = elem.get(1);

                            //creo un intent per l'allerta
                            Intent alertIntent = new Intent("ACTION_NEW_ALERT_NOTIFICATION");

                            //inserisco l'activity riconosciuta nell'intent (per capire come riprodurre la notifica via audio)
                            alertIntent.putExtra("recognizedActivity", recognizedActivity);

                            //specifico che si tratta di un aggiunta di un geofence
                            alertIntent.putExtra("add_del", "add");

                            //ottengo il geofence che sta influenzando l'utente dalla mia lista geofence (aggiornata in precedenza)
                            CustomGeofence cg = geofence.get(idGeofence);

                            //ottengo le informazioni del geofence
                            String titolo = cg.getTitolo();
                            String allarme1 = cg.getAllarme1();
                            String allarme2 = cg.getAllarme2();
                            String allarme3 = cg.getAllarme3();

                            //in base allo stato (che mi dice dove sono) inserisco l'allarme interessato, le coordinate e la priorità
                            switch (state) {
                                case "DENTRO IL GEOFENCE":
                                    alertIntent.putExtra("alertText", allarme1);
                                    alertIntent.putExtra("priority", 1);
                                    sendBroadcast(alertIntent);
                                    break;
                                case "A 1 KM DAL GEOFENCE":
                                    alertIntent.putExtra("alertText", allarme2);
                                    alertIntent.putExtra("priority", 2);
                                    sendBroadcast(alertIntent);
                                    break;
                                case "1-2 KM DAL GEOFENCE":
                                    alertIntent.putExtra("alertText", allarme3);
                                    alertIntent.putExtra("priority", 3);
                                    sendBroadcast(alertIntent);
                                    break;
                            }

                        }

                    }

                    // faccio una copia dei geofence presenti nella mia lista user_geofence (per controlli successivi)
                    user_geofence.clear();
                    for (ArrayList<String> innerList : geofence_influence) {
                        ArrayList<String> innerCopy = new ArrayList<>(innerList);
                        user_geofence.add(innerCopy);
                    }
                    thirdOperationCompleted.complete(null);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d("Errore", databaseError.getMessage());
                }
            });
        });


        //Listener per verificare se viene aggiunto un nuovo geofence
        myRef4geofence.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                //Non inserisco niente qui perché il backend pusha prima un nuovo valore con solo l'identificativo.
                //Successivamente aggiunge i vari valori, quindi devo guardare l'onChange.
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                //qui vengono aggiunti realmente i valori distintivi del geofence

                String identificativo = dataSnapshot.getKey();                                          //prendo identificativo geofence
                String titolo = dataSnapshot.child("titolo").getValue(String.class);               //prendo titolo
                String allarme1 = dataSnapshot.child("allarme1").getValue(String.class);           //prendo allarme dentro il geofence
                String allarme2 = dataSnapshot.child("allarme2").getValue(String.class);           //prendo allarme a 1km dal geofence
                String allarme3 = dataSnapshot.child("allarme3").getValue(String.class);           //prendo allarme tra 1-2 km dal geofence

                //classe necessaria a deserializzare dati generici come liste ecc, firebase non riconosce automaticamente il tipo di dato.
                // Specifichiamo che stiamo ricevendo una lista di liste di double. Ogni lista indica una coordinata del punto del geofence.
                GenericTypeIndicator<ArrayList<ArrayList<Double>>> t = new GenericTypeIndicator<ArrayList<ArrayList<Double>>>() {};

                //Uso t per ottenere la mia lista di liste di double (coordinate)
                ArrayList<ArrayList<Double>> coordinateList = dataSnapshot.child("coordinate").getValue(t);

                //richiamo drawGeofence che creerà l'oggetto e lo disegnerà
                drawGeofence(identificativo, titolo, allarme1, allarme2, allarme3, coordinateList);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Quando un geofence viene rimosso lo tolgo dalla mia lista di geofence
                String identificativo = dataSnapshot.getKey();
                deleteGeofence(identificativo);

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("ERRORE", databaseError.getMessage());
            }
        });



        //funzione che viene richiamata quando uno dei campi di user cambia (o un geofence da cui è influenzato, o lo stato)
        thirdOperationCompleted.thenRun(() -> {
            myRef4user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {


                    //creo una lista temporanea di geofence che influenzano l'utente
                    ArrayList<ArrayList<String>> geofence_influence = new ArrayList<>();

                    //se esistono geofence che influenzano l'utente
                    if (dataSnapshot.child("information").child("geofenceinfo").exists()) {
                        for (DataSnapshot childSnapshot : dataSnapshot.child("information").child("geofenceinfo").getChildren()) {         //per ogni geofence
                            //aggiungo id geofence e stato dell'utente verso il geofence alla lista
                            ArrayList<String> coppia = new ArrayList<>();
                            coppia.add(childSnapshot.child("0").getValue(String.class));
                            coppia.add(childSnapshot.child("1").getValue(String.class));
                            geofence_influence.add(coppia);
                        }
                    }

                    // se i geofence che influenzano sono meno di quelli che influenzavano prima, si è cancellato un geofence, mando una notifica
                    if(geofence_influence.size()<user_geofence.size()){
                        //creo un intent per l'allerta
                        Intent alertIntent = new Intent("ACTION_NEW_ALERT_NOTIFICATION");

                        //specifico che si tratta di una cancellazione
                        alertIntent.putExtra("add_del", "del");

                        //inserisco l'activity riconosciuta nell'intent (per capire come riprodurre la notifica)
                        alertIntent.putExtra("recognizedActivity", recognizedActivity);

                        //invio l'allerta
                        sendBroadcast(alertIntent);
                    }

                    //altrimenti, è un'aggiunta
                    else{
                        //creo due liste per determinare quali geofence sono già stati considerati (e gli stati associati)
                        ArrayList<String> idpresenti = new ArrayList<>();
                        ArrayList<Integer> statipresenti = new ArrayList<>();
                        for (ArrayList<String> coppia : user_geofence) {
                            if (!coppia.isEmpty()) {
                                idpresenti.add(coppia.get(0));
                                if (coppia.get(1).equals("DENTRO IL GEOFENCE")) {
                                    statipresenti.add(1);
                                }
                                else if (coppia.get(1).equals("A 1 KM DAL GEOFENCE")) {
                                    statipresenti.add(2);
                                }
                                else {
                                    statipresenti.add(3);
                                }
                            }
                        }

                        //per ogni geofence considerato
                        for (ArrayList<String> elem : geofence_influence) {
                            // Ottengo id e stato associato ad esso
                            String idGeofence = elem.get(0);
                            String state = elem.get(1);

                            //definisco stato come variabile intera (mi serve dopo)
                            int nstate;
                            if (state.equals("DENTRO IL GEOFENCE")) {
                                nstate = 1;
                            }
                            else if (state.equals("A 1 KM DAL GEOFENCE")) {
                                nstate = 2;
                            }
                            else {
                                nstate = 3;
                            }

                            //controllo se è già presente, e se sì, a che indice della lista
                            int isPresent = idpresenti.indexOf(idGeofence);

                            //se non è presente o è già presente ma lo stato è diventato più grave (mi sono avvicinato al geofence)
                            if ((isPresent==-1) || (isPresent!=-1 && nstate<statipresenti.get(isPresent))){
                                //creo un intent per l'allerta
                                Intent alertIntent = new Intent("ACTION_NEW_ALERT_NOTIFICATION");

                                //inserisco l'activity riconosciuta nell'intent (per capire come riprodurre la notifica)
                                alertIntent.putExtra("recognizedActivity", recognizedActivity);

                                //specifico che si tratta di un'aggiunta
                                alertIntent.putExtra("add_del", "add");

                                //ottengo il geofence che sta influenzando l'utente dalla mia lista geofence (aggiornata in precedenza)
                                CustomGeofence cg = geofence.get(idGeofence);

                                //ottengo le informazioni del geofence
                                String titolo = cg.getTitolo();
                                String allarme1 = cg.getAllarme1();
                                String allarme2 = cg.getAllarme2();
                                String allarme3 = cg.getAllarme3();

                                //in base allo stato (che mi dice dove sono) inserisco l'allarme interessato, le coordinate e la priorità
                                switch (state) {
                                    case "DENTRO IL GEOFENCE":
                                        alertIntent.putExtra("alertText", allarme1);
                                        alertIntent.putExtra("priority", 1);
                                        sendBroadcast(alertIntent);
                                        break;
                                    case "A 1 KM DAL GEOFENCE":
                                        alertIntent.putExtra("alertText", allarme2);
                                        alertIntent.putExtra("priority", 2);
                                        sendBroadcast(alertIntent);
                                        break;
                                    case "1-2 KM DAL GEOFENCE":
                                        alertIntent.putExtra("alertText", allarme3);
                                        alertIntent.putExtra("priority", 3);
                                        sendBroadcast(alertIntent);
                                        break;
                                }
                            }


                        }
                    }

                    //faccio una copia dei geofence considerati per aggiornare la mia lista locale
                    user_geofence.clear();
                    for (ArrayList<String> innerList : geofence_influence) {
                        ArrayList<String> innerCopy = new ArrayList<>(innerList);
                        user_geofence.add(innerCopy);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("ERRORE", databaseError.getMessage());
                }
            });

        });


    }


    @Override
    protected void onStart() {
        super.onStart();


        //inizializzo l'intentFilter per la ricezione delle notifiche di allerta
        IntentFilter alertIntentFilter = new IntentFilter("ACTION_NEW_ALERT_NOTIFICATION");
        // associo il broadcast receiver al filtro per la ricezione delle notifiche
        registerReceiver(alertReceiver, alertIntentFilter);

        // verifico se il GPS è attualmente disattivato
        locationManagerImpl.checkAndRequestGpsPermissionUpdates();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (locationManagerImpl.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Il GPS è stato abilitato
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
                // Il GPS è stato abilitato
                if (locationManagerImpl.getLocationDialog() != null && locationManagerImpl.getLocationDialog().isShowing()) {
                    locationManagerImpl.getLocationDialog().dismiss();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }



    /**
     * Metodo usato per creare ed inizializzare il canale delle notifiche per gli alert.
     * */
    private void initAlertNotificationChannel() {
         /* Controllo se il dispositivo Android in esecuzione ha una versione maggiore o uguale
               a Android 8.0 (API 26) perché i canali delle notifiche sono supportati solo in questa versione in poi */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //inizializzo il canale
            String channelId = "alertChannelId";
            CharSequence channelName = "ALERT CHANNEL";
            String channelDescription = "Canale utile per la ricezione delle notifiche di nuovi alert";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            alertChannel = new NotificationChannel(channelId, channelName, importance);
            alertChannel.setDescription(channelDescription);

            // Ottengo il NotificationManager
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Creo il canale
            notificationManager.createNotificationChannel(alertChannel);

            // Creo il receiver degli alert e setto l'ìd del canale
            alertReceiver = new AlertReceiver(this, alertChannel.getId());
        } else {
            throw new RuntimeException("VERSIONE SDK NON SUPPORTATA");
        }
    }

    /**
     * Metodo usato per inizializzare il launcher usato per le varie richieste di permessi
     * */
    private void initRequestPermissionsLauncher() {
        // Inizializzo il launcher per la richiesta dei permessi.
        // Viene richiamato se non abbiamo concesso le varie autorizzazioni
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
                                // Tutti i permessi sono stati concessi
                            } else {
                                // Almeno uno o più permessi sono stati negati
                                // Mostro un dialog che informa l'utente e chiede se vuole aprire le impostazioni.
                                // Se l'utente clicca "No", l'applicazione viene chiusa
                                showAlertPermissionDialog();
                            }
                        });
    }

    /**
     * Metodo che mostra una finestra di dialogo per richiedere
     * all'utente di concedere tutti i permessi richiesti dall'applicazione.
     */
    private void showAlertPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permessi richiesti per continuare");
        builder.setMessage("Per utilizzare questa applicazione, devi concedere tutti i permessi richiesti. Vuoi andare alle impostazioni?");
        builder.setPositiveButton("Sì", (dialog, which) -> {
            // L'utente ha scelto "Sì", chiudi la finestra di dialogo e apri le impostazioni di localizzazione
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            dialog.dismiss();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            // L'utente ha scelto "No", chiudi la finestra di dialogo
            dialog.dismiss();

            // chiudi l'app
            finishAffinity();
        });

        // Assegno la finestra di dialogo a locationDialog e lo mostra
        builder.create().show();
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
                    "android.permission.POST_NOTIFICATIONS"
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
        // Ottengo la view della mappa dal layout XML
        mapView = findViewById(R.id.mapView);

        mapView.setMultiTouchControls(true); // Abilito il multi-touch

        mapController = (MapController) mapView.getController();
        mapController.setZoom(20);      // Imposto il livello di zoom iniziale

        // Abilito il provider di posizione GPS
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Imposto la posizione a una iniziale fittizia per evitare valori null iniziali e lo setto come centro della mappa
        GeoPoint startPoint = new GeoPoint(41.8902, 12.4922);
        mapView.getController().setCenter(startPoint);
        scaleGestureDetector = new ScaleGestureDetector(this, new MyScaleGestureListener());


        // Aggiungo un overlay di posizione
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getApplicationContext()), mapView);
        myLocationOverlay.enableMyLocation();                     // metto l'omino sulla mappa
        mapView.getOverlays().add(myLocationOverlay);


        //flag che uso per centrare la vista
        isMapToCenter = true;

        centerMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Imposto il flag per centrare la mappa a true
                isMapToCenter = true;
                requestLocationUpdates();
            }
        });

        //se tocco la mappa, non voglio che la mappa continui a centrarsi sulla mia posizione
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isMapToCenter = false;
                }
                return false;
            }
        });
    }


    /**
     * Metodo per inviare la posizione al backend Flask
     * */
    private void sendLocationToBackend(double latitude, double longitude) {
        //setto i dati dell'utente
        userData.setData(username, latitude, longitude, recognizedActivity);

        // Effettuo la richiesta HTTP
        Call<Void> call = sendUserDataService.uploadData(userData);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    //Log.d("SEND LOCATION TO BACKEND,", "success");
                } else {
                    //Log.d("SEND LOCATION TO BACKEND,", "failure");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                //Log.e("Retrofit", "Errore durante la richiesta HTTP", t);
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
                    //ottengo la posizione
                    location = locationResult.getLastLocation();

                    //invio la posizione al backend
                    sendLocationToBackend(location.getLatitude(), location.getLongitude());

                    //creo la currentlocation (per la mappa)
                    GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());


                    //uso il flag per capire se centrare la mappa o no
                    if(isMapToCenter){
                        mapView.getController().setCenter(currentLocation);
                    }

                    // Aggiorna la mappa
                    mapView.invalidate();


                }
            }
        };

        // ulteriore controllo sui permessi (da inserire obbligatoriamente per richiedere update della location)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Richiamo requestLocationUpdates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        // indico che ho eseguito la prima richiesta di aggiornamenti posizione
        firstOperationCompleted.complete(null);
    }


    private void drawGeofence(String identificativo, String titolo, String allarme1, String allarme2, String allarme3,ArrayList<ArrayList<Double>> points){
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


        // Creazione del poligono
        Polygon polygon = new Polygon();
        polygon.setPoints(polygonPoints);
        polygon.setFillColor(0x22FF0000); // Colore di riempimento con alpha
        polygon.setStrokeColor(Color.RED); // Colore del bordo
        polygon.setStrokeWidth(2); // Larghezza del bordo

        CustomGeofence cg = new CustomGeofence(titolo, allarme1, allarme2, allarme3, polygon);
        geofence.put(identificativo, cg);

        mapView.getOverlayManager().add(polygon);
    }


    //metodo per cancellare un geofence
    private void deleteGeofence(String identificativo){
        CustomGeofence cg = geofence.get(identificativo);
        Polygon polygon = cg.getPolygon();
        mapView.getOverlayManager().remove(polygon);

        geofence.remove(identificativo);
    }


    // Listener per il gesto di pizzicamento (per zoommare pizzicando)
    private class MyScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float zoomLevel = (float) mapView.getZoomLevelDouble();

            // Calcolo il nuovo livello di zoom
            zoomLevel *= scaleFactor;

            // Limito il livello di zoom minimo e massimo
            if (zoomLevel < mapView.getMinZoomLevel()) {
                zoomLevel = (float) mapView.getMinZoomLevel();
            } else if (zoomLevel > mapView.getMaxZoomLevel()) {
                zoomLevel = (float) mapView.getMaxZoomLevel();
            }

            // Imposto il nuovo livello di zoom
            mapController.setZoom((int) zoomLevel);

            return true;
        }
    }
}