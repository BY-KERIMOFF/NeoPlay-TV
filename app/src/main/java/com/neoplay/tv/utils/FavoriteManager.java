package com.neoplay.tv.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavoriteManager {
    private static final String PREF_NAME = "neoplay_favorites";
    private static final String KEY_FAVORITES = "favorite_channels";
    private final SharedPreferences prefs;

    public FavoriteManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void toggleFavorite(String channelId) {
        Set<String> favorites = getFavorites();
        if (favorites.contains(channelId)) {
            favorites.remove(channelId);
        } else {
            favorites.add(channelId);
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    public boolean isFavorite(String channelId) {
        return getFavorites().contains(channelId);
    }

    public Set<String> getFavorites() {
        return new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
    }
}
