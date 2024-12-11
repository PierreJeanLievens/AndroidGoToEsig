package com.example.projetandroidgotoesig;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projetandroidgotoesig.fetch.LocationFetcher;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchTravelActivity extends AppCompatActivity {

    private DatePicker datePicker;
    private EditText etAddress;
    private Button btnSearch;
    private FirebaseFirestore db;
    private List<String> travelResults = new ArrayList<>();
    private List<QueryDocumentSnapshot> documentList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_travel);

        // Utilisation du bouton de retour
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Initialiser Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialiser les vues
        datePicker = findViewById(R.id.datePicker);
        etAddress = findViewById(R.id.etAddress);
        btnSearch = findViewById(R.id.btnSearch);

        // Initialisation de l'adaptateur
        ListView lvResults = findViewById(R.id.lvResults);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, travelResults);
        lvResults.setAdapter(adapter);

        // Lors du clic sur un élément de la liste, ouvrir la nouvelle activité
        lvResults.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = travelResults.get(position);
            Toast.makeText(this, "Trajet sélectionné:\n" + selectedItem, Toast.LENGTH_SHORT).show();

            // Récupérer le document sélectionné à partir de la liste
            QueryDocumentSnapshot selectedDocument = documentList.get(position);

            // Récupérer l'ID du document
            String documentId = selectedDocument.getId();

            // Créer un Intent pour ouvrir la nouvelle activité
            Intent intent = new Intent(this, DocumentDetailsActivity.class);
            intent.putExtra("DOCUMENT_ID", documentId);  // Passer l'ID du document
            intent.putExtra("Address_input", etAddress.getText().toString().trim());  // Passer l'adresse entrée
            startActivity(intent);
        });

        // Lors du clic sur le bouton de recherche
        btnSearch.setOnClickListener(v -> performSearch());
    }

    /**
     * Effectue la recherche des trajets en fonction de la date et de l'adresse.
     */
    private void performSearch() {
        // Récupérer la date sélectionnée
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();

        // Construire la date sélectionnée
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        Date selectedDateStart = calendar.getTime(); // Début du jour sélectionné

        calendar.set(year, month, day, 23, 59, 59);
        Date selectedDateEnd = calendar.getTime(); // Fin du jour sélectionné

        // Récupérer l'adresse saisie
        String address = etAddress.getText().toString().trim();

        // Vérifier que l'adresse n'est pas vide
        if (address.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer une adresse de départ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Récupérer le GeoPoint correspondant à l'adresse
        LocationFetcher.getGeoPointFromAddress(address, addressGeoPoint -> {
            if (addressGeoPoint == null) {
                Toast.makeText(this, "Adresse introuvable. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Récupérer l'idUser depuis SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            String idUser = sharedPreferences.getString("idUser", null);
            if (idUser == null) {
                Toast.makeText(SearchTravelActivity.this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
                return;
            }

            // Rechercher les trajets correspondant à la date recherchée, on trie aussi par date croissante
            db.collection("travel")
                    .whereGreaterThanOrEqualTo("date", new Timestamp(selectedDateStart))
                    .whereLessThanOrEqualTo("date", new Timestamp(selectedDateEnd))
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        travelResults.clear(); // Efface les résultats précédents
                        documentList.clear();  // Vider la liste des documents

                        // Traiter tous les documents trouvés
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Log.d("Document","Docuement : " + document);

                            // Récupérer la référence de l'utilisateur
                            DocumentReference userRef = document.getDocumentReference("userId");

                            // Vérifier si la référence ne correspond pas à celle de l'utilisateur courant
                            if (userRef != null && !userRef.getId().equals(idUser)) {

                                // Traiter et afficher les résultats
                                GeoPoint startPoint = document.getGeoPoint("startPoint");
                                if (startPoint != null) {
                                    // Calculer la distance
                                    double distance = calculateDistance(addressGeoPoint, startPoint);

                                    // Lire les champs avec vérification
                                    String transportMode = document.contains("transportMode") ? document.getString("transportMode") : "N/A";
                                    String distanceText = document.contains("distance") ? document.get("distance").toString() : "N/A";
                                    String price = document.contains("price") ? document.get("price").toString() : "N/A";

                                    // On récupère la date puis on la formate en dd/MM/yyyy HH:mm
                                    Timestamp timestamp = document.getTimestamp("date");
                                    Date travelDate = timestamp != null ? timestamp.toDate() : null;
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                    String formattedDate = travelDate != null ? sdf.format(travelDate) : "Date inconnue";

                                    // Ajouter le résultat formaté
                                    String result = String.format(
                                            Locale.getDefault(),
                                            "Date: %s\nTransport: %s\nDistance du trajet: %s km\nDistance vers le départ: %.2f km\nPrix: %s €",
                                            formattedDate,
                                            transportMode,
                                            distanceText,
                                            distance,
                                            price
                                    );
                                    // Ajouter le document à la liste si l'utilisateur est différent
                                    documentList.add(document);
                                    travelResults.add(result);
                                }
                            }
                        }

                        // Gérer le cas où aucun trajet n'est trouvé
                        if (travelResults.isEmpty()) {
                            travelResults.add("Aucun trajet trouvé pour cette date.");
                        }

                        // Mise à jour de l'adaptateur
                        adapter.notifyDataSetChanged();
                        ListView lvResults = findViewById(R.id.lvResults);
                        setListViewHeightBasedOnChildren(lvResults);
                    })
                    .addOnFailureListener(e -> {
                        travelResults.clear();
                        travelResults.add("Erreur lors de la récupération des trajets ou pas de trajets disponibles.");
                        adapter.notifyDataSetChanged();
                        ListView lvResults = findViewById(R.id.lvResults);
                        setListViewHeightBasedOnChildren(lvResults);
                        Toast.makeText(this, "Erreur lors de la récupération des trajets ou pas de trajets disponibles", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    /**
     * Calcule la distance entre deux points géographiques.
     * @param point1
     * @param point2
     * @return
     */
    private double calculateDistance(GeoPoint point1, GeoPoint point2) {
        double earthRadius = 6371; // Rayon de la Terre en km

        double lat1 = point1.getLatitude();
        double lon1 = point1.getLongitude();
        double lat2 = point2.getLatitude();
        double lon2 = point2.getLongitude();

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c; // Distance en km
    }

    /**
     * Met à jour la hauteur de la ListView en fonction des éléments qu'elle contient.
     * @param listView
     */
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.UNSPECIFIED
            );
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}

