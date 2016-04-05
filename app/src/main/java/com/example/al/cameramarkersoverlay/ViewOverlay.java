package com.example.al.cameramarkersoverlay;

import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

class ViewOverlay extends SurfaceView implements SurfaceHolder.Callback, ObservableWarning {
    private static final String LOG_TAG = ViewOverlay.class.getSimpleName();

    private static final String FRAGMENT_TAG = "fragmentDialog";

    private static final double ROLL_TOLERANCE = 10.0;
    private static final double PITCH_TOLERANCE = 10.0;
    private static final double FIELD_OF_VIEW = 40.0;

    private static final float OUT_OF_BOUNDS = 10000.0f;
    private static final int TOUCH_SIZE = 100;

    // коррекция значения азимута в связи с ориентацией телефона во время искользования приложения
    private static final double AZIMUTH_ORIENTATION_CORRECTION_RIGHT = -90.0;
    private static final double AZIMUTH_ORIENTATION_CORRECTION_LEFT = 90;
    public static final double RIGHT_SIDE = 90.0;
    public static final double LEFT_SIDE = -90.0;
    public static final double NO_SIDE = 0.0;

    private double mOnSide = NO_SIDE;
    private double mCorrection = AZIMUTH_ORIENTATION_CORRECTION_RIGHT;

    // колбэк для получения значений с сенсоров
    private InterfaceSensors mSensors;
    private List<ObserverWarning> mObserversWarning;
    private FragmentManager mFragmentManager;
    private Thread mDrawingThread;

    private final SurfaceHolder mSurfaceHolder;
    private final Bitmap mBitmap;
    private final Paint mBitmapPainter = new Paint();
    private final Paint mCirclePainter = new Paint();

    // X - линия горизонта, Y - вертикаль
    private ArrayList<Float> mXs;
    private float mY = OUT_OF_BOUNDS;
    private float mCanvasHalfWidth = 0, mCanvasHalfHeight = 0;

    public ViewOverlay(Context context, Bitmap bitmap, InterfaceSensors interfaceSensors,
                       FragmentManager fragmentManager) {

        super(context);

        mSensors = interfaceSensors;
        mObserversWarning = new ArrayList<>();
        mFragmentManager = fragmentManager;

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        mBitmap = bitmap;
        mBitmapPainter.setAntiAlias(true);

        // для точки в середине экрана
        mCirclePainter.setARGB(255, 255, 0, 0);
        mCirclePainter.setStyle(Paint.Style.FILL_AND_STROKE);

        initYList();
    }

    // создание заново списка Y координат пригождается при изменении списка маркеров и в конструкторе
    private void initYList() {
        mXs = new ArrayList<>(mSensors.getMarkers().size());
        for (int i = 0; i < mSensors.getMarkers().size(); i++) {
            mXs.add(OUT_OF_BOUNDS);
        }
    }

