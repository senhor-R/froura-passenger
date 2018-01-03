package com.froura.develo4.passenger;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.froura.develo4.passenger.libraries.DialogCreator;
import com.froura.develo4.passenger.libraries.RandomString;
import com.froura.develo4.passenger.libraries.SnackBarCreator;
import com.froura.develo4.passenger.tasks.CheckUserTasks;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity
        implements DialogCreator.DialogActionListener,
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private DrawerLayout drawer;
    private TextView name;
    private FloatingActionButton bookFab;
    private FloatingActionButton rsrvFab;
    private View viewFab;

    private DatabaseReference mPassengerDB;

    private GoogleMap mMap;
    private CameraPosition cameraPosition;
    private SupportMapFragment mapFragment;

    private PlaceAutocompleteFragment pickup;
    private PlaceAutocompleteFragment dropoff;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LatLng pickup_coordinates;
    private LatLng dropoff_coordinates;

    private String uid;
    private String pickup_location;
    private String dropoff_location;
    private boolean autoMarkerSet = false;
    private boolean cameraUpdated = false;
    final int LOCATION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
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
        bookFab = findViewById(R.id.bookFab);
        rsrvFab = findViewById(R.id.rsrvFab);
        viewFab = findViewById(R.id.viewFab);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mPassengerDB = FirebaseDatabase.getInstance().getReference().child("users").child("passenger").child(uid);
        mPassengerDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                name.setText(map.get("name").toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if(!locationEnabled()) {
            DialogCreator.create(this, "requestLocation")
                    .setTitle("Access Location")
                    .setMessage("Turn on your location settings to be able to get location data.")
                    .setPositiveButton("Go to Settings")
                    .show();
        }

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setCountry("PH")
                .build();

        pickup = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.pickup);
        dropoff = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.dropoff);

        pickup.setFilter(typeFilter);
        dropoff.setFilter(typeFilter);

        pickup.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                pickup_location = place.getName().toString();
                pickup_coordinates = place.getLatLng();
                setMarkers();
            }

            @Override
            public void onError(Status status) {

            }
        });
        dropoff.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                dropoff_location = place.getName().toString();
                dropoff_coordinates = place.getLatLng();
                setMarkers();
            }

            @Override
            public void onError(Status status) {

            }
        });

        pickup.getView().findViewById(R.id.clear)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        view.setVisibility(View.GONE);
                        pickup_coordinates = null;
                        autoMarkerSet = false;
                        setMarkers();
                    }
                });
        dropoff.getView().findViewById(R.id.clear)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        view.setVisibility(View.GONE);
                        dropoff.setText("");
                        dropoff_coordinates = null;
                        dropoff_location = null;
                        setMarkers();
                    }
                });

        bookFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareBooking();
            }
        });
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
            ActivityCompat.requestPermissions(HomeActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }

    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!=null){
            mLastLocation = location;
            setMarkers();
            if(!cameraUpdated) {
                LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                cameraPosition = new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(14)
                        .bearing(0)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                cameraUpdated = true;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        buildGoogleApiClient();
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.logout:
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                } else {
                    SnackBarCreator.set("Permissions denied.");
                    SnackBarCreator.show(viewFab);
                    permissionDenied();
                }
                break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onClickPositiveButton(String actionId) {
        switch (actionId) {
            case "requestLocation":
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
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

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void setMarkers() {
        mMap.clear();
        if(autoMarkerSet) {
            if(pickup_coordinates != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(pickup_coordinates.latitude, pickup_coordinates.longitude))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellow_marker)));
            } else if(mLastLocation != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellow_marker)));
            }
            if(dropoff_coordinates != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(dropoff_coordinates.latitude, dropoff_coordinates.longitude))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.black_marker)));
            }
        } else {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.yellow_marker)));
            getAddress();
            autoMarkerSet = true;
        }
    }

    private void getAddress() {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
            pickup_location = addresses.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        pickup.setText(pickup_location);
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
        LatLng latLng = new LatLng(14.6091, 121.0223);
        cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(11)
                .bearing(0)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        bookFab.setImageResource(R.mipmap.book_disabled);
        rsrvFab.setImageResource(R.mipmap.rsrv_disabled);

        bookFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SnackBarCreator.set("Permissions denied.");
                SnackBarCreator.show(view);
            }
        });

        rsrvFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SnackBarCreator.set("Permissions denied.");
                SnackBarCreator.show(view);
            }
        });
    }

    private boolean permissionStatus() {
        return ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void prepareBooking() {
        if(permissionStatus()) {
            if(pickup_location != null && dropoff_location != null) {
                Intent intent = new Intent(this, BookingActivity.class);
                intent.putExtra("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                if(pickup_coordinates != null) {
                    intent.putExtra("pickupLat", pickup_coordinates.latitude);
                    intent.putExtra("pickupLng", pickup_coordinates.longitude);
                } else {
                    intent.putExtra("pickupLat", mLastLocation.getLatitude());
                    intent.putExtra("pickupLng", mLastLocation.getLongitude());
                }
                intent.putExtra("pickupLoc", pickup_location);
                intent.putExtra("dropoffLoc", dropoff_location);
                startActivity(intent);
            } else {
                SnackBarCreator.set("Set a Drop-off point.");
                SnackBarCreator.show(viewFab);
            }
        } else {
            SnackBarCreator.set("Permissions denied.");
            SnackBarCreator.show(viewFab);
        }
    }
}
