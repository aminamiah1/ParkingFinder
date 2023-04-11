package com.example.mob_dev_portfolio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_LOCATION = 1;

    private ActivityMainBinding binding;
    private MapView mapView;
    private MapController mapController;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setTileFileSystemCacheMaxBytes((long) (100 * 1024 * 1024)); // 100 MB cache size
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mapController = (MapController) mapView.getController();
        mapController.setZoom(15.0);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        } else {
            getLastLocation();
        }

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

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        GeoPoint currentLocation = new GeoPoint(lat, lng);
                        mapController.setCenter(currentLocation);
                        getNearbyParkingSpots(currentLocation);
                    }
                })
                .addOnFailureListener(this, e -> Log.e("MainActivity", "Error getting location", e));
    }

    private void getNearbyParkingSpots(GeoPoint currentLocation) {
        new FetchParkingSpotsAsyncTask().execute(currentLocation);
    }

    private class FetchParkingSpotsAsyncTask extends AsyncTask<GeoPoint, Void, List<Marker>> {

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
                    new LatLng(currentLocation.getLatitude() - 0.1, currentLocation.getLongitude() - 0.1),
                    new LatLng(currentLocation.getLatitude() + 0.1, currentLocation.getLongitude() + 0.1));
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

        @Override
        protected void onPostExecute(List<Marker> markers) {
            for (Marker marker : markers) {
                mapView.getOverlays().add(marker);
            }
            mapView.invalidate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}