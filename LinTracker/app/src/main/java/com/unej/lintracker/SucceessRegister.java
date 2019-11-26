package com.unej.lintracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SucceessRegister extends AppCompatActivity {

    Button bttnMulai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_succeess_register);

        bttnMulai = findViewById(R.id.bttnMulai);

        bttnMulai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToHome = new Intent(SucceessRegister.this, MainMapAct.class);
                startActivity(goToHome);
                finish();
            }
        });

    }
}
