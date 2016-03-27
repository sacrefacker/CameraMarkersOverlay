package com.example.al.cameramarkersoverlay;


import android.content.Context;
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
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.al.cameramarkersoverlay.data.MarkersContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.Attributes;

public class FragmentOverlay extends Fragment
        implements SensorEventListener, LoaderManager.LoaderCallbacks<Cursor>,
        InterfaceSensors {

    private static final String LOG_TAG = FragmentOverlay.class.getSimpleName();
    private static final String BUNDLE_LOCATION = "location";
    private static final String BUNDLE_MARKERS = "markers";

    // id для CursorLoader
    private static final int MARKER_LOADER = 0;

    // для регистрации менеджеров локации
    private static final long POLLING_FREQ = 1000 * 10;
    private static final float MIN_DISTANCE = 10.0f;

    // выбираем колонки таблицы для извлечения курсором из БД
    private static final String[] MARKERS_COLUMNS = {
            MarkersContract.MarkersEntry._ID,
            MarkersContract.MarkersEntry.COLUMN_LAT,
            MarkersContract.MarkersEntry.COLUMN_LONG
    };
    // для получения значений из курсора
    private static final int CURSOR__ID = 0;
    private static final int CURSOR_COLUMN_LAT = 1;
    private static final int CURSOR_COLUMN_LONG = 2;

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

    // данные, которые понадобятся для отрисовки маркеров
    private Location mLocation;
    private ArrayList<Location> mMarkers;
    private double mAzimuth;
    private double mPitch;
    private double mRoll;

    private ViewOverlay mOverlaySurface;
    private TextView mAzimuthView;
    private TextView mLocationView;
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
    public ArrayList<Location> getMarkers() {
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        // вынимаем локацию, чтобы при смене ориентации и др. не ждать новую каждый раз
        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getParcelable(BUNDLE_LOCATION);
            mMarkers = savedInstanceState.getParcelableArrayList(BUNDLE_MARKERS);
        }

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            Log.e(LOG_TAG, "error getting location manager");
        }

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                mLocation = location;
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

        // пока что пытается "скачать" маркеры при каждом запуске
        downloadMarkers();
    }

    private void downloadMarkers() {
        new TaskDownloadMarkers(getContext()).execute();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {

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
                this
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

        mLocationView = new TextView(mContext);
        mLocationView.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
        mLocationView.setTextColor(Color.RED);
        frameLayout.addView(mLocationView);

        return rootView;
    }

    @Override
    public void onResume() {
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

        startPreview();
        mOverlaySurface.setVisibility(View.VISIBLE);
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

            // использует данные акселерометра и магнетометра для вычисления положения телефона относительно внешнего мира
            if (SensorManager.getRotationMatrix(rotationMatrix, null, mGravity, mGeomagnetic)) {
                float orientationMatrix[] = new float[3];

                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                mOrientationAverage = Utility.lowPassFilter(mOrientationAverage, orientationMatrix);

                // предполагаем, что телефон находится в положении камерой от пользователя на правом боку
                mAzimuth = Math.toDegrees(mOrientationAverage[0]);
                mPitch = Math.toDegrees(mOrientationAverage[1]);
                mRoll = Math.toDegrees(mOrientationAverage[2]);

                mAzimuthView.setText(String.format(mContext.getString(R.string.format_azimuth), mAzimuth));
                if (mLocation != null) {
                    mLocationView.setText(String.format(mContext.getString(R.string.format_location),
                            mLocation.getLatitude(), mLocation.getLongitude()));
                }
                else {
                    mLocationView.setText(" ");
                }

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // сохраняем локацию, чтобы потом не ждать
        outState.putParcelable(BUNDLE_LOCATION, mLocation);
        outState.putParcelableArrayList(BUNDLE_MARKERS, mMarkers);
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

        mOverlaySurface.setVisibility(View.INVISIBLE);

        super.onPause();
    }

    @Override
    public void onDestroy() {
        releaseCameraResources();
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MARKER_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
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
        mMarkers = cursorToList(cursor);
    }

    private ArrayList<Location> cursorToList(Cursor cursor) {
        ArrayList<Location> markers = new ArrayList<>();

        // заполняем ArrayList из многострочного курсора
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Location location = new Location(LocationManager.NETWORK_PROVIDER);
            location.setLatitude(cursor.getDouble(CURSOR_COLUMN_LAT));
            location.setLongitude(cursor.getDouble(CURSOR_COLUMN_LONG));
            markers.add(location);

            // задокументируем прекрасную работу
            Log.i(LOG_TAG, "Adding location from cursor: " + location.toString());
        }
        return markers;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mMarkers.clear();
    }

    private class WarningOverlayHandler implements ObserverWarning {
        boolean mIsVisible = false;
        double mOrientation = ViewOverlay.NO_ROLL;

        @Override
        public void update(boolean visible, double orientation) {
            if (mIsVisible != visible) {
                mIsVisible = visible;
                mOrientation = orientation;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rotateIfNeeded();
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

        private void rotateIfNeeded() {
            if (mOrientation == ViewOverlay.LEFT_ROLL) {
                mWarningView.setRotation(180);
            } else if (mOrientation == ViewOverlay.RIGHT_ROLL) {
                mWarningView.setRotation(0);
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

            mCamera.setDisplayOrientation(90);

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
