package com.example.al.cameramarkersoverlay.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.al.cameramarkersoverlay.data.MarkersContract.MarkersEntry;

public class MarkersDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "markers.db";

    public MarkersDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_MARKERS_TABLE = "CREATE TABLE " + MarkersEntry.TABLE_NAME + " (" +

                // Why AutoIncrement here, and not above?
                // Unique keys will be auto-generated in either case.  But for weather
                // forecasting, it's reasonable to assume the user will want information
                // for a certain date and all dates *following*, so the forecast data
                // should be sorted accordingly.
                MarkersEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // Может потребоваться not null

//                MarkersEntry.COLUMN_CHANNEL + " INTEGER NOT NULL, " +
//                MarkersEntry.COLUMN_IMAGE + " TEXT NOT NULL, " +
//                MarkersEntry.COLUMN_DESCRIPTION + " TEXT NOT NULL," +
//                MarkersEntry.COLUMN_TYPE + " TEXT NOT NULL, " +
//                MarkersEntry.COLUMN_LAT + " REAL NOT NULL, " +
//                MarkersEntry.COLUMN_LONG + " REAL NOT NULL, " +
//                MarkersEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
//                MarkersEntry.COLUMN_ALT + " REAL NOT NULL, " +
//                MarkersEntry.COLUMN_ID + " INTEGER NOT NULL, " +

                MarkersEntry.COLUMN_CHANNEL + " INTEGER, " +
                MarkersEntry.COLUMN_IMAGE + " TEXT, " +
                MarkersEntry.COLUMN_DESCRIPTION + " TEXT," +
                MarkersEntry.COLUMN_TYPE + " TEXT, " +
                MarkersEntry.COLUMN_LAT + " REAL NOT NULL, " +
                MarkersEntry.COLUMN_LONG + " REAL NOT NULL, " +
                MarkersEntry.COLUMN_DATE + " INTEGER, " +
                MarkersEntry.COLUMN_ALT + " REAL, " +
                MarkersEntry.COLUMN_ID + " INTEGER, " +

                // From intuition the coordinates should be unique
                " UNIQUE (" + MarkersEntry.COLUMN_LAT + ", " +
                MarkersEntry.COLUMN_LONG + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_MARKERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MarkersEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
