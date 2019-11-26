package com.unej.lintracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EditProfileAct extends AppCompatActivity {

    LinearLayout bttn_back;
    Button bttnSimpan, bttnAddNewPhoto;
    ImageView foto_user_edit;
    EditText xnama, xusername, xpassword;
    Spinner xlin;


    Uri photoLocation;
    int photoMax = 1;

    DatabaseReference reference;
    StorageReference storage;

    String USERNAME_KEY = "usernamekey";
    String username_key = "";
    String username_key_new = "";
    private String linDipilih;

    MainMapAct mainMapAct = new MainMapAct();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        bttn_back = findViewById(R.id.bttn_back);
        bttnSimpan = findViewById(R.id.bttnSimpan);
        bttnAddNewPhoto = findViewById(R.id.bttnAddNewPhoto);
        xnama = findViewById(R.id.xnama);
        xlin = findViewById(R.id.xlin);
        xusername = findViewById(R.id.xusername);
        xpassword = findViewById(R.id.xpassword);
        foto_user_edit = findViewById(R.id.foto_user_edit);

        final List<String> jenisLin = new ArrayList<>();
        jenisLin.add("A");
        jenisLin.add("B");
        jenisLin.add("C");
        jenisLin.add("D");
        jenisLin.add("AB");

        //Style the spinner
        final ArrayAdapter<String> dataAdapter;
        dataAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, jenisLin);

        //dropdown layout stylr
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        xlin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                linDipilih = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        getUsernameLocal();

        reference = FirebaseDatabase.getInstance().getReference().child("users").child(username_key_new);
        storage = FirebaseStorage.getInstance().getReference().child("foto_user").child(username_key_new);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                xnama.setText(dataSnapshot.child("nama").getValue().toString());
                xusername.setText(dataSnapshot.child("username").getValue().toString());
                xpassword.setText(dataSnapshot.child("password").getValue().toString());
                String lintemp = dataSnapshot.child("lin").getValue().toString();
                if (jenisLin.get(0).equalsIgnoreCase(lintemp)){
                    //implement data adapter ke spinner
                    xlin.setAdapter(dataAdapter);
                }else {
                    int index = jenisLin.indexOf(lintemp);
                    Collections.swap(jenisLin, index, 0);
                    //implement data adapter yang telah di-swap ke spinner
                    xlin.setAdapter(dataAdapter);
                }

                try {
                    Picasso.with(EditProfileAct.this)
                            .load(dataSnapshot.child("url_photo_profile")
                                    .getValue().toString()).centerCrop().fit().into(foto_user_edit);
                }catch (Exception e){

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        bttnSimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tombol buat akun gak bisa dipencet
                bttnSimpan.setEnabled(false);
                bttnSimpan.setText("Loading...");

                if (photoLocation != null){
                    final StorageReference storageReference1 = storage.child(System.currentTimeMillis()
                            + "." + getFileExtension(photoLocation));

                    storageReference1.putFile(photoLocation).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            storageReference1.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String uriPhoto = uri.toString();
                                    reference.getRef().child("url_photo_profile").setValue(uriPhoto);
                                }
                            });
                        }
                    }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            reference.getRef().child("nama").setValue(xnama.getText().toString());
                            reference.getRef().child("lin").setValue(linDipilih);
                            reference.getRef().child("username").setValue(xusername.getText().toString());
                            reference.getRef().child("password").setValue(xpassword.getText().toString());

                            Intent goToProfile = new Intent(EditProfileAct.this, Profile.class);
                            startActivity(goToProfile);
                        }
                    });
                }else {
                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            dataSnapshot.getRef().child("nama").setValue(xnama.getText().toString());
                            dataSnapshot.getRef().child("lin").setValue(linDipilih);
                            dataSnapshot.getRef().child("username").setValue(xusername.getText().toString());
                            dataSnapshot.getRef().child("password").setValue(xpassword.getText().toString());

                            Intent goToProfile = new Intent(EditProfileAct.this, Profile.class);
                            startActivity(goToProfile);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
        });

        bttnAddNewPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findPhoto();
            }
        });

        bttn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backToProfile = new Intent(EditProfileAct.this, Profile.class);
                startActivity(backToProfile);
                finish();
            }
        });

    }

    String getFileExtension (Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void findPhoto(){
        Intent pic = new Intent();
        pic.setType("image/*");
        pic.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(pic, photoMax);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == photoMax && resultCode == RESULT_OK && data != null && data.getData() != null){
            photoLocation = data.getData();
            Picasso.with(this).load(photoLocation).centerCrop().fit().into(foto_user_edit);
        }
    }

    private void getUsernameLocal(){
        SharedPreferences sharedPreferences = getSharedPreferences(USERNAME_KEY, MODE_PRIVATE);
        username_key_new = sharedPreferences.getString(username_key, "");
    }
}
