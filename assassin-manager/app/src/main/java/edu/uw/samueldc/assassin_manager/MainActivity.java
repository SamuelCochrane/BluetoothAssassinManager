package edu.uw.samueldc.assassin_manager;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements ServiceConnection, BeaconReceiver.OnBeaconReceivedListener {
    static final int NUM_SCREEN = 4;

    private static final String TAG = "MainActivity";

    PageAdapter pageAdapter;

    ViewPager viewPager;

    Firebase fireBaseRef;

    private String playerName;
    private static String roomName;

    private boolean bound;
    private Collection<Beacon> beacons;

    private BeaconReceiver receiver = null;
    private boolean isRegistered = false;
    private static HashMap<String, String> userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.activity_main);

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
        }


        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com");

        fireBaseRef.child("users/").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.v(TAG, "users: " + dataSnapshot.getValue());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }
        });


        // ============= beacon stuff
        // and register for broadcast receiver from beacon service
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BeaconApplication.BROADCAST_BEACON);
//        filter.addAction(BeaconApplication.RANGING_DONE);
//        registerReceiver(receiver, filter);


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
        Button button = (Button)findViewById(R.id.goto_first);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewPager.setCurrentItem(0);
            }
        });
        button = (Button)findViewById(R.id.goto_last);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewPager.setCurrentItem(NUM_SCREEN-1);
            }
        });
    }

    // when received beacon list
    @Override
    public void onBeaconReceived(Context context, Intent intent) {
        String str = intent.getAction();
        // if receive beacons, try to get extras
        if(str.equals(BeaconApplication.BROADCAST_BEACON)) {
//                Beacon beacon = intent.getParcelableExtra(BeaconApplication.BROADCAST_BEACON);
            // a list of beacons
            ArrayList<Beacon> beacons = intent.getParcelableArrayListExtra("beacons");
            Log.d(TAG, beacons.get(0).toString());
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
    }

    @Override
    protected void onStop() {
        stopService(new Intent(MainActivity.this, BeaconApplication.class));

        super.onStop();
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
                    return (new MapFragment()).newInstance(userData.get("room"));
                case 2:
                    return new MeFragment();
//                    return new MeFragment().newInstance(userData.get("name"),userData.get("room"));
                case 3:
                    return new TargetFragment();
                default:
                    return null;
            }
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}
