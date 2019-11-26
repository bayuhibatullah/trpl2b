package com.unej.lintracker;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.OnLocationClickListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Use the LocationComponent to easily add a device location "puck" to a Mapbox map.
 */
public class MainMapAct extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener, OnLocationClickListener, OnCameraTrackingChangedListener {

    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private Button tengah;
    private LocationComponent locationComponent;
    private boolean isInTrackingMode;
    private ImageView profil;
    private TextView textPopup, xkodim, xayani, xalun, xsmp2;
    private Button kursi, list;
    private LocationEngine locationEngine;
    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private MainMapAct.LocationChangeListeningActivityLocationCallback callback = new MainMapAct.LocationChangeListeningActivityLocationCallback(this);

    Uri photoLocation;
    int photoMax = 1;
    String USERNAME_KEY = "usernamekey";
    String username_key = "";
    String username_key_new = "";
    String kodim, ayani, smp2, alun;

    DatabaseReference reference;

    LoginAct loginAct = new LoginAct();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main_map);

        profil = findViewById(R.id.profil);
        mapView = findViewById(R.id.mapView);
        list = findViewById(R.id.list);
        kursi = findViewById(R.id.kursi);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        getUsernameLocal();

        listenerPengungguHalte();
        reference = FirebaseDatabase.getInstance().getReference().child("users").child(username_key_new);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    Picasso.with(MainMapAct.this)
                            .load(dataSnapshot.child("url_photo_profile")
                                    .getValue().toString()).centerCrop().fit().into(profil);
                }catch (Exception e){

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        profil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToProfile = new Intent(MainMapAct.this, Profile.class);
                startActivity(goToProfile);
            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupList();
            }
        });

        kursi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kursi();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MainMapAct.this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/bayuhibatullah/ck20o699e0j3p1cp3mbzeeh4p"),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                    }
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            locationComponent = mapboxMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            // Add the camera tracking listener. Fires if the map camera is manually moved.
            locationComponent.addOnCameraTrackingChangedListener(this);

            initLocationEngine();

            tengah = findViewById(R.id.tengah);
            tengah.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isInTrackingMode) {
                        isInTrackingMode = true;
                        locationComponent.setCameraMode(CameraMode.TRACKING);
                        locationComponent.setRenderMode(RenderMode.COMPASS);
                        locationComponent.zoomWhileTracking(16);
                        Toast.makeText(MainMapAct.this, getString(R.string.tracking_enabled),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainMapAct.this, getString(R.string.tracking_already_enabled),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onCameraTrackingDismissed() {
        isInTrackingMode=false;
    }

    @Override
    public void onCameraTrackingChanged(int currentMode) {

    }

    @Override
    public void onLocationComponentClick() {
        if (locationComponent.getLastKnownLocation() != null) {
            Toast.makeText(this, String.format(getString(R.string.current_location),
                    locationComponent.getLastKnownLocation().getLatitude(),
                    locationComponent.getLastKnownLocation().getLongitude()), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MainMapAct> activityWeakReference;

        LocationChangeListeningActivityLocationCallback(MainMapAct activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            MainMapAct activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("posisi");
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        dataSnapshot.getRef().child("latitude").setValue(result.getLastLocation().getLatitude());
                        dataSnapshot.getRef().child("longitude").setValue(result.getLastLocation().getLongitude());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

// Create a Toast which displays the new location's coordinates
//                Toast.makeText(activity, String.format(activity.getString(R.string.new_location),
//                        String.valueOf(result.getLastLocation().getLatitude()),
//                        String.valueOf(result.getLastLocation().getLongitude())),
//                        Toast.LENGTH_SHORT).show();

// Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d("LocationChangeActivity", exception.getLocalizedMessage());
            MainMapAct activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void getUsernameLocal(){
        SharedPreferences sharedPreferences = getSharedPreferences(USERNAME_KEY, MODE_PRIVATE);
        username_key_new = sharedPreferences.getString(username_key, "");
    }

    private void listenerPengungguHalte(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("halte");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String idHalte = dataSnapshot.child("nama halte dipilih").getValue().toString();
                int jumlahPenunggu = Integer.parseInt(dataSnapshot.child(idHalte).getValue().toString());
                if (jumlahPenunggu > 0){
                    showPopUp(idHalte, jumlahPenunggu);
                }else{
                    showPopUp(idHalte, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showPopUp(String idHalte, int jumlahPengunggu){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_notification);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        textPopup = dialog.findViewById(R.id.textPopup);
        if (jumlahPengunggu > 0){
            textPopup.setText(jumlahPengunggu + " penumpang menunggumu di halte "
                    + idHalte);
        }else{
            textPopup.setText("Halte " + idHalte + " kosong");
        }


        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);

        dialog.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        },5000);
    }

    private void showPopupList(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_list);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        xalun = dialog.findViewById(R.id.xalun);
        xayani = dialog.findViewById(R.id.xayani);
        xkodim = dialog.findViewById(R.id.xkodim);
        xsmp2 = dialog.findViewById(R.id.xsmp2);

        DatabaseReference referenceHalte = FirebaseDatabase.getInstance().getReference().child("halte");
        referenceHalte.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                xkodim.setText("Kodim : " + dataSnapshot.child("\"Kodim\"").getValue().toString());
                xayani.setText("Ahmad Yani : " + dataSnapshot.child("\"Ahmad Yani\"").getValue().toString());
                xalun.setText("Alun-Alun Jember : " + dataSnapshot.child("\"Alun-Alun Jember\"").getValue().toString());
                xsmp2.setText("SMPN 2 Jember : " + dataSnapshot.child("\"SMPN 2 Jember\"").getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        dialog.show();
    }

    private void kursi(){
        if (kursi.getText().toString().equalsIgnoreCase("KURSI : KOSONG")){
            kursi.setText("KURSI : FULL");
            kursi.setBackgroundResource(R.drawable.kursi_full);
        }else {
            kursi.setText("KURSI : KOSONG");
            kursi.setBackgroundResource(R.drawable.kursi_kosong);
        }
    }
}