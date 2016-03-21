package com.example.al.cameramarkersoverlay.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class MarkersProvider extends ContentProvider {
    static final int ALL_MARKERS = 100;

    private static final SQLiteQueryBuilder sQueryBuilder;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static {
        sQueryBuilder = new SQLiteQueryBuilder();
        sQueryBuilder.setTables(MarkersContract.MarkersEntry.TABLE_NAME);
    }

    static UriMatcher buildUriMatcher() {
        // 1) The code passed into the constructor represents the code to return for the root URI
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MarkersContract.CONTENT_AUTHORITY;
        final String markers = MarkersContract.PATH_MARKERS;
        // 2) Use the addURI function to match each of the types.
        uriMatcher.addURI(authority, markers, ALL_MARKERS);
        // 3) Return the new matcher!
        return uriMatcher;
    }

    private MarkersDbHelper mHelper;

    @Override
    public boolean onCreate() {
        mHelper = new MarkersDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);
        switch (match) {
            // Student: Uncomment and fill out these two cases
            case ALL_MARKERS:
                return MarkersContract.MarkersEntry.CONTENT_DIR_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case ALL_MARKERS: {
                retCursor = getAllMarkers(uri, projection, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    private Cursor getAllMarkers(Uri uri, String[] projection, String sortOrder) {
        return sQueryBuilder.query(
                mHelper.getReadableDatabase(), // выбор БД
                projection, // список колонок для возврата
                null, // условия
                null, // аргументы для условий (подстановка в "?")
                null, // группировка
                null, // условие для включения группы в результат
                sortOrder // сортировка
        );
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ALL_MARKERS:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(MarkersContract.MarkersEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // Пока не применяются

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
