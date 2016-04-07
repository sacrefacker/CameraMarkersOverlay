package com.example.al.cameramarkersoverlay.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class JHelper {
    private static JHelper instance;
    private static Context savedContext;

    public static JHelper getInstance(Context context) {
        savedContext = context;
        if (instance == null) {
            instance = new JHelper();
        }
        return instance;
    }

    public static final String POINTS_URL = "http://demo.geo2tag.org/instance/service/testservice/point?";
    public static final String CHANNELS_URL = "http://demo.geo2tag.org/instance/service/testservice/channel?";
    public static final String NUMBER_PARAM = "number";
    public static final String CHANNEL_IDS_PARAM = "channel_ids";
    public static final int QUANTITY = 10;

    public static final String PREFS_KEY = "channels";

//    public static final String CHANNEL = "55dc620fbe9b3bf61be83f93";

    private Set<String> mChannelIds = new HashSet<>();

    private JHelper() {
        loadChannels();
    }

    private void loadChannels() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(savedContext);
        mChannelIds = prefs.getStringSet(PREFS_KEY, mChannelIds);
    }

    public void saveChanges() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(savedContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(PREFS_KEY, mChannelIds);
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
