package com.unej.lintracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginAct extends AppCompatActivity {

    TextView bttnBuatAkunSecondary;
    Button bttnSignIn;
    EditText xusername, xpassword;

    String USERNAME_KEY = "usernamekey";
    String username_key = "";

    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bttnBuatAkunSecondary = findViewById(R.id.bttnBuatAkunSecondary);
        bttnSignIn = findViewById(R.id.bttnSignIn);
        xusername = findViewById(R.id.xusername);
        xpassword = findViewById(R.id.xpassword);

        bttnBuatAkunSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToRegister = new Intent(LoginAct.this, Register.class);
                startActivity(goToRegister);
            }
        });

        bttnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tombol buat akun gak bisa dipencet
                bttnSignIn.setEnabled(false);
                bttnSignIn.setText("Loading...");

                final String username = xusername.getText().toString();
                final String password = xpassword.getText().toString();

                reference = FirebaseDatabase.getInstance().getReference().child("users").child(username);

                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            //ambil data password dari firebase
                            String passwordFromFirebase = dataSnapshot.child("password").getValue().toString();

                            //cocokkan username dan password
                            if (passwordFromFirebase.equals(password)){
                                //simpan username pada local
                                SharedPreferences sharedPreferences = getSharedPreferences(USERNAME_KEY, MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(username_key, xusername.getText().toString());
                                editor.apply();

                                //pindah activity
                                Intent goToProfile = new Intent(LoginAct.this, Profile.class);
                                startActivity(goToProfile);
                                finish();
                            }else {
                                //tombol akun bisa dipencet lagi
                                bttnSignIn.setEnabled(true);
                                bttnSignIn.setText("SIGN IN");

                                Toast.makeText(getApplicationContext(), "Password salah", Toast.LENGTH_SHORT).show();
                            }

                        }else{
                            //tombol akun bisa dipencet lagi
                            bttnSignIn.setEnabled(true);
                            bttnSignIn.setText("SIGN IN");

                            Toast.makeText(getApplicationContext(), "Username tidak ada", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(getApplicationContext(), "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });
    }
}
