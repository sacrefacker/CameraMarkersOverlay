package com.example.al.cameramarkersoverlay.temp;

import android.content.ContentValues;
import android.location.Location;
import android.location.LocationManager;

import com.example.al.cameramarkersoverlay.data.MarkersContract;

import java.util.ArrayList;
import java.util.Vector;

public class DummyDownload {

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

    public DummyDownload() {
        //
    }

    public Vector<ContentValues> download() {

        ArrayList<Location> markers = new ArrayList<>();
        markers.add(LOCATION_MOSCOW);
        markers.add(LOCATION_TOMSK);
        markers.add(LOCATION_LETI);

        Vector<ContentValues> cVVector = new Vector<>(markers.size());

        for(int i = 0; i < markers.size(); i++) {

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(MarkersContract.MarkersEntry.COLUMN_LAT, markers.get(i).getLatitude());
            weatherValues.put(MarkersContract.MarkersEntry.COLUMN_LONG, markers.get(i).getLongitude());

            cVVector.add(weatherValues);

        }

        return cVVector;
    }
}
