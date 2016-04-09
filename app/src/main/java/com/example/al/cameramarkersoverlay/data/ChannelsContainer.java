package com.example.al.cameramarkersoverlay.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class ChannelsContainer {
    private static final String LOG_TAG = ChannelsContainer.class.getSimpleName();

    public static final String PREFS_KEY = "channels";

//    public static final String CHANNEL = "55dc620fbe9b3bf61be83f93";

    private static ChannelsContainer instance;
    private static Context savedContext;

    public static ChannelsContainer getInstance(Context context) {
        savedContext = context;
        if (instance == null) {
            instance = new ChannelsContainer();
        }
        return instance;
    }

    private Set<String> mChannelIds;

    private ChannelsContainer() {
        loadChannels();
    }

    private void loadChannels() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(savedContext);
        Set<String> loaded = prefs.getStringSet(PREFS_KEY, null);
        mChannelIds = (loaded == null) ? new HashSet<String>() : new HashSet<>(loaded);
        Log.i(LOG_TAG, "in" + mChannelIds.toString());
    }

    public void saveChanges() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(savedContext);
        SharedPreferences.Editor editor = prefs.edit();
//        editor.remove(PREFS_KEY);
        editor.putStringSet(PREFS_KEY, mChannelIds);
        Log.i(LOG_TAG, "out" + mChannelIds.toString());
        editor.apply();
    }

    public boolean isChannelChecked(String channelId) {
        return mChannelIds.contains(channelId);
    }

    public void checkChannel(String channelId, boolean check) {
        if (check) {
            mChannelIds.add(channelId);
        }
        else {
            mChannelIds.remove(channelId);
        }
    }

    public void clearChannels() {
        mChannelIds.clear();
    }

    public Set<String> getChannels() {
        return mChannelIds;
    }
}
