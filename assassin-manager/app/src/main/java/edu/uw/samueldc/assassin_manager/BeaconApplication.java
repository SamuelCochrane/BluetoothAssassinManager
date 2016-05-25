package edu.uw.samueldc.assassin_manager;

import android.app.ActivityManager;
import android.app.Application;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by OwenFlannigan on 5/21/16.
 * handle beacon service, including detecting beacon devices, receive beacon message and transmit itself into a beacon device
 */
public class BeaconApplication extends Service implements BootstrapNotifier, BeaconConsumer {
    private static final String TAG = "BeaconApplication";
    private RegionBootstrap regionBootstrap;
    private boolean haveDetectedBeaconsSinceBoot = false;
    private BackgroundPowerSaver backgroundPowerSaver;

    private BeaconManager beaconManager;

    // pass msg from back and forth
    private Handler handler;

    public static final String RANGING_DONE = "RANGING_DONE";
    public static final String BROADCAST_BEACON = "BROADCAST_BEACON";

    private Context context;
    private Beacon transmittedBeacon;
    private boolean isRunning = false;

    HashMap<String, String> userData = null;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public BeaconApplication() {
//        super("CountingService");
//
//        handler = new Handler();
    }


    @Override
    public void onCreate() {
        super.onCreate();

        isRunning = true;

        Thread serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "App started upalsdkfja;sldkfjal;sdfjl;asdjfa;lsdfkjfl;askdj");
                beaconManager = BeaconManager.getInstanceForApplication(context);

                beaconManager.bind((BeaconConsumer) context);

                // wake up the app when any beacon is seen
                Region region = new Region("edu.uw.samueldc.assassin_manager.MainActivity", null, null, null);
                regionBootstrap = new RegionBootstrap((BootstrapNotifier) context, region);

                // reduce bluetooth power consumption by around 60%
                backgroundPowerSaver = new BackgroundPowerSaver(context);

                BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
                ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();

                // ======== start advertising itself
//                if (transmittedBeacon == null) {
//                    // build a beacon
//                    transmittedBeacon = new Beacon.Builder()
//                            .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
//                            .setId2("1")
//                            .setId3("2")
//                            .setManufacturer(0x0118)
//                            .setTxPower(-59)
//                            .setDataFields(Arrays.asList(new Long[] {0l}))
//                            .build();
//
//                    // set fake beacon device type layout
//                    BeaconParser beaconParser = new BeaconParser()
//                            .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
//
//                    // transmit itself into a beacon device
//                    BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
//                    beaconTransmitter.startAdvertising(transmittedBeacon);
//                }
            }
        });

        serviceThread.start();
        this.context = this;
    }

    @Override
    // this method is automatically proceeded by the android and will call onHanldeIntent
    // when received intent service start command
    // when clip multiple services, it will queue all services
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Intent received!");



        Bundle receivedInfo = intent.getExtras();
        if (receivedInfo != null) {
            userData = (HashMap) receivedInfo.getSerializable("userData");
        }

        if (transmittedBeacon == null && userData != null) {
            // build a beacon
            transmittedBeacon = new Beacon.Builder()
                    .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
                    .setId2(userData.get("id2"))
                    .setId3(userData.get("id3"))
                    .setManufacturer(0x0118)
                    .setTxPower(-59)
                    .setDataFields(Arrays.asList(new Long[]{Long.valueOf(userData.get("nameHash")), Long.valueOf(userData.get("roomHash"))}))
                    .build();

            // set fake beacon device type layout
            BeaconParser beaconParser = new BeaconParser()
                    .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");

            // transmit itself into a beacon device
            BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
            beaconTransmitter.startAdvertising(transmittedBeacon);
        }

        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (beaconManager != null) {
            beaconManager.unbind(this);
        }

    }


    // TODO: WHY NOT RUN IN BACKGROUND??
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.d(TAG, "RECEIVE BEACON MESSAGE!!");
//                Toast.makeText(context, "RECEIVE BEACON MESSAGE!!", Toast.LENGTH_SHORT).show();
                if (beacons.size() > 0) {

                    // send collections of beacons as broadcast message to other activities
                    Intent broadcastBeaconsIntent = new Intent(BeaconApplication.BROADCAST_BEACON);
                    Bundle beaconBundle = new Bundle();
                    ArrayList<Beacon> temList = new ArrayList<Beacon>(beacons);
                    Log.d(TAG, "" + beacons.size());
                    Log.d(TAG, temList.get(0).toString());
                    beaconBundle.putParcelableArrayList("beacons", temList);
//                    beaconBundle.putParcelable("beacons", beacons.iterator().next());
                    broadcastBeaconsIntent.putExtras(beaconBundle);
                    sendBroadcast(broadcastBeaconsIntent);
                    //EditText editText = (EditText)RangingActivity.this.findViewById(R.id.rangingText);
//                    Beacon firstBeacon = beacons.iterator().next();
//                    logToDisplay("The first beacon " + firstBeacon.toString() + " is about " + firstBeacon.getDistance() + " meters away.");
                }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }

    @Override
    public void didEnterRegion(Region region) {
        // Launch Main activity every time a beacon enters the region.
        // Might want to pass extras to Main, which will open the correct
        // target fragment if given the extras are found.
        Log.d(TAG, "Got a didEnterRegion call");

//        Intent intent = new Intent(this, MainActivity.class);

        // send broadcast msg if enter a region
        Intent rangingDoneIntent = new Intent(BeaconApplication.RANGING_DONE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(
                rangingDoneIntent);

//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        this.startActivity(intent);

        // check current activity name
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        Log.d("topActivity", "CURRENT Activity ::" + taskInfo.get(0).topActivity.getClassName());
        ComponentName componentInfo = taskInfo.get(0).topActivity;


        if (!haveDetectedBeaconsSinceBoot) {

            Log.d(TAG, "auto launching MainActivity");

            // TODO: there is a bug in this place!!!
            // The very first time since boot that we detect an beacon, we launch the
            // MainActivity
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            Bundle bundle = new Bundle();
//            bundle.putSerializable("userData", userData);
//            intent.putExtras(bundle);
//            this.startActivity(intent);

            haveDetectedBeaconsSinceBoot = true;
        } else {

            if (taskInfo.get(0).topActivity.getClassName().equalsIgnoreCase("MainActivity")) {
                // tell Main Activity to do some stuff!!
                Log.d(TAG, "BEACON DETECTED FOR THIS MAIN ACTIVITY!");
                // If the Monitoring Activity is visible, we log info about the beacons we have
                // seen on its display
//                mainActivity.logToDisplay("I see a beacon again" );
            } else {
                // If we have already seen beacons before, but the monitoring activity is not in
                // the foreground, we send a notification to the user on subsequent detections.
                Log.d(TAG, "Sending notification.");
//                sendNotification();
            }
        }
    }

    @Override
    public void didExitRegion(Region region) {
        // leave empty
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        // leave empty
    }

    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Beacon Reference Application")
                        .setContentText("An beacon is nearby.")
                        .setSmallIcon(R.drawable.cast_ic_notification_0)
                        .setPriority(NotificationCompat.PRIORITY_MAX);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }
}
