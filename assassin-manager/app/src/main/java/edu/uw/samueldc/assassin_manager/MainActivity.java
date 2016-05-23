package edu.uw.samueldc.assassin_manager;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
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

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BootstrapNotifier {
    static final int NUM_SCREEN = 4;

    private static final String TAG = "MainActivity";

    PageAdapter pageAdapter;

    ViewPager viewPager;

    Firebase fireBaseRef;

    private String playerName;
    private static String roomName;

    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private boolean haveDetectedBeaconsSinceBoot = false;

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
        playerName = getIntent().getExtras().getString("playerName");
        roomName = getIntent().getExtras().getString("roomName");

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
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);

        // wake up the app when any beacon is seen
        Region region = new Region("edu.uw.samueldc.assassin_manager.MainActivity", null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        // reduce bluetooth power consumption by around 60%
        backgroundPowerSaver = new BackgroundPowerSaver(this);

        BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createBasicSimulatedBeacons();

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

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "did enter region.");
        if (!haveDetectedBeaconsSinceBoot) {
            Log.d(TAG, "auto launching MainActivity");

            // The very first time since boot that we detect an beacon, we launch the
            // MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("playerName", playerName);
            bundle.putString("roomName", roomName);
            intent.putExtras(bundle);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
            // to keep multiple copies of this activity from getting created if the user has
            // already manually launched the app.
            this.startActivity(intent);
            haveDetectedBeaconsSinceBoot = true;
        } else {
            // else, change to target view
            Log.d(TAG, "change to target view");
            viewPager.setCurrentItem(3);
        }
    }

    @Override
    public void didExitRegion(Region region) {

    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {

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
                    return new MapFragment().newInstance(roomName);
                case 2:
                    return new MeFragment();
                case 3:
                    return new TargetFragment();
                default:
                    return null;
            }
        }
    }

    // test fragment -- used until fragment settled
    public static class ArrayListFragment extends ListFragment {
        int mNum;

        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         */
        static ArrayListFragment newInstance(int num) {
            ArrayListFragment f = new ArrayListFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_pager_list, container, false);
            View tv = v.findViewById(R.id.text);
            ((TextView)tv).setText("Fragment #" + mNum);
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ArrayList<String> alist = new ArrayList<>();
            alist.add("sdfasd");
            alist.add("fafadsfad");
            setListAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, alist));
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i("FragmentList", "Item clicked: " + id);
        }
    }
}
