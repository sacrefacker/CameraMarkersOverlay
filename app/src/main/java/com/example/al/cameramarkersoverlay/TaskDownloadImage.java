package com.example.al.cameramarkersoverlay;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TaskDownloadImage extends AsyncTask<String, Void, Bitmap> {
    private static final String LOG_TAG = TaskDownloadImage.class.getSimpleName();

    private final static String URI_START = "http://";

    private InterfaceImageDownload callBack;

    private String mImageUri;

    public TaskDownloadImage(String imageUrl, InterfaceImageDownload parent) {
        callBack = parent;
        mImageUri = imageUrl;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        Log.i(LOG_TAG, "Trying to fetch image: " + mImageUri);

        try {
            urlConnection = (HttpURLConnection) new URL(URI_START + mImageUri).openConnection();
            inputStream = urlConnection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
            Log.i(LOG_TAG, "trying");
        } catch (Throwable ex) {
            ex.printStackTrace();
            if (!this.isCancelled()) {
                this.cancel(true);
            }
        } finally {
            try {
                if (urlConnection != null)
                    urlConnection.disconnect();
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        Log.i(LOG_TAG, "onPostExecute");
        callBack.putImage(bitmap);
        super.onPostExecute(bitmap);
    }
}
