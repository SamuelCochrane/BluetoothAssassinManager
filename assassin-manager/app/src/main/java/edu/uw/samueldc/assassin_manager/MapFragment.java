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


    static final LatLng ME = new LatLng(47.654995, -122.307580);
//    static final LatLng PLAYER1 = new LatLng(47.654995, -122.307580);
//    static final LatLng PLAYER2 = new LatLng(47.654965, -122.307570);

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


    public Map<String,ArrayList<String>> getData() {
        // query to database to get all users in the room
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms");

        fireBaseRef.child(myRoom + "/users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    roomUsers.add(child.getKey());
                }

                Log.v(TAG, "List: " + roomUsers.toString());

                for (final String userID : roomUsers) {
                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
                    Log.v(TAG, "UserID data: " + fireBaseRef.getKey());

                    final ArrayList<String> data = new ArrayList<String>();

                    fireBaseRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                data.add(child.getValue().toString());
                            }
                            userData.put(userID, data);

                            Log.v(TAG,"Data List: "+data.toString());

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

        return userData;
    }


    public LatLng getLocation(String userID) {
        Map<String,ArrayList<String>> map = getData();
        Double latitude = Double.parseDouble(map.get(userID).get(3));
        Double longitude = Double.parseDouble(map.get(userID).get(4));
        LatLng location = new LatLng(latitude,longitude);

        Log.v(TAG, "location data: " + latitude + "," + longitude);
        return location;
    }

    public int assignMarker(String userID) {
        int status;
        Map<String,ArrayList<String>> map = getData();
        if (map.get(userID).get(9).equals("alive")) status = 1;
        else status = 0;
        Log.v(TAG,"status: "+status);
        return status;
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



        for (String item : getData().keySet()) {
            if (assignMarker(item) == 1) {
            Marker marker = map.addMarker(new MarkerOptions()
                               .position(getLocation(item))
                               .title(item)
                               .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_alive)));
            }
            else {
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(getLocation(item))
                        .title(item)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_dead)));
            }
//            String firstKey = getData().keySet().toArray()[0].toString();
//            ME = getLocation(firstKey);

        }


        // Move the camera instantly to myself with a zoom of 20.
        Marker player = map.addMarker(new MarkerOptions()
                .position(ME)
                .title("player")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_alive)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(ME, 100));

        // Zoom in, animating the camera.
        map.animateCamera(CameraUpdateFactory.zoomTo(20), 2000, null);

        //...

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