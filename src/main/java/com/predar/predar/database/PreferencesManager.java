package com.predar.predar.database;

import java.util.prefs.Preferences;

public class PreferencesManager {

    private static final Preferences prefs =
            Preferences.userNodeForPackage(PreferencesManager.class);

    private static final String KEY_REMEMBER = "remember_user";
    private static final String KEY_USERNAME  = "saved_username";

    public static void saveUser(String username, boolean remember) {
        prefs.putBoolean(KEY_REMEMBER, remember);
        prefs.put(KEY_USERNAME, remember ? username : "");
    }

    public static String getSavedUsername() {
        return prefs.get(KEY_USERNAME, "");
    }

    public static boolean isRemembered() {
        return prefs.getBoolean(KEY_REMEMBER, false);
    }
}