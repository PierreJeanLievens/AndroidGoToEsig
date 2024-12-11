package com.example.projetandroidgotoesig.fetch;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class LocationFetcher {

    // Fonction pour récupérer les coordonnées à partir de l'adresse
    public static void getGeoPointFromAddress(String address, LocationCallback callback) {
        // Construire l'URL pour l'API de géocodage
        String apiKey = "5b3ce3597851110001cf6248ac57c84bcec64455b61413718c74b1ac";
        String encodedAddress = address.replaceAll(" ", "+"); // Encoder l'adresse si nécessaire
        String url = "https://api.openrouteservice.org/geocode/search?api_key=" + apiKey + "&text=" + encodedAddress;

        // Effectuer la requête HTTP
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Envoi de la requête de manière asynchrone
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API Error", "Erreur lors de la requête : " + e.getMessage());
                callback.onLocationFetched(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray features = jsonResponse.getJSONArray("features");
                        GeoPoint geoPoint = null;

                        if (features.length() > 0) {
                            JSONObject firstFeature = features.getJSONObject(0);
                            JSONObject geometry = firstFeature.getJSONObject("geometry");
                            JSONArray coordinatesArray = geometry.getJSONArray("coordinates");

                            // Longitude en premier, puis latitude
                            double longitude = coordinatesArray.getDouble(0);
                            double latitude = coordinatesArray.getDouble(1);

                            // Créer un GeoPoint Firebase
                            geoPoint = new GeoPoint(latitude, longitude);
                        } else {
                            Log.e("LocationFetcher", "Aucune correspondance trouvée pour l'adresse : " + address);
                        }

                        // Retourner les coordonnées via le callback
                        // Cette ligne assure que la mise à jour se fait sur le thread principal
                        if (callback != null) {
                            callback.onLocationFetched(geoPoint);
                        }

                    } catch (Exception e) {
                        Log.e("API Error", "Erreur lors du traitement de la réponse : " + e.getMessage());
                        callback.onLocationFetched(null);
                    }
                } else {
                    Log.e("API Error", "Erreur: " + response.code());
                    callback.onLocationFetched(null);
                }
            }
        });
    }

    // Interface pour recevoir les coordonnées
    public interface LocationCallback {
        void onLocationFetched(GeoPoint geoPoint);
    }
}
