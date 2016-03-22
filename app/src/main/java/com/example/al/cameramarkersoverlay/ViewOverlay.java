package com.example.al.cameramarkersoverlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

class ViewOverlay extends SurfaceView implements SurfaceHolder.Callback {
    private static final double FIELD_OF_VIEW = 40.0;
    private static final double IDEAL_ROLL = 90.0;
    private static final double ROLL_TOLERANCE = 10.0;
    private static final double PITCH_TOLERANCE = 10.0;

    private static final float NULL_FLOAT = -1000.0f;
    private static final int TOUCH_SIZE = 100;

    private Context mContext;

    private InterfaceOverlay mInterfaceOverlay;
    private Thread mDrawingThread;

    private final SurfaceHolder mSurfaceHolder;
    private final Bitmap mBitmap;
    private final Paint mPainter = new Paint();
    private final Paint mCirclePainter = new Paint();

    // Y - линия горизонта, X - вертикаль
    private ArrayList<Float> mYList;
    float mX = NULL_FLOAT;
    private float mCanvasHalfWidth = 0, mCanvasHalfHeight = 0;

    public ViewOverlay(InterfaceOverlay interfaceOverlay, Context context, Bitmap bitmap) {
        super(context);
        mContext = context;
        mInterfaceOverlay = interfaceOverlay;

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
        mYList = new ArrayList<>(mInterfaceOverlay.getMarkers().size());
        for (int i = 0; i < mInterfaceOverlay.getMarkers().size(); i++) {
            mYList.add(NULL_FLOAT);
        }
    }

    private void drawMarkers(Canvas canvas) {

        if (mInterfaceOverlay.getMarkers().size() != mYList.size()) {
            initYList();
        }

        canvas.drawCircle(mCanvasHalfWidth, mCanvasHalfHeight, 10.0f, mCirclePainter);

        // будем рисовать только если уже определена локация и ориентация телефона удовлетворительаня
        if (mInterfaceOverlay.getLocation() != null
                && mInterfaceOverlay.getRoll() > IDEAL_ROLL - ROLL_TOLERANCE
                && mInterfaceOverlay.getRoll() < IDEAL_ROLL + ROLL_TOLERANCE
                && mInterfaceOverlay.getPitch() > -PITCH_TOLERANCE
                && mInterfaceOverlay.getPitch() < PITCH_TOLERANCE) {

            // очищаем холст
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            // точка посередине холста для лучшей ориентации на глаз
            canvas.drawCircle(mCanvasHalfWidth, mCanvasHalfHeight, 10.0f, mCirclePainter);

            // положение маркеров по вертикали - общее для всех (минус размер самого маркера)
            mX = mCanvasHalfWidth + ((float) ((mInterfaceOverlay.getRoll()
                    - IDEAL_ROLL) / ROLL_TOLERANCE * mCanvasHalfWidth));

            // отрисовка каждого маркера
            for (int i = 0; i < mInterfaceOverlay.getMarkers().size(); i++) {

                // размер маркера зависит от дальности до его местоположения
                // возможно, впоследствии он будет, наоборот, больше с расстоянием, только прозрачнее
                int bitmapSize = getBitmapSizeOnDistance(mInterfaceOverlay.getLocation().distanceTo(
                        mInterfaceOverlay.getMarkers().get(i)));

                Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap,
                        bitmapSize,
                        bitmapSize,
                        false);

                // высчитываем угол между направлением взгляда и азимутом маркера
                double azimuth = Utility.formatPiMinusPi(mInterfaceOverlay.getLocation().bearingTo(
                        mInterfaceOverlay.getMarkers().get(i)) - mInterfaceOverlay.getAzimuth());

                // если этот угол попадает в поле зрения, рисуем его в соответствующем месте
                if (azimuth > -FIELD_OF_VIEW && azimuth < FIELD_OF_VIEW) {
                    mYList.set(i, mCanvasHalfHeight - bitmapSize / 2
                            - ((float) (azimuth / FIELD_OF_VIEW * mCanvasHalfHeight)));
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
                        openLocationOnMap(mInterfaceOverlay.getMarkers().get(i));
                    }
                }
            }
        }
        return true;
    }

    // при касании маркера открываем Google Maps с содежращейся в маркере локацией
    private void openLocationOnMap(Location location) {

        String locationString = "geo:".concat(String.valueOf(location.getLatitude())).concat(",")
                .concat(String.valueOf(location.getLongitude()));
        Uri geoLocation = Uri.parse(locationString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        Log.i(FragmentOverlay.LOG_TAG, locationString);

        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            mContext.startActivity(intent);
        } else {
            Log.d(FragmentOverlay.LOG_TAG, "Couldn't resolve");
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCanvasHalfWidth = width / 2;
        mCanvasHalfHeight = height / 2;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != mDrawingThread)
            mDrawingThread.interrupt();
    }
}
