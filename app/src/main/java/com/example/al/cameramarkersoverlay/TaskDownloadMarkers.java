package com.example.al.cameramarkersoverlay;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.al.cameramarkersoverlay.data.JsonHelper;
import com.example.al.cameramarkersoverlay.data.MarkersContract;
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

public class TaskDownloadMarkers extends AsyncTask<String, Void, Void> {
    private static final String LOG_TAG = TaskDownloadMarkers.class.getSimpleName();

    private Context mContext;

    public TaskDownloadMarkers(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {

            StringBuilder buffer = new StringBuilder();
            Set<String> channels = JsonHelper.getInstance(mContext).getChannels();

            for (String channel : channels) {

                Uri builtUri = Uri.parse(JsonHelper.POINTS_URL).buildUpon()
                        .appendQueryParameter(JsonHelper.NUMBER_PARAM, String.valueOf(JsonHelper.QUANTITY))
                        .appendQueryParameter(JsonHelper.CHANNEL_IDS_PARAM, channel)
                        .build();

                URL url = new URL(builtUri.toString());

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

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            String jsonStr = buffer.toString();
            getMarkersFromJson(jsonStr);

        } catch (IOException ex) {
            Log.e(LOG_TAG, "Error ", ex);
            // If the code didn't successfully get the data, there's no point in attempting to parse it.
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex);
            ex.printStackTrace();
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


        return null;
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
                ContentValues cVArray[] = new ContentValues[cVVector.size()];
                cVVector.toArray(cVArray);
                mContext.getContentResolver().bulkInsert(MarkersEntry.CONTENT_URI,cVArray);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
