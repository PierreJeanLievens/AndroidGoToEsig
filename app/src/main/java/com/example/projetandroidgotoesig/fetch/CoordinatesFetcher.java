package com.example.projetandroidgotoesig.fetch;

import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class CoordinatesFetcher {

    /**
     * Récupère l'adresse à partir des coordonnées
     * @param latitude
     * @param longitude
     * @param callback
     */
    public static void getAddressFromGeoPoint(double latitude, double longitude, LocationCallback callback) {
        // Construire l'URL pour l'API de géocodage inverse
        String apiKey = "5b3ce3597851110001cf6248ac57c84bcec64455b61413718c74b1ac";
        String url = "https://api.openrouteservice.org/geocode/reverse?api_key=" + apiKey +
                "&point.lon=" + longitude + "&point.lat=" + latitude + "&boundary.country=FR";

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

                        String address = null;

                        if (features.length() > 0) {
                            // Récupérer la première adresse dans "features"
                            JSONObject firstFeature = features.getJSONObject(0);
                            JSONObject properties = firstFeature.getJSONObject("properties");
                            address = properties.getString("label"); // Adresse complète
                        } else {
                            Log.e("LocationFetcher", "Aucune correspondance trouvée pour les coordonnées : " + latitude + ", " + longitude);
                        }

                        // Retourner l'adresse récupérée via le callback
                        callback.onLocationFetched(address);
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

    // Interface pour obtenir l'adresse
    public interface LocationCallback {
        void onLocationFetched(String address);
    }
}
