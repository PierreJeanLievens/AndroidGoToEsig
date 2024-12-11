package com.example.projetandroidgotoesig;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projetandroidgotoesig.fetch.LocationFetcher;
import com.example.projetandroidgotoesig.fetch.RouteFetcher;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTripActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private Spinner spinnerTransportMode;
    private EditText editStartPoint, editDelayTolerance, editSeatsAvailable, editContribution;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Button buttonAddTrip;
    private List<String> transportModeValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        // Utilisation du bouton de retour
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Initialiser les inputs
        db = FirebaseFirestore.getInstance();
        spinnerTransportMode = findViewById(R.id.spinnerTypeTransportMode);
        editStartPoint = findViewById(R.id.editStartPoint);
        editDelayTolerance = findViewById(R.id.editDelayTolerance);
        editSeatsAvailable = findViewById(R.id.editSeatsAvailable);
        editContribution = findViewById(R.id.editContribution);
        datePicker = findViewById(R.id.datePicker);
        timePicker = findViewById(R.id.timePicker);
        buttonAddTrip = findViewById(R.id.buttonAddTrip);

        // Récupérer les moyens de transport depuis Firestore
        fetchTransportModes();

        // Gérer la visibilité du champ "Contribution demandée"
        spinnerTransportMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedValue = transportModeValues.get(position);
                if ("driving-car".equals(selectedValue)) { // Si "voiture"
                    editContribution.setVisibility(View.VISIBLE);
                } else {
                    editContribution.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                editContribution.setVisibility(View.GONE);
            }
        });

        // Ajouter le trajet
        buttonAddTrip.setOnClickListener(v -> saveTrip());
    }

    private void fetchTransportModes() {
        db.collection("transportMode")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Map<String, String>> transportModes = (List<Map<String, String>>) queryDocumentSnapshots.getDocuments().get(0).get("type");
                        if (transportModes != null) {
                            List<String> displayValues = new ArrayList<>();
                            transportModeValues = new ArrayList<>();
                            for (Map<String, String> mode : transportModes) {
                                displayValues.add(mode.get("valueDisplay"));
                                transportModeValues.add(mode.get("value"));
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, displayValues);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerTransportMode.setAdapter(adapter);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("AddTripActivity", "Erreur lors de la récupération des modes de transport", e));
    }

    private void saveTrip() {
        String transportMode = transportModeValues.get(spinnerTransportMode.getSelectedItemPosition());
        String startPointText = editStartPoint.getText().toString().trim();
        String delayToleranceText = editDelayTolerance.getText().toString().trim();
        String seatsAvailableText = editSeatsAvailable.getText().toString().trim();
        String contributionText = editContribution.getText().toString().trim();

        // Vérification des champs obligatoires
        if (startPointText.isEmpty() || delayToleranceText.isEmpty() || seatsAvailableText.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir les données
        double delayTolerance = Double.parseDouble(delayToleranceText);
        int seatsAvailable = Integer.parseInt(seatsAvailableText);

        // Si cela est autre que driving-car, alors on prend la contribution à 0
        double contribution = transportMode.equalsIgnoreCase("driving-car") && !contributionText.isEmpty()
                ? Double.parseDouble(contributionText)
                : 0;

        // Date et heure
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                timePicker.getCurrentHour(), timePicker.getCurrentMinute());
        Timestamp date = new Timestamp(calendar.getTime());

        // Obtention des coordonnées à partir de l'adresse rentrée
        LocationFetcher.getGeoPointFromAddress(startPointText, startGeoPoint -> {
            if (startGeoPoint == null) {
                Toast.makeText(AddTripActivity.this, "Impossible de localiser le point de départ " + startPointText, Toast.LENGTH_SHORT).show();
                return;
            }

            // Définir le point de destination (ESIGELEC)
            GeoPoint endGeoPoint = new GeoPoint(49.383293, 1.0773042);

            // Utilisation de RouteFetcher pour obtenir la distance et la durée
            RouteFetcher.getRouteSummary(startGeoPoint, endGeoPoint, transportMode, summary -> {
                if (summary == null) {
                    Toast.makeText(AddTripActivity.this, "Impossible de calculer l'itinéraire", Toast.LENGTH_SHORT).show();
                    return;
                }

                double distance = Math.round((summary.distance / 1000) * 100.0) / 100.0; // En km
                int totalSeconds = (int) Math.round(summary.duration); // Durée totale en secondes

                // Afficher la boîte de dialogue de confirmation
                showConfirmationDialog(distance, totalSeconds, transportMode, startGeoPoint, endGeoPoint, date, delayTolerance, seatsAvailable, contribution);
            });
        });
    }

    private void showConfirmationDialog(double distance, int totalSeconds, String transportMode, GeoPoint startGeoPoint, GeoPoint endGeoPoint, Timestamp date, double delayTolerance, int seatsAvailable, double contribution) {
        int minutes = totalSeconds / 60; // Minutes entières
        int seconds = totalSeconds % 60; // Secondes restantes
        // Construction de la boîte de dialogue
        new AlertDialog.Builder(this)
                .setTitle("Confirmer l'ajout du trajet")
                .setMessage("Trajet de " + distance + " km en " + minutes + " min " + seconds + " sec.\n" +
                        "Mode de transport : " + transportMode + "\n" +
                        "Contrib. demandée : " + contribution + " €\n\n" +
                        "Voulez-vous ajouter ce trajet ?")
                .setPositiveButton("Confirmer", (dialog, which) -> {
                    // Ajouter le trajet dans Firestore si confirmé
                    addTripToFirestore(distance, totalSeconds, transportMode, startGeoPoint, endGeoPoint, date, delayTolerance, seatsAvailable, contribution);
                })
                .setNegativeButton("Annuler", (dialog, which) -> {
                    dialog.dismiss(); // Fermer la boîte de dialogue
                })
                .show();
    }

    private void addTripToFirestore(double distance, int totalSeconds, String transportMode, GeoPoint startGeoPoint, GeoPoint endGeoPoint, Timestamp date, double delayTolerance, int seatsAvailable, double contribution) {
        // Récupérer l'idUser depuis SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String idUser = sharedPreferences.getString("idUser", null);
        if (idUser == null) {
            Toast.makeText(AddTripActivity.this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            return;
        }

        // Référence utilisateur pour Firestore avec l'id
        DocumentReference userRef = db.collection("user").document(idUser);

        // Préparer les données pour Firestore
        Map<String, Object> trip = new HashMap<>();
        trip.put("transportMode", transportMode);
        trip.put("startPoint", startGeoPoint);
        trip.put("endPoint", endGeoPoint);
        trip.put("date", date); // Timestamp
        trip.put("delayTolerance", delayTolerance);
        trip.put("seatsAvailable", seatsAvailable);
        trip.put("seatsBooked", 0); // Initialisation à 0
        trip.put("price", contribution);
        trip.put("userId", userRef); // Référence Firestore
        trip.put("distance", distance);
        trip.put("duration", totalSeconds);
        trip.put("seatsBooked", new ArrayList<>()); // Tableau vide pour les places réservées

        // Ajouter le document dans la collection "trips"
        db.collection("travel")
                .add(trip)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddTripActivity.this, "Trajet ajouté avec succès", Toast.LENGTH_SHORT).show();

                    // Redirection vers l'accueil
                    Intent homeIntent = new Intent(AddTripActivity.this, HomeActivity.class);
                    startActivity(homeIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddTripActivity", "Erreur lors de l'ajout du trajet", e);
                    Toast.makeText(AddTripActivity.this, "Erreur lors de l'ajout du trajet", Toast.LENGTH_SHORT).show();
                });
    }


}
