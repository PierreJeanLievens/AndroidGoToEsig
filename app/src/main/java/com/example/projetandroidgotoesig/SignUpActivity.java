package com.example.projetandroidgotoesig;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    EditText editTextFirstName, editTextLastName, editTextEmail, editTextPhoneNumber,
            editTextCity, editTextPassword;
    Button btnSubmit, btnSelectPhoto;
    ImageView imageViewPhotoPreview;

    private Uri selectedImageUri;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialiser Firestore
        db = FirebaseFirestore.getInstance();

        // Initialiser les champs
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextCity = findViewById(R.id.editTextCity);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        imageViewPhotoPreview = findViewById(R.id.imageViewPhotoPreview);

        // Listener pour sélectionner une photo
        btnSelectPhoto.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Sélectionner une photo"), PICK_IMAGE_REQUEST);
        });

        // Listener pour le bouton "S'inscrire"
        btnSubmit.setOnClickListener(v -> {
            // Récupérer les valeurs des champs
            String firstName = editTextFirstName.getText().toString();
            String lastName = editTextLastName.getText().toString();
            String email = editTextEmail.getText().toString();
            String phoneNumber = editTextPhoneNumber.getText().toString();
            String city = editTextCity.getText().toString();
            String password = editTextPassword.getText().toString();

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                    phoneNumber.isEmpty() || city.isEmpty() || password.isEmpty() || selectedImageUri == null) {
                Toast.makeText(SignUpActivity.this, "Veuillez remplir tous les champs et sélectionner une photo", Toast.LENGTH_SHORT).show();
            } else {
                // Encoder l'image
                String encodedImage = encodeImage();

                // Hashage du password
                String hashedPassword = SimpleHasher.hashPassword(password);

                // Créer une carte pour l'utilisateur
                Map<String, Object> newUser = new HashMap<>();
                newUser.put("firstName", firstName);
                newUser.put("lastName", lastName);
                newUser.put("email", email);
                newUser.put("phoneNumber", phoneNumber);
                newUser.put("city", city);
                newUser.put("password", hashedPassword);
                newUser.put("photo", encodedImage);

                // Enregistrer dans Firestore
                db.collection("user").document().set(newUser)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(SignUpActivity.this, "Inscription réussie !", Toast.LENGTH_SHORT).show();

                            // Rediriger vers LoginActivity
                            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("SignUpActivity", e.toString());
                            Toast.makeText(SignUpActivity.this, "Erreur lors de l'inscription.", Toast.LENGTH_SHORT).show();
                        });
            }
        });

    }

    // Gérer le résultat de la sélection d'image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imageViewPhotoPreview.setImageURI(selectedImageUri); // Prévisualiser l'image sélectionnée
        }
    }

    /**
     * Méthode pour encoder une image en Base64
     * @return
     */
    private String encodeImage() {
        Bitmap bitmap = ((BitmapDrawable) imageViewPhotoPreview.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); // Compresser l'image
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}
