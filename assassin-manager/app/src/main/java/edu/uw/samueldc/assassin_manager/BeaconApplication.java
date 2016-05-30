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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

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

    public Beacon hunter;
    public Beacon prey = null;
    public HashMap<String, Beacon> beaconMap = new HashMap<>();


    public String hunterUniqueID;
    public String targetUniqueID;

    // flags to check if hunter or prey is nearby
    private boolean isHunterNearby = false;
    private boolean isTargetNearby = false;

    HashMap<String, String> userData = null;

    private Firebase fireBaseRef;

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
//
//                BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
//                ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();

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


        // add event listener to update userdata's target dynamically
        if (userData != null) {
            fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userData.get("userID"));

            fireBaseRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child("target").getValue() != null) {
                        targetUniqueID = dataSnapshot.child("target").getValue().toString();
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.e(TAG, "Error when accessing DB: " + firebaseError);
                }
            });
        }


        if (transmittedBeacon == null && userData != null) {
            // build a beacon
            Log.d(TAG, "USER NAME HASH: " + userData.get("nameHash"));
            transmittedBeacon = new Beacon.Builder()
                    .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
                    .setId2(userData.get("uniqueID"))
                    .setId3(userData.get("id3"))
                    .setManufacturer(0x0118)
                    .setTxPower(-59)
                    .setDataFields(Arrays.asList(new Long[]{(Integer.valueOf(userData.get("nameHash")).longValue())}))
                    .build();
//            List<Long> datafield = transmittedBeacon.getDataFields();
//            for (Long data : datafield) {
//                Log.d(TAG, "TRANSFERRED NAME HASH: " + data);
//            }
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

    private ValueEventListener listener;

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                if (beacons.size() > 0) {
                    Log.d(TAG, "RECEIVE BEACON MESSAGE!!");

                    final Collection<Beacon> beaconList = beacons;

                    for (Beacon beacon : beaconList) {
                        Log.d(TAG, "========= RECEIVED BEACON:" + beacon.toString());
                    }

                    final String room = userData.get("room");
                    final String username = userData.get("name");
                    final String nameHash = userData.get("nameHash");


                    // ========= check if one of those beacons is hunter or prey <------ check itself for testing right now
                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room + "/users");

                   fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
//                            dataSnapshot.getValue();

                            ArrayList<String> roomUsers = new ArrayList<String>();

                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                roomUsers.add(child.getKey());
                            }

                            for (final String userID : roomUsers) {
                                Firebase smallRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
                                final HashMap<String, String> data = new HashMap<String, String>();


                                listener = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        data.clear();

                                        Log.d(TAG, "========== CHILD: " + dataSnapshot.toString());

                                        for (Beacon beacon : beaconList) {

                                            // ======== first check if this beacon is my hunter
                                            if (hunterUniqueID == null) {
//                                                Log.d(TAG, "============ NO HUNTER FOUND");
                                                // means not found a hunter yet, need to search db
                                                if (dataSnapshot.child("target") != null) {
                                                    Log.d(TAG, "+++++++ MY UNIQUE ID: " + userData.get("uniqueID"));
                                                    if (dataSnapshot.child("target").getValue().toString().equalsIgnoreCase(userData.get("uniqueID"))) {
                                                        Log.d(TAG, "======= THIS IS MY HUNTER: " + dataSnapshot.child("name"));
                                                        // if a user's target is me, this user is my hunter
                                                        hunterUniqueID = dataSnapshot.child("uniqueID").getValue().toString();
                                                        if (hunterUniqueID.equalsIgnoreCase(beacon.getId2().toString())) {
                                                            // this beacon device is my hunter
//                                                            Log.d(TAG, "========= FOUND HUNTER!!");
                                                            Log.d(TAG, "========= " + dataSnapshot.child("name").getValue().toString());
                                                            sendNotification("A HUNTER IS NEARBY!!");
                                                            isHunterNearby = true;
                                                            hunter = beacon; // update hunter anyway
                                                        }
                                                    }
                                                }

                                            } else {
                                                // already have hunter's unique id, compare to beacon directly
//                                                Log.d(TAG, "============ HUNTER already FOUND");
                                                if (hunterUniqueID.equalsIgnoreCase(beacon.getId2().toString())) {
                                                    // this beacon device is my hunter
//                                                    Log.d(TAG, "========= FOUND HUNTER!!");
                                                    Log.d(TAG, "========= " + dataSnapshot.child("name").getValue().toString());
                                                    sendNotification("A HUNTER IS NEARBY!!");
                                                    isHunterNearby = true;
                                                    hunter = beacon; // update hunter anyway
                                                }
                                            }

                                            // ============ then check if this beacon is my target
                                            if (targetUniqueID != null) {
                                                Log.d(TAG, "============ TARGET already HAVE: " + targetUniqueID);
                                                Log.d(TAG, "++++++++++ BEACON id2: " + beacon.getId2());
                                                if (targetUniqueID.equalsIgnoreCase(beacon.getId2().toString())) {

//                                                    Log.d(TAG, "========= FOUND PREY!!!");
                                                    Log.d(TAG, "========= " + dataSnapshot.child("name").getValue().toString());
                                                    prey = beacon;
                                                    sendNotification("A TARGET IS NEARBY!!");

                                                    isTargetNearby = true;
                                                }
                                            }


                                            // ========== for testing
//                                            if (dataSnapshot.child("uniqueID").getValue() != null) {
//                                                if (dataSnapshot.child("uniqueID").getValue().toString().equalsIgnoreCase(beacon.getId2().toString())) {
//                                                    Log.d(TAG, "========= FOUND ONE PLAYER!!");
//                                                    Log.d(TAG, "========= " + dataSnapshot.child("name").getValue().toString());
////                                               sendNotification();
//                                                }
//                                            }

                                        }

                                    }

                                    @Override
                                    public void onCancelled(FirebaseError firebaseError) {
                                        Log.e(TAG, "Error when accessing DB: " + firebaseError);
                                    }
                                };

                                smallRef.addListenerForSingleValueEvent(listener);
                            }



