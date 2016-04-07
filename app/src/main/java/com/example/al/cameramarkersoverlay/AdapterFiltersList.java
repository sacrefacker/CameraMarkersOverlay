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

import com.example.al.cameramarkersoverlay.data.JsonHelper;

import java.security.acl.LastOwnerException;

public class AdapterFiltersList extends CursorAdapter{
    private static final String LOG_TAG = AdapterFiltersList.class.getSimpleName();

    public static final int COLUMN_NAME = 0;
    public static final int COLUMN_ID = 1;

    public AdapterFiltersList(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
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
        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final String channelId = cursor.getString(FragmentFilters.CURSOR_COLUMN_ID);
        viewHolder.checkBox.setChecked(JsonHelper.getInstance(context).isChannelChecked(channelId));
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean currentState = viewHolder.checkBox.isChecked();
                JsonHelper.getInstance(context).checkChannel(channelId, currentState);
            }
        });

        String channelName = cursor.getString(FragmentFilters.CURSOR_COLUMN_NAME);
        viewHolder.channel.setText(channelName);

    }

    public static class ViewHolder {
        public final CheckBox checkBox;
        public final TextView channel;

        public ViewHolder(View view) {
            checkBox = (CheckBox) view.findViewById(R.id.list_item_checkbox);
            channel = (TextView) view.findViewById(R.id.list_item_text);
        }
    }
}
