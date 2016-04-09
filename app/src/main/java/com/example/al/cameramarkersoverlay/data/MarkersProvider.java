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
    static final int ALL_CHANNELS = 200;

    private static final SQLiteQueryBuilder sChannelsQueryBuilder;
    private static final SQLiteQueryBuilder sMarkersQueryBuilder;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static {
        sChannelsQueryBuilder = new SQLiteQueryBuilder();
        sChannelsQueryBuilder.setTables(MarkersContract.ChannelEntry.TABLE_NAME);
    }

    static {
        sMarkersQueryBuilder = new SQLiteQueryBuilder();
        sMarkersQueryBuilder.setTables(MarkersContract.MarkersEntry.TABLE_NAME);
    }

    static UriMatcher buildUriMatcher() {
        // 1) The code passed into the constructor represents the code to return for the root URI
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MarkersContract.CONTENT_AUTHORITY;
        // 2) Use the addURI function to match each of the types.
        uriMatcher.addURI(authority, MarkersContract.PATH_CHANNELS, ALL_CHANNELS);
        uriMatcher.addURI(authority, MarkersContract.PATH_MARKERS, ALL_MARKERS);
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
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ALL_CHANNELS:
                return MarkersContract.ChannelEntry.CONTENT_DIR_TYPE;
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
            case ALL_CHANNELS: {
                retCursor = getAllChannels(uri, projection, sortOrder);
                break;
            }
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

    private Cursor getAllChannels(Uri uri, String[] projection, String sortOrder) {
        return sChannelsQueryBuilder.query(
                mHelper.getReadableDatabase(), // выбор БД
                projection, // список колонок для возврата
                null, // условия
                null, // аргументы для условий (подстановка в "?")
                null, // группировка
                null, // условие для включения группы в результат
                sortOrder // сортировка
        );
    }

    private Cursor getAllMarkers(Uri uri, String[] projection, String sortOrder) {
        return sMarkersQueryBuilder.query(
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
        final int match = sUriMatcher.match(uri);
        int returnCount;
        switch (match) {
            case ALL_CHANNELS:
                returnCount = bulkInsertHelper(uri, values, MarkersContract.ChannelEntry.TABLE_NAME);
                break;
            case ALL_MARKERS:
                returnCount = bulkInsertHelper(uri, values, MarkersContract.MarkersEntry.TABLE_NAME);
                break;
            default:
                throw new UnsupportedOperationException("Wrong URI");
        }
        return returnCount;
    }

    private int bulkInsertHelper(Uri uri, ContentValues[] values, String tableName) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        db.beginTransaction();
        int returnCount = 0;
        try {
            for (ContentValues value : values) {
                long _id = db.insert(tableName, null, value);
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
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        int returnCount;
        switch (match) {
            case ALL_CHANNELS:
                returnCount = deleteAllRows(uri, MarkersContract.ChannelEntry.TABLE_NAME);
                break;
            case ALL_MARKERS:
                returnCount = deleteAllRows(uri, MarkersContract.MarkersEntry.TABLE_NAME);
                break;
            default:
                throw new UnsupportedOperationException("Wrong URI");
        }
        return returnCount;
    }

    private int deleteAllRows(Uri uri, String tableName) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        // говорят, если заменить на trunсate table, будет быстрее
        int returnCount = db.delete(tableName, null, null);
        getContext().getContentResolver().notifyChange(uri, null);
        return returnCount;
    }

    // Пока не применяются

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
