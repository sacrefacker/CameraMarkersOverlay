package com.example.al.cameramarkersoverlay.temp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.al.cameramarkersoverlay.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentMain extends Fragment implements SensorEventListener {
    public static final String LOG_TAG = FragmentMain.class.getSimpleName();
    public static final String BUNDLE_LOCATION = "location";

    private static final long POLLING_FREQ = 1000 * 10;
    private static final float MIN_DISTANCE = 10.0f;
    private static final float LOW_PASS_PERCENT = 0.85f;
    private static final int SHOW_NOTHING = 0;
    private static final int SHOW_MOSCOW = 1;
    private static final int SHOW_TOMSK = 2;
    private static final int SHOW_LETI = 3;

    private static final double FIELD_OF_VIEW = 40.0;
    private static final double AZIMUTH_ORIENTATION_CORRECTION = -90.0;
    private static final double IDEAL_ROLL = 90.0;
    private static final double ROLL_TOLERANCE = 10.0;
    private static final double PITCH_TOLERANCE = 10.0;

    private static final Location LOCATION_MOSCOW;
    private static final Location LOCATION_TOMSK;
    private static final Location LOCATION_LETI;

    static {
        LOCATION_MOSCOW = new Location(LocationManager.NETWORK_PROVIDER);
        LOCATION_MOSCOW.setLatitude(55.7500);
        LOCATION_MOSCOW.setLongitude(37.6167);

        LOCATION_TOMSK = new Location(LocationManager.NETWORK_PROVIDER);
        LOCATION_TOMSK.setLatitude(56.5000);
        LOCATION_TOMSK.setLongitude(84.9667);

        LOCATION_LETI = new Location(LocationManager.NETWORK_PROVIDER);
        LOCATION_LETI.setLatitude(59.976560);
        LOCATION_LETI.setLongitude(30.320852);
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
    private TextView mLocationLetiTextView;
    private TextView mAzimuthLetiTextView;
    private TextView mOrientationTextView;

    private FrameLayout mFrameLayout;
    private OverlayView mOverlayView;

    // Storage for Sensor readings
    private float[] mGravity = null;
    private float[] mGeomagnetic = null;
    private float[] mOrientationAverage;

    private int mCurrentOverlay = SHOW_NOTHING;
    private double mRoll;
    private double mAzimuthMoscow;
    private double mAzimuthTomsk;
    private double mAzimuthLeti;

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
            mLocationLetiTextView.setText(String.format(mContext.getString(R.string.format_location_bearings),
                    LOCATION_LETI.getLatitude(),
                    LOCATION_LETI.getLongitude(),
                    mLocation.bearingTo(LOCATION_LETI),
                    mLocation.distanceTo(LOCATION_LETI)));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mLocationTextView = (TextView) rootView.findViewById(R.id.text_location);
        mLocationTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mLocation != null) {
                    openLocationOnMap(mLocation);
                    return true;
                }
                return false;
            }
        });
        mLocationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentOverlay = SHOW_NOTHING;
            }

        });
        mLocationMoscowTextView = (TextView) rootView.findViewById(R.id.text_location_moscow);
        mLocationMoscowTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openLocationOnMap(LOCATION_MOSCOW);
                return true;
            }
        });
        mLocationMoscowTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentOverlay = SHOW_MOSCOW;
            }

        });
        mLocationTomskTextView = (TextView) rootView.findViewById(R.id.text_location_tomsk);
        mLocationTomskTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openLocationOnMap(LOCATION_TOMSK);
                return true;
            }
        });
        mLocationTomskTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentOverlay = SHOW_TOMSK;
            }

        });
        mLocationLetiTextView = (TextView) rootView.findViewById(R.id.text_location_leti);
        mLocationLetiTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openLocationOnMap(LOCATION_LETI);
                return true;
            }
        });
        mLocationLetiTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentOverlay = SHOW_LETI;
            }

        });

        mAzimuthMoscowTextView = (TextView) rootView.findViewById(R.id.text_azimuth_moscow);
        mAzimuthTomskTextView = (TextView) rootView.findViewById(R.id.text_azimuth_tomsk);
        mAzimuthLetiTextView = (TextView) rootView.findViewById(R.id.text_azimuth_leti);
        mOrientationTextView = (TextView) rootView.findViewById(R.id.text_orientation);

        mFrameLayout = (FrameLayout) rootView.findViewById(R.id.frame_for_overlay);
        mOverlayView = new OverlayView(mContext, BitmapFactory.decodeResource(getResources(),
                android.R.drawable.ic_delete));
        mFrameLayout.addView(mOverlayView);

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
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
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
                mOrientationAverage = lowPassFilter(mOrientationAverage, orientationMatrix);

                // Assuming the device is on it's right side and camera away from user
                double azimuth = Math.toDegrees(mOrientationAverage[0]) + AZIMUTH_ORIENTATION_CORRECTION;
                double pitch = Math.toDegrees(mOrientationAverage[1]);
                mRoll = Math.toDegrees(mOrientationAverage[2]);
                azimuth = formatPiMinusPi(azimuth);

                mOrientationTextView.setText(String.format(mContext.getString(R.string.format_sensor),
                        azimuth,
                        pitch,
                        mRoll));
                if (mRoll > IDEAL_ROLL - ROLL_TOLERANCE && mRoll < IDEAL_ROLL + ROLL_TOLERANCE
                        && pitch > -PITCH_TOLERANCE && pitch < PITCH_TOLERANCE) {
                    mOrientationTextView.setTextColor(Color.GREEN);
                }
                else {
                    mOrientationTextView.setTextColor(Color.RED);
                }

                if (mLocation != null) {

                    mAzimuthMoscow = mLocation.bearingTo(LOCATION_MOSCOW) - azimuth;
                    mAzimuthMoscow = formatPiMinusPi(mAzimuthMoscow);
                    mAzimuthMoscowTextView.setText(String.format(mContext.getString(R.string.format_azimuth),
                            mAzimuthMoscow));
                    checkInFieldOfView(mAzimuthMoscow, mAzimuthMoscowTextView);

                    mAzimuthTomsk = mLocation.bearingTo(LOCATION_TOMSK) - azimuth;
                    mAzimuthTomsk = formatPiMinusPi(mAzimuthTomsk);
                    mAzimuthTomskTextView.setText(String.format(mContext.getString(R.string.format_azimuth),
                            mAzimuthTomsk));
                    checkInFieldOfView(mAzimuthTomsk, mAzimuthTomskTextView);

                    mAzimuthLeti = mLocation.bearingTo(LOCATION_LETI) - azimuth;
                    mAzimuthLeti = formatPiMinusPi(mAzimuthLeti);
                    mAzimuthLetiTextView.setText(String.format(mContext.getString(R.string.format_azimuth),
                            mAzimuthLeti));
                    checkInFieldOfView(mAzimuthLeti, mAzimuthLetiTextView);
                }

                // Reset sensor event data arrays
                mGravity = mGeomagnetic = null;
            }
        }
    }

    private float[] lowPassFilter(float[] orientationAverage, float[] orientationMatrix) {
        if (orientationMatrix.length == orientationAverage.length) {
            // azimuth needs a special treatment because it can jump from -180 to 180 and back
            double average = Math.toDegrees(orientationAverage[0]);
            double current = Math.toDegrees(orientationMatrix[0]);
            double correction = 0;
            if (average - current > 180) {
                correction = 360;
            }
            else if (average - current < -180){
                correction = -360;
            }
            orientationAverage[0] = (float) Math.toRadians(average +
                    (current - average + correction) * (1 - LOW_PASS_PERCENT));
            // pitch and roll are fine
            for (int i = 1; i < orientationMatrix.length; i++) {
                orientationAverage[i] = orientationAverage[i] +
                        (orientationMatrix[i] - orientationAverage[i]) * (1 - LOW_PASS_PERCENT);
            }
        }
        return orientationAverage;
    }

    private double formatPiMinusPi(double angle) {
        angle = (angle < -180) ? angle + 360 : angle;
        angle = (angle > 180) ? angle - 360 : angle;
        return angle;
    }

    private void checkInFieldOfView(double azimuth, TextView textView) {
        if (azimuth > -FIELD_OF_VIEW && azimuth < FIELD_OF_VIEW) {
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


    private class OverlayView extends SurfaceView implements SurfaceHolder.Callback {

        private final SurfaceHolder mSurfaceHolder;
        private final Bitmap mBitmap;
        private final Paint mPainter = new Paint();
        private final int mBitmapSize;

        private Thread mDrawingThread;

        public OverlayView(Context context, Bitmap bitmap) {
            super(context);

            mBitmapSize = (int) getResources().getDimension(R.dimen.image_height_width);
            this.mBitmap = Bitmap.createScaledBitmap(bitmap,
                    mBitmapSize,
                    mBitmapSize,
                    false);

            mPainter.setAntiAlias(true);

            mSurfaceHolder = getHolder();
            mSurfaceHolder.addCallback(this);
        }

        private void drawMarker(Canvas canvas) {
            canvas.drawColor(Color.DKGRAY);
            double azimuth = 1000; // intentionally unrealistic just to initialize
            switch (mCurrentOverlay) {
                case SHOW_NOTHING:
                    return;
                case SHOW_MOSCOW:
                    azimuth = mAzimuthMoscow;
                    break;
                case SHOW_TOMSK:
                    azimuth = mAzimuthTomsk;
                    break;
                case SHOW_LETI:
                    azimuth = mAzimuthLeti;
                    break;
            }
            float left = mFrameLayout.getWidth()/2.0f - mBitmapSize/2.0f;
            if (mRoll > IDEAL_ROLL - ROLL_TOLERANCE && mRoll < IDEAL_ROLL + ROLL_TOLERANCE) {
                left = left + ((float) ((mRoll - IDEAL_ROLL) / ROLL_TOLERANCE * mFrameLayout.getWidth() / 2.0));
            }
            else {
                return;
            }
            float top = mFrameLayout.getHeight()/2.0f - mBitmapSize/2.0f;
            if (azimuth > -FIELD_OF_VIEW && azimuth < FIELD_OF_VIEW) {
                top = top - ((float) (azimuth / FIELD_OF_VIEW * mFrameLayout.getHeight() / 2.0));
            }
            else {
                return;
            }
            canvas.drawBitmap(mBitmap, left, top, mPainter);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
            //nothing to see here
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mDrawingThread = new Thread(new Runnable() {
                public void run() {
                    Canvas canvas = null;
                    while (!Thread.currentThread().isInterrupted()) {
                        canvas = mSurfaceHolder.lockCanvas();
                        if (null != canvas) {
                            drawMarker(canvas);
                            mSurfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }
                }
            });
            mDrawingThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (null != mDrawingThread)
                mDrawingThread.interrupt();
        }
    }
}
