package edu.uw.samueldc.assassin_manager;


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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapFragment extends Fragment {

    private GoogleMap map;

    private static View view;
    private static final String TAG = "***MapFragment***";


    private static String myRoom;
    private ArrayList<String> roomUsers = new ArrayList<String>();
    private Map<String, ArrayList<String>> userData = new HashMap<>();

    Firebase fireBaseRef;

    public static MapFragment newInstance(String room) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString("room", room);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            myRoom = getArguments().getString("room");
            Log.v(TAG, myRoom);
        }
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


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
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    roomUsers.add(child.getKey());
                }

//                Log.v(TAG, "List: " + roomUsers.toString());

                for (final String userID : roomUsers) {
                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
//                    Log.v(TAG, "UserID data: " + fireBaseRef.getKey());

                    final ArrayList<String> data = new ArrayList<String>();

                    fireBaseRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                data.add(child.getValue().toString());
                            }

                            Log.v(TAG,"The data list: "+data.toString());

                            Double latitude = Double.parseDouble(data.get(3));
                            Double longitude = Double.parseDouble(data.get(4));
                            LatLng location = new LatLng(latitude, longitude);

                            Log.v(TAG,"Latitude and Longitude: "+latitude+", "+longitude);

                            if (data.get(9).equals("alive")) {
                                Marker marker = map.addMarker(new MarkerOptions()
                                        .position(location)
                                        .title(data.get(5))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_alive)));
                            } else {
                                Marker marker = map.addMarker(new MarkerOptions()
                                        .position(location)
                                        .title(data.get(5))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_dead)));
                            }

                            Log.v(TAG, "Status: " + data.get(9));

                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 100));

                            // Zoom in, animating the camera.
                            map.animateCamera(CameraUpdateFactory.zoomTo(20), 2000, null);

                            }



                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });

                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


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