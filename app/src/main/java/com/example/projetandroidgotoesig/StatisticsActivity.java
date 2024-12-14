package com.example.projetandroidgotoesig;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Utilisation du bouton de retour
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Récupérer l'idUser depuis SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String idUser = sharedPreferences.getString("idUser", null);
        if (idUser == null) {
            Toast.makeText(StatisticsActivity.this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            return;
        }

        // Récupérer les références aux éléments UI pour afficher les statistiques
        TextView tripsCountTextView = findViewById(R.id.tripsCountTextView);
        TextView totalAmountTextView = findViewById(R.id.totalAmountTextView);
        TextView totalCarbonTextView = findViewById(R.id.totalCarbonTextView);

        // Récupérer la référence à l'utilisateur dans la collection "user"
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("user").document(idUser);

        // Rechercher les trajets associés à cet utilisateur en comparant avec la référence
        db.collection("travel")
                .whereEqualTo("userId", userRef)  // Comparer avec la référence
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int tripsCount = 0;  // Compteur de trajets
                    double totalAmount = 0.0;  // Total des montants encaissés
                    double distanceTotal = 0.0; // Total des distances
                    // Parcourir tous les trajets de l'utilisateur
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        // On ajoute le trajet au total
                        tripsCount++;

                        // Récupérer la date du trajet
                        Date date = document.getDate("date");

                        if (date != null && date.before(new Date())) {  // Vérifier si la date est passée
                            // Récupérer le prix
                            Double price = document.getDouble("price");

                            // Récupérer la distance
                            Double distance = document.getDouble("distance");

                            // Récupérer le tableau seatsBooked
                            List<?> seatsBooked = (List<?>) document.get("seatsBooked");

                            if (seatsBooked != null) {

                                if (distance != null) {
                                    // Calculer la distance totale pour ce trajet
                                    distanceTotal += distance * seatsBooked.size();
                                }

                                if (price != null) {
                                    // Calculer le montant total pour ce trajet
                                    totalAmount += price * seatsBooked.size();  // Multiplier le prix par le nombre de places réservées
                                }
                            }
                        }
                    }

                    // Calculer l'émission carbone
                    Double carbone =  distanceTotal * (6.5/100)*2.31;
                    // Formater l'émission carbone avec 2 décimales
                    String carboneFormate = String.format("%.2f", carbone);

                    // Afficher le nombre de trajets et le total des montants
                    tripsCountTextView.setText("Nombre de trajets créés : " + tripsCount);
                    totalAmountTextView.setText("Total des gains: " + totalAmount + " €");
                    totalCarbonTextView.setText("Emissions carbones économisés: " + carboneFormate + " kg CO₂ ");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StatisticsActivity.this, "Erreur lors de la récupération des trajets : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
