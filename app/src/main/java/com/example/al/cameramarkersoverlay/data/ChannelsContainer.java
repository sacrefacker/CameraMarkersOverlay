package com.example.al.cameramarkersoverlay.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class ChannelsContainer {
    private static final String LOG_TAG = ChannelsContainer.class.getSimpleName();

    public static final String PREFS_KEY_CHANNELS = "channels";
    public static final String PREFS_KEY_CHANGES = "changes";

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

    private boolean mHasChanges = true;

    private ChannelsContainer() {
        loadPrefs();
    }

    private void loadPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(savedContext);
        Set<String> loaded = prefs.getStringSet(PREFS_KEY_CHANNELS, null);
        mChannelIds = (loaded == null) ? new HashSet<String>() : new HashSet<>(loaded);
        mHasChanges = prefs.getBoolean(PREFS_KEY_CHANGES, true);
        Log.i(LOG_TAG, "in" + mChannelIds.toString() + ", flag " + mHasChanges);
    }

    public Set<String> getChannels() {
        return mChannelIds;
    }

    public void saveChanges() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(savedContext);
        SharedPreferences.Editor editor = prefs.edit();
//        editor.remove(PREFS_KEY_CHANNELS);
        editor.putStringSet(PREFS_KEY_CHANNELS, mChannelIds);
        editor.putBoolean(PREFS_KEY_CHANGES, mHasChanges);
        Log.i(LOG_TAG, "out" + mChannelIds.toString() + ", flag " + mHasChanges);
        editor.apply();
    }

    public void resetChanges() {
        loadPrefs();
    }

    public void clearChannels() {
        mChannelIds.clear();
        mHasChanges = true;
    }

    public void checkChannel(String channelId, boolean check) {
        if (check) {
            mChannelIds.add(channelId);
        }
        else {
            mChannelIds.remove(channelId);
        }
        mHasChanges = true;
    }

    public boolean isChannelChecked(String channelId) {
        return mChannelIds.contains(channelId);
    }

    public boolean hasChanges() {
        return mHasChanges;
    }

    public void clearChangesFlag() {
        Log.i(LOG_TAG, "Clearing changes flag, was: " + mHasChanges);
        mHasChanges = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(savedContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFS_KEY_CHANGES, false);
        // apply() возможно не успевает сработать, с ним маркеры качаются 2 раза
        editor.commit();
    }
}
