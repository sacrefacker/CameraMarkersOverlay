package com.example.al.cameramarkersoverlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentMain extends Fragment implements SensorEventListener {
    public static final String LOG_TAG = FragmentMain.class.getSimpleName();
    public static final String BUNDLE_LOCATION = "location";

    private static final long POLLING_FREQ = 1000 * 10;
    private static final float MIN_DISTANCE = 10.0f;
    public static final float LOW_PASS_PERCENT = 0.85f;
//    private static final int UPDATE_THRESHOLD = 500;

    private static final Location LOCATION_MOSCOW;
    private static final Location LOCATION_TOMSK;

    static {
        LOCATION_MOSCOW = new Location(LocationManager.NETWORK_PROVIDER);
        LOCATION_MOSCOW.setLatitude(55.7500);
        LOCATION_MOSCOW.setLongitude(37.6167);
        LOCATION_TOMSK = new Location(LocationManager.NETWORK_PROVIDER);
        LOCATION_TOMSK.setLatitude(56.5000);
        LOCATION_TOMSK.setLongitude(84.9667);
    }

    private Context mContext;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLocation;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private TextView mLocationTextView;
    private TextView mLocationMoscowTextView;
    private TextView mAzimuthMoscowTextView;
    private TextView mLocationTomskTextView;
    private TextView mAzimuthTomskTextView;
    private TextView mOrientationTextView;

//    private long mLastUpdateAccelerometer;
//    private long mLastUpdateMagnetometer;

    // Storage for Sensor readings
    private float[] mGravity = null;
    private float[] mGeomagnetic = null;
    private float[] mOrientationAverage;

    public FragmentMain() {
        //nothing to see here
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getParcelable(BUNDLE_LOCATION);
        }

        // Acquire reference to the LocationManager
        if (null == (mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE))) {
            Log.e(LOG_TAG, "error getting location manager");
        }
        
        mLocationListener = new LocationListener() {
            // Called back when location changes
            public void onLocationChanged(Location location) {
                mLocation = location;
                updateLocationTextView();
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //nothing to see here
            }
            public void onProviderEnabled(String provider) {
                //nothing to see here
            }
            public void onProviderDisabled(String provider) {
                //nothing to see here
            }
        };

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mOrientationAverage = new float[3];
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();

        if (type == Sensor.TYPE_ACCELEROMETER) {
            mGravity = new float[3];
            System.arraycopy(event.values, 0, mGravity, 0, 3);
        }

        else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = new float[3];
            System.arraycopy(event.values, 0, mGeomagnetic, 0, 3);
        }

        if (mGravity != null && mGeomagnetic != null) {
            float rotationMatrix[] = new float[9];

            // Users the accelerometer and magnetometer readings to compute the device's rotation
            // with respect to a real world coordinate system
            if (SensorManager.getRotationMatrix(rotationMatrix, null, mGravity, mGeomagnetic)) {
                float orientationMatrix[] = new float[3];

                // Returns the device's orientation given the rotationMatrix
                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                mOrientationAverage = lowPass(orientationMatrix, mOrientationAverage);

                // Assuming the device is on it's right side and camera away from user
                double azimuth = Math.toDegrees(mOrientationAverage[0]) - 90;
                double pitch = Math.toDegrees(mOrientationAverage[1]);
                double roll = Math.toDegrees(mOrientationAverage[2]);
                azimuth = formatPiMinusPi(azimuth);

                mOrientationTextView.setText(String.format(mContext.getString(R.string.format_sensor),
                        azimuth,
                        pitch,
                        roll));
                if (roll > 80 && roll < 100
                        && pitch > -10 && pitch < 10) {
                    mOrientationTextView.setTextColor(Color.GREEN);
                }
                else {
                    mOrientationTextView.setTextColor(Color.RED);
                }

                if (mLocation != null) {

                    double moscowAzimuth = mLocation.bearingTo(LOCATION_MOSCOW) - azimuth;
                    moscowAzimuth = formatPiMinusPi(moscowAzimuth);
                    mAzimuthMoscowTextView.setText(String.format(mContext.getString(R.string.format_azimuth),
                            moscowAzimuth));
                    checkInFieldOfView(moscowAzimuth, mAzimuthMoscowTextView);

                    double tomskAzimuth = mLocation.bearingTo(LOCATION_TOMSK) - azimuth;
                    tomskAzimuth = formatPiMinusPi(tomskAzimuth);
                    mAzimuthTomskTextView.setText(String.format(mContext.getString(R.string.format_azimuth),
                            tomskAzimuth));
                    checkInFieldOfView(tomskAzimuth, mAzimuthTomskTextView);
                }

                // Reset sensor event data arrays
                mGravity = mGeomagnetic = null;
            }
        }

    }

    private double formatPiMinusPi(double angle) {
        angle = (angle < -180) ? angle + 360 : angle;
        angle = (angle > 180) ? angle - 360 : angle;
        return angle;
    }

    private float[] lowPass(float[] orientationMatrix, float[] orientationAverage) {
        if (orientationMatrix.length == orientationAverage.length) {
            for (int i = 0; i < orientationMatrix.length; i++) {
                orientationAverage[i] = orientationAverage[i] * LOW_PASS_PERCENT +
                        orientationMatrix[i] * (1 - LOW_PASS_PERCENT);
            }
        }
        return orientationAverage;
    }

    private void checkInFieldOfView(double azimuth, TextView textView) {
        if (azimuth > -30 && azimuth < 30) {
            textView.setTextColor(Color.GREEN);
        }
        else {
            textView.setTextColor(Color.RED);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //nothing to see here
    }

    private void updateLocationTextView() {
        if (mLocation != null) {
            mLocationTextView.setText(String.format(mContext.getString(R.string.format_location),
                    mLocation.getLatitude(),
                    mLocation.getLongitude()));
            mLocationMoscowTextView.setText(String.format(mContext.getString(R.string.format_location_bearings),
                    LOCATION_MOSCOW.getLatitude(),
                    LOCATION_MOSCOW.getLongitude(),
                    mLocation.bearingTo(LOCATION_MOSCOW),
                    mLocation.distanceTo(LOCATION_MOSCOW)));
            mLocationTomskTextView.setText(String.format(mContext.getString(R.string.format_location_bearings),
                    LOCATION_TOMSK.getLatitude(),
                    LOCATION_TOMSK.getLongitude(),
                    mLocation.bearingTo(LOCATION_TOMSK),
                    mLocation.distanceTo(LOCATION_TOMSK)));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mLocationTextView = (TextView) rootView.findViewById(R.id.text_location);
        mLocationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocation != null) {
                    openLocationOnMap(mLocation);
                }
            }
        });
        mLocationMoscowTextView = (TextView) rootView.findViewById(R.id.text_location_moscow);
        mLocationMoscowTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLocationOnMap(LOCATION_MOSCOW);
            }
        });
        mLocationTomskTextView = (TextView) rootView.findViewById(R.id.text_location_tomsk);
        mLocationTomskTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLocationOnMap(LOCATION_TOMSK);
            }
        });

        mAzimuthMoscowTextView = (TextView) rootView.findViewById(R.id.text_azimuth_moscow);
        mAzimuthTomskTextView = (TextView) rootView.findViewById(R.id.text_azimuth_tomsk);
        mOrientationTextView = (TextView) rootView.findViewById(R.id.text_orientation);

        return rootView;
    }

    private void openLocationOnMap(Location location) {

        String locationString = "geo:".concat(String.valueOf(location.getLatitude())).concat(",")
                .concat(String.valueOf(location.getLongitude()));
        Uri geoLocation = Uri.parse(locationString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        Log.i(LOG_TAG, locationString);

        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't resolve");
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        updateLocationTextView();

        // Register for network location updates
        try {
            if (null != mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager
                        .NETWORK_PROVIDER, POLLING_FREQ, MIN_DISTANCE, mLocationListener);
            }
            // Register for GPS location updates
            if (null != mLocationManager.getProvider(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager
                        .GPS_PROVIDER, POLLING_FREQ, MIN_DISTANCE, mLocationListener);
            }
        }
        catch (SecurityException ex) {
            ex.printStackTrace();
        }

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
//        mLastUpdateAccelerometer = System.currentTimeMillis();
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
//        mLastUpdateMagnetometer = System.currentTimeMillis();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_LOCATION, mLocation);
    }

    @Override
    public void onPause() {
        try {
            mLocationManager.removeUpdates(mLocationListener);
        }
        catch (SecurityException ex) {
            ex.printStackTrace();
        }

        mSensorManager.unregisterListener(this);

        super.onPause();
    }
}
