package com.example.al.cameramarkersoverlay;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;

public class FragmentOverlay extends Fragment implements SensorEventListener {
    public static final String LOG_TAG = FragmentOverlay.class.getSimpleName();
    public static final String BUNDLE_LOCATION = "location";

    private static final long POLLING_FREQ = 1000 * 10;
    private static final float MIN_DISTANCE = 10.0f;
    private static final float LOW_PASS_PERCENT = 0.85f;

    private static final double FIELD_OF_VIEW = 40.0;
    private static final double AZIMUTH_ORIENTATION_CORRECTION = -90.0;
    private static final double IDEAL_ROLL = 90.0;
    private static final double ROLL_TOLERANCE = 10.0;
    private static final double PITCH_TOLERANCE = 10.0;

    private Context mContext;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLocation;

    private ArrayList<Location> mMarkers;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    // Storage for Sensor readings
    private float[] mGravity = null;
    private float[] mGeomagnetic = null;
    private float[] mOrientationAverage;

    private double mAzimuth;
    private double mPitch;
    private double mRoll;

    // Views
//    private FrameLayout mOverlayFrame;
    private OverlayView mOverlaySurface;
    // For camera
    private Camera mCamera;
    private FrameLayout mFrameLayout;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsPreviewing;

    public FragmentOverlay() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getParcelable(BUNDLE_LOCATION);
        }

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            Log.e(LOG_TAG, "error getting location manager");
        }

        mLocationListener = new LocationListener() {
            // Called back when location changes
            public void onLocationChanged(Location location) {
                mLocation = location;
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

//        mMarkers = new ArrayList<>();
//        mMarkers.add(LOCATION_MOSCOW);
//        mMarkers.add(LOCATION_TOMSK);
//        mMarkers.add(LOCATION_LETI);
        loadMarkers();

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mOrientationAverage = new float[3];

        downloadMarkers();
    }

    private void loadMarkers() {
        // TODO: load markers with cursor
    }

    private void downloadMarkers() {
        new TaskDownloadMarkers(getContext()).execute();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_overlay, container, false);

        mFrameLayout = (FrameLayout) rootView.findViewById(R.id.frame_layout);

        // Setup SurfaceView for previewing camera image
//        SurfaceView surfaceView = (SurfaceView) rootView.findViewById(R.id.camera_surface);
        SurfaceView surfaceView = new SurfaceView(mContext);
        surfaceView.setZOrderMediaOverlay(false);
        mFrameLayout.addView(surfaceView);
        // Get SurfaceHolder for accessing the SurfaceView's Surface
        mSurfaceHolder = surfaceView.getHolder();
        // Set callback Object for the SurfaceHolder
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);

        mOverlaySurface = new OverlayView(mContext, BitmapFactory.decodeResource(getResources(),
                android.R.drawable.ic_delete));
        mOverlaySurface.setZOrderMediaOverlay(true);
        SurfaceHolder overlayHolder = mOverlaySurface.getHolder();
        overlayHolder.setFormat(PixelFormat.TRANSPARENT);
        mFrameLayout.addView(mOverlaySurface);
