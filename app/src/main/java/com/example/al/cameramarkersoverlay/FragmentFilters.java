package com.example.al.cameramarkersoverlay;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.al.cameramarkersoverlay.data.JHelper;
import com.example.al.cameramarkersoverlay.data.MarkersContract;
import com.example.al.cameramarkersoverlay.data.MarkersContract.ChannelEntry;

public class FragmentFilters extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = FragmentFilters.class.getSimpleName();
    private static final int CHANNELS_LOADER = 1;

    // выбираем колонки таблицы для извлечения курсором из БД
    private static final String[] CHANNELS_COLUMNS = {
            ChannelEntry.TABLE_NAME + "." + ChannelEntry._ID,
            ChannelEntry.COLUMN_NAME,
            ChannelEntry.COLUMN_ID
    };
    // для получения значений из курсора
    static final int CURSOR_COLUMN_NAME = 1;
    static final int CURSOR_COLUMN_ID = 2;

    private Context mContext;

    private AdapterFiltersList mAdapterFiltersList;

    public FragmentFilters() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        setHasOptionsMenu(true);
        new TaskDownloadChannels(mContext).execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_filters, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            downloadChannels();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void downloadChannels() {
        new TaskDownloadChannels(mContext).execute();
        getLoaderManager().restartLoader(CHANNELS_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_filters, container, false);

        mAdapterFiltersList = new AdapterFiltersList(mContext,null,0);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_channels);
        listView.setAdapter(mAdapterFiltersList);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onActivityCreated");
        getLoaderManager().initLoader(CHANNELS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        JHelper.getInstance(mContext).saveChanges();
        super.onPause();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.i(LOG_TAG, "onCreateLoader");
        Uri channelsUri = MarkersContract.ChannelEntry.buildAllChannels();
        return new CursorLoader(
                mContext, // контекст
                channelsUri, // URI
                CHANNELS_COLUMNS, // выбор колонок таблицы
                null, // условия
                null, // аргументы для условий (подстановка в "?")
                null // сортировка
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i(LOG_TAG, "onLoadFinished");
        mAdapterFiltersList.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.i(LOG_TAG, "onLoaderReset");
        mAdapterFiltersList.swapCursor(null);
    }
}
