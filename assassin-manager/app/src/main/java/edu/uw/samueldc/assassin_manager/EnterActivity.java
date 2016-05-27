package edu.uw.samueldc.assassin_manager;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.altbeacon.beacon.BeaconManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EnterActivity extends AppCompatActivity {
    private static final String TAG = "***EnterActivity***";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    public EnterActivity() {
        // Required empty public constructor
    }

    EditText etPlayerName;
    EditText etRoomName;
    Button btnEnter;

    private String userId;
    private long startTime;

    Firebase fireBaseRef;
    HashMap<String, String> userData;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fireBaseRef.setAndroidContext(this);
        setContentView(R.layout.activity_enter);

        etPlayerName = (EditText) findViewById(R.id.etPlayerName);
        etRoomName = (EditText) findViewById(R.id.etRoomName);
        btnEnter = (Button) findViewById(R.id.btnEnter);

        // Listening to register new account link
        btnEnter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                final String room = etRoomName.getText().toString();
                final String username = etPlayerName.getText().toString();


                // Add restrictions for username and room name here, if wanted
                if(room.length() >= 4 && username.length() >= 3) {
                    checkRoomExistence(username, room);


                } else {
                    if (room.length() < 4) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(EnterActivity.this);

                        dialog.setCancelable(false);
                        dialog.setIcon(R.drawable.login_icon);
                        dialog.setTitle("Room Name Too Short!");
                        dialog.setMessage("Room Name Should Contain at Least 4 Letters or Digits.");
                        dialog.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();

                        if (username.length() < 3) {
                            AlertDialog.Builder dialog2 = new AlertDialog.Builder(EnterActivity.this);

                            dialog2.setCancelable(false);
                            dialog2.setIcon(R.drawable.login_icon);
                            dialog2.setTitle("User Name Too Short!");
                            dialog2.setMessage("User Name Should Contain at Least 3 Letters or Digits.");
                            dialog2.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dialog2.show();
                        }

                    }
                    else {
                        AlertDialog.Builder dialog2 = new AlertDialog.Builder(EnterActivity.this);

                        dialog2.setCancelable(false);
                        dialog2.setIcon(R.drawable.login_icon);
                        dialog2.setTitle("User Name Too Short!");
                        dialog2.setMessage("User Name Should Contain at Least 3 Letters or Digits.");
                        dialog2.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        dialog2.show();
                    }

                }

            }
        });

        // check permission
//        verifyBluetooth();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();

        }

    }

    private boolean roomExists;
    public void checkRoomExistence(final String username, final String room) {
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/");
        Log.v(TAG, "Checking Time");

        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(room)) {
//                    String time = dataSnapshot.child("timer").getValue().toString();
                    Log.v(TAG, "Current Timer: " + dataSnapshot.child(room).child("timer").getValue());
                    startTime = (long) dataSnapshot.child(room).child("timer").getValue();
                    roomExists = true;


                    if(Calendar.getInstance().getTimeInMillis() < startTime) {
                        checkUserExistence(username, room);
                    } else {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(EnterActivity.this);

                        dialog.setCancelable(false);
                        dialog.setIcon(R.drawable.login_icon);
                        dialog.setTitle("Too late!");
                        dialog.setMessage("A game with that room name has already begun.");
                        dialog.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                    }


                } else {
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.MINUTE, 5);
                    startTime = c.getTimeInMillis();

                    Log.v(TAG, "setting the current time: " + startTime);
                    Log.v(TAG, "It is: " + Calendar.getInstance().getTimeInMillis());
                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms");
                    fireBaseRef.child(room + "/timer").setValue(startTime);
                    roomExists = false;
                    checkUserExistence(username, room);
                }


            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    private boolean playerExists;
    public void checkUserExistence(final String username, final String room) {
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room + "/users");

        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer uniqueID = (int) dataSnapshot.getChildrenCount() + 1;
                for(DataSnapshot child : dataSnapshot.getChildren()) {
                    if(child.child("name").getValue().toString().equalsIgnoreCase(username)) {
                        Log.v(TAG, "Setting PlayerExistence to True");
                        playerExists = true;
                    }
                }

                if(playerExists) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(EnterActivity.this);

                    dialog.setCancelable(false);
                    dialog.setIcon(R.drawable.login_icon);
                    dialog.setTitle("Name Already in Use!");
                    dialog.setMessage("Player Name Already Exists. Please Find a New Name.");
                    dialog.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            playerExists = false;
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                } else {

                    Map<String, Object> user = new HashMap<String, Object>();
                    user.put("name", username);
                    user.put("nameHash", Integer.toString(username.hashCode()));
                    user.put("uniqueID", uniqueID);

                    // Add user to list of users, and get userID
                    String userID = createNewUser(username, room, uniqueID);

                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms");
                    fireBaseRef.child(room + "/users/" + userID).setValue(user);

                    // start MainActivity
                    // put the strings into bundle to be stored in the database
                    String playerName = etPlayerName.getText().toString();
                    Log.d(TAG, "player name: " + playerName);
                    String roomName = etRoomName.getText().toString();

                    Intent intent = new Intent(EnterActivity.this, MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("userID", userID);
                    bundle.putSerializable("userData", userData);
                    bundle.putLong("startTime", startTime);
                    intent.putExtras(bundle);

                    startActivity(intent);

                }
            }



            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }
        });

    }

    public String createNewUser(String username, String room, Integer uniqueID) {
        Log.v(TAG, "Creating new User!");

        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users");


        userData = new HashMap<String, String>();
        userData.put("name", username);
        userData.put("id2", "1");
        userData.put("id3", "255");
        userData.put("uniqueID", uniqueID.toString());
        userData.put("room", room);
        userData.put("nameHash", Integer.toString(username.hashCode()));
        userData.put("roomHash", Integer.toString(room.hashCode()));
        userData.put("kills", "0");
        userData.put("latitude", "47.6553");
        userData.put("longitude", "-122.3035");
        userData.put("status", "alive");
        userData.put("target", "");


        // Add user to users list, with unique ID
        Firebase newUserRef = fireBaseRef.push();
        newUserRef.setValue(userData);
        userId = newUserRef.getKey();
        userData.put("userID", userId);

        Log.d(TAG, "USER ID IS: " + userId);

        return userId;
    }

}