//                            Intent broadcastBeaconsIntent = new Intent(BeaconApplication.BROADCAST_BEACON);
//                            Bundle beaconBundle = new Bundle();
//
//                            if (hunter != null) {
//                                Log.d(TAG, "+++++++++++ HUNTER FOUND: " + prey.toString());
//                                beaconMap.put("hunter", hunter);
//                            }
//                            if (prey != null) {
//                                Log.d(TAG, "+++++++++++ PREY FOUND: " + prey.toString());
//                                beaconMap.put("target", prey);
//                            }
//                    Log.d(TAG, "" + beacons.size());
//                    Log.d(TAG, temList.get(0).toString());

//                            beaconBundle.putParcelable("target", prey == null? transmittedBeacon:prey);
//                            beaconBundle.putParcelable("hunter", hunter);
////                    beaconBundle.putSerializable("beaconMap", beaconMap);
////                    beaconBundle.putParcelable("beacons", beacons.iterator().next());
//                            broadcastBeaconsIntent.putExtras(beaconBundle);
//                            sendBroadcast(broadcastBeaconsIntent);
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                           Log.e(TAG, "Error when accessing DB: " + firebaseError);
                        }
                    });

                    // send hunter and prey as map of beacons as broadcast message to other activities



//                    Log.d(TAG, "" + beacons.size());
//                    Log.d(TAG, temList.get(0).toString());

                    //EditText editText = (EditText)RangingActivity.this.findViewById(R.id.rangingText);
//                    Beacon firstBeacon = beacons.iterator().next();
//                    logToDisplay("The first beacon " + firstBeacon.toString() + " is about " + firstBeacon.getDistance() + " meters away.");
                }
                // after that, need to check if hunter or prey is within range,
                // set them to false every time traverse a beacon list
                if (!isHunterNearby) {
                    // if cannot find hunter within range, need to set it to null
                    hunter = null;
                } else {
                    isHunterNearby = false;
                }
                if (!isTargetNearby) {
                    prey = null;
                } else {
                    isTargetNearby = false;
                }

                if (hunter != null) {
                    Log.d(TAG, "+++++++++++ HUNTER FOUND: " + hunter.toString());
                    beaconMap.put("hunter", hunter);
                }
                if (prey != null) {
                    Log.d(TAG, "+++++++++++ PREY FOUND: " + prey.toString());
                    beaconMap.put("target", prey);
                } else {
                    Log.d(TAG, "----------- PREY IS ESCAPING!!");
                }
                Intent broadcastBeaconsIntent = new Intent(BeaconApplication.BROADCAST_BEACON);
                Bundle beaconBundle = new Bundle();

                beaconBundle.putParcelable("target", prey);
                beaconBundle.putParcelable("hunter", hunter);
                beaconBundle.putString("ttt", "asdfasdfasdf");
//                    beaconBundle.putSerializable("beaconMap", beaconMap);
//                    beaconBundle.putParcelable("beacons", beacons.iterator().next());
                broadcastBeaconsIntent.putExtras(beaconBundle);
                sendBroadcast(broadcastBeaconsIntent);
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

        // check if the beacon is its hunter or prey <------- testing itself right now


        // since enter a region, no matter if the main activity is opening right now, we push a notification to user
//        sendNotification();
    }

    @Override
    public void didExitRegion(Region region) {
        // leave empty
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        // leave empty
    }

    private void sendNotification(String text) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // determine if it should show popup notification
        boolean showPopup = prefs.getBoolean("pref_show_notification", true);

        if (showPopup) {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setContentTitle("Beacon Reference Application")
                            .setContentText(text)
                            .setSmallIcon(R.drawable.cast_ic_notification_0)
//                        .setVibrate(new long[] {0, 500, 500, 500})
                            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setVisibility(0);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainActivity.class);
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
        } else {
            Toast.makeText(context, "SOMEONE IS NEARBY!", Toast.LENGTH_SHORT).show();
        }

    }
}
