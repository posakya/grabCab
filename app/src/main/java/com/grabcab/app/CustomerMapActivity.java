package com.grabcab.app;

import android.*;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.text.Line;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    private Button mLogout, mRequest, mSettings, mHistory;

    private LatLng pickupLocation;

    private Boolean requestBol = false;

    private Marker pickupMarker;

    private SupportMapFragment mapFragment;

    private String destination, requestService;

    private LatLng destinationLatLng;

    private LinearLayout mDriverInfo;

    private ImageView mDriverProfileImage;

    private TextView mDriverName, mDriverPhone, mDriverCar;

    private RadioGroup mRadioGroup;

    private RatingBar mRatingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_costumer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        destinationLatLng = new LatLng(0.0, 0.0);

        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);

        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);

        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.Taxi);

        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);
        mHistory = (Button) findViewById(R.id.history);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        /*

            make a request

         */
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (requestBol) {
                    endRide();

                } else {
                    int selectId = mRadioGroup.getCheckedRadioButtonId();

                    final RadioButton radioButton = (RadioButton) findViewById(selectId);

                    if (radioButton.getText() == null) {
                        return;
                    }

                    requestService = radioButton.getText().toString();
                    System.out.println("request_service : " + requestService);

                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.pointer)));

                    mRequest.setText("Getting your Driver....");

                    getClosestDriver();
                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                return;
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });


    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;

    GeoQuery geoQuery;

    private void getClosestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol) {
//                    String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                if (driverFound) {
                                    return;
                                }

                                try {
                                    if (driverMap.get("service").equals(requestService)) {
                                        driverFound = true;
                                        driverFoundID = dataSnapshot.getKey();

                                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                                        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                        HashMap map = new HashMap();
                                        map.put("customerRideId", customerId);
                                        map.put("destination", destination);
                                        map.put("destinationLat", destinationLatLng.latitude);
                                        map.put("destinationLng", destinationLatLng.longitude);
                                        map.put("flag", "0");
                                        driverRef.updateChildren(map);

                                        getDriverLocation();
                                        getDriverInfo();
                                        getHasRideEnded();
                                        mRequest.setText("Looking for Driver Location....");


                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    /*-------------------------------------------- Map specific functions -----
    |  Function(s) getDriverLocation
    |
    |  Purpose:  Get's most updated driver location and it's always checking for movements.
    |
    |  Note:
    |	   Even tho we used geofire to push the location of the driver we can use a normal
    |      Listener to get it's location with no problem.
    |
    |      0 -> Latitude
    |      1 -> Longitudde
    |
    *-------------------------------------------------------------------*/
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundID).child("l");

        DatabaseReference flagRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest").child("flag");

        flagRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String flag = dataSnapshot.getValue().toString();
                    System.out.println("Flag : " + flag);
                    if (flag.equals("1")) {

                        AlertDialog alertDialog = new AlertDialog.Builder(CustomerMapActivity.this, android.app.AlertDialog.THEME_HOLO_LIGHT).create();


                        alertDialog.setTitle(R.string.app_name);
                        alertDialog.setMessage("Request Approved by Driver");


                        alertDialog.setButton(android.app.AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        dialog.dismiss();
                                    }
                                });

                        alertDialog.show();
                        final Button neutralButton = alertDialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL);
                        LinearLayout.LayoutParams neutralButtonLL = (LinearLayout.LayoutParams) neutralButton.getLayoutParams();
                        neutralButtonLL.gravity = Gravity.CENTER;
                        neutralButton.setTextColor(getResources().getColor(R.color.black));
                        neutralButton.setLayoutParams(neutralButtonLL);

                        Toast.makeText(CustomerMapActivity.this, "Approved", Toast.LENGTH_SHORT).show();
                    } else if (flag.equals("2")) {
                        mDriverInfo.setVisibility(View.GONE);
                        endRide();
                        AlertDialog alertDialog = new AlertDialog.Builder(CustomerMapActivity.this, android.app.AlertDialog.THEME_HOLO_LIGHT).create();


                        alertDialog.setTitle(R.string.app_name);
                        alertDialog.setMessage("Request Cancelled by driver");


                        alertDialog.setButton(android.app.AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        dialog.dismiss();
                                    }
                                });

                        alertDialog.show();
                        final Button neutralButton = alertDialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL);
                        LinearLayout.LayoutParams neutralButtonLL = (LinearLayout.LayoutParams) neutralButton.getLayoutParams();
                        neutralButtonLL.gravity = Gravity.CENTER;
                        neutralButton.setTextColor(getResources().getColor(R.color.black));
                        neutralButton.setLayoutParams(neutralButtonLL);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {
                        mRequest.setText("Driver's Here");
                    } else {
                        mRequest.setText("Driver Found: " + String.valueOf(distance));
                    }


                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.taxi)));
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    /*-------------------------------------------- getDriverInfo -----
    |  Function(s) getDriverInfo
    |
    |  Purpose:  Get all the user information that we can get from the user's database.
    |
    |  Note: --
    |
    *-------------------------------------------------------------------*/
    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    if (dataSnapshot.child("name") != null) {
                        mDriverName.setText(dataSnapshot.child("name").getValue().toString());
                    }
                    if (dataSnapshot.child("phone") != null) {
                        mDriverPhone.setText(dataSnapshot.child("phone").getValue().toString());
                    }
                    if (dataSnapshot.child("car") != null) {
                        mDriverCar.setText(dataSnapshot.child("car").getValue().toString());
                    }
                    if (dataSnapshot.child("profileImageUrl").getValue() != null) {
                        Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").getValue().toString()).into(mDriverProfileImage);
                    }

                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingsAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()) {
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if (ratingsTotal != 0) {
                        ratingsAvg = ratingSum / ratingsTotal;
                        mRatingBar.setRating(ratingsAvg);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;

    /*
     send customer request
      */

    private void getHasRideEnded() {
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void endRide() {
        requestBol = false;

        try{
            geoQuery.removeAllListeners();
            driverLocationRef.removeEventListener(driverLocationRefListener);
            driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
        }catch (Exception e){
            e.printStackTrace();
        }


        if (driverFoundID != null) {
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
            driverRef.removeValue();
            driverFoundID = null;

        }
        driverFound = false;
        radius = 1;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

        if (pickupMarker != null) {
            pickupMarker.remove();
//            Toast.makeText(this, "Request Cancelled", Toast.LENGTH_SHORT).show();
        } else {
//            Toast.makeText(this, "Request Approved", Toast.LENGTH_SHORT).show();
        }
        if (mDriverMarker != null) {
            mDriverMarker.remove();
//            Toast.makeText(this, "Request canceled", Toast.LENGTH_SHORT).show();
        }
        mRequest.setText("call GrabCab");

        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("Destination: --");
        mDriverProfileImage.setImageResource(R.mipmap.ic_default_user);
    }

    /*-------------------------------------------- Map specific functions -----
    |  Function(s) onMapReady, buildGoogleApiClient, onLocationChanged, onConnected
    |
    |  Purpose:  Find and update user's location.
    |
    |  Note:
    |	   The update interval is set to 1000Ms and the accuracy is set to PRIORITY_HIGH_ACCURACY,
    |      If you're having trouble with battery draining too fast then change these to lower values
    |
    |
    *-------------------------------------------------------------------*/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                checkLocationPermission();
            }
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    ///// get last location///
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    mLastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    //mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                    if (!getDriversAroundStarted)
                        getDriversAround();
                }
            }
        }
    };

    /*-------------------------------------------- onRequestPermissionsResult -----
    |  Function onRequestPermissionsResult
    |
    |  Purpose:  Get permissions for our app if they didn't previously exist.
    |
    |  Note:
    |	requestCode: the nubmer assigned to the request that we've made. Each
    |                request has it's own unique request code.
    |
    *-------------------------------------------------------------------*/
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


    boolean getDriversAroundStarted = false;
    List<Marker> markers = new ArrayList<Marker>();

    private void getDriversAround() {

        getDriversAroundStarted = true;

        final DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        DatabaseReference cancelDriverId = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        cancelDriverId.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String id = dataSnapshot.getKey();


                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        GeoFire geoFire = new GeoFire(driverLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude()), 999999999);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                try {
                    for (Marker markerIt : markers) {
                        if (markerIt.getTag().equals(key))
                            return;
                    }

                    LatLng driverLocation = new LatLng(location.latitude, location.longitude);

                    Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title(key).icon(BitmapDescriptorFactory.fromResource(R.mipmap.taxi)));
                    mDriverMarker.setTag(key);

                    markers.add(mDriverMarker);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onKeyExited(String key) {
                try {
                    for (Marker markerIt : markers) {
                        if (!key.equals("")) {
                            if (markerIt.getTag().equals(key)) {
                                markerIt.remove();
                            }
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                try {
                    for (Marker markerIt : markers) {
                        if (!key.equals("")) {
                            if (markerIt.getTag().equals(key)) {
                                markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                            }
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });


//        DatabaseReference driverStatus = FirebaseDatabase.getInstance().getReference().child("driversWorking").child("status");
//        driverStatus.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });


    }
}
