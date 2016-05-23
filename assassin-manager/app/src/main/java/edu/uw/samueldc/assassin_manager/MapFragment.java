package edu.uw.samueldc.assassin_manager;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment {

    private GoogleMap map;

    

    static final LatLng ME = new LatLng(47.654980, -122.307560);
    static final LatLng PLAYER1 = new LatLng(47.654995, -122.307580);
    static final LatLng PLAYER2 = new LatLng(47.654965, -122.307570);

    private static String myRoom;

    public static LobbyFragment newInstance(String room) {
        LobbyFragment fragment = new LobbyFragment();
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
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_map, null, false);

        map = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map))
                .getMap();

        Marker me = map.addMarker(new MarkerOptions()
                .position(ME)
                .title("Me"));
//                .icon(BitmapDescriptorFactory;
//                        .fromResource(R.drawable.map_me)));
        Marker player1 = map.addMarker(new MarkerOptions()
                .position(PLAYER1)
                .title("Player 1"));
//                .icon(BitmapDescriptorFactory;
//                        .fromResource(R.drawable.map_dead)));
        Marker player2 = map.addMarker(new MarkerOptions()
                .position(PLAYER2)
                .title("Player 2"));
//                .icon(BitmapDescriptorFactory;
//                        .fromResource(R.drawable.map_alive)));

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