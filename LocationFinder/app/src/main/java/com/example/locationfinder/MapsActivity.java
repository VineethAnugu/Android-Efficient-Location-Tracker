package com.example.locationfinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.example.locationfinder.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, SensorEventListener {

    private GoogleMap mMap;
    private LocationManager myLocationManager;
    private Handler myHandler;
    int i = 0;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private SensorManager sensorManager_;
    private Sensor accelerometer, magnetometer;
    private double distance, resTime = 0.2;
    private double[] gravity={0,0,0}, Final_Acc={0,0,0}, velocity={0,0,0}, distance_={0,0,0};
    private float[] magnetometer_={0,0,0}, acceleration_={0,0,0};
    private float azimuth;
    private LatLng currentLocation;
    private double latitude_, longitude_;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Setting up sensors
        sensorManager_ = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager_.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            Toast.makeText(getApplicationContext(), "Accelerometer not available on this device", Toast.LENGTH_LONG).show();

        }
        magnetometer = sensorManager_.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (accelerometer == null) {
            Toast.makeText(getApplicationContext(), "Magnetometer not available on this device", Toast.LENGTH_LONG).show();

        }
        sensorManager_.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager_.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_NORMAL);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Code for run-time user permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

        }

        //GPS location request
        myLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        myHandler = new Handler();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
        Toast.makeText(getApplicationContext(), "GPS activated", Toast.LENGTH_LONG).show();

    }

    //Run-time user permission ends up here
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
                    Toast.makeText(getApplicationContext(), "GPS activated", Toast.LENGTH_LONG).show();


                } else {
                    Toast.makeText(getApplicationContext(), "Location permission needed for the application to work", Toast.LENGTH_LONG);
                }
                return;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //Accelerometer code
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            final double alpha = 0.8;
            acceleration_ = event.values.clone();

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            Final_Acc[0] = event.values[0] - gravity[0];
            Final_Acc[1] = event.values[1] - gravity[1];
            Final_Acc[2] = event.values[2] - gravity[2];

            velocity[0] = Final_Acc[0] * resTime;
            velocity[1] = Final_Acc[1] * resTime;
            velocity[2] = Final_Acc[2] * resTime;

            distance_[0] = velocity[0] * resTime;
            distance_[1] = velocity[1] * resTime;
            distance_[2] = velocity[2] * resTime;

            distance = Math.sqrt((distance_[0] * distance_[0]) + (distance_[1] * distance_[1]) + (distance_[2] * distance_[2]));

        }
        //Magnetometer code
        if(event.sensor.getType()== Sensor.TYPE_MAGNETIC_FIELD){
            magnetometer_ = event.values.clone();

            float[] mRotationMatrix = new float[9];
            float[] orientationVals = new float[3];
            SensorManager.getRotationMatrix(mRotationMatrix,null,acceleration_,magnetometer_);
            SensorManager.getOrientation(mRotationMatrix,orientationVals);
            azimuth = (float) (orientationVals[0] *(180/3.14));

            if(currentLocation != null)//Run only if coordinates are not null
            {
                currentLocation = SphericalUtil.computeOffset(currentLocation, distance, azimuth);
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(currentLocation).title("Lat:" + latitude_ + ", Long:" + longitude_ )).showInfoWindow();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //Runnable work for adding marker at given LatLng
    private class LocationWork implements Runnable {


        public LocationWork(double lat_, double long_) {
            latitude_ = lat_;
            longitude_ = long_;
        }

        @Override
        public void run() {
            currentLocation = new LatLng(latitude_, longitude_);
            distance = 0;
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Lat:"+latitude_+", Long:"+longitude_)).showInfoWindow();
            Toast.makeText(getApplicationContext(), "GPS activated", Toast.LENGTH_LONG).show();
            if (i == 0) {
                float zoomLevel = 16.0f;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel));
                i++;
            }
        }
    }

    //Getting current LatLng data
    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LocationWork myWork = new LocationWork(latitude, longitude);
        myHandler.post(myWork);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        myLocationManager.removeUpdates(this);
        sensorManager_.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);
        Toast.makeText(getApplicationContext(), "GPS activated", Toast.LENGTH_LONG).show();
        sensorManager_.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager_.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_NORMAL);


    }
}
