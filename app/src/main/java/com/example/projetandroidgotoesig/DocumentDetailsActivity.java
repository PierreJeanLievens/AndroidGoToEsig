package com.example.projetandroidgotoesig;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.GeoPoint;

import org.jetbrains.annotations.NotNull;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Cette activité permet d'afficher le trajet d'un document 'travel' dans une carte.
 */
public class DocumentDetailsActivity extends AppCompatActivity {
    private String documentId, addressInput;
    private MapView mapView;
    private IMapController mapController;
    private Button backButton, chooseButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_details);

        // Utilisation du bouton de retour
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        chooseButton = findViewById(R.id.chooseButton);
        chooseButton.setOnClickListener(v -> chooseTravel());
        // Configuration de la carte
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        mapView = findViewById(R.id.mapView);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMultiTouchControls(true);
        mapController = mapView.getController();
        mapController.setZoom(13.0);

        // Récupérer l'ID du document passé dans l'Intent
        Intent intent = getIntent();
        documentId = intent.getStringExtra("DOCUMENT_ID");
        addressInput = intent.getStringExtra("Address_input");

        if (documentId != null) {
            // Récupérer les détails du document Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("travel").document(documentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            com.google.firebase.firestore.GeoPoint startPoint = documentSnapshot.getGeoPoint("startPoint");
                            com.google.firebase.firestore.GeoPoint endPoint = documentSnapshot.getGeoPoint("endPoint");

                            if (startPoint != null && endPoint != null) {
                                // Convertir les GeoPoints Firestore en OSMGeoPoints
                                GeoPoint startGeoPoint = new GeoPoint(startPoint.getLatitude(), startPoint.getLongitude());
                                GeoPoint endGeoPoint = new GeoPoint(endPoint.getLatitude(), endPoint.getLongitude());

                                // Ajouter un marqueur pour le point de départ
                                Marker startMarker = new Marker(mapView);
                                startMarker.setPosition(startGeoPoint);
                                startMarker.setTitle("Point de départ");
                                mapView.getOverlays().add(startMarker);

                                // Ajouter un marqueur pour le point d'arrivée
                                Drawable drawable = getResources().getDrawable(R.drawable.placeholder);
                                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false); // Taille 50x50

                                Marker endMarker = new Marker(mapView);
                                endMarker.setPosition(endGeoPoint);
                                endMarker.setIcon(new BitmapDrawable(getResources(), resizedBitmap));
                                endMarker.setTitle("Point d'arrivée");
                                mapView.getOverlays().add(endMarker);

                                // Centrer la carte sur le point de départ
                                mapController.setCenter(startGeoPoint);

                                // Récupérer et afficher le tracé entre les deux points
                                fetchRouteAndDisplay(startGeoPoint, endGeoPoint);
                            } else {
                                Toast.makeText(this, "Points de départ ou d'arrivée manquants.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erreur lors de la récupération des données Firestore.", Toast.LENGTH_SHORT).show();
                    });
        }
    }



    private void fetchRouteAndDisplay(GeoPoint startGeoPoint, GeoPoint endGeoPoint) {
        // Construire l'URL pour OpenRouteService
        String apiKey = "5b3ce3597851110001cf6248ac57c84bcec64455b61413718c74b1ac";
        String url = "https://api.openrouteservice.org/v2/directions/cycling-regular?api_key=" + apiKey +
                "&start=" + startGeoPoint.getLongitude() + "," + startGeoPoint.getLatitude() +
                "&end=" + endGeoPoint.getLongitude() + "," + endGeoPoint.getLatitude();

        // Effectuer la requête HTTP
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Toast.makeText(DocumentDetailsActivity.this, "Erreur réseau : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray coordinates = json.getJSONArray("features")
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONArray("coordinates");

                        // On ajoute tous les points du trajet dans une liste
                        List<GeoPoint> geoPoints = new ArrayList<>();
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray point = coordinates.getJSONArray(i);
                            double lon = point.getDouble(0);
                            double lat = point.getDouble(1);
                            geoPoints.add(new GeoPoint(lat, lon));
                        }

                        // Afficher le tracé sur la carte
                        Polyline line = new Polyline();
                        line.setPoints(geoPoints);
                        mapView.getOverlayManager().add(line);

                        mapView.invalidate(); // Rafraîchir la carte

                    } catch (Exception e) {
                        Toast.makeText(DocumentDetailsActivity.this, "Erreur lors du traitement des données de l'itinéraire.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(DocumentDetailsActivity.this, "Erreur API : " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void chooseTravel() {
        // Récupérer l'idUser depuis SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String idUser = sharedPreferences.getString("idUser", null);

        if (idUser == null) {
            Toast.makeText(DocumentDetailsActivity.this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            return;
        }

        // Référence utilisateur
        String userReference = "/user/" + idUser;

        if (documentId == null) {
            Toast.makeText(DocumentDetailsActivity.this, "Document non valide.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vérifier si la référence existe déjà dans seatsBooked et si des places sont encore disponibles
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("travel").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> seatsBooked = (List<String>) documentSnapshot.get("seatsBooked");
                        Long seatsAvailable = documentSnapshot.getLong("seatsAvailable");

                        if (seatsAvailable == null) {
                            Toast.makeText(DocumentDetailsActivity.this, "Information sur les places indisponible.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (seatsBooked != null && seatsBooked.contains(userReference)) {
                            // Si l'utilisateur a déjà réservé
                            Toast.makeText(DocumentDetailsActivity.this, "Vous avez déjà réservé ce trajet.", Toast.LENGTH_SHORT).show();
                        } else if (seatsBooked != null && seatsBooked.size() >= seatsAvailable) {
                            // Si toutes les places sont réservées
                            Toast.makeText(DocumentDetailsActivity.this, "Aucune place disponible pour ce trajet.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Ajouter la référence dans le champ seatsBooked
                            db.collection("travel").document(documentId)
                                    .update("seatsBooked", FieldValue.arrayUnion(userReference))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(DocumentDetailsActivity.this, "Trajet réservé avec succès.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(DocumentDetailsActivity.this, "Erreur lors de la réservation : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(DocumentDetailsActivity.this, "Document introuvable.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DocumentDetailsActivity.this, "Erreur lors de la vérification des réservations : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



}
