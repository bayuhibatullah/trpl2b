package com.unej.lintracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class Profile extends AppCompatActivity {

    Button on, bttnEditProfile, bttnSignOut, bttnMulai;
    ImageView foto_user;
    TextView nama, username, lin;
    Spinner xarah;

    DatabaseReference reference;

    String USERNAME_KEY = "usernamekey";
    String username_key = "";
    String username_key_new = "";
    String linYangLogin, arahDipilih;

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
        on = findViewById(R.id.on);

        reference = FirebaseDatabase.getInstance().getReference().child("users").child(username_key_new);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nama.setText(dataSnapshot.child("nama").getValue().toString());
                username.setText(dataSnapshot.child("username").getValue().toString());
                lin.setText(dataSnapshot.child("lin").getValue().toString());
                linYangLogin = dataSnapshot.child("lin").getValue().toString();
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

        on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupArah();
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

    private void showPopupArah(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_arah);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        xarah = dialog.findViewById(R.id.xarah);
        bttnMulai = dialog.findViewById(R.id.bttnMulai);

        final List<String> arah = new ArrayList<>();
        arah.add("--Pilih Arah--");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("arah").child(linYangLogin);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                arah.add(dataSnapshot.child("arah 1").getValue().toString());
                arah.add(dataSnapshot.child("arah 2").getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Style the spinner
        final ArrayAdapter<String> dataAdapterArah;
        dataAdapterArah = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, arah);

        //dropdown layout style
        dataAdapterArah.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        xarah.setAdapter(dataAdapterArah);
        xarah.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                arahDipilih = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        dialog.show();

        bttnMulai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (arahDipilih.equalsIgnoreCase("--Pilih Arah--")){
                    Toast.makeText(getApplicationContext(), "Mohon pilih arah", Toast.LENGTH_SHORT).show();
                }else {
                    DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference().child("status");
                    reference2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            dataSnapshot.getRef().setValue("aktif");
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    Intent goToMap = new Intent(Profile.this, MainMapAct.class);
                    startActivity(goToMap);
                    finish();
                }
            }
        });
    }
}
