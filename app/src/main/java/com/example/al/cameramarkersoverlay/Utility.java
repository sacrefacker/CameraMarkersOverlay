package com.example.al.cameramarkersoverlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Utility {
    // коэффициент для фильтра
    private static final float LOW_PASS_PERCENT = 0.85f;

    // фильтрация значений, акцентирующий внимание на постоянную составляющую, а не мгновенную
    public static float[] lowPassFilter(float[] orientationAverage, float[] orientationMatrix) {
        if (orientationMatrix.length == orientationAverage.length) {

            // у азимута отдельный фильтр для обработки значений близких к 180 и -180
            double average = Math.toDegrees(orientationAverage[0]);
            double current = Math.toDegrees(orientationMatrix[0]);
            double correction = 0;
            if (average - current > 180) {
                correction = 360;
            }
            else if (average - current < -180){
                correction = -360;
            }
            orientationAverage[0] = (float) Math.toRadians(formatPiMinusPi(average +
                    (current - average + correction) * (1 - LOW_PASS_PERCENT)));

            // у поворота и наклона одинаковый, более простой фильтр
            for (int i = 1; i < orientationMatrix.length; i++) {
                orientationAverage[i] = orientationAverage[i] +
                        (orientationMatrix[i] - orientationAverage[i]) * (1 - LOW_PASS_PERCENT);
            }
        }
        return orientationAverage;
    }

    // убеждаемся, что угол находится в промежутке -180 .. 180
    public static double formatPiMinusPi(double angle) {
        angle = (angle < -180) ? angle + 360 : angle;
        angle = (angle > 180) ? angle - 360 : angle;
        return angle;
    }
}
