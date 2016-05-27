package edu.uw.samueldc.assassin_manager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.Calendar;

public class TimerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        long startTime = 0;

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            startTime = bundle.getLong("startTime");
        }

        Calendar c = Calendar.getInstance();
        if(c.getTimeInMillis() < startTime) {

        }

    }
}
