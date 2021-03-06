package edu.uw.samueldc.assassin_manager;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.altbeacon.beacon.Beacon;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TargetFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TargetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TargetFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "***TargetFrag***";
    Firebase fireBaseRef;
    static String playerName;
    static String playerID;
    static String playerRoom;

    static String targetName;
    static String targetID;
    static Double targetDistance;
    static boolean inRange;

    String playerScore;
    String playerStatus;
    TextView tvTargetName;
    TextView tvTargetDistance;
    TextView tvStatus;
    Button killButton;

    ValueEventListener targetListener;
    Firebase ref;

    private ValueEventListener listener;

    private static final double KILL_RANGE = 5.0f;

    public static TargetFragment newInstance(String name, String room, String myID, String targetID) {
        TargetFragment fragment = new TargetFragment();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("room", room);
        args.putString("playerID", myID);
        args.putString("targetID", targetID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playerName = getArguments().getString("name");
            playerRoom = getArguments().getString("room");
            playerID = getArguments().getString("playerID");
            targetID = getArguments().getString("targetID");
        }
    }

    public TargetFragment() {
        // Required empty public constructor
    }

    // helper method for main activity to call when updated target's data
    public void updateTarget(Beacon target) {
        if (target != null) {
            targetDistance = target.getDistance();
            set(targetName, targetDistance);
        } else {
            set(targetName, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_target, container, false);
        Button b = (Button) v.findViewById(R.id.killButton);
        b.setOnClickListener(this);


        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + playerRoom + "/users");

        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                            dataSnapshot.getValue();
                if (dataSnapshot.exists()) {
                    ArrayList<String> roomUsers = new ArrayList<String>();

                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        roomUsers.add(child.getKey());
                    }

                    for (String userID : roomUsers) {
                        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
                        final HashMap<String, String> data = new HashMap<String, String>();

                        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    data.clear();
                                    if (dataSnapshot.child("uniqueID").getValue().toString().equalsIgnoreCase(targetID)) {
                                        Log.d(TAG, "========= FOUND TARGET!!!");
                                        Log.d(TAG, "========= " + dataSnapshot.child("name").getValue().toString());

                                        targetName = dataSnapshot.child("name").getValue().toString();

                                        set(targetName, null);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                Log.e(TAG, "Error when accessing DB: " + firebaseError);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }
        });

        return v;
    }


    private void set(String targetName, Double distance) {

        tvStatus = (TextView) getView().findViewById(R.id.targetSpottedText);
        tvTargetName = (TextView) getView().findViewById(R.id.targetSpottedNameText);
        tvTargetDistance = (TextView) getView().findViewById(R.id.targetSpottedDistance);
        killButton = (Button) getView().findViewById(R.id.killButton);

        this.targetName = targetName;
        this.targetDistance = distance;
        this.inRange = isInRange(distance);

        if (targetName != null) {
            tvTargetName.setText(targetName);
        }


        String targetDistanceText;
        if (distance != null) {
            targetDistanceText = new DecimalFormat("#.00m").format(targetDistance);
        } else {
            targetDistanceText = "Not Detected";
        }

        tvTargetDistance.setText(targetDistanceText);
        killButton.setClickable(inRange);
        if(inRange) {
            tvStatus.setText(R.string.target_spotted_text_in_range);
        } else {
            tvStatus.setText(R.string.target_spotted_text_not_in_range);
        }

    }


    public boolean isInRange(Double distance) {
        if (distance != null) {
            return (distance < KILL_RANGE);
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.killButton:
                killTarget();
                break;
        }
    }


    private String targetsTarget;
    private int ourScore;
//    private Firebase myFireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + playerID);
    //sets a player to dead,
    //called by killButton
    public void killTarget() {
        Log.i(TAG, "Killing");
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + playerRoom + "/users");
        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
//                            dataSnapshot.getValue();
                if (dataSnapshot.exists()) {
                    ArrayList<String> roomUsers = new ArrayList<String>();

                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        roomUsers.add(child.getKey());
                    }

                    for (final String userID : roomUsers) {
                        final Firebase curFireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + userID);
                        final HashMap<String, String> data = new HashMap<String, String>();

                        curFireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    data.clear();
                                    if (dataSnapshot.child("uniqueID").getValue().toString().equalsIgnoreCase(targetID)) {
                                        Log.d(TAG, "========= FOUND TARGET!!!");
                                        Log.d(TAG, "========= " + dataSnapshot.child("name").getValue().toString());
                                        targetName = dataSnapshot.child("name").getValue().toString();

                                        targetsTarget = dataSnapshot.child("target").getValue().toString();



                                        Log.i(TAG, "new target:" + targetsTarget);
                                        Log.i(TAG, playerName + " just killed " + targetName + "!");
                                        //
                                        //
                                        final Firebase meFireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + playerID);
                                        meFireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                Log.i(TAG, "UniqueID: " + dataSnapshot.child("uniqueID").getValue().toString() + "    new target:" + targetsTarget);
                                                if(dataSnapshot.child("uniqueID").getValue().toString().equalsIgnoreCase(targetsTarget)) {
                                                    Log.i(TAG, "---------GAME IS OVER!-----------");
                                                    fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/rooms/" + playerRoom);
                                                    fireBaseRef.child("GameWinner").setValue(playerName);
                                                }
                                                ourScore = Integer.parseInt(dataSnapshot.child("kills").getValue().toString());
                                            }

                                            @Override
                                            public void onCancelled(FirebaseError firebaseError) {
                                                Log.e(TAG, "Error when accessing DB: " + firebaseError);
                                            }
                                        });
                                        Log.i(TAG, "~~~~~~~~~~~~~~~~~~~~" + curFireBaseRef.child("name") + "<------------ this guy is DEAD");
                                        curFireBaseRef.child("status").setValue("dead"); // kill them
                                        meFireBaseRef.child("kills").setValue(ourScore + 1);
                                        meFireBaseRef.child("target").setValue(targetsTarget);
//                                        targetID = targetsTarget;


                                    }

                                    //set(targetName, null);
                                }

                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                Log.e(TAG, "Error when accessing DB: " + firebaseError);
                            }
                        });

                    }
                }
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }
        });

    }


/*final Firebase meFireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + playerID);
                                        meFireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                ourScore = Integer.parseInt(dataSnapshot.child("kills").getValue().toString());
                                            }

        Log.i(TAG, "Killing");
        Firebase targetFireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + targetID);
        Log.i(TAG, "setting listener for: " + targetID);
        targetFireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                Log.i(TAG, "Starting data gather...");
                targetsTarget = dataSnapshot.child("target").getValue().toString();
                targetID = targetsTarget;
                Log.i(TAG, "new target:" + targetID);
                Log.i(TAG, playerName + " just killed " + dataSnapshot.child("name").getValue().toString() + "!");


            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }
        });
        targetFireBaseRef.child("status").setValue(EnterActivity.STATUS_DEAD);



        targetFireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ourScore = Integer.parseInt(dataSnapshot.child("kills").getValue().toString());


                myFireBaseRef.child("kills").setValue(ourScore + 1);
                myFireBaseRef.child("target").setValue(targetsTarget);

            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }

        });
*/

/*        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment newFragment = new TargetFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.targetFragment, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();*/


    @Override
    public void onDetach() {
        super.onDetach();
    }

 /*   @Override
    public void onResume() {
        super.onResume();
        if(playerScore != null) {
            set(playerScore, playerStatus);
        }
    }*/

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
