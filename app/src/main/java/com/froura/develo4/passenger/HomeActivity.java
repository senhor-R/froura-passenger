package com.froura.develo4.passenger;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.froura.develo4.passenger.config.TaskConfig;
import com.froura.develo4.passenger.libraries.DialogCreator;
import com.froura.develo4.passenger.libraries.SnackBarCreator;
import com.froura.develo4.passenger.tasks.SuperTask;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;

import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;
import me.grantland.widget.AutofitHelper;

public class HomeActivity extends AppCompatActivity
        implements DialogCreator.DialogActionListener,
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        SuperTask.TaskListener {

    private DrawerLayout drawer;
    private TextView name;
    private CardView cardView;
    private CardView viewDetails;
    private Button bookBtn;
    private CircleImageView prof_pic;
    private TextView pickupTxtVw;
    private TextView dropoffTxtVw;
    private TextView taxifareTxtVw;
    private TextView distanceTxtVw;
    private TextView durationTxtVw;

    private GoogleMap mMap;
    private CameraPosition cameraPosition;
    private SupportMapFragment mapFragment;

    private GoogleApiClient mGoogleApiClient;
    private GeoDataClient mGeoDataClient;
    private Location mLastLocation;
    private LatLng pickupLocation;
    private LatLng dropoffLocation;

    private String uid;
    private String pickupName;
    private String dropoffName;
    private String pickupPlaceId;
    private String dropoffPlaceId;
    private int hasPickup = -1;
    private int hasDropoff = -1;
    private boolean cameraUpdated = false;
    final int LOCATION_REQUEST_CODE = 1;

    //User Details
    private String user_name;
    private String user_mobnum;
    private String user_email;
    private String taxi_fare = "0.00";
    private String duration = "0KM";
    private String distance = "0M";

    private ViewFlipper viewFlipper;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mGeoDataClient = Places.getGeoDataClient(this, null);
        viewFlipper = findViewById(R.id.app_bar_include).findViewById(R.id.vf);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Booking");
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);
        View v = navigationView.getHeaderView(0);
        name = v.findViewById(R.id.txtVw_name);
        prof_pic = v.findViewById(R.id.imgVw_profile_pic);
        AutofitHelper.create(name);

        Glide.with(this)
                .load(getImage("placeholder"))
                .into(prof_pic);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setDetails();

        bookBtn = findViewById(R.id.bookingButton);
        viewDetails = findViewById(R.id.details);
        cardView = findViewById(R.id.cardView);
        pickupTxtVw = findViewById(R.id.txtVw_pickup);
        dropoffTxtVw = findViewById(R.id.txtVw_dropoff);
        taxifareTxtVw = findViewById(R.id.txtVw_taxi_fare);
        distanceTxtVw = findViewById(R.id.txtVw_distance);
        durationTxtVw = findViewById(R.id.txtVw_duration);

        taxifareTxtVw.setText(taxi_fare);
        distanceTxtVw.setText(distance);
        durationTxtVw.setText(duration);

        if(!locationEnabled())
            DialogCreator.create(this, "requestLocation")
                    .setTitle("Access Location")
                    .setMessage("Turn on your location settings to be able to get location data.")
                    .setPositiveButton("Go to Settings")
                    .show();

        hasPickup = getIntent().getIntExtra("hasPickup", -1);
        hasDropoff = getIntent().getIntExtra("hasDropoff", -1);

        if(hasPickup == 1)
            findPlaceById(getIntent().getStringExtra("pickupPlaceId"), 0);
        
        if(hasDropoff == 1) {
            cameraUpdated = true;
            findPlaceById(getIntent().getStringExtra("dropoffPlaceId"), 1);
            bookBtn.setText("Book");
            bookBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    prepareBooking();
                }
            });
        } else {
            bookBtn.setText("Set Drop-off Point");
            bookBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setDropoff();
                }
            });
        }

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        pickupTxtVw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
                if(pickupPlaceId != null) {
                    intent.putExtra("pickupPlaceId", pickupPlaceId);
                    intent.putExtra("from", 0);
                }

                if(dropoffPlaceId != null) {
                    intent.putExtra("dropoffPlaceId", dropoffPlaceId);
                    intent.putExtra("from", 0);
                }
                startActivity(intent);
                finish();
            }
        });

        dropoffTxtVw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDropoff();
            }
        });
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.booking:
                viewFlipper.setDisplayedChild(0);
                toolbar.setTitle("Booking");
                break;
            case R.id.reservation:
                viewFlipper.setDisplayedChild(1);
                toolbar.setTitle("Reservation");
                break;
            case R.id.history:
                viewFlipper.setDisplayedChild(2);
                toolbar.setTitle("History");
                break;
            case R.id.profile:
                setAccountProfile();
                viewFlipper.setDisplayedChild(3);
                toolbar.setTitle("Profile");
                break;
            case R.id.settings:
                viewFlipper.setDisplayedChild(4);
                toolbar.setTitle("Settings");
                break;
            case R.id.logout:
                String providerid = "";
                for(UserInfo user: FirebaseAuth.getInstance().getCurrentUser().getProviderData()) {
                    providerid = user.getProviderId();
                }
                if(AccessToken.getCurrentAccessToken() != null && providerid.equals("facebook.com")) {
                    LoginManager.getInstance().logOut();
                } else if(providerid.equals("google.com")) {
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build();

                    GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

                    mGoogleSignInClient.signOut();
                }
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.commit();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
        }

        drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setAccountProfile() {
        TextView nameTxtVw = findViewById(R.id.nameTxtVw);
        EditText nameEt = findViewById(R.id.nameEt);
        EditText emailEt = findViewById(R.id.emailEt);
        EditText numberEt = findViewById(R.id.numberEt);

        nameTxtVw.setText(user_name);
        nameEt.setText(user_name);
        emailEt.setText(user_email);
        numberEt.setText(user_mobnum);
    }

    private void prepareBooking() {
        if(checkDetails() == 1) {
            Intent intent = new Intent(HomeActivity.this, BookingActivity.class);
            intent.putExtra("pickupName", pickupName);
            intent.putExtra("pickupLat", pickupLocation.latitude);
            intent.putExtra("pickupLng", pickupLocation.longitude);
            intent.putExtra("pickupPlaceId", pickupPlaceId);
            intent.putExtra("dropoffName", dropoffName);
            intent.putExtra("dropoffLat", dropoffLocation.latitude);
            intent.putExtra("dropoffLng", dropoffLocation.longitude);
            intent.putExtra("dropoffPlaceId", dropoffPlaceId);
            intent.putExtra("fare", taxi_fare);
            startActivity(intent);
            finish();
        } else if(checkDetails() == -1) {
            showSnackbarMessage(bookBtn, "Please set your Mobile Number.");
        } else if(checkDetails() == 2) {
            showSnackbarMessage(bookBtn, "Please set your Email Address.");
        } else {
            showSnackbarMessage(bookBtn, "Please set your Mobile Number and Email Address.");
        }
    }

    private void setDropoff() {
        Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
        if(pickupPlaceId != null) {
            intent.putExtra("pickupPlaceId", pickupPlaceId);
            intent.putExtra("from", 1);
        }

        if(dropoffPlaceId != null) {
            intent.putExtra("dropoffPlaceId", dropoffPlaceId);
            intent.putExtra("from", 1);
        }
        startActivity(intent);
        finish();
    }

    private void setFare() {
        SuperTask.execute(this, TaskConfig.CREATE_TAXI_FARE_URL);
    }

    @Override
    public void onTaskRespond(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            Toast.makeText(this, jsonString, Toast.LENGTH_SHORT).show();
            if(jsonObject.getString("status").equals("OK")) {
                taxi_fare = jsonObject.getString("fare");
                distance = jsonObject.getString("distance");
                duration = jsonObject.getString("duration");

                taxifareTxtVw.setText(taxi_fare);
                distanceTxtVw.setText(distance);
                durationTxtVw.setText(duration);
            }
        } catch(Exception e) {}
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLocation);
        builder.include(dropoffLocation);
        LatLngBounds bounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
    }

    @Override
    public ContentValues setRequestValues(ContentValues contentValues) {
        contentValues.put("android", 1);
        contentValues.put("origins", pickupPlaceId);
        contentValues.put("destinations", dropoffPlaceId);
        return (contentValues);
    }

    private void findPlaceById(String placeId, int from) {
        final int setTo = from;
        mGeoDataClient.getPlaceById(placeId).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                if (task.isSuccessful()) {
                    PlaceBufferResponse places = task.getResult();
                    Place myPlace = places.get(0);
                    if(setTo == 0) {
                        pickupPlaceId = myPlace.getId();
                        pickupName = myPlace.getName().toString();
                        setText(pickupTxtVw, pickupName);
                        pickupLocation = myPlace.getLatLng();
                    } else {
                        dropoffPlaceId = myPlace.getId();
                        dropoffName = myPlace.getName().toString();
                        setText(dropoffTxtVw, dropoffName);
                        dropoffLocation = myPlace.getLatLng();
                        setFare();
                    }
                    setMarkers(false);
                    places.release();
                }
            }
        });
    }

    private void setMarkers(boolean autoMarker) {
        if(autoMarker) mMap.clear();

        if(pickupLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(pickupLocation.latitude, pickupLocation.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellow_marker)));
            if(hasDropoff == -1) {
                cameraPosition = new CameraPosition.Builder()
                        .target(pickupLocation)
                        .zoom(14)
                        .bearing(0)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                cameraUpdated = true;
            }
        }
        if(dropoffLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(dropoffLocation.latitude, dropoffLocation.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.black_marker)));
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            DialogCreator.create(this, "locationPermission")
                    .setMessage("We need to access your location and device state to continue using FROURÁ.")
                    .setPositiveButton("OK")
                    .show();
            return;
        }
        mMap.setMyLocationEnabled(true);
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        if(hasPickup == -1) {
            PlaceDetectionClient mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
            final Task<PlaceLikelihoodBufferResponse> placeResult =
                    mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener
                    (new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                                    pickupName = placeLikelihood.getPlace().getName().toString();
                                    pickupLocation = placeLikelihood.getPlace().getLatLng();
                                    pickupPlaceId = placeLikelihood.getPlace().getId();
                                    setText(pickupTxtVw, pickupName);
                                    setMarkers(true);
                                    if(!cameraUpdated) {
                                        cameraPosition = new CameraPosition.Builder()
                                                .target(pickupLocation)
                                                .zoom(14)
                                                .bearing(0)
                                                .build();
                                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                        cameraUpdated = true;
                                    }
                                }
                                likelyPlaces.release();
                            }
                        }
                    });
        }
    }

    private void setDetails() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String JSON_DETAILS_KEY = "userDetails";
        String userDetails = sharedPref.getString(JSON_DETAILS_KEY, "{ \"name\" : NULL }");
        try {
            JSONObject jsonObject = new JSONObject(userDetails);
            if(!jsonObject.getString("name").equals("NULL")) {
                name.setText(jsonObject.getString("name"));
                if(!jsonObject.getString("profile_pic").equals("default"))
                    Glide.with(this)
                            .load(jsonObject.getString("profile_pic"))
                            .into(prof_pic);
                user_name = jsonObject.getString("name");
                user_email = jsonObject.getString("email");
                user_mobnum = jsonObject.getString("mobnum");
            }
        } catch (Exception e) { }
    }

    private boolean locationEnabled() {
        int locationMode = 0;
        String locationProviders;
        boolean isAvailable;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            isAvailable = (locationMode != Settings.Secure.LOCATION_MODE_OFF);
        } else {
            locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            isAvailable = !TextUtils.isEmpty(locationProviders);
        }

        return isAvailable;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) cardView.getLayoutParams();
        ViewGroup.MarginLayoutParams lp2 = (ViewGroup.MarginLayoutParams) viewDetails.getLayoutParams();
        mMap.setPadding(0, cardView.getLayoutParams().height + lp.topMargin + lp2.bottomMargin + viewDetails.getLayoutParams().height, 0 ,bookBtn.getLayoutParams().height);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }

    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!=null){
            mLastLocation = location;
        }
    }

    private void setText(TextView txtVw, String str) {
        txtVw.setText(str);
        txtVw.setTextColor(getResources().getColor(R.color.place_autocomplete_search_text));
    }

    @Override
    public void onBackPressed() {
        drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                } else {
                    finish();
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onClickPositiveButton(String actionId) {
        switch (actionId) {
            case "requestLocation":
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                break;
            case "locationPermission":
                ActivityCompat.requestPermissions(HomeActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
                break;
        }
    }

    @Override
    public void onClickNegativeButton(String actionId) { }

    @Override
    public void onClickNeutralButton(String actionId) { }

    @Override
    public void onClickMultiChoiceItem(String actionId, int which, boolean isChecked) { }

    @Override
    public void onCreateDialogView(String actionId, View view) { }

    public int getImage(String imageName) {
        int drawableResourceId = this.getResources()
                .getIdentifier(imageName, "drawable", this.getPackageName());
        return drawableResourceId;
    }

    private void askPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(HomeActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "All Permissions granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void permissionDenied() {
        bookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSnackbarMessage(bookBtn, "Permissions denied.");
            }
        });
    }

    private boolean permissionStatus() {
        return ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private int checkDetails() {
        if(user_mobnum.equals("null") && user_email.equals("null")) {
            return  -3;
        }
        if(user_mobnum.equals("null")) {
            return -1;
        }
        if(user_email.equals("null")) {
            return -2;
        }
        return 1;
    }

    private void setHint(TextView txtVw, String str) {
        txtVw.setText(str);
        txtVw.setTextColor(getResources().getColor(R.color.place_autocomplete_search_hint));
    }

    private void showSnackbarMessage(View view, String msg) {
        SnackBarCreator.set(msg);
        SnackBarCreator.show(view);
    }

}