    private void drawMarkers(Canvas canvas) {

        if (mSensors.getMarkers().size() != mXs.size()) {
            initYList();
        }

        // точка посередине холста для лучшей ориентации на глаз
        canvas.drawCircle(mCanvasHalfWidth, mCanvasHalfHeight, 10.0f, mCirclePainter);

        refreshOrientation();

        // будем рисовать только если уже определена локация и ориентация телефона удовлетворительаня
        if (mSensors.getLocation() != null
                && mOnSide != NO_SIDE
                && mSensors.getPitch() > -PITCH_TOLERANCE
                && mSensors.getPitch() < PITCH_TOLERANCE) {

            notifyObservers(false);

            // очищаем холст
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            // точка посередине холста для лучшей ориентации на глаз
            canvas.drawCircle(mCanvasHalfWidth, mCanvasHalfHeight, 10.0f, mCirclePainter);

            // положение маркеров по вертикали - общее для всех (минус размер самого маркера)
            mY = mCanvasHalfHeight;
            if (mOnSide == RIGHT_SIDE) {
                mY += ((float) ((mSensors.getRoll() - mOnSide) / ROLL_TOLERANCE * mCanvasHalfHeight));
            }
            else {
                mY -= ((float) ((mSensors.getRoll() - mOnSide) / ROLL_TOLERANCE * mCanvasHalfHeight));
            }

            // отрисовка каждого маркера
            for (int i = 0; i < mSensors.getMarkers().size(); i++) {

                // размер маркера зависит от дальности до его местоположения
                // возможно, впоследствии он будет, наоборот, больше с расстоянием, только прозрачнее
                int bitmapSize = getBitmapSizeOnDistance(mSensors.getLocation().distanceTo(
                        mSensors.getMarkers().get(i).getLocation()));

                Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap,
                        bitmapSize,
                        bitmapSize,
                        false);

                // высчитываем угол между направлением взгляда и азимутом маркера
                double azimuth = Utility.formatPiMinusPi(mSensors.getLocation().bearingTo(
                        mSensors.getMarkers().get(i).getLocation()) - Utility.formatPiMinusPi(
                        mSensors.getAzimuth()+ mCorrection));

                // если этот угол попадает в поле зрения, рисуем его в соответствующем месте
                if (azimuth > -FIELD_OF_VIEW && azimuth < FIELD_OF_VIEW) {
                    mXs.set(i, mCanvasHalfWidth - bitmapSize / 2 +
                            ((float) (azimuth / FIELD_OF_VIEW * mCanvasHalfWidth)));
                }
                // если нет - обнуляем координату Y в списке, чтобы нажатие не срабатывало
                else {
                    mXs.set(i, OUT_OF_BOUNDS);
                    continue;
                }

                canvas.drawBitmap(bitmap, mXs.get(i), mY - bitmapSize / 2, mBitmapPainter);
            }
        }
        // очищаем холст и обнуляем координаты, чтобы не срабатывали нажатия
        else {
            notifyObservers(true);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            mY = OUT_OF_BOUNDS;
            for (int i = 0; i < mXs.size(); i++) {
                mXs.set(i, OUT_OF_BOUNDS);
            }
        }
    }

    private void refreshOrientation() {

        if (mSensors.getRoll() > RIGHT_SIDE - ROLL_TOLERANCE
                && mSensors.getRoll() < RIGHT_SIDE + ROLL_TOLERANCE) {

            mOnSide = RIGHT_SIDE;
            mCorrection = AZIMUTH_ORIENTATION_CORRECTION_RIGHT;
        }

        else if (mSensors.getRoll() > LEFT_SIDE - ROLL_TOLERANCE
                && mSensors.getRoll() < LEFT_SIDE + ROLL_TOLERANCE) {

            mOnSide = LEFT_SIDE;
            mCorrection = AZIMUTH_ORIENTATION_CORRECTION_LEFT;
        }

        else {
            mOnSide = NO_SIDE;
        }
    }

    private int getBitmapSizeOnDistance(float distanceTo) {
        double size = (double) getResources().getDimension(R.dimen.marker_size);
        size = size / Math.log10((double) distanceTo);
        return (int) size;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // проверяем на пересечение касания с маркерами (пока принимаем их размер константным)
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();
            if (touchY > mY - TOUCH_SIZE && touchY < mY + TOUCH_SIZE) {
                for (int i = 0; i < mXs.size(); i++) {
                    if (touchX > mXs.get(i) - TOUCH_SIZE && touchX < mXs.get(i) + TOUCH_SIZE) {
                        openDetailView(mSensors.getMarkers().get(i).getLocation());
                    }
                }
            }
        }
        return true;
    }

    // при касании маркера открываем диалог с информацией о маркере
    private void openDetailView(Location location) {
        FragmentDialog dialogFragment = new FragmentDialog();
        Bundle bundle = new Bundle();
        bundle.putDouble(FragmentDialog.BUNDLE_LAT, location.getLatitude());
        bundle.putDouble(FragmentDialog.BUNDLE_LONG, location.getLongitude());
        dialogFragment.setArguments(bundle);
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
            observer.update(visible);
        }
    }
}