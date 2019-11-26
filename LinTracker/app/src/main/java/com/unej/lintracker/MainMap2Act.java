package com.unej.lintracker;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.BubbleLayout;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.OnLocationClickListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

/**
 * Display map property information for a clicked map feature.
 */
public class MainMap2Act extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, PermissionsListener, OnLocationClickListener,
        OnCameraTrackingChangedListener {

    private static final String GEOJSON_SOURCE_ID = "GEOJSON_SOURCE_ID";
    private static final String MARKER_IMAGE_ID = "MARKER_IMAGE_ID";
    private static final String CALLOUT_IMAGE_ID = "CALLOUT_IMAGE_ID";
    private static final String MARKER_LAYER_ID = "MARKER_LAYER_ID";
    private static final String CALLOUT_LAYER_ID = "CALLOUT_LAYER_ID";
    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private Point originPoint;
    private Point destinationPoint;
    private GeoJsonSource source;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    private LocationEngine locationEngine;
    private LocationChangeListeningActivityLocationCallback callback = new LocationChangeListeningActivityLocationCallback(this);
    private boolean isInTrackingMode;
    private ImageView tengah, profil;
    private TextView textPopup;

    Uri photoLocation;
    int photoMax = 1;
    private Dialog dialog;

    String USERNAME_KEY = "usernamekey";
    String username_key = "";
    String username_key_new = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main_map2);

        getUsernameLocal();

        mapView = findViewById(R.id.mapView);
        profil = findViewById(R.id.profil);

        listenerPengungguHalte();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("users").child(username_key_new);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    Picasso.with(MainMap2Act.this)
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
                Intent goToProfile = new Intent(MainMap2Act.this, Profile.class);
                startActivity(goToProfile);
            }
        });

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MainMap2Act.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/bayuhibatullah/ck20o699e0j3p1cp3mbzeeh4p"),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                        mapboxMap.addOnMapClickListener(MainMap2Act.this);
                        Toast.makeText(MainMap2Act.this,
                                getString(R.string.click_on_map_instruction), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Sets up all of the sources and layers needed for this example
     */
    public void setUpData() {
        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> {
                setupSource(style);
                setUpClickLocationIconImage(style);
                setUpClickLocationMarkerLayer(style);
                setUpInfoWindowLayer(style);
                addCalloutLayer(style);
            });
        }
    }

    /**
     * Adds the GeoJSON source to the map
     */
    private void setupSource(@NonNull Style loadedStyle) {
        source = new GeoJsonSource(GEOJSON_SOURCE_ID);
        loadedStyle.addSource(source);
    }

    /**
     * Adds the marker image to the map for use as a SymbolLayer icon
     */
    private void setUpClickLocationIconImage(@NonNull Style loadedStyle) {
        loadedStyle.addImage(MARKER_IMAGE_ID, BitmapFactory.decodeResource(
                this.getResources(), R.drawable.red_marker));
    }

    /**
     * Needed to show the Feature properties info window.
     */
    private void refreshSource(Feature featureAtClickPoint) {
        if (source != null) {
            source.setGeoJson(featureAtClickPoint);
        }
    }

    /**
     * Adds a SymbolLayer to the map to show the click location marker icon.
     */
    private void setUpClickLocationMarkerLayer(@NonNull Style loadedStyle) {
        loadedStyle.addLayer(new SymbolLayer(MARKER_LAYER_ID, GEOJSON_SOURCE_ID)
                .withProperties(
                        iconImage(MARKER_IMAGE_ID),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true),
                        iconOffset(new Float[] {0f, -8f})
                ));
    }

    /**
     * Adds a SymbolLayer to the map to show the Feature properties info window.
     */
    private void setUpInfoWindowLayer(@NonNull Style loadedStyle) {
        loadedStyle.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, GEOJSON_SOURCE_ID)
                .withProperties(
                        // show image with id title based on the value of the name feature property
                        iconImage(CALLOUT_IMAGE_ID),

                        // set anchor of icon to bottom-left
                        iconAnchor(ICON_ANCHOR_BOTTOM),

                        // prevent the feature property window icon from being visible even
                        // if it collides with other previously drawn symbols
                        iconAllowOverlap(true),

                        // prevent other symbols from being visible even if they collide with the feature property window icon
                        iconIgnorePlacement(true),

                        // offset the info window to be above the marker
                        iconOffset(new Float[] {-2f, -28f})
                ));
    }

    /**
     * This method handles click events for SymbolLayer symbols.
     *
     * param screenPoint the point on screen clicked
     */
    private void addCalloutLayer(@NonNull Style loadedStyle) {
        GeoJsonSource source = loadedStyle.getSourceAs("halte");
        loadedStyle.addImage(CALLOUT_IMAGE_ID, BitmapFactory.decodeResource(
                this.getResources(), R.drawable.red_marker));
        loadedStyle.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, CALLOUT_IMAGE_ID).withProperties(
                iconImage(CALLOUT_IMAGE_ID),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconOffset(new Float[] {0f, -8f}),
                iconSize(.2f)
        ));
        if (source != null){
            List<Feature> features = source.querySourceFeatures(Expression.get("halte"));
        }
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        return false;
    }

    /**
     * Invoked when the bitmap has been generated from a view.
     */
    public void setImageGenResults(HashMap<String, Bitmap> imageMap) {
        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> {
                style.addImages(imageMap);
            });
        }
    }

    /**
     * AsyncTask to generate Bitmap from Views to be used as iconImage in a SymbolLayer.
     * <p>
     * Call be optionally be called to update the underlying data source after execution.
     * </p>
     * <p>
     * Generating Views on background thread since we are not going to be adding them to the view hierarchy.
     * </p>
     */
    private static class GenerateViewIconTask extends AsyncTask<FeatureCollection, Void, HashMap<String, Bitmap>> {

        private final WeakReference<MainMap2Act> activityRef;
        private Feature featureAtMapClickPoint;

        GenerateViewIconTask(MainMap2Act activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected HashMap<String, Bitmap> doInBackground(FeatureCollection... params) {
            MainMap2Act activity = activityRef.get();
            HashMap<String, Bitmap> imagesMap = new HashMap<>();
            if (activity != null) {
                LayoutInflater inflater = LayoutInflater.from(activity);

                if (params[0].features() != null) {
                    featureAtMapClickPoint = params[0].features().get(0);

                    StringBuilder stringBuilder = new StringBuilder();

                    BubbleLayout bubbleLayout = (BubbleLayout) inflater.inflate(
                            R.layout.activity_query_feature_window_symbol_layer, null);

                    TextView titleTextView = bubbleLayout.findViewById(R.id.info_window_title);
                    titleTextView.setText(activity.getString(R.string.query_feature_marker_title));

                    if (featureAtMapClickPoint.properties() != null) {
                        for (Map.Entry<String, JsonElement> entry : featureAtMapClickPoint.properties().entrySet()) {
                            stringBuilder.append(String.format("%s - %s", entry.getKey(), entry.getValue()));
                            stringBuilder.append(System.getProperty("line.separator"));
                        }

                        TextView propertiesListTextView = bubbleLayout.findViewById(R.id.info_window_feature_properties_list);
                        propertiesListTextView.setText(stringBuilder.toString());

                        int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        bubbleLayout.measure(measureSpec, measureSpec);

                        float measuredWidth = bubbleLayout.getMeasuredWidth();

                        bubbleLayout.setArrowPosition(measuredWidth / 2 - 5);

                        Bitmap bitmap = MainMap2Act.SymbolGenerator.generate(bubbleLayout);
                        imagesMap.put(CALLOUT_IMAGE_ID, bitmap);
                    }
                }
            }

            return imagesMap;
        }

        @Override
        protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
            super.onPostExecute(bitmapHashMap);
            MainMap2Act activity = activityRef.get();
            if (activity != null && bitmapHashMap != null) {
                activity.setImageGenResults(bitmapHashMap);
                activity.refreshSource(featureAtMapClickPoint);
            }
        }

    }

    /**
     * Utility class to generate Bitmaps for Symbol.
     */
    private static class SymbolGenerator {

        /**
         * Generate a Bitmap from an Android SDK View.
         *
         * @param view the View to be drawn to a Bitmap
         * @return the generated bitmap
         */
        static Bitmap generate(@NonNull View view) {
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(measureSpec, measureSpec);

            int measuredWidth = view.getMeasuredWidth();
            int measuredHeight = view.getMeasuredHeight();

            view.layout(0, 0, measuredWidth, measuredHeight);
            Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.TRANSPARENT);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        }
    }

