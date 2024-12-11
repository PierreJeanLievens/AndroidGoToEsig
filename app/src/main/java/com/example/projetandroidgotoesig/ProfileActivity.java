package com.example.projetandroidgotoesig;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private String photoBase64 = null;

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

        // Changer la photo
        changePhotoButton.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        // Sauvegarder les changements
        saveButton.setOnClickListener(v -> {
            String phone = phoneEditText.getText().toString();
            String city = cityEditText.getText().toString();

            db.collection("user").document(idUser)
                    .update("phoneNumber", phone, "city", city, "photo", photoBase64)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profil mis à jour", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Erreur de sauvegarde", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            profileImageView.setImageBitmap(photo);
            photoBase64 = encodeToBase64(photo);
        }
    }

    private Bitmap decodeBase64(String encodedImage) {
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private String encodeToBase64(Bitmap image) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }
}
