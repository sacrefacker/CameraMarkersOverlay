package com.example.al.cameramarkersoverlay;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.al.cameramarkersoverlay.data.ChannelsContainer;
import com.example.al.cameramarkersoverlay.data.MarkersContract;
import com.example.al.cameramarkersoverlay.data.MarkersContract.ChannelEntry;
import com.example.al.cameramarkersoverlay.data.MarkersContract.MarkersEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

public class TaskDownloadData extends AsyncTask<String, Void, Void> {
    private static final String LOG_TAG = TaskDownloadData.class.getSimpleName();

    public static final int DOWNLOAD_CHANNELS = 0;
    public static final int DOWNLOAD_MARKERS = 1;

    public static final String CHANNELS_URL = "http://demo.geo2tag.org/instance/service/testservice/channel?";
    public static final String POINTS_URL = "http://demo.geo2tag.org/instance/service/testservice/point?";
    public static final String NUMBER_PARAM = "number";
    public static final int QUANTITY = 10;
    public static final String CHANNEL_IDS_PARAM = "channel_ids";
    public static final String GEOMETRY_PARAM = "geometry";
    public static final String RADIUS_PARAM = "radius";
    // TODO: поменять после появления возможности добавлять свои точки
    public static final int RADIUS = 10000;

    private Context mContext;

    private int mDownloadMode;

    public TaskDownloadData(Context context, int mode) {
        mContext = context;
        mDownloadMode = mode;
    }

    @Override
    protected Void doInBackground(String... params) {
        String query;
        StringBuilder buffer = new StringBuilder();

        switch (mDownloadMode) {
            case DOWNLOAD_CHANNELS:
                query = Uri.parse(CHANNELS_URL).buildUpon()
                        .appendQueryParameter(NUMBER_PARAM, String.valueOf(QUANTITY))
                        .build()
                        .toString();
                buffer.append(fetchData(query));
                // перенесено из getChannelsFromJson - удаляем раньше времени?
                mContext.getContentResolver().delete(ChannelEntry.CONTENT_URI, null, null);
                break;
            case DOWNLOAD_MARKERS:
                Set<String> channels = ChannelsContainer.getInstance(mContext).getChannels();
                for (String channel : channels) {
                    Log.i(LOG_TAG, "Channel " + channel + "\n");
                    float lat = Float.valueOf(params[0]);
                    float lon = Float.valueOf(params[1]);

                    // решить, как лучше формировать строку
//                    String geom = String.format(Locale.ENGLISH, mContext.getString(R.string.format_geometry), lat, lon);
                    String geom = "{\"type\":\"Point\",\"coordinates\":[" + lat + "," + lon + "]}";
                    Log.i(LOG_TAG, geom);
                    query = Uri.parse(POINTS_URL).buildUpon()
                            .appendQueryParameter(NUMBER_PARAM, String.valueOf(QUANTITY))
                            .appendQueryParameter(CHANNEL_IDS_PARAM, channel)
                            .appendQueryParameter(RADIUS_PARAM, String.valueOf(RADIUS))
                            .build()
                            .toString()
                            .concat("&" + GEOMETRY_PARAM + "=")
                            .concat(geom);
                    buffer.append(fetchData(query));
                    // перенесено из getMarkersFromJson - удаляем раньше времени?
                    mContext.getContentResolver().delete(MarkersEntry.CONTENT_URI, null, null);
                }
                break;
            default:
                Log.i(LOG_TAG, "Wrong mode");
                return null;
        }

        if (buffer.length() != 0) {
            String jsonStr = buffer.toString();
            Log.i(LOG_TAG, jsonStr);

            try {
                switch (mDownloadMode) {
                    case DOWNLOAD_CHANNELS:
                        getChannelsFromJson(jsonStr);
                        break;
                    case DOWNLOAD_MARKERS:
                        getMarkersFromJson(jsonStr);
                        break;
                    default:
                        Log.i(LOG_TAG, "Wrong mode");
                        return null;
                }
            } catch (JSONException ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                ex.printStackTrace();
            }
        }
        return null;
    }

    private StringBuilder fetchData(String query) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        StringBuilder buffer = new StringBuilder();

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        try {

            if (query != null) {

                URL url = new URL(query);

                // Create the request, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line);
                    buffer.append("\n");
                }
            }
        }

        catch (IOException ex) {
            Log.e(LOG_TAG, "Error ", ex);
            // If the code didn't successfully get the data, there's no point in attempting to parse it.
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        return buffer;
    }

    private void getMarkersFromJson(String jsonStr) throws JSONException {
//        mContentVector = new DummyDownload().download();

        // Location information
        final String JSON_LOCATION = "location";
        final String JSON_COORDINATES = "coordinates";

        try {
            JSONArray jsonArray = new JSONArray(jsonStr);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(jsonArray.length());

            for(int i = 0; i < jsonArray.length(); i++) {
                // These are the values that will be collected.
                double lat, lon;

                // Get the JSON object representing a marker
                JSONObject marker = jsonArray.getJSONObject(i);
                JSONObject location = marker.getJSONObject(JSON_LOCATION);
                JSONArray coordinates = location.getJSONArray(JSON_COORDINATES);
                lat = coordinates.getDouble(0);
                lon = coordinates.getDouble(1);

                ContentValues values = new ContentValues();
                values.put(MarkersContract.MarkersEntry.COLUMN_LAT, lat);
                values.put(MarkersContract.MarkersEntry.COLUMN_LONG, lon);
                cVVector.add(values);
            }

            // добавляем значения в БД
            if ( cVVector.size() > 0 ) {
                insertData(cVVector, MarkersEntry.CONTENT_URI);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void getChannelsFromJson(String jsonStr) throws JSONException {

        final String JSON_NAME = "name";
        final String JSON_ID = "_id";
        final String JSON_OID = "$oid";

        try {
            JSONArray jsonArray = new JSONArray(jsonStr);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(jsonArray.length());

            for(int i = 0; i < jsonArray.length(); i++) {

                JSONObject channel = jsonArray.getJSONObject(i);

                JSONObject _id = channel.getJSONObject(JSON_ID);
                String id = _id.getString(JSON_OID);
                String name = channel.getString(JSON_NAME);

                ContentValues values = new ContentValues();
                values.put(ChannelEntry.COLUMN_ID, id);
                values.put(ChannelEntry.COLUMN_NAME, name);
                cVVector.add(values);
            }

            // заменяем значения в БД
            if ( cVVector.size() > 0 ) {
                insertData(cVVector, ChannelEntry.CONTENT_URI);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void insertData(Vector<ContentValues> vector, Uri contentUri) {
        Log.i(LOG_TAG, "Refreshing data");
        ContentValues cVArray[] = new ContentValues[vector.size()];
        vector.toArray(cVArray);
        mContext.getContentResolver().bulkInsert(contentUri, cVArray);
    }
}