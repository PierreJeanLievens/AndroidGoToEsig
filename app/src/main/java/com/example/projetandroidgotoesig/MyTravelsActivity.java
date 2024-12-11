package com.example.projetandroidgotoesig;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projetandroidgotoesig.fetch.CoordinatesFetcher;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyTravelsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    //private ListView listViewIncoming, listViewPast; // Déclaration des ListViews

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_travels);

        // Utilisation du bouton de retour
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Initialiser Firestore
        db = FirebaseFirestore.getInstance();
        //listViewPast = findViewById(R.id.listViewPast); // Lier à la ListView dans XML
        //listViewIncoming = findViewById(R.id.listViewIncoming); // Lier à la ListView dans XML

        // Charger les données Firestore
        fetchAllTravels();
    }

    /**
     * Charge toutes les données Firestore pour les trajets.
     */
    private void fetchAllTravels() {
        db.collection("travel")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    //<String> travelList = new ArrayList<>();
                    List<String> pastTravels = new ArrayList<>();
                    List<String> upcomingTravels = new ArrayList<>();
                    Date currentDate = new Date(); // Obtenir la date actuelle

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String distance = (String) document.get("distance").toString();

                        // Récupérer la durée (s'assurer qu'elle soit bien un Long ou un Double)
                        Object durationObj = document.get("duration");
                        Double durationInSeconds = 0.0;

                        if (durationObj instanceof Long) {
                            durationInSeconds = ((Long) durationObj).doubleValue();
                        } else if (durationObj instanceof Double) {
                            durationInSeconds = (Double) durationObj;
                        }

                        // Calcul des minutes et secondes
                        int minutes = (int) (durationInSeconds / 60);  // Minutes entières
                        int seconds = (int) (durationInSeconds % 60);  // Secondes restantes
                        // Formater la durée
                        String durationFormatted = String.format("%d min %d sec", minutes, seconds);

                        // On formatte la date ici
                        Timestamp timestamp = document.getTimestamp("date");
                        Date travelDate = timestamp != null ? timestamp.toDate() : null;
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        String formattedDate = travelDate != null ? sdf.format(travelDate) : "Date incconue";

                        // On récupère le point de départ
                        GeoPoint startPoint = document.getGeoPoint("startPoint");

                        // On récupère le moyen de transport
                        String transportMode = document.getString("transportMode");

                        // On récuère le délai accepté
                        Integer delayTolerance =  document.getDouble("delayTolerance").intValue();

                        // On récupère le prix du trajet
                        Double price = document.getDouble("price");

                        // On récupère le nombre de places disponibles que l'on convertit en int
                        Integer seatsAvailable =  document.getDouble("seatsAvailable").intValue();

                        // On teste si c'est une liste, puis on compte le nombre d'éléments
                        Integer seatsBookedCount = ((List<?>) document.get("seatsBooked")).size();

                        // Si nous avons des coordonnées, appeler l'API pour récupérer l'adresse
                        if (startPoint != null) {
                            double latitude = startPoint.getLatitude();
                            double longitude = startPoint.getLongitude();

                            // Appeler l'API pour obtenir l'adresse
                            CoordinatesFetcher.getAddressFromGeoPoint(latitude, longitude, new CoordinatesFetcher.LocationCallback() {
                                @Override
                                public void onLocationFetched(String address) {
                                    // Utiliser runOnUiThread pour mettre à jour l'UI sur le thread principal
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String travelDescription = "Date: " + formattedDate +
                                                    "\nAdresse de départ: " + (address != null ? address : "Address not found") +
                                                    "\nDistance: " + distance + " km" +
                                                    "\nDurée estimée: " + durationFormatted +
                                                    "\nMoyen de transport: " + transportMode +
                                                    "\nRetard accordé: " + delayTolerance + "min" +
                                                    "\nPrix: " + price + "€" +
                                                    "\nTotal des places disponibles: " + seatsAvailable +
                                                    "\nTotal des places réservées: " + seatsBookedCount;

                                            // Séparer les trajets passés et à venir en fonction de la date
                                            if (travelDate != null) {
                                                if (travelDate.before(currentDate)) {
                                                    pastTravels.add(travelDescription);  // Trajet passé
                                                } else {
                                                    upcomingTravels.add(travelDescription);  // Trajet à venir
                                                }
                                            }

                                            // Mettre à jour les ListViews une fois toutes les adresses récupérées
                                            if (pastTravels.size() + upcomingTravels.size() == queryDocumentSnapshots.size()) {
                                                displayTravels(pastTravels, upcomingTravels);  // Afficher les trajets
                                            }
                                        }
                                    });
                                }
                            });

                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MyTravelsActivity", "Erreur lors de la récupération des trajets", e);
                    Toast.makeText(this, "Erreur lors du chargement des trajets", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Affiche les trajets passés et à venir dans les ListViews.
     * @param pastTravels
     * @param upcomingTravels
     */
    private void displayTravels(List<String> pastTravels, List<String> upcomingTravels) {
        LinearLayout containerIncoming = findViewById(R.id.containerIncoming);
        LinearLayout containerPast = findViewById(R.id.containerPast);

        // Ajouter les trajets à venir
        for (String travel : upcomingTravels) {
            TextView travelView = new TextView(this);
            travelView.setText(travel);
            travelView.setPadding(8, 12, 8, 12);
            //travelView.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            containerIncoming.addView(travelView);
        }

        // Ajouter les trajets passés
        for (String travel : pastTravels) {
            TextView travelView = new TextView(this);
            travelView.setText(travel);
            travelView.setPadding(8, 12, 8, 12);
            //travelView.setBackgroundResource(androidid.R.drawable.dialog_holo_light_frame);
            containerPast.addView(travelView);
        }
    }

}
