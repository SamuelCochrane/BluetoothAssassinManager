package edu.uw.samueldc.assassin_manager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class EnterActivity extends AppCompatActivity {
    private static final String TAG = "***EnterActivity***";

    public EnterActivity() {
        // Required empty public constructor
    }

    EditText etPlayerName;
    EditText etRoomName;
    Button btnEnter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter);

        etPlayerName = (EditText) findViewById(R.id.etPlayerName);
        etRoomName = (EditText) findViewById(R.id.etRoomName);
        btnEnter = (Button) findViewById(R.id.btnEnter);

        // Listening to register new account link
        btnEnter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // if play name already exists???
                // make a query; not finished here

                // if ()
                //    {
//                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
//
//                    dialog.setCancelable(false);
//                    dialog.setIcon(R.drawable.login_icon);
//                    dialog.setMessage("Player Name Already Exists. Please Find a New Name.");
//                    dialog.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                        }
//                    });
//                    dialog.show();

                // } else {

                // start MainActivity
                // put the strings into bundle to be stored in the database
                String playerName = etPlayerName.getText().toString();
                String roomName = etRoomName.getText().toString();

                Intent intent = new Intent(EnterActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("playerName", playerName);
                bundle.putString("roomName", roomName);
                intent.putExtras(bundle);

                startActivity(intent);
            }
        });
    }
}