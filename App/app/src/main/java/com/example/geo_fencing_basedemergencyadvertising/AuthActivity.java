package com.example.geo_fencing_basedemergencyadvertising;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private Button signUpBtn;
    private Button signInBtn;
    private EditText editTextEmail;
    private EditText editTextPassword;

//    FirebaseFirestore db = FirebaseFirestore.getInstance();

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
    }

    private void registerUser(){
        mAuth.createUserWithEmailAndPassword(editTextEmail.getText().toString(), editTextPassword.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();

                            goToMainActivity();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(AuthActivity.this, "Impossibile effettuare la registrazione",
                                    Toast.LENGTH_LONG).show();

                        }
                    }
                });
    }

    private void signInUser(){
        mAuth.signInWithEmailAndPassword(editTextEmail.getText().toString(), editTextPassword.getText().toString())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
//                        FirebaseUser user = mAuth.getCurrentUser();
                        goToMainActivity();
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(AuthActivity.this, "Credenziali errate",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}