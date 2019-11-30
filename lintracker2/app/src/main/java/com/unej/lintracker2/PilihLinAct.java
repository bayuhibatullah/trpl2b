package com.unej.lintracker2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PilihLinAct extends AppCompatActivity {

    Spinner xlin, xarah;
    Button bttnPilihHalte;
    String linDipilih = "A";
    String arahDipilih;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pilih_lin);

        init();
        setupLin();

        bttnPilihHalte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (arahDipilih.equalsIgnoreCase("--Pilih Arah--")){
                    Toast.makeText(getApplicationContext(), "Mohon pilih arah", Toast.LENGTH_SHORT).show();
                }else {
                    Intent goToMap = new Intent(PilihLinAct.this, MainMap2Act.class);
                    startActivity(goToMap);
                    finish();
                }
            }
        });
    }

    private void init(){
        xlin = findViewById(R.id.xlin);
        xarah = findViewById(R.id.xarah);
        bttnPilihHalte = findViewById(R.id.bttnPilihHalte);
    }

    private void setupLin(){
        final List<String> jenisLin = new ArrayList<>();
        jenisLin.add("A");
        jenisLin.add("B");
        jenisLin.add("C");
        jenisLin.add("D");
        jenisLin.add("AB");

        //Style the spinner
        final ArrayAdapter<String> dataAdapterLin;
        dataAdapterLin = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, jenisLin);

        //dropdown layout style
        dataAdapterLin.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        xlin.setAdapter(dataAdapterLin);
        xlin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                linDipilih = parent.getItemAtPosition(position).toString();
                setupArah(linDipilih);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupArah(String lin){
        final List<String> arah = new ArrayList<>();
        arah.add("--Pilih Arah--");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("arah").child(lin);
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
    }
}
