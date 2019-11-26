package com.unej.lintracker;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class GetStartedAct extends AppCompatActivity {

    Button bttnSignIn, bttnBuatAkunPrimary;
    private Dialog dialog;
    TextView textPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        bttnSignIn = findViewById(R.id.bttnSignIn);
        bttnBuatAkunPrimary =findViewById(R.id.bttnBuatAkunPrimary);

        dialog = new Dialog(this);

        bttnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signIn = new Intent(GetStartedAct.this, LoginAct.class);
                startActivity(signIn);
                finish();
            }
        });

        bttnBuatAkunPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToRegister = new Intent(GetStartedAct.this, Register.class);
                startActivity(goToRegister);
            }
        });
    }
}
