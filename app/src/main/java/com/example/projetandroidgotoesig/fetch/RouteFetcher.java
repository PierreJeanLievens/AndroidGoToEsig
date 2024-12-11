package com.example.projetandroidgotoesig.fetch;

import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

            // Si le moyen de transport n'est pas valide, on prend la voiture par défaut
            if (!transportMode.equals("driving-car") && !transportMode.equals("cycling-regular") && !transportMode.equals("foot-walking")) {
                transportMode = "driving-car";
            }

            String apiKey = "5b3ce3597851110001cf6248ac57c84bcec64455b61413718c74b1ac";
            RouteSummary summary = null;

            try {
                String startCoordinates = startPoint.getLongitude() + "," + startPoint.getLatitude();
                String endCoordinates = endPoint.getLongitude() + "," + endPoint.getLatitude();
                String url = "https://api.openrouteservice.org/v2/directions/" + transportMode
                        + "?api_key=" + apiKey
                        + "&start=" + startCoordinates
                        + "&end=" + endCoordinates;

                // Créer un client OkHttp
                OkHttpClient client = new OkHttpClient();

                // Créer la requête
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                // Exécuter la requête et récupérer la réponse
                Response response = client.newCall(request).execute();

                // Vérifier le code de réponse
                if (response.isSuccessful()) {
                    String responseString = response.body().string();

                    // Parse JSON pour obtenir l'objet summary
                    JSONObject jsonResponse = new JSONObject(responseString);
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
                    Log.e("API Error", "Erreur: " + response.code());
                }

                response.close();
            } catch (IOException e) {
                Log.e("API Error", "Exception: " + e.getMessage(), e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
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
