package com.example.al.cameramarkersoverlay;

import android.app.DialogFragment;
import android.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

class ViewOverlay extends SurfaceView implements SurfaceHolder.Callback, ObservableWarning {

    private static final String LOG_TAG = ViewOverlay.class.getSimpleName();
    private static final String FRAGMENT_TAG = "fragmentDialog";

    private static final double FIELD_OF_VIEW = 40.0;
    private static final double ROLL_TOLERANCE = 10.0;
    private static final double PITCH_TOLERANCE = 10.0;

    private static final float NULL_FLOAT = -1000.0f;
    private static final int TOUCH_SIZE = 100;

    // коррекция значения азимута в связи с ориентацией телефона во время искользования приложения
    private static final double AZIMUTH_ORIENTATION_CORRECTION_RIGHT = -90.0;
    private static final double AZIMUTH_ORIENTATION_CORRECTION_LEFT = 90;
    public static final double RIGHT_ROLL = 90.0;
    public static final double LEFT_ROLL = -90.0;
    public static final double NO_ROLL = 0.0;

    private double mOrientation = NO_ROLL;
    private double mCorrection = AZIMUTH_ORIENTATION_CORRECTION_RIGHT;

    private Context mContext;
    private FragmentManager mFragmentManager;

    private InterfaceSensors mSensors;
    private List<ObserverWarning> mObserversWarning;
    private Thread mDrawingThread;

    private final SurfaceHolder mSurfaceHolder;
    private final Bitmap mBitmap;
    private final Paint mPainter = new Paint();
    private final Paint mCirclePainter = new Paint();

    // Y - линия горизонта, X - вертикаль
    private ArrayList<Float> mYList;
    private float mX = NULL_FLOAT;
    private float mCanvasHalfWidth = 0, mCanvasHalfHeight = 0;

    public ViewOverlay(Context context, Bitmap bitmap, InterfaceSensors interfaceSensors, FragmentManager fragmentManager) {

        super(context);
        mContext = context;
        mFragmentManager = fragmentManager;
        mSensors = interfaceSensors;
        mObserversWarning = new ArrayList<>();

        initYList();

        mBitmap = bitmap;
        mPainter.setAntiAlias(true);

        // для точки в середине экрана
        mCirclePainter.setARGB(255, 255, 0, 0);
        mCirclePainter.setStyle(Paint.Style.FILL_AND_STROKE);

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    // создание заново списка Y координат пригождается при изменении списка маркеров и в конструкторе
    private void initYList() {
        mYList = new ArrayList<>(mSensors.getMarkers().size());
        for (int i = 0; i < mSensors.getMarkers().size(); i++) {
            mYList.add(NULL_FLOAT);
        }
    }

    private void drawMarkers(Canvas canvas) {

        if (mSensors.getMarkers().size() != mYList.size()) {
            initYList();
        }

        // точка посередине холста для лучшей ориентации на глаз
        canvas.drawCircle(mCanvasHalfWidth, mCanvasHalfHeight, 10.0f, mCirclePainter);

        if (mSensors.getRoll() > RIGHT_ROLL - ROLL_TOLERANCE
                && mSensors.getRoll() < RIGHT_ROLL + ROLL_TOLERANCE) {

            mOrientation = RIGHT_ROLL;
            mCorrection = AZIMUTH_ORIENTATION_CORRECTION_RIGHT;
        }

        else if (mSensors.getRoll() > LEFT_ROLL - ROLL_TOLERANCE
                && mSensors.getRoll() < LEFT_ROLL + ROLL_TOLERANCE) {

            mOrientation = LEFT_ROLL;
            mCorrection = AZIMUTH_ORIENTATION_CORRECTION_LEFT;
        }

        else {
            mOrientation = NO_ROLL;
        }

        // будем рисовать только если уже определена локация и ориентация телефона удовлетворительаня
        if (mSensors.getLocation() != null
                && mOrientation != NO_ROLL
                && mSensors.getPitch() > -PITCH_TOLERANCE
                && mSensors.getPitch() < PITCH_TOLERANCE) {

            // очищаем холст
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            notifyObservers(false);

            // точка посередине холста для лучшей ориентации на глаз
            canvas.drawCircle(mCanvasHalfWidth, mCanvasHalfHeight, 10.0f, mCirclePainter);

            // положение маркеров по вертикали - общее для всех (минус размер самого маркера)
            mX = mCanvasHalfWidth + ((float) ((mSensors.getRoll() - mOrientation) / ROLL_TOLERANCE * mCanvasHalfWidth));

            // отрисовка каждого маркера
            for (int i = 0; i < mSensors.getMarkers().size(); i++) {

                // размер маркера зависит от дальности до его местоположения
                // возможно, впоследствии он будет, наоборот, больше с расстоянием, только прозрачнее
                int bitmapSize = getBitmapSizeOnDistance(mSensors.getLocation().distanceTo(
                        mSensors.getMarkers().get(i)));

                Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap,
                        bitmapSize,
                        bitmapSize,
                        false);

                // высчитываем угол между направлением взгляда и азимутом маркера
                double azimuth = Utility.formatPiMinusPi(mSensors.getLocation().bearingTo(
                        mSensors.getMarkers().get(i)) - Utility.formatPiMinusPi(mSensors.getAzimuth()+ mCorrection));

                // если этот угол попадает в поле зрения, рисуем его в соответствующем месте
                if (azimuth > -FIELD_OF_VIEW && azimuth < FIELD_OF_VIEW) {
                    if (mOrientation == RIGHT_ROLL) {
                        mYList.set(i, mCanvasHalfHeight - bitmapSize / 2 - ((float) (azimuth / FIELD_OF_VIEW * mCanvasHalfHeight)));
                    }
                    else {
                        mYList.set(i, mCanvasHalfHeight - bitmapSize / 2 + ((float) (azimuth / FIELD_OF_VIEW * mCanvasHalfHeight)));
                    }
                }
                // если нет - обнуляем координату Y в списке, чтобы нажатие не срабатывало
                else {
                    mYList.set(i, NULL_FLOAT);
                    continue;
                }

                canvas.drawBitmap(bitmap, mX - bitmapSize / 2, mYList.get(i), mPainter);
            }
        }
        // очищаем холст и обнуляем координаты, чтобы не срабатывали нажатия
        else {
            notifyObservers(true);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            mX = NULL_FLOAT;
            for (int i = 0; i < mYList.size(); i++) {
                mYList.set(i, NULL_FLOAT);
            }
        }
    }

