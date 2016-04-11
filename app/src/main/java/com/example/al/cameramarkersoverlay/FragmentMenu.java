package com.example.al.cameramarkersoverlay;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class FragmentMenu extends Fragment {
    private static final String LOG_TAG = FragmentMenu.class.getSimpleName();

    private static int DOWNSIZE = 3;

    private Context mContext;

    private enum Action {
        MAIN, FILTERS, NONE
    };

    public FragmentMenu() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mContext = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_menu, container, false);

        int iconSize = (int) getResources().getDimension(R.dimen.main_menu_icon_size);
        Resources res = getResources();

        ImageView buttonOverlay = (ImageView) rootView.findViewById(R.id.main_menu_overlay_button);
        buttonOverlay.setImageBitmap(sampledBitmap(res, R.drawable.earth_icon, iconSize, iconSize));
        buttonOverlay.setOnTouchListener(new ImageTouchListener(Action.MAIN));

        ImageView buttonFilters = (ImageView) rootView.findViewById(R.id.main_menu_filters_button);
        buttonFilters.setImageBitmap(sampledBitmap(res, R.drawable.checklist_icon, iconSize, iconSize));
        buttonFilters.setOnTouchListener(new ImageTouchListener(Action.FILTERS));

        final ImageView buttonSearch = (ImageView) rootView.findViewById(R.id.main_menu_find_button);
        buttonSearch.setImageBitmap(sampledBitmap(res, R.drawable.binoculars_icon, iconSize, iconSize));
        buttonSearch.setOnTouchListener(new ImageTouchListener(Action.NONE));

        ImageView buttonSettings = (ImageView) rootView.findViewById(R.id.main_menu_settings_button);
        buttonSettings.setImageBitmap(sampledBitmap(res, R.drawable.settings_icon, iconSize, iconSize));
        buttonSettings.setOnTouchListener(new ImageTouchListener(Action.NONE));

        return rootView;
    }

    private class ImageTouchListener implements View.OnTouchListener {
        private Action mAction;

        public ImageTouchListener(Action action) {
            mAction = action;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ImageView imageView = (ImageView) v;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    imageView.setBackgroundColor(Color.parseColor("#AAAAAA"));
                    imageView.invalidate();
                    break;
                }
                case MotionEvent.ACTION_UP:
                    Intent intent = null;
                    switch (mAction) {
                        case MAIN:
                            intent = new Intent(mContext, ActivityMain.class);
                            break;
                        case FILTERS:
                            intent = new Intent(mContext, ActivityFilters.class);
                            break;
                    }
                    if (intent != null) {
                        startActivity(intent);
                    }
                case MotionEvent.ACTION_CANCEL: {
                    imageView.setBackgroundColor(Color.parseColor("#CCCCCC"));
                    imageView.invalidate();
                    break;
                }
            }
            return true;
        }
    }

    public static Bitmap sampledBitmap(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight / DOWNSIZE
                    && (halfWidth / inSampleSize) > reqWidth / DOWNSIZE) {

                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
