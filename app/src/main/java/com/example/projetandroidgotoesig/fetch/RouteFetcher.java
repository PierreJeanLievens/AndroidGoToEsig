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

public class RouteFetcher {

    public static void getRouteSummary(GeoPoint startPoint, GeoPoint endPoint, String transportMode, RouteCallback callback) {
        new GetRouteTask(callback).execute(startPoint, endPoint, transportMode);
    }

    private static class GetRouteTask extends AsyncTask<Object, Void, RouteSummary> {

        private RouteCallback callback;

        public GetRouteTask(RouteCallback callback) {
            this.callback = callback;
        }

        @Override
        protected RouteSummary doInBackground(Object... params) {
            GeoPoint startPoint = (GeoPoint) params[0];
            GeoPoint endPoint = (GeoPoint) params[1];
            String transportMode = (String) params[2];

            String apiKey = "5b3ce3597851110001cf6248ac57c84bcec64455b61413718c74b1ac";
            RouteSummary summary = null;

            try {
                String startCoordinates = startPoint.getLongitude() + "," + startPoint.getLatitude();
                String endCoordinates = endPoint.getLongitude() + "," + endPoint.getLatitude();
                String url = "https://api.openrouteservice.org/v2/directions/" + transportMode
                        + "?api_key=" + apiKey
                        + "&start=" + startCoordinates
                        + "&end=" + endCoordinates;

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

                    // Parse JSON pour obtenir l'objet summary
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray features = jsonResponse.getJSONArray("features");
                    if (features.length() > 0) {
                        JSONObject properties = features.getJSONObject(0).getJSONObject("properties");
                        JSONObject summaryObject = properties.getJSONObject("summary");

                        double distance = summaryObject.getDouble("distance");
                        double duration = summaryObject.getDouble("duration");

                        summary = new RouteSummary(distance, duration);
                    } else {
                        Log.e("RouteFetcher", "Aucune route trouvée.");
                    }
                } else {
                    Log.e("API Error", "Erreur: " + connection.getResponseCode());
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e("API Error", "Exception: " + e.getMessage(), e);
            }

            return summary;
        }

        @Override
        protected void onPostExecute(RouteSummary summary) {
            if (callback != null) {
                callback.onRouteFetched(summary);
            }
        }
    }

    public interface RouteCallback {
        void onRouteFetched(RouteSummary summary);
    }

    public static class RouteSummary {
        public final double distance; // En mètres
        public final double duration; // En secondes

        public RouteSummary(double distance, double duration) {
            this.distance = distance;
            this.duration = duration;
        }
    }
}
