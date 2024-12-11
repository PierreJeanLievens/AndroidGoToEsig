package com.example.projetandroidgotoesig;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameTextView, phoneTextView, cityTextView;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Utilisation du bouton de retour
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Lier les composants XML avec les objets Java
        nameTextView = findViewById(R.id.nameTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        cityTextView = findViewById(R.id.cityTextView);
        profileImageView = findViewById(R.id.profileImageView);

        // Récupérer l'id utilisateur depuis SharedPreferences
        String idUser = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("idUser", null);

        if (idUser == null) {
            Toast.makeText(this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            return;
        }

        // Récupérer les informations depuis Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user")
                .document(idUser)  // Utiliser l'id utilisateur pour récupérer les données
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Extraire les informations utilisateur
                        String name = documentSnapshot.getString("lastName");
                        String surname = documentSnapshot.getString("firstName");
                        String phone = documentSnapshot.getString("phoneNumber");
                        String city = documentSnapshot.getString("city");
                        String photoBase64 = documentSnapshot.getString("photo");

                        // Afficher les informations dans les TextViews
                        nameTextView.setText(String.format("%s %s", name, surname));
                        phoneTextView.setText(phone);
                        cityTextView.setText(city);

                        // Vérifier et charger l'image
                        if (photoBase64 != null && !photoBase64.isEmpty()) {
                            // Décoder et afficher l'image
                            Bitmap decodedImage = decodeBase64(photoBase64);
                            profileImageView.setImageBitmap(decodedImage);
                        } else {
                            profileImageView.setImageResource(R.drawable.placeholder); // Image par défaut
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Aucun utilisateur trouvé", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Erreur lors de la récupération des données.", Toast.LENGTH_SHORT).show();
                });
    }

    // Méthode pour décoder une image en Base64
    private Bitmap decodeBase64(String encodedImage) {
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }
}
