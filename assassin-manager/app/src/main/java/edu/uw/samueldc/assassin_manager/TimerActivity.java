package edu.uw.samueldc.assassin_manager;

import android.content.Intent;
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
    private HashMap<String, Object> userData;
    private String userID, room;
    private long startTime;

    Timer timer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        userData = new HashMap<String, Object>();

        final Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
//            userData = (HashMap) bundle.getSerializable("userData");
            userID = bundle.getString("userID");
            startTime = bundle.getLong("startTime");
            room = bundle.getString("room");
        }


        timer = new Timer();
        timer.schedule(new TimerTask(bundle), 0, 1000);


        Button start = (Button) findViewById(R.id.btnStart);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Make sure you don't have some async issue here
                Intent intent = new Intent(TimerActivity.this, MainActivity.class);
                Bundle mainBundle = new Bundle();
                mainBundle.putString("userID", userID);
                mainBundle.putString("room", room);
                mainBundle.putSerializable("userData", bundle.getSerializable("userData"));
                intent.putExtras(bundle);

                startActivity(intent);
                timer.cancel();
            }
        });


    }

    public void checkTime() {

    }

    public void adjustTargets() {
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users");


        String previousID = "";
        String firstID = "";


        for(String s : userData.keySet()) {

            HashMap<String, String> data = (HashMap<String, String>) userData.get(s);

            // target is index 10
            if(firstID.length() > 0) {
                data.put("target", previousID);
                fireBaseRef.child(s).setValue(data);
            } else {
                firstID = s;
            }
            previousID = s;

        }

        //fence post, set first player's target to last player.
        HashMap<String, String> data = (HashMap<String, String>) userData.get(firstID);
        data.put("target", previousID);
        fireBaseRef.child(firstID).setValue(data);


    }

    // Updates userData variable to hold info for all current users
    public void adjustUsers() {
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room + "/users");

        fireBaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // New User is added, set last users target to current target
                ArrayList<String> roomUsers = new ArrayList<String>();

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    roomUsers.add(child.getKey());
                }

                for (final String userID : roomUsers) {
                    Firebase ref = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
                    final HashMap<String, String> data = new HashMap<String, String>();
                    ref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            data.clear();
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                data.put(child.getKey().toString(), child.getValue().toString());
                            }

                            userData.put(userID, data);

                            adjustTargets();


                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            Log.e(TAG, "Error when accessing DB: " + firebaseError);
                        }
                    });
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

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Calendar c = Calendar.getInstance();
                    if(c.getTimeInMillis() < startTime) {
                        adjustUsers();

                        TextView clock = (TextView) findViewById(R.id.time);

                        String time = String.format("%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(startTime - c.getTimeInMillis()),
                                TimeUnit.MILLISECONDS.toSeconds(startTime - c.getTimeInMillis()) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime - c.getTimeInMillis()))
                        );

                        clock.setText(time);

                    } else {
                        // Make sure you don't have some async issue here
                        Intent intent = new Intent(TimerActivity.this, MainActivity.class);
                        Bundle mainBundle = new Bundle();
                        mainBundle.putString("userID", userID);
                        mainBundle.putString("room", room);
                        mainBundle.putSerializable("userData", bundle.getSerializable("userData"));
                        intent.putExtras(bundle);

                        startActivity(intent);
                        timer.cancel();
                    }
                }
            });


        }
    }
}
