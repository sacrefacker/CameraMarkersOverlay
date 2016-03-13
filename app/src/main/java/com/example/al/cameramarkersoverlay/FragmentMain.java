package com.example.al.cameramarkersoverlay;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentMain extends Fragment {
    public static final String LOG_TAG = FragmentMain.class.getSimpleName();
    public static final String BUNDLE_LOCATION = "location";

    private static final long POLLING_FREQ = 1000 * 10;
    private static final float MIN_DISTANCE = 10.0f;

    private static final Location LOCATION_MOSCOW;
    private static final Location LOCATION_TOMSK;

    static {
        LOCATION_MOSCOW = new Location(LocationManager.NETWORK_PROVIDER);
        LOCATION_MOSCOW.setLatitude(55.7500);
        LOCATION_MOSCOW.setLongitude(37.6167);
        LOCATION_TOMSK = new Location(LocationManager.NETWORK_PROVIDER);
        LOCATION_TOMSK.setLatitude(56.5000);
        LOCATION_TOMSK.setLongitude(84.9667);
    }

    private Context mContext;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLocation;

    private TextView mLocationTextView;
    private ArrayAdapter mArrayAdapter;
    private String[] mLocationArray;

    public FragmentMain() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getParcelable(BUNDLE_LOCATION);
        }

        // Acquire reference to the LocationManager
        if (null == (mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE))) {
            Log.e(LOG_TAG, "error getting location manager");
        }
        
        mLocationListener = new LocationListener() {
            // Called back when location changes
            public void onLocationChanged(Location location) {
                mLocation = location;
                updateLocationTextView();
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
            public void onProviderEnabled(String provider) {

            }
            public void onProviderDisabled(String provider) {

            }
        };
    }

    private void updateLocationTextView() {
        if (mLocation != null) {
            mLocationTextView.setText(String.format(mContext.getString(R.string.format_location),
                    mLocation.getLatitude(), mLocation.getLongitude()));
            mLocationArray[0] = String.format(mContext.getString(R.string.format_location_bearings),
                    LOCATION_MOSCOW.getLatitude(), LOCATION_MOSCOW.getLongitude(),
                    mLocation.bearingTo(LOCATION_MOSCOW));
            mLocationArray[1] = String.format(mContext.getString(R.string.format_location_bearings),
                    LOCATION_TOMSK.getLatitude(), LOCATION_TOMSK.getLongitude(),
                    mLocation.bearingTo(LOCATION_TOMSK));
            mArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mLocationTextView = (TextView) rootView.findViewById(R.id.text_location);
        mLocationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocation != null) {
                    openLocationOnMap(mLocation);
                }
            }
        });

        mLocationArray = new String[2];
        mLocationArray[0] = String.format(mContext.getString(R.string.format_location),
                LOCATION_MOSCOW.getLatitude(), LOCATION_MOSCOW.getLongitude());
        mLocationArray[1] = String.format(mContext.getString(R.string.format_location),
                LOCATION_TOMSK.getLatitude(), LOCATION_TOMSK.getLongitude());

        mArrayAdapter = new ArrayAdapter<>(mContext,
                R.layout.list_item, R.id.list_item_id, mLocationArray);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_points);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                switch (position) {
                    case 0:
                        openLocationOnMap(LOCATION_MOSCOW);
                        break;
                    case 1:
                        openLocationOnMap(LOCATION_TOMSK);
                        break;
                    default:
                        break;
                }
            }
        });

        return rootView;
    }

    private void openLocationOnMap(Location location) {
        Log.i(LOG_TAG, "called");

        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        String locationString = "geo:".concat(String.valueOf(location.getLatitude())).concat(",")
                .concat(String.valueOf(location.getLongitude()));
        Uri geoLocation = Uri.parse(locationString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        Log.i(LOG_TAG, locationString);

        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + locationString + ", no receiving apps installed!");
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        updateLocationTextView();

        // Register for network location updates
        try {
            if (null != mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager
                        .NETWORK_PROVIDER, POLLING_FREQ, MIN_DISTANCE, mLocationListener);
            }
            // Register for GPS location updates
            if (null != mLocationManager.getProvider(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager
                        .GPS_PROVIDER, POLLING_FREQ, MIN_DISTANCE, mLocationListener);
            }
        }
        catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_LOCATION, mLocation);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            mLocationManager.removeUpdates(mLocationListener);
        }
        catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }
}
