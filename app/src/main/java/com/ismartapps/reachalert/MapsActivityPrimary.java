package com.ismartapps.reachalert;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.SphericalUtil;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.android.FlutterActivityLaunchConfigs;

public class MapsActivityPrimary extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private ImageView mCurrLoc, zoomIn, zoomOut, userPic, mapType;
    private CardView mLocationTick;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String TAG = "MapsActivityPrimary";
    private FusedLocationProviderClient mFusedLocationClient;
    private static float zoom = 20f;
    private TextView targetPlaceName, targetPlaceType, targetPlaceAddress, userName, userEmail, confirm;
    private ImageView[] targetPlaceImages = new ImageView[4];
    private LinearLayout targetPlacePhotosScrollViewLinearLayout;
    private CardView searchContainer;
    private RelativeLayout placeDetailsContainer;
    public LatLng currentLocationLatlng = null, targetLatLng;
    private static Marker marker = null;
    private Bitmap[] targetPlacebitmaps = new Bitmap[4];
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TargetDetails targetDetails;
    private boolean fromNotification = false, dark, fromShared;
    private int created = 0, back = 0;
    private Intent mainIntent;
    private String targetPlaceId = null;
    private Menu menu;
    private SharedPreferences searched;
    private SharedPreferences recents;
    private SharedPreferences settings;
    private AutocompleteSupportFragment autocompleteFragment;
    private List<String> notPermittedPermission = new ArrayList<String>();
    int askignBgLocPerms = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        dark = settings.getBoolean("dark", false);

        if (dark)
            setContentView(R.layout.activity_maps_dark);
        else {
            setTheme(R.style.AppTheme);
            setContentView(R.layout.activity_maps);
        }
        initVars();
        searched = getSharedPreferences("searched", MODE_PRIVATE);
        recents = getSharedPreferences("recent", MODE_PRIVATE);
        updateSearchMenu();

        scheduleNotification();

        if (!checkPermissions()) {
            Log.d(TAG, "Permission asking");
            askPermission();
        } else {
            checkLocation();
        }

