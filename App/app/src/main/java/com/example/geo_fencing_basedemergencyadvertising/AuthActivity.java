package com.example.geo_fencing_basedemergencyadvertising;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private Button signUpBtn;
    private Button signInBtn;
    private EditText editTextEmail;
    private EditText editTextPassword;

    // Launcher per la richieste di autorizzazione.
    // Nelle nuove versioni di android bisogna richiederla anche da codice e non solo nel manifest
    private static ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        signUpBtn = findViewById(R.id.signUpButton);
        signInBtn = findViewById(R.id.signInButton);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        signUpBtn.setOnClickListener(view -> {
            registerUser();
        });

        signInBtn.setOnClickListener(view -> {
            signInUser();
        });

        //inizializzo launcher per richiedere permessi
        initRequestPermissionsLauncher();

        //richiamo il dialog se ci sono permessi da concedere
        checkAllPermissions();

        // Se l'utente ha fatto in precedenza il login senza effettuare il logout
        // reindirizzo l'utente direttamente alla main activity
        if (mAuth.getCurrentUser() != null){
            goToMainActivity();
        }
    }

    private void registerUser(){
        mAuth.createUserWithEmailAndPassword(editTextEmail.getText().toString(), editTextPassword.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {                             //se faccio la registrazione correttamente
                            goToMainActivity();
                        } else {
                            Toast.makeText(AuthActivity.this, "Unable to register. " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            System.out.println();
                        }
                    }
                });
    }

    private void signInUser(){
        mAuth.signInWithEmailAndPassword(editTextEmail.getText().toString(), editTextPassword.getText().toString())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {                                  //se faccio il login correttamente
                        goToMainActivity();
                    } else {
                        Toast.makeText(AuthActivity.this, "Credenziali errate", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToMainActivity(){
        startActivity(new Intent(this, MainActivity.class));
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
     * Metodo utilizzato per verificare la richiesta che tutti i permessi siano stati concessi.
     * Viene verificato dapprima se si usa l'app su un dispositivo con Android 10 o superiore:
     * in caso positivo allora viene controllato che tutti i permessi necessari siano garantiti;
     * se non sono garantiti, vengono richiesti.
     * */
    private void checkAllPermissions() {

        // Controllo se stiamo usando l'app su un dispositivo con Android 10 o superiore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String[] permissionsToCheck = {
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
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

}