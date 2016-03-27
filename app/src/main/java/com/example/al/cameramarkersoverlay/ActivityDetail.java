package com.example.al.cameramarkersoverlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ActivityDetail extends Activity {

    private static final String LOG_TAG = ActivityDetail.class.getSimpleName();

    public static final String BUNDLE_LAT = "lat";
    public static final String BUNDLE_LONG = "long";

    private double mLatitude = 0.0, mLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = getIntent().getExtras();
            mLatitude = bundle.getDouble(BUNDLE_LAT);
            mLongitude = bundle.getDouble(BUNDLE_LONG);
        }

        TextView locationTextView = (TextView) findViewById(R.id.dialog_location_text);
        locationTextView.setText(String.format(getString(R.string.format_location), mLatitude, mLongitude));

        Button buttonShow = (Button) findViewById(R.id.detail_show_button);
        buttonShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLocationOnMap();
            }
        });

        Button buttonBack = (Button) findViewById(R.id.detail_close_button);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    // при нажатии кнопки открываем Google Maps с содежращейся в маркере локацией
    private void openLocationOnMap() {

        String locationString = "geo:".concat(String.valueOf(mLatitude)).concat(",").concat(String.valueOf(mLongitude));
        Uri geoLocation = Uri.parse(locationString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        Log.i(LOG_TAG, locationString);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't resolve");
        }
    }
}
