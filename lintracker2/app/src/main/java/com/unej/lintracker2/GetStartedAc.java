package com.unej.lintracker2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GetStartedAc extends AppCompatActivity {

    Button bttnSignIn, bttnBuatAkunPrimary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        bttnSignIn = findViewById(R.id.bttnSignIn);

        bttnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signIn = new Intent(GetStartedAc.this, SetDestinationAct.class);
                startActivity(signIn);
                finish();
            }
        });
    }
}
