package edu.uw.samueldc.assassin_manager;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
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

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class MapFragment extends Fragment {

    private GoogleMap map;

    

    static final LatLng ME = new LatLng(47.654980, -122.307560);
    static final LatLng PLAYER1 = new LatLng(47.654995, -122.307580);
    static final LatLng PLAYER2 = new LatLng(47.654965, -122.307570);

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_map, null, false);
        
        MapsInitializer.initialize(getContext());

        map = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map))
                .getMap();

        Marker me = map.addMarker(new MarkerOptions()
                .position(ME)
                .title("Me")
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.map_me)));
        Marker player1 = map.addMarker(new MarkerOptions()
                .position(PLAYER1)
                .title("Player 1")
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.map_dead)));
        Marker player2 = map.addMarker(new MarkerOptions()
                .position(PLAYER2)
                .title("Player 2")
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.map_alive)));

        // Move the camera instantly to myself with a zoom of 20.
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(ME, 100));

        // Zoom in, animating the camera.
        map.animateCamera(CameraUpdateFactory.zoomTo(20), 2000, null);

        //...

        return v;
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