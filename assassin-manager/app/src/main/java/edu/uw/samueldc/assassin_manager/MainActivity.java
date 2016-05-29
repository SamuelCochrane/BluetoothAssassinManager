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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    private static final String TAG = "MainActivity";

    PageAdapter pageAdapter;

    ViewPager viewPager;

    Firebase fireBaseRef;

    private static String room;
    private static String userID;
    private static HashMap<String, String> userData;

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
//        themeID = savedInstanceState.getInt("theme");
//        Log.d(TAG, "========== THEME ID: " + themeID);
        if(savedInstanceState != null && savedInstanceState.getInt("theme", -1) != -1) {

            themeID = savedInstanceState.getInt("theme");
            Log.d(TAG, "========== THEME ID: " + themeID);
            this.setTheme(themeID);
        }

//        this.setTheme(R.style.AppTheme_Night);

        super.onCreate(savedInstanceState);




        Firebase.setAndroidContext(this);
        setContentView(R.layout.activity_main);

        // store intent if needs to recreate this activity
        starterIntent = getIntent();


    //    nightMode(MainActivity.this);

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
        if (bundle != null && userData == null) {
            userData = (HashMap) bundle.getSerializable("userData");
            userID = bundle.getString("userID");
            room = bundle.getString("room");
            Log.d(TAG, "========== USER ID: " + userID);
        }


        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
//        if (curLocation != null) {
//            fireBaseRef.child("latitude").setValue(curLocation.getLatitude());
//            fireBaseRef.child("longitude").setValue(curLocation.getLongitude());
//        }

        fireBaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("status").getValue().toString().equalsIgnoreCase("alive")) {
                    Log.d(TAG, "========= USER ALIVE!!");
                } else {
                    // if dead, switch to end activity screen and close background beacon service
                    stopService(new Intent(MainActivity.this, BeaconApplication.class));
                    finish();
                    startActivity(new Intent(MainActivity.this, EndActivity.class));
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }
        });


        // ============= beacon stuff
        // and register for broadcast receiver from beacon service
//        if (!isRegistered) {
//            IntentFilter filter = new IntentFilter();
//            filter.addAction(BeaconApplication.BROADCAST_BEACON);
//            filter.addAction(BeaconApplication.RANGING_DONE);
//            registerReceiver(receiver, filter);
//            isRegistered = true;
//        }

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


        // Watch for button clicks.
        Button button = (Button) findViewById(R.id.goto_first);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewPager.setCurrentItem(0);
            }
        });
        button = (Button) findViewById(R.id.goto_last);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewPager.setCurrentItem(NUM_SCREEN - 1);
            }
        });


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
        Log.v(TAG,"Origin Location is: "+bestLocation.toString());
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
        Log.v(TAG, "Current Location: " + curLocation.getLatitude() + ", " + curLocation.getLongitude());
    }


    // when received beacon list
    @Override
    public void onBeaconReceived(Context context, Intent intent) {
        String str = intent.getAction();
        // if receive beacons, try to get extras
        if(str.equals(BeaconApplication.BROADCAST_BEACON)) {
//                Beacon beacon = intent.getParcelableExtra(BeaconApplication.BROADCAST_BEACON);
            // a map of hunter and target
            Bundle bundle = this.getIntent().getExtras();
            beacons = (HashMap<String, Beacon>) bundle.getSerializable("beaconMap");
            Log.d(TAG, "" + beacons.size());
            if (beacons != null) {
                if (beacons.get("target") != null) {
                    Log.d(TAG, "============ YOUR TARGET: " + beacons.get("target").toString());
                }
                if (beacons.get("hunter") != null) {
                    Log.d(TAG, "============ YOUR HUNTER: " + beacons.get("hunter").toString());
                }
            }

            // pass newly received beacon list to each fragment by calling their specified method
            for(int i = 0; i < pageAdapter.getCount(); i++) {
                Fragment viewPagerFragment = pageAdapter.getItem(i);
                if(viewPagerFragment != null) {
                    // TODO: add update beacon list to specified fragment!
                }
            }
        } else if (str.equals(BeaconApplication.RANGING_DONE)) {
            Log.d(TAG, "ENTER A NEW BEACON REGION!");
        } else {
            Log.d(TAG, "NOOOOO BEACON!!");
        }
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
        myGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        stopService(new Intent(MainActivity.this, BeaconApplication.class));

        super.onStop();
        myGoogleApiClient.disconnect();
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
        super.onDestroy();
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
                case 0:
                    return new LobbyFragment().newInstance(userData.get("name"),userData.get("room"));
                case 1:
                    return new MapFragment().newInstance(userData.get("room"),originLat.toString(),originLog.toString());
                case 2:
//                    return new MeFragment();
                    return new MeFragment().newInstance(userData.get("name"), userData.get("room"), userID);
                case 3:
                    return new TargetFragment();
                default:
                    return null;
            }
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

    public void nightMode(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // determine switch to night mode or not
        boolean nightMode = prefs.getBoolean("pref_night",true);
        if (nightMode) {
            themeID = R.style.AppTheme_Night;
            this.setTheme(themeID);
            this.finish();
            startActivity(starterIntent);
        } else {
            themeID = R.style.AppTheme_Daylight;
            this.setTheme(themeID);
            this.finish();
            startActivity(starterIntent);
        }
    }
}
