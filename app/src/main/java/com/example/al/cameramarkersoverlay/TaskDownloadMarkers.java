package com.example.al.cameramarkersoverlay;

import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;

import com.example.al.cameramarkersoverlay.data.MarkersContract.MarkersEntry;

import java.util.ArrayList;
import java.util.Vector;

public class TaskDownloadMarkers extends AsyncTask<String, Void, Void> {
    private final String TAG = TaskDownloadMarkers.class.getSimpleName();

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
        LOCATION_LETI.setLatitude(59.9765);
        LOCATION_LETI.setLongitude(30.3208);
    }

    private final Context mContext;
    private ArrayList<Location> mMarkers;

    public TaskDownloadMarkers(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {
        // networking
        markersIntoDb();
        return null;
    }

    private void markersIntoDb() {

        mMarkers = new ArrayList<>();
        mMarkers.add(LOCATION_MOSCOW);
        mMarkers.add(LOCATION_TOMSK);
        mMarkers.add(LOCATION_LETI);

        Vector<ContentValues> cVVector = new Vector<>(mMarkers.size());

        for(int i = 0; i < mMarkers.size(); i++) {

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(MarkersEntry.COLUMN_LAT, mMarkers.get(i).getLatitude());
            weatherValues.put(MarkersEntry.COLUMN_LONG, mMarkers.get(i).getLongitude());

            cVVector.add(weatherValues);

        }

        // add to database
        if ( cVVector.size() > 0 ) {
            ContentValues cVArray[] = new ContentValues[cVVector.size()];
            cVVector.toArray(cVArray);
            mContext.getContentResolver().bulkInsert(MarkersEntry.CONTENT_URI,cVArray);
        }

    }
}
