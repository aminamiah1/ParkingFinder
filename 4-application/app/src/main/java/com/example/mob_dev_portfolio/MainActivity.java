package com.example.mob_dev_portfolio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mob_dev_portfolio.Handler.LocationHandler;
import com.example.mob_dev_portfolio.databinding.ActivityMainBinding;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_LOCATION = 1;
    public static final int REQUEST_CODE_ADD_FAVOURITE_ACTIVITY = 1;

    private ActivityMainBinding binding;
    private MapView mapView;
    private LocationHandler locationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapView = findViewById(R.id.map);
        locationHandler = new LocationHandler(this, mapView);

        ImageButton centerButton = binding.toolbar.findViewById(R.id.center_button);
        centerButton.setOnClickListener(view -> locationHandler.getLastLocation());

        ImageButton addButton = binding.toolbar.findViewById(R.id.add_button);
        addButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddFavouriteActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_FAVOURITE_ACTIVITY);
        });

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setTileFileSystemCacheMaxBytes((long) (100 * 1024 * 1024)); // 100 MB cache size

        setSupportActionBar(binding.toolbar);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        } else {
            locationHandler.getLastLocation();
        }

        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                locationHandler.searchAddress(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }

            public boolean overlayTap(Marker marker) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("google.navigation:q=" + marker.getPosition().getLatitude() + "," + marker.getPosition().getLongitude()));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
                return true;
            }
        });
        mapView.getOverlays().add(mapEventsOverlay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        locationHandler.startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        locationHandler.stopLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationHandler.getLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_FAVOURITE_ACTIVITY && resultCode == RESULT_OK) {
            String selectedAddress = data.getStringExtra("selectedAddress");
            if (selectedAddress != null) {
                // Navigate to the selected address on the map
                locationHandler.searchAddress(selectedAddress);
            }
        }
    }
}