//        mOverlayFrame = (FrameLayout) rootView.findViewById(R.id.overlay_frame);
//        mOverlayFrame.addView(mOverlaySurface);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            // Register for network location updates
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

        if (null == mCamera) {
            try {
                // Returns first back-facing camera or null if no camera is available.
                // May take a long time to complete. Consider moving this to an AsyncTask
                mCamera = Camera.open();
            }
            catch (RuntimeException e) {
                Log.e(LOG_TAG, "Failed to acquire camera");
            }
        }
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
                mAzimuth = Math.toDegrees(mOrientationAverage[0]) + AZIMUTH_ORIENTATION_CORRECTION;
                mAzimuth = formatPiMinusPi(mAzimuth);
                mPitch = Math.toDegrees(mOrientationAverage[1]);
                mRoll = Math.toDegrees(mOrientationAverage[2]);

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

        releaseCameraResources();

        super.onPause();
    }

    // Overlay part

    private class OverlayView extends SurfaceView implements SurfaceHolder.Callback {
        private static final float NULL_FLOAT = -1.0f;
        private static final int TOUCH_SIZE = 100;

        private Thread mDrawingThread;

        private final SurfaceHolder mSurfaceHolder;
        private final Bitmap mBitmap;
        private final Paint mPainter = new Paint();

        // Y is the line of horizon and X is height
        private ArrayList<Float> mYList;
        float mX = NULL_FLOAT;
        private float mCanvasHalfWidth = 0, mCanvasHalfHeight = 0;

        public OverlayView(Context context, Bitmap bitmap) {
            super(context);

            mYList = new ArrayList<>(mMarkers.size());
            for (int i = 0; i < mMarkers.size(); i++) {
                mYList.add(NULL_FLOAT);
            }

            mBitmap = bitmap;
            mPainter.setAntiAlias(true);

            mSurfaceHolder = getHolder();
            mSurfaceHolder.addCallback(this);
        }

        private void drawMarkers(Canvas canvas) {

            if (mLocation != null
                    && mRoll > IDEAL_ROLL - ROLL_TOLERANCE && mRoll < IDEAL_ROLL + ROLL_TOLERANCE
                    && mPitch > -PITCH_TOLERANCE && mPitch < PITCH_TOLERANCE) {

                canvas.drawColor(0, PorterDuff.Mode.CLEAR);

                mX = mCanvasHalfWidth + ((float) ((mRoll - IDEAL_ROLL) / ROLL_TOLERANCE * mCanvasHalfWidth));

                for (int i = 0; i < mMarkers.size(); i++) {

                    int bitmapSize = getBitmapSizeOnDistance(mLocation.distanceTo(mMarkers.get(i)));
                    Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap,
                            bitmapSize,
                            bitmapSize,
                            false);

                    double azimuth = formatPiMinusPi(mLocation.bearingTo(mMarkers.get(i)) - mAzimuth);

                    if (azimuth > -FIELD_OF_VIEW && azimuth < FIELD_OF_VIEW) {
                        mYList.set(i, mCanvasHalfHeight - bitmapSize / 2
                                - ((float) (azimuth / FIELD_OF_VIEW * mCanvasHalfHeight)));
                    } else {
                        mYList.set(i, NULL_FLOAT);
                        continue;
                    }

                    canvas.drawBitmap(bitmap, mX - bitmapSize/2, mYList.get(i), mPainter);
                }
            }
            else {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                mX = NULL_FLOAT;
            }
        }

        private int getBitmapSizeOnDistance(float distanceTo) {
            double size = (double) getResources().getDimension(R.dimen.image_height_width);
            size = size / Math.log10((double) distanceTo);
            return (int) size;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN){
                float touchX = event.getX();
                float touchY = event.getY();
                if (touchX > mX - TOUCH_SIZE && touchX < mX + TOUCH_SIZE) {
                    for (int i = 0; i < mYList.size(); i++) {
                        if (touchY > mYList.get(i) - TOUCH_SIZE && touchY < mYList.get(i) + TOUCH_SIZE) {
                            openLocationOnMap(mMarkers.get(i));
                        }
                    }
                }
            }
            return true;
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
        public void surfaceCreated(SurfaceHolder holder) {
            mDrawingThread = new Thread(new Runnable() {
                public void run() {
                    Canvas canvas = null;
                    while (!Thread.currentThread().isInterrupted()) {
                        canvas = mSurfaceHolder.lockCanvas();
                        if (null != canvas) {
                            drawMarkers(canvas);
                            mSurfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }
                }
            });
            mDrawingThread.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
            mCanvasHalfWidth = width / 2;
            mCanvasHalfHeight = height / 2;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (null != mDrawingThread)
                mDrawingThread.interrupt();
        }
    }

    // Camera part

    private void startPreview() {
        if (null != mCamera) {
            try {
                mCamera.startPreview();
                mIsPreviewing = true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to start preview");
            }
        }
    }

    private void stopPreview() {
        if (null != mCamera && mIsPreviewing) {
            try {
                mCamera.stopPreview();
                mIsPreviewing = false;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to stop preview");
            }
        }
    }

    // Release camera so other applications can use it.
    private void releaseCameraResources() {
        if (null != mCamera) {
            mCamera.release();
            mCamera = null;
        }
    }

    // SurfaceHolder callback Object
    SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        @Override
        @SuppressWarnings("deprecation")
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            if (mSurfaceHolder.getSurface() == null) {
                return;
            }

            // Shutdown current preview
            stopPreview();

            mCamera.setDisplayOrientation(90);

            // Initialize preview display
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to set preview display in ");
            }

            // Start preview
            try {
                startPreview();
            } catch (RuntimeException e) {
                Log.e(LOG_TAG, "Failed to start preview in surfaceChanged()");
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // Do nothing
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Do Nothing
        }
    };

}
