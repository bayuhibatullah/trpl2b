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
import java.util.List;

public class Register extends AppCompatActivity {

    LinearLayout bttn_back;
    Button bttnBuatAkun, bttnAddPhoto;
    EditText nama, username, password;
    Spinner lin;
    ImageView photoRegister;

    Uri photoLocation;
    int photoMax = 1;

    DatabaseReference reference;
    StorageReference storage;

    String USERNAME_KEY = "usernamekey";
    String username_key = "";
    private String linDipilih;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        bttn_back = findViewById(R.id.bttn_back);
        bttnBuatAkun = findViewById(R.id.bttnBuatAkun);
        nama = findViewById(R.id.nama);
        lin = findViewById(R.id.lin);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        bttnAddPhoto = findViewById(R.id.bttnAddPhoto);
        photoRegister = findViewById(R.id.photoRegister);
        lin = findViewById(R.id.lin);

        List<String> jenisLin = new ArrayList<>();
        jenisLin.add("A");
        jenisLin.add("B");
        jenisLin.add("C");
        jenisLin.add("D");
        jenisLin.add("AB");

        //Style the spinner
        ArrayAdapter<String> dataAdapter;
        dataAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, jenisLin);

        //dropdown layout stylr
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //implement data adapter ke spinner
        lin.setAdapter(dataAdapter);

        lin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                linDipilih = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bttnAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findPhoto();
            }
        });

        bttn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backToGetStarted = new Intent(Register.this, GetStartedAct.class);
                startActivity(backToGetStarted);
                finish();
            }
        });
        bttnBuatAkun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tombol buat akun gak bisa dipencet
                bttnBuatAkun.setEnabled(false);
                bttnBuatAkun.setText("Loading...");

                storage = FirebaseStorage.getInstance().getReference().child("foto_user").child(username.getText().toString());
                reference = FirebaseDatabase.getInstance().getReference().child("users").child(username.getText().toString());

                //cek ketersediaan username
                reference = FirebaseDatabase.getInstance().getReference().child("users").child(username.getText().toString());
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            Toast.makeText(getApplicationContext(),
                                    "Username sudah pernah digunakan", Toast.LENGTH_SHORT).show();

                            //tombol buat akun bisa dipencet lagi
                            bttnBuatAkun.setEnabled(true);
                            bttnBuatAkun.setText("BUAT AKUN");
                        }else {
                            //simpan pada local
                            SharedPreferences sharedPreferences = getSharedPreferences(USERNAME_KEY, MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(username_key, username.getText().toString());
                            editor.apply();

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
                                });
                            }reference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    dataSnapshot.getRef().child("nama").setValue(nama.getText().toString());
                                    dataSnapshot.getRef().child("lin").setValue(linDipilih);
                                    dataSnapshot.getRef().child("username").setValue(username.getText().toString());
                                    dataSnapshot.getRef().child("password").setValue(password.getText().toString());

                                    Intent goToProfile = new Intent(Register.this, SucceessRegister.class);
                                    startActivity(goToProfile);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });




                //Simpan data sebelum ada foto
//                reference.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        dataSnapshot.getRef().child("nama").setValue(nama.getText().toString());
//                        dataSnapshot.getRef().child("lin").setValue(lin.getText().toString());
//                        dataSnapshot.getRef().child("username").setValue(username.getText().toString());
//                        dataSnapshot.getRef().child("password").setValue(password.getText().toString());
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                    }
//                });

//                //Pindah Activity
//                Intent goToSuccess = new Intent(Register.this, SucceessRegister.class);
//                startActivity(goToSuccess);
//                finish();
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
            Picasso.with(this).load(photoLocation).centerCrop().fit().into(photoRegister);
        }
    }
}
