package com.example.projetandroidgotoesig;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity {

    Button btnProfile, btnAddTrip, btnMyTrips, btnSearchTrip, btnStatistics, btnQuit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //String idUser = getIntent().getStringExtra("idUser");

        // Initialiser les boutons
        btnProfile = findViewById(R.id.btnProfile);
        btnAddTrip = findViewById(R.id.btnAddTrip);
        btnMyTrips = findViewById(R.id.btnMyTrips);
        btnSearchTrip = findViewById(R.id.btnSearchTrip);
        btnStatistics = findViewById(R.id.btnStatistics);
        btnQuit = findViewById(R.id.btnQuit);

        // Listener pour chaque bouton
        btnProfile.setOnClickListener(v -> {
            Intent profileIntent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
        });

        btnAddTrip.setOnClickListener(v -> {
            Intent addTripIntent = new Intent(HomeActivity.this, AddTripActivity.class);
            startActivity(addTripIntent);
        });

        btnMyTrips.setOnClickListener(v -> {
            Intent myTripsIntent = new Intent(HomeActivity.this, MyTravelsActivity.class);
            startActivity(myTripsIntent);
        });

        btnSearchTrip.setOnClickListener(v -> {
            Intent searchTripIntent = new Intent(HomeActivity.this, SearchTravelActivity.class);
            startActivity(searchTripIntent);
        });

        btnStatistics.setOnClickListener(v -> {
            Intent statisticsIntent = new Intent(HomeActivity.this, StatisticsActivity.class);
            startActivity(statisticsIntent);
        });

        btnQuit.setOnClickListener(v -> {
            // Supprime le idUser de sharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("idUser");
            editor.apply();

            Toast.makeText(HomeActivity.this, "Au revoir !", Toast.LENGTH_SHORT).show();
            finishAffinity();
        });

    }
}