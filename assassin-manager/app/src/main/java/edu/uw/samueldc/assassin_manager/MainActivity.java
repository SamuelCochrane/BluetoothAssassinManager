package edu.uw.samueldc.assassin_manager;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ServiceConnection, BeaconReceiver.OnBeaconReceivedListener,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    static final int NUM_SCREEN = 4;
    static final int LOBBY_POSITION = 0;
    static final int MAP_POSITION = 1;
    static final int ME_POSITION = 2;
    static final int TARGET_POSITION = 3;

    private static final String TAG = "MainActivity";

    PageAdapter pageAdapter;

    ViewPager viewPager;

    Firebase fireBaseRef;

    private static String room;
    private static String userID;
    private static HashMap<String, String> userData;

    private static String targetID = null;

    private boolean bound;
    private HashMap<String, Beacon> beacons;

    private BeaconReceiver receiver = null;
    private boolean isRegistered = false;


    private Location curLocation;
    Location originLocation;
    private static Double originLat;
    private static Double originLog;
    LocationManager locationManager;
    private final int PERMISSION_CODE = 1;

    private int themeID = -1;
    private boolean isSettingsClick = false;

    private Intent starterIntent;

    private GoogleApiClient myGoogleApiClient;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt("theme", themeID );

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ======= theme stuff
        if(savedInstanceState != null && savedInstanceState.getInt("theme", -1) != -1) {

            themeID = savedInstanceState.getInt("theme");
            Log.d(TAG, "========== THEME ID: " + themeID);
            this.setTheme(themeID);
        }

        super.onCreate(savedInstanceState);

        Firebase.setAndroidContext(this);
        setContentView(R.layout.activity_main);

        // store intent if needs to recreate this activity
        starterIntent = getIntent();

        // ============= deal with toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Assassin");
        setSupportActionBar(toolbar);

        // add tab layout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Lobby"));
        tabLayout.addTab(tabLayout.newTab().setText("Map"));
        tabLayout.addTab(tabLayout.newTab().setText("Me"));
        tabLayout.addTab(tabLayout.newTab().setText("Target"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        pageAdapter = new PageAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pageAdapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // ============ db stuff
        // Passed bundle info to use for the database
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            userData = (HashMap) bundle.getSerializable("userData");
            userID = bundle.getString("userID");
            room = bundle.getString("room");
            Log.d(TAG, "========== USER INFO: " + userData.toString());
        }

        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);

        fireBaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("status").getValue() != null) {
                    if (dataSnapshot.child("status").getValue().toString().equalsIgnoreCase(EnterActivity.STATUS_ALIVE)) {

                    } else if (dataSnapshot.child("status").getValue().toString().equalsIgnoreCase(EnterActivity.STATUS_DEAD)){
                        // if dead, switch to end activity screen and close background beacon service
                        stopService(new Intent(MainActivity.this, BeaconApplication.class));
                        finish();
                        startActivity(new Intent(MainActivity.this, EndActivity.class));
                    }

                    if (dataSnapshot.child("target") != null) {
                        targetID = dataSnapshot.child("target").getValue().toString();
                    }
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }
        });


        // ============= beacon stuff
        // and register for broadcast receiver from beacon service
        receiver = new BeaconReceiver();
        receiver.setOnBeaconReceivedListener(this);
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BeaconApplication.BROADCAST_BEACON);
//        filter.addAction(BeaconApplication.RANGING_DONE);
//        registerReceiver(receiver, filter);
//        isRegistered = true;

        // start the beacon service in the backgorund thread
        // and pass userName and roonName to Beacon
        Intent intent = new Intent(MainActivity.this, BeaconApplication.class);
        Bundle userInfo = new Bundle();
        userInfo.putSerializable("userData", userData);
        intent.putExtras(userInfo);
        startService(intent);


        if (myGoogleApiClient == null) {
            myGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        }
        originLocation = getLastKnownLocation();
        originLat = originLocation.getLatitude();
        originLog = originLocation.getLongitude();


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BeaconApplication.BROADCAST_BEACON);
            filter.addAction(BeaconApplication.RANGING_DONE);
            registerReceiver(receiver, filter);
            isRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

