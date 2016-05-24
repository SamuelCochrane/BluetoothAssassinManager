package edu.uw.samueldc.assassin_manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;

/**
 * Created by Vermilion on 5/24/16.
 */
public class BeaconReceiver extends BroadcastReceiver {
    private static final String TAG = "**Beacon Receiver**";

    @Override
    public void onReceive(Context context, Intent intent) {
        String str = intent.getAction();
        // if receive beacons, try to get extras
        if(str.equals(BeaconApplication.BROADCAST_BEACON)) {
//                Beacon beacon = intent.getParcelableExtra(BeaconApplication.BROADCAST_BEACON);
            // a list of beacons
            ArrayList<Beacon> beacons = intent.getParcelableArrayListExtra("beacons");
            Log.d(TAG, beacons.get(0).toString());
        } else if (str.equals(BeaconApplication.RANGING_DONE)) {
            Log.d(TAG, "ENTER A NEW BEACON REGION!");
        } else {
            Log.d(TAG, "NOOOOO BEACON!!");
        }
    }
}
