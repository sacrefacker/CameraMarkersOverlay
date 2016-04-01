package com.example.al.cameramarkersoverlay;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.al.cameramarkersoverlay.data.LocationMarker;
import com.example.al.cameramarkersoverlay.data.MarkersContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class FragmentOverlay extends Fragment
        implements SensorEventListener, LoaderManager.LoaderCallbacks<Cursor>,
        InterfaceSensors {

    private static final String LOG_TAG = FragmentOverlay.class.getSimpleName();
    // для сохранения в shared preferences
    private static final String PREFS_Q_LAT = "qLat";
    private static final String PREFS_Q_LONG = "qLong";
    private static final String PREFS_LAT = "lat";
    private static final String PREFS_LONG = "long";

    // id для CursorLoader
    private static final int MARKER_LOADER = 0;

    // для регистрации менеджеров локации
    private static final long POLLING_FREQ = 1000 * 10;
    private static final float MIN_DISTANCE = 10.0f;
    private static final float DISTANCE_BEFORE_MARKERS_UPDATE = 500.0f;

    // выбираем колонки таблицы для извлечения курсором из БД
    private static final String[] MARKERS_COLUMNS = {
            MarkersContract.MarkersEntry.COLUMN_LAT,
            MarkersContract.MarkersEntry.COLUMN_LONG
    };
    // для получения значений из курсора
    private static final int CURSOR_COLUMN_LAT = 0;
    private static final int CURSOR_COLUMN_LONG = 1;

    private Context mContext;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    // данные с сенсоров и средние значения из getOrientation для фильтрации
    private float[] mGravity = null;
    private float[] mGeomagnetic = null;
    private float[] mOrientationAverage;

    // для работы камеры
    private Camera mCamera;
    private SurfaceHolder mCameraHolder;
    private boolean mIsPreviewing;

    private Location mQueryLocation = null; // локация, из которой запрашивались точки с сервера
    // данные, которые понадобятся для отрисовки маркеров
    private Location mLocation = null; // текущая локация
    private ArrayList<LocationMarker> mMarkers;
    private double mAzimuth;
    private double mPitch;
    private double mRoll;

    private ViewOverlay mOverlaySurface;
    private TextView mAzimuthView;
    private ImageView mWarningView;

    public FragmentOverlay() {
        // Необходим пустой публичный контейнер (не помню, по какой причине)
    }

    @Override
    public double getAzimuth() {
        return mAzimuth;
    }

    @Override
    public Location getLocation() {
        return mLocation;
    }

    @Override
    public ArrayList<LocationMarker> getMarkers() {
        return mMarkers;
    }

    @Override
    public double getPitch() {
        return mPitch;
    }

    @Override
    public double getRoll() {
        return mRoll;
    }

    @Override
    public int getScreenRotation() {
        WindowManager wm = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE));
        return wm.getDefaultDisplay().getRotation();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            Log.e(LOG_TAG, "error getting location manager");
        }

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (mLocation == null) {
                    mLocation = location;
                    initLoader();
                }
                else {
                    mLocation = location;
                }
                // если достаточно отошли от места, с которого запрашивали точки с сервера
                // (или не запрашивали вообще), делаем это ещё раз
                if (mQueryLocation == null
                        || mQueryLocation.distanceTo(mLocation) > DISTANCE_BEFORE_MARKERS_UPDATE) {

                    Log.i(LOG_TAG, "Downloading markers");
                    downloadMarkers();
                    mQueryLocation = new Location(mLocation);
                }
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //
            }
            public void onProviderEnabled(String provider) {
                //
            }
            public void onProviderDisabled(String provider) {
                //
            }
        };

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mOrientationAverage = new float[3];

        mMarkers = new ArrayList<>();
    }

    private void downloadMarkers() {
        new TaskDownloadMarkers(getContext()).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        Log.i(LOG_TAG, "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_overlay, container, false);

        FrameLayout frameLayout = (FrameLayout) rootView.findViewById(R.id.frame_layout);

        // SurfaceView для просмотра видео с камеры
        SurfaceView surfaceView = new SurfaceView(mContext);
        surfaceView.setZOrderMediaOverlay(false);
        frameLayout.addView(surfaceView);
        mCameraHolder = surfaceView.getHolder();
        mCameraHolder.addCallback(mSurfaceHolderCallback);

        // ViewOverlay для отображения маркеров поверх видео (унаследован от SurfaceView)
        mOverlaySurface = new ViewOverlay(
                mContext,
                BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_delete),
                this,
                getFragmentManager()
        );
        mOverlaySurface.registerObserver(new WarningOverlayHandler());
        mOverlaySurface.setZOrderMediaOverlay(true);
        SurfaceHolder overlayHolder = mOverlaySurface.getHolder();
        overlayHolder.setFormat(PixelFormat.TRANSPARENT);
        frameLayout.addView(mOverlaySurface);

        LinearLayout warningFrame = new LinearLayout(mContext);
        warningFrame.setGravity(Gravity.CENTER);
        // ImageView для отображения картинки-подсказки
        mWarningView = new ImageView(mContext);
        mWarningView.setImageResource(R.drawable.overlay_warning);
        warningFrame.addView(mWarningView);
        frameLayout.addView(warningFrame);

        // TextView для отображения текущего значения азимута и локации
        mAzimuthView = new TextView(mContext);
        mAzimuthView.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
        mAzimuthView.setTextColor(Color.RED);
        frameLayout.addView(mAzimuthView);

        return rootView;
    }

    @Override
    public void onResume() {
        Log.i(LOG_TAG, "onResume");
        super.onResume();

        try {
            // Обновления локации от провайдера
            if (null != mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager
                        .NETWORK_PROVIDER, POLLING_FREQ, MIN_DISTANCE, mLocationListener);
            }
            // Обновления локации от GPS
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
                // Отдаёт камеру, смотрящую вперёд, или null в случае её отсутствия
                // Может занять длительное время, рекомендуется перенести в AsyncTask
                mCamera = Camera.open();
            }
            catch (RuntimeException e) {
                Log.e(LOG_TAG, "Failed to acquire camera");
            }
        }

        // TODO: comment this line to debug downloading
        resumeLocation();

        startPreview();
        mOverlaySurface.setVisibility(View.VISIBLE);
    }

    private void resumeLocation() {
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        double qLat = Double.longBitsToDouble(prefs.getLong(PREFS_Q_LAT, 0));
        double qLon = Double.longBitsToDouble(prefs.getLong(PREFS_Q_LONG, 0));
        if (mQueryLocation == null && qLat != 0.0 && qLon != 0.0) {
            mQueryLocation = new Location(LocationManager.NETWORK_PROVIDER);
            mQueryLocation.setLatitude(qLat);
            mQueryLocation.setLongitude(qLon);
        }
        double lat = Double.longBitsToDouble(prefs.getLong(PREFS_LAT, 0));
        double lon = Double.longBitsToDouble(prefs.getLong(PREFS_LONG, 0));
        if (mLocation == null && lat != 0.0 && lon != 0.0) {
            mLocation = new Location(LocationManager.NETWORK_PROVIDER);
            mLocation.setLatitude(lat);
            mLocation.setLongitude(lon);
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

        // сначала получаем новые данные от обоих сенсоров, только после этого ведём расчёты
        if (mGravity != null && mGeomagnetic != null) {
            float rotationMatrix[] = new float[9];

            // использует данные акселерометра и магнетометра для вычисления положения телефона
            if (SensorManager.getRotationMatrix(rotationMatrix, null, mGravity, mGeomagnetic)) {
                float orientationMatrix[] = new float[3];

                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                mOrientationAverage = Utility.lowPassFilter(mOrientationAverage, orientationMatrix);

                // предполагаем, что телефон находится в положении камерой от пользователя на правом боку
                mAzimuth = Math.toDegrees(mOrientationAverage[0]);
                mPitch = Math.toDegrees(mOrientationAverage[1]);
                mRoll = Math.toDegrees(mOrientationAverage[2]);

                mAzimuthView.setText(String.format(mContext.getString(R.string.format_azimuth), mAzimuth));

                // обнуляем данные, чтобы ждать новые - так они каждый раз будут свеженькими
                mGravity = mGeomagnetic = null;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //
    }

    @Override
    public void onPause() {
        Log.i(LOG_TAG, "onPause");
        try {
            mLocationManager.removeUpdates(mLocationListener);
        }
        catch (SecurityException ex) {
            ex.printStackTrace();
        }

        saveLocation();

        releaseCameraResources();

        mSensorManager.unregisterListener(this);

        mOverlaySurface.setVisibility(View.INVISIBLE);

        super.onPause();
    }

    private void saveLocation() {
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (mQueryLocation != null) {
            editor.putLong(PREFS_Q_LAT, Double.doubleToLongBits(mQueryLocation.getLatitude()));
            editor.putLong(PREFS_Q_LONG, Double.doubleToLongBits(mQueryLocation.getLongitude()));
        }
        if (mLocation != null) {
            editor.putLong(PREFS_LAT, Double.doubleToLongBits(mLocation.getLatitude()));
            editor.putLong(PREFS_LONG, Double.doubleToLongBits(mLocation.getLongitude()));
        }
        editor.apply();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onActivityCreated");
        initLoader();
        super.onActivityCreated(savedInstanceState);
    }

    private void initLoader() {
        getLoaderManager().initLoader(MARKER_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri markersUri = MarkersContract.MarkersEntry.buildAllMarkers();

        return new CursorLoader(
                mContext, // контекст
                markersUri, // URI
                MARKERS_COLUMNS, // выбор колонок таблицы
                null, // условия
                null, // аргументы для условий (подстановка в "?")
                null // сортировка
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i(LOG_TAG, "LoaderFinished");
        mMarkers = cursorToList(cursor);
    }

    private ArrayList<LocationMarker> cursorToList(Cursor cursor) {
        ArrayList<LocationMarker> markers = new ArrayList<>();
        Log.i(LOG_TAG, "Cursor entries count = " + cursor.getCount());

        // заполняем ArrayList из многострочного курсора
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            LocationMarker locationMarker = new LocationMarker(
                    cursor.getDouble(CURSOR_COLUMN_LAT),
                    cursor.getDouble(CURSOR_COLUMN_LONG));
            if (mLocation != null) {
                locationMarker.setDistance(mLocation.distanceTo(locationMarker.getLocation()));
            }
            markers.add(locationMarker);

            // задокументируем прекрасную работу
            Log.i(LOG_TAG, "Adding location from cursor: " + locationMarker.getInfo());
        }
        Collections.sort(mMarkers);

        return markers;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mMarkers.clear();
    }

    private class WarningOverlayHandler implements ObserverWarning {
        boolean mIsVisible = false;

        @Override
        public void update(boolean visible) {
            if (mIsVisible != visible) {
                mIsVisible = visible;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsVisible) {
                            mWarningView.setVisibility(View.VISIBLE);
                        }
                        else {
                            mWarningView.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        }
    }

    // Часть, ответственная за отрисовку картинки с камеры

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

    // освободить ресурс камеры, чтобы другие программы могли её использовать
    private void releaseCameraResources() {
        if (null != mCamera) {
            mCamera.release();
            mCamera = null;
        }
    }

    SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        @Override
        @SuppressWarnings("deprecation")
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            if (mCameraHolder.getSurface() == null) {
                return;
            }
            stopPreview();

            int screenRotation = getScreenRotation();
            switch (screenRotation) {
                case Surface.ROTATION_90:
                    Log.i(LOG_TAG, "ROTATION_90");
                    mCamera.setDisplayOrientation(0);
                    break;
                case Surface.ROTATION_270:
                    Log.i(LOG_TAG, "ROTATION_270");
                    mCamera.setDisplayOrientation(180);
                    break;
                case Surface.ROTATION_0:
                    Log.i(LOG_TAG, "ROTATION_0");
                default:
                    Log.i(LOG_TAG, "default");
                    mCamera.setDisplayOrientation(90);
                    break;
            }

            // инициализировать поверхность отображения просмотра
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to set preview display in ");
            }

            try {
                startPreview();
            } catch (RuntimeException e) {
                Log.e(LOG_TAG, "Failed to start preview in surfaceChanged()");
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            //
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            //
        }
    };

}
