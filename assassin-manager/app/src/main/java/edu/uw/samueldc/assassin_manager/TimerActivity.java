package edu.uw.samueldc.assassin_manager;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class TimerActivity extends AppCompatActivity {

    private static final String TAG = "***TimerActivity***";

    private Firebase fireBaseRef;
    private HashMap<String, HashMap<String, String>> userData;
    private String userID, room;
    private long startTime;
    private Bundle bundleFromLastActivity;

    static Timer timer;
    static TimerTask task;

    private Button startBtn;

    private boolean buttonPressed;

    public void onClickEnter(View view) {
        if(playerCount >= 1) {
            startBtn.setEnabled(false);
            startBtn.setBackgroundColor(Color.GRAY);

            buttonPressed = true;
            fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room);

            Log.v(TAG, "Timer: " + fireBaseRef.child("timer").getKey());

            fireBaseRef.child("timer").setValue("0");

            startTime = 0;

            // Make sure you don't have some async issue here
            Intent intent = new Intent(TimerActivity.this, MainActivity.class);
            Bundle mainBundle = new Bundle();
            mainBundle.putString("userID", userID);
            mainBundle.putString("room", room);
            mainBundle.putSerializable("userData", bundleFromLastActivity.getSerializable("userData"));
            intent.putExtras(bundleFromLastActivity);

            fireBaseRef.removeEventListener(listener);


            task.cancel();
            timer.cancel();
            timer = null;


            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buttonPressed = false;
        setContentView(R.layout.activity_timer);

        startBtn = (Button) findViewById(R.id.btnStart);
        startBtn.setEnabled(false);
        startBtn.setBackgroundColor(Color.GRAY);


        userData = new HashMap<String, HashMap<String, String>>();
        playerCount = 0;

        bundleFromLastActivity = getIntent().getExtras();
        if(bundleFromLastActivity != null) {
//            userData = (HashMap) bundle.getSerializable("userData");
            userID = bundleFromLastActivity.getString("userID");
            startTime = bundleFromLastActivity.getLong("startTime");
            room = bundleFromLastActivity.getString("room");
        }

        Log.d(TAG, "============ USER ROOM: " + room);

        adjustUsers();


        timer = new Timer();
        task = new TimerTask(bundleFromLastActivity);
        timer.schedule(task, 0, 1000);

        // Listener for start time change, will start game for everyone when one person clicks enter

        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room);
        fireBaseRef.child("timer").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(!buttonPressed) {
                    Log.v(TAG, "Start Time thingy: " + dataSnapshot.getValue());
                    if (Long.parseLong(dataSnapshot.getValue().toString()) == 0) {
                        Intent intent = new Intent(TimerActivity.this, MainActivity.class);
//                        Bundle mainBundle = new Bundle();
//                        mainBundle.putString("userID", userID);
//                        mainBundle.putString("room", room);
//                        mainBundle.putSerializable("userData", userData);
                        intent.putExtras(bundleFromLastActivity);

                        fireBaseRef.removeEventListener(listener);

//                    timer = null;

                        startActivity(intent);
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(timer != null)
            timer.cancel();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timer != null)
            timer.cancel();
    }

    public void adjustTargets() {
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users");

        String previousID = "";
        String firstID = "";
        String firstUserID = "";

        Log.d(TAG, "======== USER NUM: " + userData.keySet().size());
        for(String s : userData.keySet()) {

            HashMap<String, String> data = userData.get(s);
            Log.d(TAG, "============= DATA FROM USER ID: " + data);
            String uniqueID = data.get("uniqueID"); // get uniqueID of this user in this room
            Log.v(TAG, "current userData: " + userData.get(s));
            Log.v(TAG, "timerAct data: " + data);

            if(firstID.length() > 0) {

                Log.d(TAG, "++++++++== CURRENT USER: " + userData.get(s).toString());
                Log.d(TAG, "++++++++++ SET TARGET");
                data.put("target", previousID);
                fireBaseRef.child(s).setValue(data);
            } else {
                firstID = uniqueID;
                firstUserID = s;
            }
            previousID = uniqueID;

        }

        //fence post, set first player's target to last player.
        HashMap<String, String> data = userData.get(firstUserID);
        data.put("target", previousID);
        fireBaseRef.child(firstUserID).setValue(data);


    }

    private ValueEventListener listener;
    private int playerCount;

    // Updates userData variable to hold info for all current users
    public void adjustUsers() {
        Firebase.setAndroidContext(this);

        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room + "/users");

        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // New User is added, set last users target to current target
                ArrayList<String> roomUsers = new ArrayList<String>();

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    roomUsers.add(child.getKey());
                }

                Log.v(TAG, "Users in room: " + roomUsers);
                TextView playerCounter = (TextView) findViewById(R.id.tvPlayerCount);
                playerCounter.setText("Players in lobby: " + roomUsers.size());
                playerCount = roomUsers.size();

                if(playerCount >= 1) {
                    startBtn.setEnabled(true);
                    startBtn.setBackgroundColor(Color.rgb(58, 152, 50));

                }

                for (final String userID : roomUsers) {
                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
                    final HashMap<String, String> data = new HashMap<String, String>();


                    listener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                data.clear();
                                for (DataSnapshot child : dataSnapshot.getChildren()) {
                                    data.put(child.getKey(), child.getValue().toString());
                                }

                                userData.put(userID, data);

                                adjustTargets();
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            Log.e(TAG, "Error when accessing DB: " + firebaseError);
                        }
                    };

                    fireBaseRef.addListenerForSingleValueEvent(listener);

                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }


    public class TimerTask extends java.util.TimerTask {

        private Bundle bundle;

        public TimerTask(Bundle b) {
            bundle = b;
        }

        @Override
        public void run() {

            if(timer != null) {
                final Calendar c = Calendar.getInstance();

                if (c.getTimeInMillis() < startTime) {

//                    adjustUsers();


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            TextView clock = (TextView) findViewById(R.id.time);


                            String time = String.format("%02d:%02d",
                                    TimeUnit.MILLISECONDS.toMinutes(startTime - c.getTimeInMillis()),
                                    TimeUnit.MILLISECONDS.toSeconds(startTime - c.getTimeInMillis()) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime - c.getTimeInMillis()))
                            );

                            clock.setText(time);


                        }
                    });


                } else {
                    // Make sure you don't have some async issue here
                    Intent intent = new Intent(TimerActivity.this, MainActivity.class);
                    Bundle mainBundle = new Bundle();
                    mainBundle.putString("userID", userID);
                    mainBundle.putString("room", room);
                    mainBundle.putSerializable("userData", bundle.getSerializable("userData"));
                    intent.putExtras(mainBundle);

                    fireBaseRef.removeEventListener(listener);

                    task.cancel();
                    timer.cancel();
                    timer = null;

                    startActivity(intent);

                }
            }
        }
    }
}
