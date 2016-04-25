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
import java.util.Set;
import java.util.Vector;

public class TaskDownloadData extends AsyncTask<String, Void, Void> {
    private static final String LOG_TAG = TaskDownloadData.class.getSimpleName();

    public static final int DOWNLOAD_CHANNELS = 0;
    public static final int DOWNLOAD_MARKERS = 1;

    public static final String CHANNELS_URL = "http://demo.geo2tag.org/instance/service/CameraMarkersOverlay/channel?";
    public static final String POINTS_URL = "http://demo.geo2tag.org/instance/service/CameraMarkersOverlay/point?";
    public static final String NUMBER_PARAM = "number";
    public static final String OFFSET_PARAM = "offset";
    public static final int QUANTITY_CHANNELS = 100;
    public static final int QUANTITY_MARKERS = 10;
    public static final String CHANNEL_IDS_PARAM = "channel_ids";
    public static final String GEOMETRY_PARAM = "geometry";
    public static final String RADIUS_PARAM = "radius";
    // TODO: поменять после появления возможности добавлять свои точки
    public static final int RADIUS = 100;

    private Context mContext;

    private InterfaceTaskNotifier mTaskNotifier;

    private int mDownloadMode;

    private int mInserted = 0;

    public TaskDownloadData(Context context, InterfaceTaskNotifier callBack, int mode) {
        mContext = context;
        mTaskNotifier = callBack;
        mDownloadMode = mode;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.i(LOG_TAG, "taskDownloaderStarted");
        mTaskNotifier.taskDownloaderStarted();
    }

