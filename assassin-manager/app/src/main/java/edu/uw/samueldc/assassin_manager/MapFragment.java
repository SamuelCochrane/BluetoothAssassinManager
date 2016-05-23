package edu.uw.samueldc.assassin_manager;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class MapFragment extends Fragment implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,com.google.android.gms.location.LocationListener {

    private static final String TAG = "***MapFragment***";
    private final int PERMISSION_CODE = 1;
    private GoogleApiClient myGoogleApiClient;
    private GoogleMap myMap;
    private Location curLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment



    // create api client
    if (myGoogleApiClient == null) {
        myGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

        // let the user locate to the current location
        SupportMapFragment mapFragment = (SupportMapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        myMap = mapFragment.getMap();

        // retain the fragment when activity re-creates
        mapFragment.setRetainInstance(true);

        try {
            myMap.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String bestProvider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(bestProvider);
        } catch (SecurityException e) {
        }

        LatLng me = new LatLng(curLocation.getLatitude(),curLocation.getLongitude());
        LatLng player1 = new LatLng(47.654995, -122.307577);
        LatLng player2 = new LatLng(47.654998, -122.307570);

        Marker marker0  = myMap.addMarker(new MarkerOptions().position(me)
                .title("Me")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_me)));
        Marker marker1 = myMap.addMarker(new MarkerOptions().position(player1)
                .title("Alive")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_alive)));
        Marker marker2 = myMap.addMarker(new MarkerOptions()
                .position(player2)
                .title("Dead")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_dead)));

        return inflater.inflate(R.layout.fragment_target, container, false);
    }

    public void onStart() {
        super.onStart();
        myGoogleApiClient.connect();
    }

    public void onStop() {
        super.onStop();
        myGoogleApiClient.disconnect();
    }


    @Override
    public void onMapReady(GoogleMap map) {
        myMap = map;
        myMap.animateCamera(CameraUpdateFactory.zoomTo(10));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // build GPS request
        LocationRequest request = new LocationRequest();
        request.setInterval(1000);
        request.setFastestInterval(500);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // check permission from the user
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(myGoogleApiClient, request, this);
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    //    LatLng newLocation = new LatLng(location.getLatitude(),location.getLongitude());
        curLocation = location;
        }

    @Override
    public void onConnectionSuspended(int i) {
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permission[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE:
                // if have permission
                if (permission.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onConnected(null);
                }
        }
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
    }

//    @Override
//    public void onPause() {
//        super.onPause();
//
//        getChildFragmentManager().beginTransaction()
//                .remove(this)
//                .commit();
//    }
}