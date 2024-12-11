package com.example.projetandroidgotoesig.fetch;

import android.os.AsyncTask;
import android.util.Log;
import com.google.firebase.firestore.GeoPoint;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LocationFetcher {

    public static void getGeoPointFromAddress(String address, LocationCallback callback) {
        new GetLocationTask(callback).execute(address);
    }

    private static class GetLocationTask extends AsyncTask<String, Void, GeoPoint> {

        private LocationCallback callback;

        public GetLocationTask(LocationCallback callback) {
            this.callback = callback;
        }

        @Override
        protected GeoPoint doInBackground(String... addresses) {
            String address = addresses[0];
            String apiKey = "5b3ce3597851110001cf6248ac57c84bcec64455b61413718c74b1ac";
            GeoPoint geoPoint = null;

            try {
                // Encoder l'adresse pour l'URL
                String encodedAddress = URLEncoder.encode(address, "UTF-8");
                String url = "https://api.openrouteservice.org/geocode/search?api_key=" + apiKey + "&text=" + encodedAddress;

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

                    // Parse JSON pour extraire les coordonnées
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray features = jsonResponse.getJSONArray("features");
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
                } else {
                    Log.e("API Error", "Erreur: " + connection.getResponseCode());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("API Error", "Exception: " + e.getMessage(), e);
            }

            return geoPoint;
        }

        @Override
        protected void onPostExecute(GeoPoint geoPoint) {
            if (callback != null) {
                callback.onLocationFetched(geoPoint);
            }
        }
    }

    public interface LocationCallback {
        void onLocationFetched(GeoPoint geoPoint);
    }
}