    @Override
    protected Void doInBackground(String... params) {
        String query;
        StringBuilder resultBuffer = null;
        int inserted = 0;

        try {
            switch (mDownloadMode) {
                case DOWNLOAD_CHANNELS:

                    // перенесено из getChannelsFromJson - удаляем раньше времени?
                    mContext.getContentResolver().delete(ChannelEntry.CONTENT_URI, null, null);

                    query = Uri.parse(CHANNELS_URL).buildUpon()
                            .appendQueryParameter(NUMBER_PARAM, String.valueOf(QUANTITY_CHANNELS))
                            .build()
                            .toString();

                    resultBuffer = fetchData(query);

                    if (resultBuffer != null && resultBuffer.length() != 0) {
                        String jsonStr = resultBuffer.toString();
                        Log.i(LOG_TAG, jsonStr);
                        mInserted = getChannelsFromJson(jsonStr);
                    }
                    break;

                case DOWNLOAD_MARKERS:

                    // перенесено из getMarkersFromJson - удаляем раньше времени?
                    mContext.getContentResolver().delete(MarkersEntry.CONTENT_URI, null, null);

                    Set<String> channels = ChannelsContainer.getInstance(mContext).getChannels();
                    for (String channel : channels) {
                        Log.i(LOG_TAG, "Channel " + channel + "\n");
                        float lat = Float.valueOf(params[0]);
                        float lon = Float.valueOf(params[1]);

                        // TODO: решить, как лучше формировать строку
                        // String geom = String.format(Locale.ENGLISH, mContext.getString(R.string.format_geometry), lat, lon);
                        String geom = "{\"type\":\"Point\",\"coordinates\":[" + lon + "," + lat + "]}";

                        for (int i = 0; i == 0 || inserted == QUANTITY_MARKERS; i++) {
                            query = Uri.parse(POINTS_URL).buildUpon()
                                    .appendQueryParameter(NUMBER_PARAM, String.valueOf(QUANTITY_MARKERS))
                                    .appendQueryParameter(OFFSET_PARAM, String.valueOf(QUANTITY_MARKERS * i))
                                    .appendQueryParameter(CHANNEL_IDS_PARAM, channel)
                                    .appendQueryParameter(RADIUS_PARAM, String.valueOf(RADIUS))
                                    .build()
                                    .toString()
                                    .concat("&" + GEOMETRY_PARAM + "=")
                                    .concat(geom)
                            ;
                            Log.i(LOG_TAG, query);

                            resultBuffer = fetchData(query);

                            if (resultBuffer != null && resultBuffer.length() != 0) {
                                String jsonStr = resultBuffer.toString();
                                Log.i(LOG_TAG, "i " + i + ", string size " + jsonStr.length());
                                Log.i(LOG_TAG, jsonStr);
                                inserted = getMarkersFromJson(jsonStr);
                                mInserted += inserted;
                            }
                        }
                    }
                    break;

                default:
                    Log.i(LOG_TAG, "Wrong mode");
                    return null;
            }
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex);
            ex.printStackTrace();
        }
        return null;
    }

    private StringBuilder fetchData(String query) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        StringBuilder buffer = new StringBuilder();
        buffer.setLength(0);

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

    private int getMarkersFromJson(String jsonStr) throws JSONException {
        int inserted = 0;
//        mContentVector = new DummyDownload().download();

        // Location information
        final String JSON_LOCATION = "location";
        final String JSON_COORDINATES = "coordinates";
        final String JSON_DESC = "json";
        final String JSON_NAME = "name";
        final String JSON_TYPE = "type";
        final String JSON_IMG = "img";

        try {
            JSONArray jsonArray = new JSONArray(jsonStr);

            Vector<ContentValues> cVVector = new Vector<>(jsonArray.length());
            for(int i = 0; i < jsonArray.length(); i++) {
                // These are the values that will be collected.
                double lat, lon;

                // Get the JSON object representing a marker
                JSONObject marker = jsonArray.getJSONObject(i);
                JSONObject location = marker.getJSONObject(JSON_LOCATION);
                JSONArray coordinates = location.getJSONArray(JSON_COORDINATES);
                // TODO: координаты в обратном порядке?
                lat = coordinates.getDouble(1);
                lon = coordinates.getDouble(0);
                /*lat = coordinates.getDouble(0);
                lon = coordinates.getDouble(1);*/

                JSONObject desc = marker.getJSONObject(JSON_DESC);
                String name = desc.getString(JSON_NAME);
                // TODO: в стандартных типов может не быть
                String type = desc.getString(JSON_TYPE);
                String img = desc.getString(JSON_IMG);

                ContentValues values = new ContentValues();
                values.put(MarkersContract.MarkersEntry.COLUMN_LAT, lat);
                values.put(MarkersContract.MarkersEntry.COLUMN_LONG, lon);
                values.put(MarkersEntry.COLUMN_NAME, name);
                values.put(MarkersEntry.COLUMN_TYPE, type);
                values.put(MarkersEntry.COLUMN_IMAGE, img);
                cVVector.add(values);
            }

            // добавляем значения в БД
            if ( cVVector.size() > 0 ) {
                inserted = insertData(cVVector, MarkersEntry.CONTENT_URI);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return inserted;
    }

    private int getChannelsFromJson(String jsonStr) throws JSONException {
        int inserted = 0;

        final String JSON_NAME = "name";
        final String JSON_ID = "_id";
        final String JSON_OID = "$oid";

        try {
            JSONArray jsonArray = new JSONArray(jsonStr);

            Vector<ContentValues> cVVector = new Vector<>(jsonArray.length());

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
                inserted = insertData(cVVector, ChannelEntry.CONTENT_URI);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return inserted;
    }

    private int insertData(Vector<ContentValues> vector, Uri contentUri) {
        Log.i(LOG_TAG, "insertData");
        ContentValues cVArray[] = new ContentValues[vector.size()];
        vector.toArray(cVArray);
        int inserted = mContext.getContentResolver().bulkInsert(contentUri, cVArray);
        Log.i(LOG_TAG, "inserted " + inserted);
        return inserted;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.i(LOG_TAG, "taskDownloadFinished");
        mTaskNotifier.taskDownloadFinished(mInserted);
    }
}