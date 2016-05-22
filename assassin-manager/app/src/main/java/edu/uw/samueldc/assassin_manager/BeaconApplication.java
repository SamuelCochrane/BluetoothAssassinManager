package edu.uw.samueldc.assassin_manager;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

/**
 * Created by OwenFlannigan on 5/21/16.
 */
public class BeaconApplication extends Application implements BootstrapNotifier {
    private static final String TAG = "BeaconApplication";
    private RegionBootstrap regionBootstrap;

    private BackgroundPowerSaver backgroundPowerSaver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App started up");
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);

        // wake up the app when any beacon is seen
        Region region = new Region("edu.uw.samueldc.assassin_manager.MainActivity", null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        // reduce bluetooth power consumption by around 60%
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }

    @Override
    public void didEnterRegion(Region region) {
        // Launch Main activity every time a beacon enters the region.
        // Might want to pass extras to Main, which will open the correct
        // target fragment if given the extras are found.
        Log.d(TAG, "Got a didEnterRegion call");

        Intent intent = new Intent(this, MainActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    @Override
    public void didExitRegion(Region region) {
        // leave empty
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        // leave empty
    }
}
