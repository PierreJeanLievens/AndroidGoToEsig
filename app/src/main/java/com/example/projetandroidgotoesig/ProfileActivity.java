package com.example.projetandroidgotoesig;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private TextView lastNameTextView, firstNameTextView, emailTextView;
    private EditText phoneEditText, cityEditText;
    private ImageView profileImageView;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Bouton Retour
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Liaison des composants
        lastNameTextView = findViewById(R.id.lastNameTextView);
        firstNameTextView = findViewById(R.id.firstNameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        phoneEditText = findViewById(R.id.phoneEditText);
        cityEditText = findViewById(R.id.cityEditText);
        profileImageView = findViewById(R.id.profileImageView);

        Button changePhotoButton = findViewById(R.id.changePhotoButton);
        Button saveButton = findViewById(R.id.saveButton);

        // Récupérer l'utilisateur depuis Firestore
        String idUser = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("idUser", null);
        if (idUser == null) {
            Toast.makeText(this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user")
                .document(idUser)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        lastNameTextView.setText(documentSnapshot.getString("lastName"));
                        firstNameTextView.setText(documentSnapshot.getString("firstName"));
                        emailTextView.setText(documentSnapshot.getString("email"));
                        phoneEditText.setText(documentSnapshot.getString("phoneNumber"));
                        cityEditText.setText(documentSnapshot.getString("city"));

                        String photo = documentSnapshot.getString("photo");
                        if (photo != null) {
                            profileImageView.setImageBitmap(decodeBase64(photo));
                        }
                    }
                });

        // Listener pour sélectionner une photo
        changePhotoButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Sélectionner une photo"), REQUEST_IMAGE_CAPTURE);
        });


        // Sauvegarder les changements
        saveButton.setOnClickListener(v -> {
            String phone = phoneEditText.getText().toString();
            String city = cityEditText.getText().toString();
            String encodedImage = encodeImage();
            db.collection("user").document(idUser)
                    .update("phoneNumber", phone, "city", city, "photo", encodedImage)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profil mis à jour", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Erreur de sauvegarde", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            profileImageView.setImageURI(selectedImageUri); // Prévisualiser l'image sélectionnée
        }
    }

    private Bitmap decodeBase64(String encodedImage) {
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }
    /**
     * Méthode pour encoder une image en Base64
     * @return
     */
    private String encodeImage() {
        Bitmap bitmap = ((BitmapDrawable) profileImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); // Compresser l'image
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}
