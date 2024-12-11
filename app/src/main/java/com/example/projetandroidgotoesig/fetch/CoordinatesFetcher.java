package com.example.projetandroidgotoesig.fetch;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CoordinatesFetcher {

    // Fonction pour récupérer l'adresse à partir des coordonnées
    public static void getAddressFromGeoPoint(double latitude, double longitude, LocationCallback callback) {
        new GetAddressTask(callback).execute(latitude, longitude);
    }

    private static class GetAddressTask extends AsyncTask<Double, Void, String> {

        private LocationCallback callback;

        public GetAddressTask(LocationCallback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Double... coordinates) {
            double latitude = coordinates[0];
            double longitude = coordinates[1];
            String apiKey = "5b3ce3597851110001cf6248ac57c84bcec64455b61413718c74b1ac";
            String address = null;

            try {
                // Construire l'URL pour l'API de géocodage inverse
                String url = "https://api.openrouteservice.org/geocode/reverse?api_key=" + apiKey +
                        "&point.lon=" + longitude + "&point.lat=" + latitude;

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();

                    String response = responseBuilder.toString();

                    // Analyser la réponse JSON pour extraire l'adresse
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray features = jsonResponse.getJSONArray("features");

                    if (features.length() > 0) {
                        // Récupérer la première adresse dans "features"
                        JSONObject firstFeature = features.getJSONObject(0);
                        JSONObject properties = firstFeature.getJSONObject("properties");
                        address = properties.getString("label"); // Adresse complète
                    } else {
                        Log.e("LocationFetcher", "Aucune correspondance trouvée pour les coordonnées : " + latitude + ", " + longitude);
                    }
                } else {
                    Log.e("API Error", "Erreur: " + connection.getResponseCode());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("API Error", "Exception: " + e.getMessage(), e);
            }

            return address;
        }

        @Override
        protected void onPostExecute(String address) {
            if (callback != null) {
                callback.onLocationFetched(address);
            }
        }
    }

    public interface LocationCallback {
        void onLocationFetched(String address);
    }
}
