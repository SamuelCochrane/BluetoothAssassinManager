package edu.uw.samueldc.assassin_manager;


import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapFragment extends Fragment {

    private GoogleMap map;

    private static View view;
    private static final String TAG = "***MapFragment***";


    private static String myRoom;
    private static String myName;
    private static String myLatitude;
    private static String myLongitude;
    private ArrayList<String> roomUsers = new ArrayList<String>();
//    private Map<String, ArrayList<String>> userData = new HashMap<>();
    LatLng point = new LatLng(47.0,-122.0);

    Firebase fireBaseRef;

    // get user room, user name, and user origin location from MainActivity bundle
    public static MapFragment newInstance(String room, String latitude, String longitude, String name) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString("room", room);
        args.putString("latitude",latitude);
        args.putString("longitude",longitude);
        args.putString("name",name);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            myRoom = getArguments().getString("room");
            myLatitude = getArguments().getString("latitude");
            myLongitude = getArguments().getString("longitude");
            myName = getArguments().getString("name");

        }
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // inflate Google map view
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e) {

        }

        MapsInitializer.initialize(getContext());

        map = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map))
                .getMap();

        // query to database to get all users in the room
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms");

        fireBaseRef.child(myRoom + "/users").addValueEventListener(new ValueEventListener() {

        // when data changes in the database
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    roomUsers.add(child.getKey());
                }

//                Log.v(TAG, "List: " + roomUsers.toString());

                // for every user in that room

                for (final String userID : roomUsers) {
                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
//                    Log.v(TAG, "UserID data: " + fireBaseRef.getKey());

                 // add markers in the map
                    final Marker marker;
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(point);
                    marker = map.addMarker(markerOptions);

//                    final ArrayList<String> data = new ArrayList<String>();

                    fireBaseRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.child("longitude").getValue() != null) {

                                // get updated location data
                                Double latitude = Double.parseDouble(dataSnapshot.child("latitude").getValue().toString());
                                Double longitude = Double.parseDouble(dataSnapshot.child("longitude").getValue().toString());
                                LatLng location = new LatLng(latitude, longitude);

//                              Log.v(TAG, "Latitude and Longitude: " + latitude + ", " + longitude);


                                // move the marker
                                if (getActivity() != null) {
                                    marker.setPosition(location);
                                    if (dataSnapshot.child("name").getValue() != null) {
                                        marker.setTitle(dataSnapshot.child("name").getValue().toString());

                                        if (dataSnapshot.child("name").getValue().toString().equals(myName)) {
                                            marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_me)));

                                        } else if (dataSnapshot.child("status").getValue().toString().equalsIgnoreCase(EnterActivity.STATUS_ALIVE)) {
                                            //    if (marker != null) marker.remove();
                                            marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_default)));

                                        } else {
                                            //   if (marker != null) marker.remove();
                                            marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_dead)));
                                        }
                                    }
                            }


                        }

                    }

                    @Override
                    public void onCancelled (FirebaseError firebaseError){

                    }
                }

                );

            }

        }

        @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        // set the map to original location of the user by default

        LatLng myLocation = new LatLng(Double.valueOf(myLatitude),Double.valueOf(myLongitude));

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 100));

        // Zoom in, animating the camera.
        map.animateCamera(CameraUpdateFactory.zoomTo(20), 2000, null);

        return view;
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