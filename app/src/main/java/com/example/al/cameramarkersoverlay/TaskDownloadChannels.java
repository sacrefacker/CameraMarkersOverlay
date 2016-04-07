package com.example.al.cameramarkersoverlay;


import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.al.cameramarkersoverlay.data.JHelper;
import com.example.al.cameramarkersoverlay.data.MarkersContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

public class TaskDownloadChannels extends AsyncTask<String, Void, Void> {
    private static final String LOG_TAG = TaskDownloadChannels.class.getSimpleName();

    private Context mContext;

    public TaskDownloadChannels(Context context) {
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

            Uri builtUri = Uri.parse(JHelper.CHANNELS_URL).buildUpon()
                    .appendQueryParameter(JHelper.NUMBER_PARAM, String.valueOf(JHelper.QUANTITY))
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

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            String jsonStr = buffer.toString();
            getChannelsFromJson(jsonStr);

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
                values.put(MarkersContract.ChannelEntry.COLUMN_ID, id);
                values.put(MarkersContract.ChannelEntry.COLUMN_NAME, name);
                cVVector.add(values);
            }

            // добавляем значения в БД
            if ( cVVector.size() > 0 ) {
                ContentValues cVArray[] = new ContentValues[cVVector.size()];
                cVVector.toArray(cVArray);
                mContext.getContentResolver().bulkInsert(MarkersContract.ChannelEntry.CONTENT_URI,cVArray);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }
}