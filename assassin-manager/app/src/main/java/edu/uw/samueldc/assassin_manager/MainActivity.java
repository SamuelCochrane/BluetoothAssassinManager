package edu.uw.samueldc.assassin_manager;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.design.widget.TabLayout;

import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    static final int NUM_SCREEN = 4;

    private static final String TAG = "MainActivity";

    PageAdapter pageAdapter;

    ViewPager viewPager;

    Firebase fireBaseRef;

    private String playerName;
    private String roomName;

    private boolean bound;
    private Collection<Beacon> beacons;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String str = intent.getAction();
            // if receive beacons, try to get extras
            if(str.equals(BeaconApplication.BROADCAST_BEACON)) {
                Beacon beacon = intent.getParcelableExtra(BeaconApplication.BROADCAST_BEACON);
                Log.d(TAG, beacon.toString());
            } else if (str.equals(BeaconApplication.RANGING_DONE)) {
                Log.d(TAG, "ENTER A NEW BEACON REGION!");
            }
        }
    };

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
        if (bundle != null && playerName == null && roomName == null) {
            playerName = bundle.getString("playerName");
            roomName = bundle.getString("roomName");
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
        // start the beacon service in the backgorund thread
        Intent intent = new Intent(MainActivity.this, BeaconApplication.class);
        startService(intent);
        // and register for broadcast receiver from beacon service
        IntentFilter filter = new IntentFilter();
        filter.addAction(BeaconApplication.BROADCAST_BEACON);
        filter.addAction(BeaconApplication.RANGING_DONE);
        registerReceiver(receiver, filter);

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

    @Override
    protected void onStart() {
        startService(new Intent(MainActivity.this, BeaconApplication.class));

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
                    return new LobbyFragment();
                case 1:
                    return new MapFragment();
                case 2:
                    return new MeFragment();
                case 3:
                    return new TargetFragment();
                default:
                    return null;
            }
        }
    }
}
