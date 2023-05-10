package com.example.mob_dev_portfolio.Handler;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.example.mob_dev_portfolio.MainActivity;
import com.example.mob_dev_portfolio.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
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
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LocationHandler {
    private final AppCompatActivity activity;
    private final MapView mapView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    public LocationHandler(AppCompatActivity activity, MapView mapView) {
        this.activity = activity;
        this.mapView = mapView;
        setupFusedLocationProviderClient();
    }

    private void setupFusedLocationProviderClient() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    return;
                }
                // Handle location updates here
            }
        };
    }

    // Request location permission
    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MainActivity.PERMISSION_REQUEST_LOCATION);
    }

    public void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permissions
            requestLocationPermission();
            return;
        }

        Marker currentLocationMarker = new Marker(mapView);
        Drawable icon = ContextCompat.getDrawable(activity, R.drawable.ic_location);
        currentLocationMarker.setIcon(icon);
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        currentLocationMarker.setInfoWindow(null);
        currentLocationMarker.setTitle(null);

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(activity, new OnSuccessListener<android.location.Location>() {
                    @Override
                    public void onSuccess(android.location.Location location) {
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
                    }
                })
                .addOnFailureListener(activity, e -> Log.e("MainActivity", "Error getting location", e));
    }

    private void getNearbyParkingSpots(GeoPoint currentLocation) {
        new FetchParkingSpotsAsyncTask().execute(currentLocation);
    }

    private class FetchParkingSpotsAsyncTask extends AsyncTask<GeoPoint, Void, List<Marker>> {

        @Override
        protected List<Marker> doInBackground(GeoPoint... geoPoints) {
            GeoPoint currentLocation = geoPoints[0];
            List<Marker> markers = new ArrayList<>();

            String openStreetMapUrl = String.format("https://nominatim.openstreetmap.org/search?q=parking&format=json&lat=%f&lon=%f&zoom=18", currentLocation.getLatitude(), currentLocation.getLongitude());
            RequestFuture<JSONArray> future = RequestFuture.newFuture();
            JsonArrayRequest openStreetMapRequest = new JsonArrayRequest(Request.Method.GET, openStreetMapUrl, null, future, future);
            RequestQueue requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
            requestQueue.add(openStreetMapRequest);

            try {
                JSONArray jsonArray = future.get(30, TimeUnit.SECONDS);
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

            Places.initialize(activity.getApplicationContext(), "AIzaSyD_ULCp-e54LRG9DgmABYQ_Owr2XzT3k2Y");
            PlacesClient placesClient = Places.createClient(activity.getApplicationContext());
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

        @Override
        protected void onPostExecute(List<Marker> markers) {
            for (Marker marker : markers) {
                mapView.getOverlays().add(marker);
            }
            mapView.invalidate();
        }
    }

    public void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(new LocationRequest(), locationCallback, null);
        }
    }

    public void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    public void searchAddress(String address) {
        Geocoder geocoder = new Geocoder(activity);
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapView.getController().setCenter(geoPoint);
                mapView.getController().setZoom(15.0);
                mapView.invalidate();

                getNearbyParkingSpots(geoPoint);
            } else {
                Toast.makeText(activity, "Address not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error finding address", Toast.LENGTH_SHORT).show();
        }
    }
}

