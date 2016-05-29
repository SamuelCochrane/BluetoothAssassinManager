package edu.uw.samueldc.assassin_manager;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class EndActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);

        Button end = (Button) findViewById(R.id.btnEnd);

        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // clear all backstack and start from enter screen
                Intent intent = new Intent(EndActivity.this, EnterActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }
}
