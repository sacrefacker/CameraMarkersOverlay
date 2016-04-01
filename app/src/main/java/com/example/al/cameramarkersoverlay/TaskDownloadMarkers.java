package com.example.al.cameramarkersoverlay;

import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

import com.example.al.cameramarkersoverlay.data.MarkersContract.MarkersEntry;
import com.example.al.cameramarkersoverlay.temp.DummyDownload;

import java.util.ArrayList;
import java.util.Vector;

public class TaskDownloadMarkers extends AsyncTask<String, Void, Void> {
    private final String TAG = TaskDownloadMarkers.class.getSimpleName();

    private final Context mContext;
    Vector<ContentValues> mContentVector;

    public TaskDownloadMarkers(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {

        mContentVector = new DummyDownload().download();

        markersIntoDb();
        return null;
    }

    private void markersIntoDb() {

        // добавляем значения в БД
        if ( mContentVector.size() > 0 ) {
            ContentValues cVArray[] = new ContentValues[mContentVector.size()];
            mContentVector.toArray(cVArray);
            mContext.getContentResolver().bulkInsert(MarkersEntry.CONTENT_URI,cVArray);
        }

    }
}
