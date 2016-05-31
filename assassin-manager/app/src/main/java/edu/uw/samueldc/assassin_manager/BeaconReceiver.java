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

    private OnBeaconReceivedListener listener = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (listener != null) {
            listener.onBeaconReceived(context, intent);
        }
    }

    public void setOnBeaconReceivedListener(Context context) {
        this.listener = (OnBeaconReceivedListener) context;
    }

    public interface OnBeaconReceivedListener {
        public void onBeaconReceived(Context context, Intent intent);
    }
}
