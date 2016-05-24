package edu.uw.samueldc.assassin_manager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.HashMap;
import java.util.Map;


public class MeFragment extends Fragment {
    private static final String TAG = "***MeFrag***";
    Firebase fireBaseRef;
    String playerName;
    String playerScore;
    String playerStatus;
    TextView tvName;
    TextView tvScore;
    TextView tvStatus;

    public MeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_me, container, false);
        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users");

        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (playerName == null) {
                    playerName = ((MainActivity) getActivity()).getPlayerName();
                }

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if (child.child("name").getValue().toString().equalsIgnoreCase(playerName)) {
                        //found our player
                        String kills = child.child("kills").getValue().toString();
                        String status = child.child("status").getValue().toString();
                        set(kills, status);
                    }

                }



            }
            @Override
            public void onCancelled (FirebaseError firebaseError){
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
