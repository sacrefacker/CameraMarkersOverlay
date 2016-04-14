package com.example.al.cameramarkersoverlay;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class FragmentDialog extends DialogFragment {
    private static final String LOG_TAG = FragmentDialog.class.getSimpleName();

    public static final String BUNDLE_LAT = "lat";
    public static final String BUNDLE_LONG = "long";
    public static final String BUNDLE_DIST = "dist";
    public static final String BUNDLE_NAME = "name";
    public static final String BUNDLE_TYPE = "type";

    private double mLatitude = 0.0, mLongitude = 0.0;
    private double mDistance = 0.0;
    private String mName = "", mType = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mLatitude = bundle.getDouble(BUNDLE_LAT);
            mLongitude = bundle.getDouble(BUNDLE_LONG);
            mDistance = bundle.getDouble(BUNDLE_DIST);
            mName = bundle.getString(BUNDLE_NAME);
            mType = bundle.getString(BUNDLE_TYPE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View customDialogView = inflater.inflate(R.layout.fragment_dialog, null);

        TextView textView = (TextView) customDialogView.findViewById(R.id.dialog_location_text);
        textView.setText(String.format(getString(R.string.format_dialog),
                mName, mType, mDistance, mLatitude, mLongitude));

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(customDialogView)
                .setNegativeButton(
                        R.string.button_show_map,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                openLocationOnMap();
                            }
                        })
                .setPositiveButton(
                        R.string.button_close,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    // при нажатии кнопки открываем Google Maps с содежращейся в маркере локацией
    private void openLocationOnMap() {

        String locationString = "geo:".concat(String.valueOf(mLatitude)).concat(",").concat(String.valueOf(mLongitude));
        Uri geoLocation = Uri.parse(locationString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        Log.i(LOG_TAG, locationString);

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't resolve");
        }
    }
}
