package com.unej.lintracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class Profile extends AppCompatActivity {

    Button bttnEditProfile, bttnSignOut;
    ImageView foto_user, bttnBack;
    TextView nama, username, lin;

    DatabaseReference reference;

    String USERNAME_KEY = "usernamekey";
    String username_key = "";
    String username_key_new = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getUsernameLocal();

        bttnEditProfile = findViewById(R.id.bttnEditProfile);
        bttnSignOut = findViewById(R.id.bttnSignOut);
        foto_user = findViewById(R.id.foto_user);
        nama = findViewById(R.id.nama);
        username = findViewById(R.id.username);
        lin = findViewById(R.id.lin);
        bttnBack = findViewById(R.id.bttnBack);

        reference = FirebaseDatabase.getInstance().getReference().child("users").child(username_key_new);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nama.setText(dataSnapshot.child("nama").getValue().toString());
                username.setText(dataSnapshot.child("username").getValue().toString());
                lin.setText(dataSnapshot.child("lin").getValue().toString());
                try {
                    Picasso.with(Profile.this)
                            .load(dataSnapshot.child("url_photo_profile")
                                    .getValue().toString()).centerCrop().fit().into(foto_user);
                }catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        bttnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToMap = new Intent(Profile.this, MainMapAct.class);
                startActivity(goToMap);
                finish();
            }
        });

        bttnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToEditProfile = new Intent(Profile.this, EditProfileAct.class);
                startActivity(goToEditProfile);
            }
        });

        bttnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences(USERNAME_KEY, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(username_key, null);
                editor.apply();

                Intent goToLogin = new Intent(Profile.this, LoginAct.class);
                startActivity(goToLogin);
                finish();
            }
        });

    }

    private void getUsernameLocal(){
        SharedPreferences sharedPreferences = getSharedPreferences(USERNAME_KEY, MODE_PRIVATE);
        username_key_new = sharedPreferences.getString(username_key, "");
    }
}
