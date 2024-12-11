package com.example.projetandroidgotoesig;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    EditText editTextEmail, editTextPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Listener pour le bouton "S'inscrire"
        btnLogin.setOnClickListener(v -> {
            // Récupérer les valeurs des champs
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();

            // Vérifier si tous les champs sont remplis
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }
            // Requête pour trouver l'utilisateur par email
            Query query = db.collection("user").whereEqualTo("email", email);
            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    // Récupérer le document de l'utilisateur
                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                    String storedPassword = document.getString("password");
                    String idUser = document.getId();

                    // Vérifier si le mot de passe correspond
                    if (storedPassword != null && storedPassword.equals(password)) {
                        Toast.makeText(LoginActivity.this, "Connexion réussie !", Toast.LENGTH_SHORT).show();
                        Intent homeIntent = new Intent(LoginActivity.this, HomeActivity.class);
                        homeIntent.putExtra("idUser", idUser);

                        // Stocke idUser dans SharedPreferences
                        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("idUser", idUser);
                        editor.apply();

                        startActivity(homeIntent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show();
                    Log.e("GoToEsig", "Erreur : " + task.getException());
                }
            });
        });
    }
}