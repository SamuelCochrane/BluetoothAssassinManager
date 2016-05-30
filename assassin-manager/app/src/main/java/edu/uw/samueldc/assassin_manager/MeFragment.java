package edu.uw.samueldc.assassin_manager;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;


public class MeFragment extends Fragment {
    private static final String TAG = "***MeFrag***";
    Firebase fireBaseRef;
    static String playerName;
    static String playerID;
    static String playerRoom;
    String playerScore;
    String playerStatus;
    TextView tvName;
    TextView tvScore;
    TextView tvStatus;

    public static MeFragment newInstance(String name, String room, String id) {
        MeFragment fragment = new MeFragment();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("room", room);
        args.putString("playerID", id);
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
        }
    }

    public MeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_me, container, false);

        Firebase ref = new Firebase("https://infoassassinmanager.firebaseio.com/users/" + playerID);
        final ArrayList<String> data = new ArrayList<String>();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                data.clear();
                Log.i(TAG, "Starting data gather...");
                Log.d(TAG, "========== USER INFO: " + dataSnapshot.toString());
                String kills = dataSnapshot.child("kills").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();

                tvName = (TextView) getView().findViewById(R.id.myName);
                tvScore = (TextView) getView().findViewById(R.id.myScore);
                tvStatus = (TextView) getView().findViewById(R.id.myStatus);

                playerScore = kills;
                playerStatus = status;

                tvName.setText(playerName);
                tvStatus.setText(status);
                tvScore.setText(kills);

            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }
        });

        return v;
    }


    private void set(String kills, String status) {
        if(tvName == null) {
            tvName = (TextView) getView().findViewById(R.id.myName);
            tvScore = (TextView) getView().findViewById(R.id.myScore);
            tvStatus = (TextView) getView().findViewById(R.id.myStatus);
        }
        playerScore = kills;
        playerStatus = status;


        tvName.setText(playerName);
        tvStatus.setText(status);
        tvScore.setText(kills);

    }


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