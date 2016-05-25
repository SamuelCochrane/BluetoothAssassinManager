package edu.uw.samueldc.assassin_manager;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Collection;



import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LobbyFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LobbyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LobbyFragment extends ListFragment {
    private static final String TAG = "***LobbyFrag***";
    Firebase fireBaseRef;
    static String playerName;
    static String playerRoom;

    private OnFragmentInteractionListener mListener;

    public LobbyFragment() {
        // Required empty public constructor
    }


    public void receivedBeacons(Collection<Beacon> beacons) {

    }

    public static LobbyFragment newInstance(String name, String room) {
        LobbyFragment fragment = new LobbyFragment();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("room", room);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playerName = getArguments().getString("name");
            playerRoom = getArguments().getString("room");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_lobby, container, false);

        fireBaseRef = new Firebase("https://infoassassinmanager.firebaseio.com/users");

        fireBaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                ArrayList<String> namesArrayList = new ArrayList<String>();
                ArrayList<String> statusArrayList = new ArrayList<String>();
                ArrayList<String> killsArrayList = new ArrayList<String>();

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String name = child.child("name").getValue().toString();
                    namesArrayList.add(name);

                    String status = child.child("status").getValue().toString();
                    statusArrayList.add(status);

                    String kill = child.child("kills").getValue().toString();
                    killsArrayList.add(kill);
                }


                String[] names = namesArrayList.toArray(new String[0]);
                String[] status = statusArrayList.toArray(new String[0]);
                String[] kills = killsArrayList.toArray(new String[0]);

                setListAdapter(new ImageAndTextAdapter(getContext(), R.layout.fragment_lobby_item,
                        names, status, kills, null)); //null -> TypedArray icons
        /**/



            }
            @Override
            public void onCancelled (FirebaseError firebaseError){
                Log.e(TAG, "Error when accessing DB: " + firebaseError);
            }

        });

        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    class ImageAndTextAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;

        private String[] mNames;
        private String[] mStatus;
        private String[] mKills;
        private TypedArray mIcons;

        private int mViewResourceId;

        public ImageAndTextAdapter(Context ctx, int viewResourceId,
                                   String[] names, String[] status, String[] kills, TypedArray icons) {
            super(ctx, viewResourceId, names);

            mInflater = (LayoutInflater) ctx.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

            mNames = names;
            mStatus = status;
            mKills = kills;

            mIcons = icons;

            mViewResourceId = viewResourceId;
        }

        @Override
        public int getCount() {
            return mNames.length;
        }

        @Override
        public String getItem(int position) {
            return mNames[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(mViewResourceId, null);

/*            ImageView iv = (ImageView) convertView.findViewById(R.id.itemIcon);
            iv.setImageDrawable(mIcons.getDrawable(position));*/

            TextView tvName = (TextView) convertView.findViewById(R.id.itemTitle);
            tvName.setText(mNames[position]);

            TextView tvKills = (TextView) convertView.findViewById(R.id.itemScore);
            tvKills.setText(mKills[position]);

            ImageView ivStatus = (ImageView) convertView.findViewById(R.id.itemIcon);
            if(mStatus[position].equals("dead")) {
                ivStatus.setImageResource(R.drawable.ic_dead);
            } else if (mStatus[position].equals("target")) {
                ivStatus.setImageResource(R.drawable.ic_target);
            } else {
                ivStatus.setImageResource(R.drawable.ic_default);
            }



            return convertView;
        }
    }
}


