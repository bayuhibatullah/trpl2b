package com.unej.lintracker2;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.BubbleLayout;
import com.mapbox.mapboxsdk.geometry.LatLng;
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
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Display map property information for a clicked map feature.
 */
public class MainMap2Act extends AppCompatActivity implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, PermissionsListener, OnLocationClickListener,
        OnCameraTrackingChangedListener {

    private static final String GEOJSON_SOURCE_ID = "GEOJSON_SOURCE_ID";
    private static final String DRIVER_SOURCE_ID = "DRIVER_SOURCE_ID";
    private static final String MARKER_IMAGE_ID = "MARKER_IMAGE_ID";
    private static final String DRIVER_IMAGE_ID = "DRIVER_IMAGE_ID";
    private static final String CALLOUT_IMAGE_ID = "CALLOUT_IMAGE_ID";
    private static final String MARKER_LAYER_ID = "MARKER_LAYER_ID";
    private static final String DRIVER_LAYER_ID = "DRIVER_LAYER_ID";
    private static final String CALLOUT_LAYER_ID = "CALLOUT_LAYER_ID";
    private int penunggu;
    private boolean halteDipilih = false;
    private String namaHalteDipilih;
    private Point originPoint;
    private Point destinationPoint;
    private GeoJsonSource source;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    private boolean isInTrackingMode;
    private ImageView tengah;
    private Button buttonPilihHalte, tidakPindah, yaPindah;
    private DatabaseReference reference;
    private TextView textpindah;
    private PointF tempScreenPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main_map2);

        reference = FirebaseDatabase.getInstance().getReference().child("posisi");

        buttonPilihHalte = findViewById(R.id.buttonPilihHalte);

        mapView = findViewById(R.id.mapView);
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
                setUpData();
                setUpDriverLayer(style);
                linVisibility(style);
                enableLocationComponent(style);
                driverGeoJsonUpdate();
                mapboxMap.addOnMapClickListener(MainMap2Act.this);
                Toast.makeText(MainMap2Act.this,
                        getString(R.string.click_on_map_instruction), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUpDriverLayer(Style loadedStyle){
        loadedStyle.addImage(DRIVER_IMAGE_ID, BitmapFactory.decodeResource(
                this.getResources(), R.drawable.car_yellow));

        loadedStyle.addSource(new GeoJsonSource(DRIVER_SOURCE_ID));

        loadedStyle.addLayer(new SymbolLayer(DRIVER_LAYER_ID, DRIVER_SOURCE_ID).withProperties(
                iconImage(DRIVER_IMAGE_ID),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconOffset(new Float[] {0f, -8f}),
                iconSize(.2f),
                visibility(NONE)
        ));
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
        loadedStyle.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, GEOJSON_SOURCE_ID).withProperties(
                // show image with id title based on the value of the name feature property
                iconImage(CALLOUT_IMAGE_ID),

                // set anchor of icon to bottom-left
                iconAnchor(ICON_ANCHOR_BOTTOM),

                // prevent the feature property window icon from being visible even
                // if it collides with other previously drawn symbols
                iconAllowOverlap(false),

                // prevent other symbols from being visible even if they collide with the feature property window icon
                iconIgnorePlacement(false),

                // offset the info window to be above the marker
                iconOffset(new Float[] {-2f, -28f})
        ));
    }

    private void driverGeoJsonUpdate(){
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GeoJsonSource driverSource = mapboxMap.getStyle().getSourceAs(DRIVER_SOURCE_ID);
                String driverLatitude = dataSnapshot.child("latitude").getValue().toString();
                String driverLongitude = dataSnapshot.child("longitude").getValue().toString();
                if (mapboxMap.getStyle() != null) {
                    if (driverSource != null) {
                        driverSource.setGeoJson(FeatureCollection.fromFeature(
                                Feature.fromGeometry(Point.fromLngLat(Double.parseDouble(driverLongitude)
                                        , Double.parseDouble(driverLatitude)))
                        ));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    /**
     * This method handles click events for SymbolLayer symbols.
     *
     * @param screenPoint the point on screen clicked
     */
    private boolean handleClickIcon(PointF screenPoint) {
        tempScreenPoint = screenPoint;
        if (buttonPilihHalte.getText().toString().equalsIgnoreCase("batal pilih halte")){
            showPopUp(namaHalteDipilih);
        }else{
            List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, "halte");
            if (!features.isEmpty()) {
                Feature feature = features.get(0);

                pilihHaltre();

                StringBuilder stringBuilder = new StringBuilder();
                StringBuilder stringBuilder2 = new StringBuilder();

                if (feature.properties() != null) {
                    for (Map.Entry<String, JsonElement> entry : feature.properties().entrySet()) {
                        stringBuilder.append(String.format("%s - %s", entry.getKey(), entry.getValue()));
                        stringBuilder2.append(entry.getValue());
                        namaHalteDipilih = stringBuilder2.toString();
//                    break;
                    }
                    new GenerateViewIconTask(MainMap2Act.this).execute(FeatureCollection.fromFeature(feature));
                }
            } else {
                Toast.makeText(this, getString(R.string.query_feature_no_properties_found), Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        return handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
    }

    private void pilihHaltre(){
        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference().child("halte");
        buttonPilihHalte.setEnabled(true);
        buttonPilihHalte.setBackgroundResource(R.drawable.bttn_primary);
        buttonPilihHalte.setTextColor(Color.parseColor("#FFFFFFFF"));
        buttonPilihHalte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reference2.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        penunggu = dataSnapshot.child(namaHalteDipilih).getValue(Integer.class);
                        if (buttonPilihHalte.getText().toString().equalsIgnoreCase("batal pilih halte")){
                            dataSnapshot.getRef().child("nama halte dipilih").setValue(namaHalteDipilih);
                            dataSnapshot.getRef().child(namaHalteDipilih).setValue(penunggu - 1);
                            buttonPilihHalte.setText("Pilih Halte");
                            buttonPilihHalte.setBackgroundResource(R.drawable.bttn_primary);
                            halteDipilih = false;
                        }else{
                            dataSnapshot.getRef().child("nama halte dipilih").setValue(namaHalteDipilih);
                            dataSnapshot.getRef().child(namaHalteDipilih).setValue(penunggu + 1);
                            buttonPilihHalte.setText("Batal Pilih Halte");
                            buttonPilihHalte.setBackgroundResource(R.drawable.bttn_batal_pilih_halte);
                            halteDipilih = true;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        });
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

    private void showPopUp(String namahalte){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_konfirmasi_pindah_halte);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        textpindah = dialog.findViewById(R.id.textpindah);
        yaPindah = dialog.findViewById(R.id.yaPindah);
        tidakPindah = dialog.findViewById(R.id.tidakPindah);
        textpindah.setText("Anda telah memilih halte " + namaHalteDipilih + ". Apakah anda yakin ingin pindah halte?");
        String halteTemp = namaHalteDipilih;

        dialog.show();

        yaPindah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference().child("halte");
                reference2.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        penunggu = dataSnapshot.child(halteTemp).getValue(Integer.class);
                        dataSnapshot.getRef().child(halteTemp).setValue(penunggu - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                buttonPilihHalte.setText("Pilih Halte");
                dialog.dismiss();
                handleClickIcon(tempScreenPoint);
            }
        });

        tidakPindah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void linVisibility(Style style){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("status");
        Layer layer = style.getLayer(DRIVER_LAYER_ID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue().toString().equalsIgnoreCase("aktif")){
                    layer.setProperties(visibility(VISIBLE));
                }else{
                    layer.setProperties(visibility(NONE));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
