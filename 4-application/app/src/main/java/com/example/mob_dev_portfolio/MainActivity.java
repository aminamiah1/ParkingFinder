package com.example.mob_dev_portfolio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.example.mob_dev_portfolio.databinding.ActivityMainBinding;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MapEventsOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_LOCATION = 1;

    private ActivityMainBinding binding;
    private MapView mapView;

    // FusedLocationProviderClient and LocationCallback for location updates
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the Mapbox configuration
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setTileFileSystemCacheMaxBytes((long) (100 * 1024 * 1024)); // 100 MB cache size

        // Inflate the layout and set the content view
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Button centerButton = findViewById(R.id.center_button);
        centerButton.setOnClickListener(view -> getLastLocation());

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the map view
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Set up the location provider client and location callback
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    return;
                }
                // Handle location updates here
            }
        };

        // Check if we have location permission and request it if necessary
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        } else {
            getLastLocation();
        }

        // Set up the map events overlay
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

    // Request location permission
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                getLastLocation();
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                // Disable functionality that requires location permission
            }
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permissions
            requestLocationPermission();
            return;
        }

        // Create a blue dot marker for the current location
        Marker currentLocationMarker = new Marker(mapView);
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_location);
        currentLocationMarker.setIcon(icon);
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        currentLocationMarker.setInfoWindow(null);
        currentLocationMarker.setTitle(null);

        // Get the last known location and update the map
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        GeoPoint currentLocation = new GeoPoint(lat, lng);
                        mapView.getController().setCenter(currentLocation);
                        // Set the marker position to the current location
                        currentLocationMarker.setPosition(currentLocation);

                        // Add the marker to the map overlay and redraw the map
                        mapView.getOverlays().add(currentLocationMarker);
                        mapView.invalidate();

                        // Get nearby parking spots
                        getNearbyParkingSpots(currentLocation);
                    }
                })
                .addOnFailureListener(this, e -> Log.e("MainActivity", "Error getting location", e));
    }

    // Fetch nearby parking spots
    private void getNearbyParkingSpots(GeoPoint currentLocation) {
        new FetchParkingSpotsAsyncTask().execute(currentLocation);
    }

    // AsyncTask to fetch parking spots from OpenStreetMap and Google Places API
    private class FetchParkingSpotsAsyncTask extends AsyncTask<GeoPoint, Void, List<Marker>> {

        // Perform the fetch operation in the background
        @Override
        protected List<Marker> doInBackground(GeoPoint... geoPoints) {
            GeoPoint currentLocation = geoPoints[0];
            List<Marker> markers = new ArrayList<>();

            // Fetch parking spots from OpenStreetMap
            String openStreetMapUrl = String.format("https://nominatim.openstreetmap.org/search?q=parking&format=json&lat=%f&lon=%f&zoom=18", currentLocation.getLatitude(), currentLocation.getLongitude());
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            JsonObjectRequest openStreetMapRequest = new JsonObjectRequest(Request.Method.GET, openStreetMapUrl, null, future, future);
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(openStreetMapRequest);

            try {
                JSONObject response = future.get(30, TimeUnit.SECONDS);
                JSONArray jsonArray = response.getJSONArray("OpenStreetMap");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    double lat = jsonObject.getDouble("lat");
                    double lon = jsonObject.getDouble("lon");
                    Marker marker = new Marker(mapView);
                    marker.setPosition(new GeoPoint(lat, lon));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    markers.add(marker);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
                Log.e("MainActivity", "Error getting OpenStreetMap data", e);
            }

            // Fetch parking spots from Google Places API
            Places.initialize(getApplicationContext(), "AIzaSyD_ULCp-e54LRG9DgmABYQ_Owr2XzT3k2Y");
            PlacesClient placesClient = Places.createClient(getApplicationContext());
            LatLngBounds bounds = new LatLngBounds(
                    new LatLng(currentLocation.getLatitude() - 0.1, currentLocation.getLongitude() - 0.1), new LatLng(currentLocation.getLatitude() + 0.1, currentLocation.getLongitude() + 0.1));
            FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                    .setLocationBias(RectangularBounds.newInstance(bounds))
                    .setTypeFilter(TypeFilter.ESTABLISHMENT)
                    .setSessionToken(AutocompleteSessionToken.newInstance())
                    .setQuery("parking")
                    .build();

            try {
                FindAutocompletePredictionsResponse response = Tasks.await(placesClient.findAutocompletePredictions(request));
                List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                for (AutocompletePrediction prediction : predictions) {
                    String placeId = prediction.getPlaceId();
                    List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG);
                    FetchPlaceRequest request1 = FetchPlaceRequest.builder(placeId, fields).build();
                    FetchPlaceResponse response1 = Tasks.await(placesClient.fetchPlace(request1));
                    Place place = response1.getPlace();
                    Marker marker = new Marker(mapView);
                    marker.setPosition(new GeoPoint(place.getLatLng().latitude, place.getLatLng().longitude));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    markers.add(marker);
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e("MainActivity", "Error getting Places data", e);
            }

            return markers;
        }

        // Update the map with the fetched parking spots
        @Override
        protected void onPostExecute(List<Marker> markers) {
            for (Marker marker : markers) {
                mapView.getOverlays().add(marker);
            }
            mapView.invalidate();
        }
    }

    // Handle activity lifecycle events
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        stopLocationUpdates();
    }

    // Start location updates
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(new LocationRequest(), locationCallback, null);
        }
    }

    // Stop location updates
    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
}
