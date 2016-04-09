package com.example.al.cameramarkersoverlay;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.support.v4.widget.CursorAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.al.cameramarkersoverlay.data.ChannelsContainer;

public class AdapterFiltersList extends CursorAdapter{
    private static final String LOG_TAG = AdapterFiltersList.class.getSimpleName();

//    public static final int COLUMN_NAME = 0;
//    public static final int COLUMN_ID = 1;

    private InterfaceRefreshList mRefreshCallback;

    public AdapterFiltersList(Context context, Cursor c, int flags, InterfaceRefreshList callback) {
        super(context, c, flags);
        mRefreshCallback = callback;
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Log.i(LOG_TAG, "newView");
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_filters, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        Log.i(LOG_TAG, "bindView");

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        final String channelId = cursor.getString(FragmentFilters.CURSOR_COLUMN_ID);

        viewHolder.checkBox.setChecked(ChannelsContainer.getInstance(context).isChannelChecked(channelId));
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // потому что onCheckedChanged срабатывает, когда скроллишь список
                if (buttonView.isPressed()) {
                    Log.i(LOG_TAG, "checked " + isChecked);
                    ChannelsContainer.getInstance(context).checkChannel(channelId, isChecked);
                }
            }
        });

        String channelName = cursor.getString(FragmentFilters.CURSOR_COLUMN_NAME);
        viewHolder.textView.setText(channelName);
        viewHolder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(LOG_TAG, "super checked");
                ChannelsContainer.getInstance(context).clearChannels();
                ChannelsContainer.getInstance(context).checkChannel(channelId, true);
                mRefreshCallback.refreshList();
            }
        });
    }

    public static class ViewHolder {
        public final CheckBox checkBox;
        public final TextView textView;

        public ViewHolder(View view) {
            checkBox = (CheckBox) view.findViewById(R.id.list_item_checkbox);
            textView = (TextView) view.findViewById(R.id.list_item_text);
        }
    }
}
