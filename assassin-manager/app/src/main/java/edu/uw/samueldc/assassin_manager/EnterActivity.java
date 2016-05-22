package edu.uw.samueldc.assassin_manager;

import android.content.DialogInterface;
import android.content.Intent;
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

import java.util.HashMap;
import java.util.Map;

public class EnterActivity extends AppCompatActivity {
    private static final String TAG = "***EnterActivity***";

    public EnterActivity() {
        // Required empty public constructor
    }

    EditText etPlayerName;
    EditText etRoomName;
    Button btnEnter;

    private String userId;

    Firebase fireBaseRef;


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
                    checkUserExistence(username, room);
                }

            }
        });
    }

    private boolean playerExists;
    public void checkUserExistence(final String username, final String room) {
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + room + "/users");

        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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

                    // Add user to list of users, and get userID
                    String userID = createNewUser(username);

                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms");
                    fireBaseRef.child(room + "/users/" + userID).setValue(user);

                    // start MainActivity
                    // put the strings into bundle to be stored in the database
                    String playerName = etPlayerName.getText().toString();
                    Log.d(TAG, "player name: " + playerName);
                    String roomName = etRoomName.getText().toString();

                    Intent intent = new Intent(EnterActivity.this, MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("playerName", playerName);
                    bundle.putString("roomName", roomName);
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

    public String createNewUser(String username) {
        Log.v(TAG, "Creating new User!");

        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users");


        Map<String, String> userData = new HashMap<String, String>();
        userData.put("name", username);
        userData.put("id2", "1");
        userData.put("id3", "2");
        userData.put("kills", "0");
        userData.put("latitude", "");
        userData.put("longitude", "");
        userData.put("status", "alive");
        userData.put("target", "");

        // Add user to users list, with unique ID
        Firebase newUserRef = fireBaseRef.push();
        newUserRef.setValue(userData);
        userId = newUserRef.getKey();

        return userId;
    }


}