    private int getBitmapSizeOnDistance(float distanceTo) {
        double size = (double) getResources().getDimension(R.dimen.image_height_width);
        size = size / Math.log10((double) distanceTo);
        return (int) size;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // проверяем на пересечение касания с маркерами (пока принимаем их размер константным)
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();
            if (touchX > mX - TOUCH_SIZE && touchX < mX + TOUCH_SIZE) {
                for (int i = 0; i < mYList.size(); i++) {
                    if (touchY > mYList.get(i) - TOUCH_SIZE && touchY < mYList.get(i) + TOUCH_SIZE) {
                        openDetailView(mSensors.getMarkers().get(i));
                    }
                }
            }
        }
        return true;
    }

    // при касании маркера открываем ActivityDetail с информацией о маркере
    private void openDetailView(Location location) {

//        Intent intent = new Intent(mContext, ActivityDetail.class);
//        Bundle bundle = new Bundle();
//        bundle.putDouble(ActivityDetail.BUNDLE_LAT, location.getLatitude());
//        bundle.putDouble(ActivityDetail.BUNDLE_LONG, location.getLongitude());
//        intent.putExtras(bundle);
//        mContext.startActivity(intent);

        FragmentDialog dialogFragment = new FragmentDialog();
        dialogFragment.show(mFragmentManager, FRAGMENT_TAG);
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCanvasHalfWidth = width / 2;
        mCanvasHalfHeight = height / 2;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != mDrawingThread)
            mDrawingThread.interrupt();
    }

    @Override
    public void registerObserver(ObserverWarning o) {
        mObserversWarning.add(o);

    }

    @Override
    public void removeObserver(ObserverWarning o) {
        mObserversWarning.remove(o);
    }

    @Override
    public void notifyObservers(boolean visible) {
        for (ObserverWarning observer : mObserversWarning) {
            observer.update(visible, mOrientation);
        }
    }
}