//    FIND LOKASI DEVICE

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
                        Toast.makeText(MainMap2Act.this, getString(R.string.tracking_enabled),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainMap2Act.this, getString(R.string.tracking_already_enabled),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
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

    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MainMap2Act> activityWeakReference;

        LocationChangeListeningActivityLocationCallback(MainMap2Act activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            MainMap2Act activity = activityWeakReference.get();

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
                Toast.makeText(activity, String.format(activity.getString(R.string.new_location),
                        String.valueOf(result.getLastLocation().getLatitude()),
                        String.valueOf(result.getLastLocation().getLongitude())),
                        Toast.LENGTH_SHORT).show();

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
            MainMap2Act activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
        }
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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
                String idHalte = reference.push().getKey();
                Toast.makeText(getApplicationContext(), idHalte, Toast.LENGTH_SHORT).show();
//                String jumlahPenunggu = dataSnapshot.child(idHalte).getValue().toString();
//                showPopUp(idHalte, jumlahPenunggu);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showPopUp(String idHalte, String jumlahPengunggu){
        dialog.setContentView(R.layout.popup_notification);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        textPopup = dialog.findViewById(R.id.textPopup);
        textPopup.setText("Terdapat " + jumlahPengunggu + " penumpang yang menunggu anda di halte"
        + idHalte);

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
        },2000);
    }
}
