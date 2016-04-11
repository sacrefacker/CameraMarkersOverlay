package com.example.al.cameramarkersoverlay.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class MarkersContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.example.al.cameramarkersoverlay.app";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_MARKERS = "markers";

    public static final String PATH_CHANNELS = "channels";

    public static final class ChannelEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_CHANNELS).build();

        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CHANNELS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CHANNELS;

        public static Uri buildAllChannels() {
            return CONTENT_URI;
        }

        public static final String TABLE_NAME = "channels";

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_ID = "server_id";
    }

    public static final class MarkersEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MARKERS).build();

        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MARKERS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MARKERS;

        public static Uri buildAllMarkers() {
            return CONTENT_URI;
        }


        public static final String TABLE_NAME = "markers";

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_LAT = "coordinate0";
        public static final String COLUMN_LONG = "coordinate1";
        public static final String COLUMN_ALT = "alt";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_IMAGE = "image_url";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_BC = "bc";
        public static final String COLUMN_CHANNEL = "channel_id";
        public static final String COLUMN_ID = "server_id";
    }
}