//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).update("last_opened", FieldValue.serverTimestamp(), "devices", FieldValue.arrayUnion(Build.BRAND +" - "+ Build.DEVICE +" - "+ Build.HARDWARE), "versions", FieldValue.arrayUnion("4.4"))
//                .addOnCompleteListener(t -> {
//                    Log.d(TAG, "onCreate: Last Opened updated - "+t.isSuccessful());
//                });
    }

    private void updateSearched(String locationName, String placeId) {
        SharedPreferences.Editor editor = searched.edit();
        if (!placeId.equals(searched.getString("searched_one_pid", "Searched Location"))
                && !placeId.equals(searched.getString("searched_two_pid", "Searched Location"))
                && !placeId.equals(searched.getString("searched_three_pid", "Searched Location"))
        ) {
            editor.putString("searched_three", searched.getString("searched_two", "Searched Location"));
            editor.putString("searched_three_pid", searched.getString("searched_two_pid", ""));
            editor.putString("searched_two", searched.getString("searched_one", "Searched Location"));
            editor.putString("searched_two_pid", searched.getString("searched_one_pid", ""));
            editor.putString("searched_one", locationName);
            editor.putString("searched_one_pid", placeId);
            editor.apply();
        } else if (placeId.equals(searched.getString("searched_two_pid", "Searched Location"))) {
            editor.putString("searched_two", searched.getString("searched_one", "Searched Location"));
            editor.putString("searched_two_pid", searched.getString("searched_one_pid", ""));
            editor.putString("searched_one", locationName);
            editor.putString("searched_one_pid", placeId);
            editor.apply();
        }
        updateSearchMenu();
    }

    private void updateSearchMenu() {
        MenuItem[] searchList = {menu.findItem(R.id.searched_one), menu.findItem(R.id.searched_two), menu.findItem(R.id.searched_three)};
        MenuItem clear = menu.findItem(R.id.clear_search);
        searchList[0].setTitle(searched.getString("searched_one", "Searched Location"));
        searchList[1].setTitle(searched.getString("searched_two", "Searched Location"));
        searchList[2].setTitle(searched.getString("searched_three", "Searched Location"));
        for (int i = 0; i < 3; i++) {
            if (searchList[i].getTitle().equals("Searched Location")) {
                if (i != 0)
                    searchList[i].setVisible(false);
                else {
                    searchList[i].setTitle("No Recent Searches");
                    clear.setVisible(false);
                }
            } else {
                searchList[i].setVisible(true);
                clear.setVisible(true);
            }
        }
    }

    private void updateRecentMenu() {
        MenuItem[] recentList = {menu.findItem(R.id.recent_one), menu.findItem(R.id.recent_two), menu.findItem(R.id.recent_three)};
        MenuItem clear = menu.findItem(R.id.clear_reached);
        recentList[0].setTitle(recents.getString("recent_one", "Recent Location"));
        recentList[1].setTitle(recents.getString("recent_two", "Recent Location"));
        recentList[2].setTitle(recents.getString("recent_three", "Recent Location"));
        for (int i = 0; i < 3; i++) {
            if (recentList[i].getTitle().equals("Recent Location")) {
                if (i != 0)
                    recentList[i].setVisible(false);
                else {
                    recentList[i].setTitle("No Recent Locations");
                    clear.setVisible(false);
                }
            } else {
                recentList[i].setVisible(true);
                clear.setVisible(true);
            }
        }
        if (recents.getBoolean("Reached Location?", false)) {
            SharedPreferences.Editor editor = recents.edit();
            editor.putBoolean("Reached Location?", false);
            editor.apply();
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.icon_round)
                    .setTitle("Reached Successfully")
                    .setMessage("Reach Alert successfully alerted you on reaching your location.")
                    .setPositiveButton("Share", (dialogInterface, i) -> {
                        Toast.makeText(MapsActivityPrimary.this, "Share", Toast.LENGTH_SHORT).show();
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, "Hey,\n\nReach Alert alerts you on reaching a location you desire when you are not aware.\nBest app for people who often tend to fall asleep during travelling.\nJust Set and Sleep\n\nDownload from Play Store - http://bit.ly/reachAlert");
                        sendIntent.setType("text/plain");
                        Intent chooser = Intent.createChooser(sendIntent, "Share");
                        startActivity(chooser);
                    })
                    .setNegativeButton("Dismiss", (dialogInterface, i) -> {
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        back = 0;
        Log.d(TAG, "onResume");

        mainIntent = getIntent();
        Log.d(TAG, "onertyu" + mainIntent);
        if (mainIntent != null && mainIntent.getStringExtra("name") != null) {
            Log.d(TAG, "onCreate: fromNot");
            fromNotification = true;
        } else if (mainIntent != null && mainIntent.getStringExtra("shared location") != null) {
            Log.d(TAG, "onCreate:----shared ");
            fromShared = true;
        }

        updateRecentMenu();
        checkLocation();
    }

    @Override
    public void onBackPressed() {
        if (marker == null) {
            if (back == 0) {
                back++;
                final Handler handler = new Handler();
                handler.postDelayed(() -> back = 0, 1000);
                Toast.makeText(this, "Press Back again to exit", Toast.LENGTH_SHORT).show();
            } else {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
        } else
            getDeviceLocation(1);
    }

    private void init() {

        Log.d(TAG, "init:(Primary) initializing");

        navigationView.setVisibility(View.VISIBLE);
        mapType.setVisibility(View.VISIBLE);

        mCurrLoc.setOnClickListener(v -> {
            Log.d(TAG, "onClick:(Primary) Location Button is Clicked");
            getDeviceLocation(1);
        });

        mMap.setOnMapLongClickListener(latLng -> {
            Log.d(TAG, "onMapLongClick: Long Clicked On Map Moving Camera to " + latLng);
            targetPlaceId = null;
            String address = getAddressFromMarker(latLng);
            movecamera(latLng, mMap.getCameraPosition().zoom, "Dropped Pin", address, "Point of interest");
        });

        mMap.setOnPoiClickListener(pointOfInterest -> {
            Log.d(TAG, "onPoiClick: (Primary) Clicked on " + pointOfInterest.name);
            targetPlaceId = pointOfInterest.placeId;
            fetchPlaceDetails(pointOfInterest.placeId);

        });

        mMap.setOnCameraMoveStartedListener(i -> {
//            searchContainer.setVisibility(View.INVISIBLE);
            searchContainer.setAlpha((float) 0.3);
//            placeDetailsContainer.setVisibility(View.INVISIBLE);
            placeDetailsContainer.setAlpha((float) 0.3);
            mLocationTick.setVisibility(View.INVISIBLE);
        });

        mMap.setOnCameraIdleListener(() -> {

//            searchContainer.setVisibility(View.VISIBLE);
            searchContainer.setAlpha(1);
//            placeDetailsContainer.setVisibility(View.VISIBLE);
            placeDetailsContainer.setAlpha(1);
            if (marker != null)
                mLocationTick.setVisibility(View.VISIBLE);

            if (created == 0) {
                created++;
                drawerLayout.openDrawer(GravityCompat.START, true);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        drawerLayout.closeDrawer(GravityCompat.START, true);
                        }
                }, 500);
            }

            if (mLocationTick.getVisibility() == View.VISIBLE) {
                Log.d(TAG, "onCameraIdle");
//                Animation animation = AnimationUtils.loadAnimation(MapsActivityPrimary.this,R.anim.tick_anim);
//                mLocationTick.startAnimation(animation);
            }
        });

        mapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.getMapType() == 1) {
                    mMap.setMapType(2);
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(mMap.getCameraPosition().target).zoom(mMap.getCameraPosition().zoom).tilt(0f).build()));
                } else {
                    mMap.setMapType(1);
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(mMap.getCameraPosition().target).zoom(mMap.getCameraPosition().zoom).tilt(32f).build()));
                }
            }
        });

        autocomplete();

        MenuItem theme = menu.findItem(R.id.theme);
        Switch aSwitch = (Switch) theme.getActionView();
        if (dark) {
            aSwitch.setChecked(true);
        } else {
            aSwitch.setChecked(false);
        }

        aSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            dark = b;
            drawerLayout.closeDrawer(GravityCompat.START, true);
            SharedPreferences.Editor themeEditor = settings.edit();
            themeEditor.putBoolean("dark", dark);
            themeEditor.apply();
            changeTheme();
        });

        mLocationTick.setOnLongClickListener(view -> {
            Toast.makeText(MapsActivityPrimary.this, " Confirm Location", Toast.LENGTH_SHORT).show();
            return true;
        });

        mLocationTick.setOnClickListener(v -> {
            Log.d(TAG, "onClick: (Primary) Target Location Confirmed : (Primary) TPID" + targetLatLng + targetPlaceId);
            String trackId = Utils.generateId("tr_");
            Log.d(TAG, "init: "+trackId);
            targetDetails = new TargetDetails(trackId,targetPlaceName.getText().toString(), targetPlaceType.getText().toString(), targetPlaceAddress.getText().toString(), new double[]{currentLocationLatlng.latitude, currentLocationLatlng.longitude}, new double[]{targetLatLng.latitude, targetLatLng.longitude}, targetPlaceId);
            showDialog();
        });

        confirm.setOnClickListener(view -> {
            Log.d(TAG, "onClick: (Primary) Target Location Confirmed : (Primary) TPID" + targetLatLng + targetPlaceId);
            String trackId = Utils.generateId("tr_");
            Log.d(TAG, "init: "+trackId);
            targetDetails = new TargetDetails(trackId,targetPlaceName.getText().toString(), targetPlaceType.getText().toString(), targetPlaceAddress.getText().toString(), new double[]{currentLocationLatlng.latitude, currentLocationLatlng.longitude}, new double[]{targetLatLng.latitude, targetLatLng.longitude}, targetPlaceId);
            showDialog();
        });

        zoomOut.setOnClickListener(view -> {
            CameraPosition zoomOutPosition = new CameraPosition.Builder()
                    .target(mMap.getCameraPosition().target)
                    .bearing(mMap.getCameraPosition().bearing)
                    .tilt(mMap.getCameraPosition().tilt).zoom((float) (mMap.getCameraPosition().zoom - (0.75))).build();
            Log.d(TAG, "onClick: Zoom Out");
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(zoomOutPosition));
        });

        zoomIn.setOnClickListener(view -> {
            CameraPosition zoomInPosition = new CameraPosition.Builder()
                    .target(mMap.getCameraPosition().target)
                    .bearing(mMap.getCameraPosition().bearing)
                    .tilt(mMap.getCameraPosition().tilt).zoom((float) (mMap.getCameraPosition().zoom + (0.75))).build();
            Log.d(TAG, "onClick: Zoom In");
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(zoomInPosition));
        });

        zoomIn.setOnLongClickListener(view -> {
            Toast.makeText(MapsActivityPrimary.this, "Zoom In", Toast.LENGTH_SHORT).show();
            return true;
        });

        zoomOut.setOnLongClickListener(view -> {
            Toast.makeText(MapsActivityPrimary.this, "Zoom Out", Toast.LENGTH_SHORT).show();
            return false;
        });

        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);

        navigationView.setNavigationItemSelectedListener(menuItem -> {

            switch (menuItem.getItemId()) {
                case R.id.share:
                    Toast.makeText(MapsActivityPrimary.this, "Share", Toast.LENGTH_SHORT).show();
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Hey,\n\nReach Alert alerts you on reaching a location you desire when you are not aware.\nBest app for people who often tend to fall asleep during travelling.\nJust Set and Sleep\n\nDownload from Play Store - http://bit.ly/reachAlert");
                    sendIntent.setType("text/plain");
                    Intent chooser = Intent.createChooser(sendIntent, "Share");
                    startActivity(chooser);
                    break;

                case R.id.sign_out:
                    FirebaseAuth.getInstance().signOut();
                    clearSharedPreferences(1);
                    Intent signOut = new Intent(this, LoginActivity.class);
                    signOut.putExtra("from", "MAP");
                    startActivity(signOut);
                    finish();
                    break;

                case R.id.searched_one:
                    fetchPlaceDetails(searched.getString("searched_one_pid", ""));
                    drawerLayout.closeDrawer(GravityCompat.START, true);
                    break;

                case R.id.searched_two:
                    fetchPlaceDetails(searched.getString("searched_two_pid", ""));
                    drawerLayout.closeDrawer(GravityCompat.START, true);
                    break;

                case R.id.searched_three:
                    fetchPlaceDetails(searched.getString("searched_three_pid", ""));
                    drawerLayout.closeDrawer(GravityCompat.START, true);
                    break;

                case R.id.clear_search:
                    clearSharedPreferences(2);
                    break;

                case R.id.recent_one:
                    if (recents.getString("recent_one_pid", null) == null || recents.getString("recent_one_pid", null).equals("")) {
                        LatLng t = new LatLng(recents.getFloat("recent_one_lat", 0), recents.getFloat("recent_one_long", 0));
                        if (!t.equals(new LatLng(0, 0)))
                            movecamera(t, zoom, recents.getString("recent_one", "Recent Location"),
                                    getAddressFromMarker(new LatLng(recents.getFloat("recent_one_lat", 0), recents.getFloat("recent_one_long", 0))),
                                    "Point of interest");
                    } else {
                        fetchPlaceDetails(recents.getString("recent_one_pid", ""));
                    }
                    drawerLayout.closeDrawer(GravityCompat.START, true);
                    break;

                case R.id.recent_two:
                    if (recents.getString("recent_two_pid", null) == null || recents.getString("recent_two_pid", null).equals("")) {
                        LatLng t = new LatLng(recents.getFloat("recent_two_lat", 0), recents.getFloat("recent_two_long", 0));
                        if (!t.equals(new LatLng(0, 0)))
                            movecamera(t, zoom, recents.getString("recent_two", "Recent Location"),
                                    getAddressFromMarker(new LatLng(recents.getFloat("recent_two_lat", 0), recents.getFloat("recent_two_long", 0))),
                                    "Point of interest");
                    } else {
                        fetchPlaceDetails(recents.getString("recent_two_pid", ""));
                    }
                    drawerLayout.closeDrawer(GravityCompat.START, true);
                    break;

                case R.id.recent_three:
                    if (recents.getString("recent_three_pid", null) == null || recents.getString("recent_three_pid", null).equals("")) {
                        LatLng t = new LatLng(recents.getFloat("recent_three_lat", 0), recents.getFloat("recent_three_long", 0));
                        if (!t.equals(new LatLng(0, 0)))
                            movecamera(t, zoom, recents.getString("recent_three", "Recent Location"),
                                    getAddressFromMarker(new LatLng(recents.getFloat("recent_three_lat", 0), recents.getFloat("recent_three_long", 0))),
                                    "Point of interest");
                    } else {
                        fetchPlaceDetails(recents.getString("recent_three_pid", ""));
                    }
                    drawerLayout.closeDrawer(GravityCompat.START, true);
                    break;

                case R.id.clear_reached:
                    clearSharedPreferences(3);
                    break;

                case R.id.theme:
                    dark = !dark;
                    aSwitch.setChecked(dark);
                    drawerLayout.closeDrawer(GravityCompat.START, true);
                    SharedPreferences.Editor themeEditor = settings.edit();
                    themeEditor.putBoolean("dark", dark);
                    themeEditor.apply();
                    changeTheme();
                    break;
            }

            return true;
        });

    }

    private void changeTheme() {
        this.recreate();
    }

    private void showDialog() {
        ConfirmationDialog confirmationDialog = new ConfirmationDialog(this, targetDetails.getName(), targetDetails.getAddress(), targetDetails, this) {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.confirm_button:
                        goToSecondary();
                        dismiss();
                        break;

                    case R.id.no_button:
                        dismiss();
                        break;
                }
            }
        };
        confirmationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        confirmationDialog.show();
    }

    private void goToSecondary() {
        if (targetPlaceName.getText().equals("Dropped Pin")) {
            Dialog dialog = new Dialog(this);
            if (dark) {
                dialog.setContentView(R.layout.dropped_pin_location_prompt_dark);
            } else {
                dialog.setContentView(R.layout.dropped_pin_location_prompt);
            }
            EditText editText = dialog.findViewById(R.id.name_box);
            Button button = dialog.findViewById(R.id.save_button);
            button.setActivated(false);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (!editable.toString().equals("")) {
                        button.setActivated(true);
                    }

                }
            });

            button.setOnClickListener(view -> {
                if (!editText.getText().toString().isEmpty()) {
                    String trackId = Utils.generateId("tr_");
                    Log.d(TAG, "init: "+trackId);
                    targetDetails = new TargetDetails(trackId,editText.getText().toString(), targetPlaceType.getText().toString(), targetPlaceAddress.getText().toString(), new double[]{currentLocationLatlng.latitude, currentLocationLatlng.longitude}, new double[]{targetLatLng.latitude, targetLatLng.longitude}, targetPlaceId);
                    Intent intent = new Intent(MapsActivityPrimary.this, MapsActivitySecondary.class);
                    intent.putExtra("targetDetails", targetDetails);
                    MapsActivityPrimary.this.startActivity(intent);
                    button.setClickable(false);
                    dialog.dismiss();
                } else {
                    editText.setError("Field Cannot be Blank!");
                    editText.requestFocus();
                }
            });
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
        } else {
            String trackId = Utils.generateId("tr_");
            Log.d(TAG, "init: "+trackId);
            targetDetails = new TargetDetails(trackId,targetPlaceName.getText().toString(), targetPlaceType.getText().toString(), targetPlaceAddress.getText().toString(), new double[]{currentLocationLatlng.latitude, currentLocationLatlng.longitude}, new double[]{targetLatLng.latitude, targetLatLng.longitude}, targetPlaceId);
            Intent intent = new Intent(MapsActivityPrimary.this, MapsActivitySecondary.class);
            intent.putExtra("targetDetails", targetDetails);
            MapsActivityPrimary.this.startActivity(intent);
        }
    }

    private void clearSharedPreferences(int o) {
        SharedPreferences.Editor editor;
        if (o == 1) {
            SharedPreferences sharedPreferences = getSharedPreferences("userdetails", MODE_PRIVATE);
            editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            searched = getSharedPreferences("searched", MODE_PRIVATE);
            editor = searched.edit();
            editor.clear();
            editor.apply();
            SharedPreferences recents = getSharedPreferences("recent", MODE_PRIVATE);
            editor = recents.edit();
            editor.clear();
            editor.apply();
            updateSearchMenu();
            updateSearchMenu();
            updateRecentMenu();
            SharedPreferences notification = getSharedPreferences("notifications", MODE_PRIVATE);
            editor = notification.edit();
            editor.clear();
            editor.apply();
        } else if (o == 2) {
            searched = getSharedPreferences("searched", MODE_PRIVATE);
            editor = searched.edit();
            editor.clear();
            editor.apply();
            updateSearchMenu();
        } else if (o == 3) {
            SharedPreferences recents = getSharedPreferences("recent", MODE_PRIVATE);
            editor = recents.edit();
            editor.clear();
            editor.apply();
            updateSearchMenu();
            updateRecentMenu();
        }

    }

    private void initVars() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        String qwert = getResources().getString(R.string.google_maps_key);
        Places.initialize(this, qwert);
        Log.d(TAG, "initVars: ");
        mCurrLoc = findViewById(R.id.location_btn_img);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        userName = header.findViewById(R.id.user_name);
        userEmail = header.findViewById(R.id.user_email);
        confirm = findViewById(R.id.confirm);
        userPic = header.findViewById(R.id.user_image);
        targetPlaceName = findViewById(R.id.place_name);
        targetPlaceType = findViewById(R.id.place_type);
        targetPlaceAddress = findViewById(R.id.place_address);
        targetPlaceImages[0] = findViewById(R.id.place_images_1);
        targetPlaceImages[1] = findViewById(R.id.place_images_2);
        targetPlaceImages[2] = findViewById(R.id.place_images_3);
        targetPlaceImages[3] = findViewById(R.id.place_images_4);
        mLocationTick = findViewById(R.id.place_tick_image);
        zoomIn = findViewById(R.id.zoom_in);
        zoomIn.setVisibility(View.VISIBLE);
        zoomOut = findViewById(R.id.zoom_ot);
        zoomOut.setVisibility(View.VISIBLE);
        mapType = findViewById(R.id.map_type);
        targetPlacePhotosScrollViewLinearLayout = findViewById(R.id.place_images_scroll_linearLayout);
        placeDetailsContainer = findViewById(R.id.Place_details_view_relative_container);
        searchContainer = findViewById(R.id.searchbar_layout_card);
        menu = navigationView.getMenu();

        SharedPreferences sharedPreferences = getSharedPreferences("userdetails", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String dbname = "User Name";

        /*FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user.getEmail() != null && !user.getEmail().equals("")) {
            Log.d(TAG, "init email" + user.getEmail());
            userName.setText(user.getDisplayName());
            userEmail.setText(user.getEmail());
            dbname = user.getDisplayName();
            if (dbname != null) {
                dbname = dbname.replaceAll("\\.", "_").replaceAll("#", "_").replaceAll("\\$", "_").replaceAll("\\[", "_").replaceAll("]", "_");
            }
            editor.putString("dbname", dbname);
        } else {
            Log.d(TAG, "init phone number ");
            userEmail.setText(user.getPhoneNumber());
            sharedPreferences = getSharedPreferences("userdetails", MODE_PRIVATE);
            userName.setText(sharedPreferences.getString("name", "User Name"));
            dbname = user.getPhoneNumber();
            editor.putString("dbname", dbname);
        }*/

        userName.setText("Hello");
        userEmail.setText("");

        editor.apply();
        Picasso.get()
//                .load(user.getPhotoUrl())
                .load("https://play-lh.googleusercontent.com/qtODjjYNfpfaaH7_QOglvxFDjdtkOdP6aPzPDztyiUkxwU7yplZYg3sVXqeT91b3ejI=s94-rw")
                .error(R.mipmap.ic_user_image)
                .placeholder(R.mipmap.ic_user_image)
                .into(userPic);
    }

    void showAd() {}

    private void checkLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "checkLocation: checking....");
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "checkLocation: Not On");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.gps_not_found_title)  // GPS not found
                    .setMessage(R.string.gps_not_found_message) // Want to enable?
                    .setPositiveButton(R.string.okay, (dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .show();
        } else {
            if (created > 0) {
                showAd();
            } else {
                initMap();
            }
        }
    }

    private void autocomplete() {
        // Initialize the AutocompleteSupportFragment.
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            autocompleteFragment.setHint("Enter your Destination");
        }

        if (dark) {
            EditText editText = autocompleteFragment.getView().findViewById(R.id.places_autocomplete_search_input);
            editText.setTextColor(getResources().getColor(R.color.white));
            editText.setHintTextColor(getResources().getColor(R.color.light_white));
        }


        // Specify the types of place data to return.
        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
        }

        // Set up a PlaceSelectionListener to handle the response.
        if (autocompleteFragment != null) {
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    Log.i(TAG, "Place Selected From Search: (Primary) " + place.getName());
                    targetPlaceId = place.getId();
                    updateSearched(place.getName(), place.getId());
                    fetchPlaceDetails(place.getId());
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.i(TAG, "An error occurred: (Primary) " + status);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String getAddressFromMarker(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String address = "";
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            Address obj = addresses.get(0);
            address = obj.getAddressLine(0);
            Log.d(TAG, "getAddressFromMarker: (Primary) " + address);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    private void fetchPlaceDetails(String placeId) {
        targetPlaceImages[0].setImageResource(R.color.imageColor1);
        targetPlaceImages[1].setImageResource(R.color.imageColor2);
        targetPlaceImages[2].setImageResource(R.color.imageColor3);
        targetPlaceImages[3].setImageResource(R.color.imageColor4);
        Log.d(TAG, "fetchPlaceDetails: TPID " + targetPlaceId + " , " + placeId);
        targetPlaceId = placeId;
        targetPlacebitmaps[0] = targetPlacebitmaps[1] = targetPlacebitmaps[2] = targetPlacebitmaps[3] = null;

        targetPlacePhotosScrollViewLinearLayout.getLayoutParams().height = (int) getResources().getDimension(R.dimen.dp0);

        PlacesClient placesClient = Places.createClient(this);

        List<Place.Field> fields = Arrays.asList(Place.Field.PHOTO_METADATAS, Place.Field.ADDRESS, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES);

        FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(placeId, fields).build();

        placesClient.fetchPlace(placeRequest).addOnSuccessListener((response) -> {
            Place place = response.getPlace();

            Log.d(TAG, "fetchPlaceDetails: (Primary) " + place.getName() + " (" + place.getLatLng() + ") ," + place.getAddress() + " , " + place.getTypes());
            String placeType = "POINT_OF_INTEREST";
            if (!place.getTypes().get(0).name().equals("POINT_OF_INTEREST")) {
                String temp = "";
                placeType = temp;
                if (!place.getTypes().get(0).name().equals("POLITICAL")) {
                    temp = place.getTypes().get(0).name();
                    placeType = temp.substring(0, 1).toUpperCase() + temp.substring(1).toLowerCase();
                }
                if (place.getTypes().get(place.getTypes().size() - 1).name().equals("POLITICAL")) {
                    placeType = place.getTypes().get(place.getTypes().size() - 2).toString();
                } else if (place.getTypes().get(place.getTypes().size() - 1).name().equals("ROUTE")) {
                    placeType = "ROUTE";
                } else {
                    for (int j = 1; !place.getTypes().get(j).name().equals("POINT_OF_INTEREST") && !place.getTypes().get(j).name().equals("ESTABLISHMENT"); j++) {
                        Log.d(TAG, "fetchPlaceDetails: " + place.getTypes().get(j).name().equals("POLITICAL") + " " + place.getTypes().get(j).name());
                        temp = place.getTypes().get(j).name();
                        placeType = placeType + " / " + temp.substring(0, 1).toUpperCase() + temp.substring(1).toLowerCase();
                    }
                }
                placeType = placeType.replace(" ", "_");
                placeType = placeType.toLowerCase();
                placeType = placeType.substring(0, 1).toUpperCase() + placeType.substring(1);
                while (placeType.contains("_")) {
                    Log.d(TAG, "fetchPlaceDetails:" + placeType);
                    placeType = placeType.substring(0, placeType.indexOf("_") + 1) + placeType.substring(placeType.indexOf("_") + 1, placeType.indexOf("_") + 2).toUpperCase() + placeType.substring(placeType.indexOf("_") + 2);
                    placeType = placeType.replaceFirst("_", " ");
                }
                if (placeType.substring(1, 2).equals("/")) {
                    placeType = placeType.substring(3);
                }
            }

            if (placeType.contains("Administrative Area Level 1") || placeType.contains("Locality"))
                zoom = 10f;
            else if (placeType.contains("Neighborhood") || placeType.contains("Sublocality"))
                zoom = 15f;
            else if (placeType.contains("Natural Feature") || placeType.contains("Country"))
                zoom = 5f;
            else zoom = 20f;
            movecamera(place.getLatLng(), zoom, place.getName(), place.getAddress(), placeType);

            if (place.getPhotoMetadatas() != null) {
                if (place.getPhotoMetadatas().size() > 0) {
                    targetPlacePhotosScrollViewLinearLayout.getLayoutParams().height = (int) getResources().getDimension(R.dimen.dp100);
                }

                for (int i = 0; i < place.getPhotoMetadatas().size() && i < 4; i++) {
                    // Get the photo metadata.
                    PhotoMetadata photoMetadata = place.getPhotoMetadatas().get(i);

                    // Get the attribution text.
                    String attributions = photoMetadata.getAttributions();
                    // Create a FetchPhotoRequest.
                    FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                            .build();
                    int finalI = i;
                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener(fetchPhotoResponse -> {
                        targetPlacebitmaps[finalI] = fetchPhotoResponse.getBitmap();
                        targetPlaceImages[finalI].setImageBitmap(targetPlacebitmaps[finalI]);
                    }).addOnFailureListener((exception) -> {
                        if (exception instanceof ApiException) {
                            ApiException apiException = (ApiException) exception;
                            int statusCode = apiException.getStatusCode();
                            Log.e(TAG, "Place not found: (Primary) " + exception.getMessage() + " , " + statusCode);
                        }
                    });
                }
            }
        }).addOnFailureListener((f) -> {
            Log.i(TAG, "fetchPlaceDetails: " + f);
        });

    }

    private void movecamera(LatLng latLng, float zoom, String title, String placeAddress, String placeType) {
        Log.d(TAG, "movecamera: (Primary) moving the camera to lat : " + latLng.latitude + " lang : " + latLng.longitude + " Name: " + title + " Type: " + placeType + " Address: " + placeAddress);

        if (marker != null) {
            marker.remove();
            marker = null;
        }

        mLocationTick.setVisibility(View.INVISIBLE);
        confirm.setText("");
        float tilt = 32f;
        if (mMap.getMapType() == 2) tilt = 0f;

        CameraPosition movingPlace = new CameraPosition.Builder()
                .target(latLng)
                .tilt(tilt).zoom(zoom).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(movingPlace));

        if (!title.equals("Current Location")) {
            marker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).draggable(true));
            marker.setDraggable(true);
            mLocationTick.setVisibility(View.VISIBLE);
            confirm.setText("Confirm Location");
            targetLatLng = latLng;
        } else
            currentLocationLatlng = latLng;

        if (title.equals("Current Location") || title.equals("Dropped Pin")) {

            targetPlaceImages[0].setImageResource(R.color.imageColor1);
            targetPlaceImages[1].setImageResource(R.color.imageColor2);
            targetPlaceImages[2].setImageResource(R.color.imageColor3);
            targetPlaceImages[3].setImageResource(R.color.imageColor4);

            targetPlacePhotosScrollViewLinearLayout.getLayoutParams().height = (int) getResources().getDimension(R.dimen.dp0);
        }

        DecimalFormat decimalFormat = new DecimalFormat("#0.00000");
        String latlong = decimalFormat.format(latLng.latitude) + " , " + decimalFormat.format(latLng.longitude);

        targetPlaceName.setText(title);

        if (!placeType.equals("Point of interest")) {
            targetPlaceType.setText(placeType);
        } else {
            targetPlaceType.setText(latlong);
        }

        targetPlaceAddress.setText(placeAddress);

        hideSoftKeyboard();

    }

    private void initMap() {
        Log.d(TAG, "initMap: (Primary) initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maps_primary);
        mapFragment.getMapAsync(this);
    }

    private void getDeviceLocation(int t) {
        Log.d(TAG, "getDeviceLocation: (Primary) getting device location.");

        try {
            if (checkPermissions()) {

                final Task location = mFusedLocationClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: (Primary) found location");
                        Location currentLocation = (Location) task.getResult();
                        currentLocationLatlng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        autocompleteFragment.setLocationBias(RectangularBounds.newInstance(toBounds(currentLocationLatlng, 1000000)));
                        String address = getAddressFromMarker(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                        if (t == 1)
                            movecamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 15f, "Current Location", address, "Point of interest");
                    } else {
                        Log.d(TAG, "onComplete: (Primary) location not found");
                        Toast.makeText(MapsActivityPrimary.this, "Unable to find the location.Please Check the Internet", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                askPermission();
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: (Primary) SecurityException" + e.getMessage());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: (Primary) map is ready");
        mMap = googleMap;

        if (checkPermissions()) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            if (dark)
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_night));

            init();

            if (fromNotification) {
                getDeviceLocation(0);
                fromNotification = false;
                targetPlaceId = mainIntent.getStringExtra("placeId");
                Log.d(TAG, "onMapReady: TPID" + targetPlaceId);
                double[] latLng = mainIntent.getExtras().getDoubleArray("latlng");
                targetLatLng = new LatLng(latLng[0], latLng[1]);
                String text = mainIntent.getStringExtra("text");
                if (targetPlaceId == null) {
                    movecamera(targetLatLng, 15f, "Dropped Pin", getAddressFromMarker(targetLatLng), "Point of interest");
                } else
                    fetchPlaceDetails(targetPlaceId);
            } else if (fromShared) {
                getDeviceLocation(0);
                fromShared = false;
                String action = mainIntent.getStringExtra("shared location");
                targetLatLng = new LatLng(Double.parseDouble(action.substring(action.indexOf(":") + 1, action.indexOf(","))), Double.parseDouble(action.substring(action.indexOf(",") + 1, action.indexOf("?"))));
                movecamera(targetLatLng, 15f, "Dropped Pin", getAddressFromMarker(targetLatLng), "Point of interest");
            } else
                getDeviceLocation(1);
        } else {
            askPermission();
        }
    }

    private boolean checkPermissions() {

        notPermittedPermission.clear();

        List<String> l = new ArrayList<String>();
        l.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            l.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        boolean ret = (ActivityCompat.checkSelfPermission(this, l.get(0)) == PackageManager.PERMISSION_GRANTED);

        if(!ret) {
            notPermittedPermission.add(l.get(0));
        }

        for(int i = 1; i < l.size() ; i++){
            boolean t = (ActivityCompat.checkSelfPermission(this, l.get(i)) == PackageManager.PERMISSION_GRANTED);
            if(!t){
                if(l.get(i).equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                    if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q){
                        notPermittedPermission.add(l.get(i));
                        ret = false;
                    } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && (ActivityCompat.checkSelfPermission(this, l.get(0)) == PackageManager.PERMISSION_GRANTED)){
                        notPermittedPermission.add(l.get(i));
                        ret = false;
                    }
                } else {
                    notPermittedPermission.add(l.get(i));
                    ret = false;
                }
            }
        }

        return ret;
    }

    private void requestPermissions() {
        Log.d(TAG, "requestPermissions");
        if(askignBgLocPerms == 0) {
            startActivityForResult(new FlutterActivity.NewEngineIntentBuilder(FlutterEmbeddingActivity.class).backgroundMode(FlutterActivityLaunchConfigs.BackgroundMode.transparent).build(this), 202009);
            askignBgLocPerms = 1;
        }
    }

    private void askPermission() {

        Log.d(TAG, "askPermission");

        /*boolean shouldProvideRationale =
                ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "askPermission: is >= Q: " + Build.VERSION.SDK_INT);
            shouldProvideRationale = shouldProvideRationale && ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }*/

        if (notPermittedPermission.size() == 0) {
            Log.i(TAG, "Permissions Ok");
            checkLocation();
        } else if (notPermittedPermission.get(0).equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            notPermittedPermission.clear();
            requestPermissions();
        } else {
            Log.i(TAG, "Requesting permission");
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "askPermission: asking is >= Q: " + Build.VERSION.SDK_INT);
                try {
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            REQUEST_PERMISSIONS_REQUEST_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Error e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "askPermission: asking is < Q: " + Build.VERSION.SDK_INT);
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }*/

            String[] l = new String[notPermittedPermission.size()];

            for(int i = 0; i < notPermittedPermission.size(); i++){
                l[i] = notPermittedPermission.get(i);
            }

            try {
                ActivityCompat.requestPermissions(this,
                        l,
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onrequestPermissionsResult");

        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {

            notPermittedPermission.clear();

            for (int i = 0; i < grantResults.length; i++) {
                Log.d(TAG, permissions[i] + ": " + (grantResults[i] == PackageManager.PERMISSION_GRANTED));
                if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION) && (grantResults[i] == PackageManager.PERMISSION_GRANTED)){
                    if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
                        requestPermissions();
                    }
                }
            }

            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public LatLngBounds toBounds(LatLng center, double radiusInMeters) {
        double distanceFromCenterToCorner = radiusInMeters * Math.sqrt(2.0);
        LatLng southwestCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0);
        LatLng northeastCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0);
        return new LatLngBounds(southwestCorner, northeastCorner);
    }

    void scheduleNotification() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        PendingIntent notificationPendingIntent;

        Intent notificationIntent = new Intent(this, RemainderReceiver.class);
        notificationIntent.putExtra("text", "Wanna Go Somewhere?");
        notificationIntent.putExtra("New?", true);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        SharedPreferences notification = getSharedPreferences("notifications", MODE_PRIVATE);
        SharedPreferences.Editor editor = notification.edit();

        Log.d(TAG, "scheduleNotification");

        if (!notification.getBoolean("morning", false)) {
            calendar.set(Calendar.HOUR_OF_DAY, 7);
            Log.d(TAG, "scheduleNotification: m" + calendar.getTimeInMillis());
            editor.putBoolean("morning", true);
            notificationIntent.putExtra("id", 3);
            notificationPendingIntent = PendingIntent.getBroadcast(this, 3, notificationIntent, PendingIntent.FLAG_MUTABLE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, notificationPendingIntent);
        }

        if (!notification.getBoolean("afternoon", false)) {
            calendar.set(Calendar.HOUR_OF_DAY, 15);
            editor.putBoolean("afternoon", true);
            Log.d(TAG, "scheduleNotification: a" + calendar.getTimeInMillis());
            notificationIntent.putExtra("id", 4);
            notificationPendingIntent = PendingIntent.getBroadcast(this, 4, notificationIntent, PendingIntent.FLAG_MUTABLE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, notificationPendingIntent);
        }

        if (!notification.getBoolean("evening", false)) {
            calendar.set(Calendar.HOUR_OF_DAY, 20);
            editor.putBoolean("evening", true);
            Log.d(TAG, "scheduleNotification: e" + calendar.getTimeInMillis());
            notificationIntent.putExtra("id", 5);
            notificationPendingIntent = PendingIntent.getBroadcast(this, 5, notificationIntent, PendingIntent.FLAG_MUTABLE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, notificationPendingIntent);
        }

        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 202009) {
            Log.d(TAG, "onActivityResult: " + data);
            if (data != null && data.getBooleanExtra("accepted", false)) {
                Log.d(TAG, "onActivityResult: " + data.getBooleanExtra("accepted", false));
                Log.i(TAG, "Requesting Background Location Permission");
                try {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            REQUEST_PERMISSIONS_REQUEST_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Error e) {
                    e.printStackTrace();
                }

            } else {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
        }
        if (requestCode == 132) {
            Log.d(TAG, "onActivityResult: "+data.getBooleanExtra("ad_status", false));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