//        if (isRegistered) {
//            unregisterReceiver(receiver);
//            isRegistered = false;
//        }
    }

    @Override
    protected void onStart() {
//        startService(new Intent(MainActivity.this, BeaconApplication.class));

        super.onStart();
        if (!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BeaconApplication.BROADCAST_BEACON);
            filter.addAction(BeaconApplication.RANGING_DONE);
            registerReceiver(receiver, filter);
            isRegistered = true;
        }
        myGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        stopService(new Intent(MainActivity.this, BeaconApplication.class));

        super.onStop();
        if (isRegistered) {
            unregisterReceiver(receiver);
            isRegistered = false;
        }
        myGoogleApiClient.disconnect();
    }

    // create a new enter activity and clear all back stack!
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        Intent intent = new Intent(MainActivity.this, EnterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "Activity destroyed");
        try{
            if(receiver != null)
                unregisterReceiver(receiver);
        }catch(Exception e)
        {

        }

        // stop beacon service as well only if it is not switch mode behavior
        if (!isSettingsClick) {
            Intent svc=new Intent(MainActivity.this, BeaconApplication.class);
            stopService(svc);

            // check if itself is the last user in this room, delete this room if true
            Firebase roomCntRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room + "/users");

            roomCntRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getChildrenCount() <= 1) {
                        // delete the room
                        Firebase roomDeleteRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room);
                        roomDeleteRef.removeValue();
                    } else {
                        // if the user is not the only one, delete this user in that room
                        Firebase roomRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room + "/users" + userID);

                        roomRef.removeValue();
                    }

                    // also, remove this user in firebase
                    Firebase userRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);

                    userRef.removeValue();
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.e(TAG, "Error when accessing DB: " + firebaseError);
                }
            });
        }

        super.onDestroy();
    }

    // when received beacon list
    @Override
    public void onBeaconReceived(Context context, Intent intent) {
        String str = intent.getAction();
        Log.d(TAG, "========= YOU RECEIVE SOMETHING: " + str);
        // if receive beacons, try to get extras
        if(str.equals(BeaconApplication.BROADCAST_BEACON)) {
//                Beacon beacon = intent.getParcelableExtra(BeaconApplication.BROADCAST_BEACON);
            // a map of hunter and target
            Bundle bundle = intent.getExtras();
//            String strings = intent.getExtras().getString("beaconStr");
            Log.d(TAG, "+++++++++ YOU GET BUNDLE: " + bundle);
//            beacons = (HashMap<String, Beacon>) bundle.getSerializable("beaconMap");

            Beacon target = bundle.getParcelable("target");

            TargetFragment targetFrag = (TargetFragment) pageAdapter.getRegisteredFragment(TARGET_POSITION);
            if (targetFrag != null) {
                Log.d(TAG, "============ YOU ARE SETTING FRAGMENT");
                targetFrag.updateTarget(target); // pass null if target not found
            }


        } else if (str.equals(BeaconApplication.RANGING_DONE)) {
            Log.d(TAG, "ENTER A NEW BEACON REGION!");
        } else {
            Log.d(TAG, "NOOOOO BEACON!!");
        }
    }


    private Location getLastKnownLocation() {
        locationManager = (LocationManager)MainActivity.this.getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            try {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            } catch (SecurityException e) {
            }
        }
        return bestLocation;
    }


    @Override
    public void onLocationChanged(Location location) {
        curLocation = location;

        if (curLocation != null) {
            fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
            fireBaseRef.child("latitude").setValue(curLocation.getLatitude());
            fireBaseRef.child("longitude").setValue(curLocation.getLongitude());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // build GPS request
        LocationRequest request = new LocationRequest();
        request.setInterval(1000);
        request.setFastestInterval(500);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // check permission from the user
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(myGoogleApiClient, request, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
        }

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
        super.onRequestPermissionsResult(requestCode,permission,grantResults);
    }



    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.miSettings) {
            isSettingsClick = true;
            Log.v(TAG, "Start settings");
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
            return true;

        } else if (id == R.id.toggleTheme) {
            // change to night mode
            if (themeID == R.style.AppTheme_Night) {
                themeID = R.style.AppTheme;
            } else {
                themeID = R.style.AppTheme_Night;
            }
            isSettingsClick = true;
            this.recreate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }


    public static class PageAdapter extends FragmentPagerAdapter {
        SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

        public PageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public int getCount() {
            return NUM_SCREEN;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case LOBBY_POSITION:
                    return new LobbyFragment().newInstance(userData.get("name"),userData.get("room"));
                case MAP_POSITION:
                    return new MapFragment().newInstance(userData.get("room"),originLat.toString(),originLog.toString(),userData.get("name"));
                case ME_POSITION:
//                    return new MeFragment();
                    return new MeFragment().newInstance(userData.get("name"), userData.get("room"), userID);
                case TARGET_POSITION:
                    return new TargetFragment().newInstance(userData.get("name"), userData.get("room"), userID, targetID);
                default:
                    return null;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }
}
