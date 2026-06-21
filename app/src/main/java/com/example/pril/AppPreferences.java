package com.example.pril;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "WhisperPrefs";
    private static final String KEY_MSG_NOTIF = "msg_notif";
    private static final String KEY_CALL_NOTIF = "call_notif";
    private static final String KEY_VIBRO = "vibro";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_AVATAR_URI = "avatar_uri";
    private static final String KEY_THEME_DARK = "theme_dark";
    private static final String KEY_ENERGY_SAVER = "energy_saver";
    private static final String KEY_AUTO_DOWNLOAD = "auto_download";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_PRIVACY_ONLINE = "privacy_online";

    private static final String KEY_SAVED_ACCOUNTS = "saved_accounts";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveAccount(String email, String password, String name, String avatar) {
        try {
            org.json.JSONArray accounts;
            String saved = prefs.getString(KEY_SAVED_ACCOUNTS, "[]");
            accounts = new org.json.JSONArray(saved);
            
            int existingIndex = -1;
            for (int i = 0; i < accounts.length(); i++) {
                if (accounts.getJSONObject(i).getString("email").equals(email)) {
                    existingIndex = i;
                    break;
                }
            }
            
            org.json.JSONObject account = new org.json.JSONObject();
            account.put("email", email);
            account.put("password", password);
            account.put("name", name != null ? name : "");
            account.put("avatar", avatar != null ? avatar : "");
            
            if (existingIndex != -1) {
                accounts.put(existingIndex, account);
            } else {
                accounts.put(account);
            }
            
            prefs.edit().putString(KEY_SAVED_ACCOUNTS, accounts.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getSavedAccounts() {
        return prefs.getString(KEY_SAVED_ACCOUNTS, "[]");
    }

    public void removeAccount(String email) {
        try {
            org.json.JSONArray accounts = new org.json.JSONArray(prefs.getString(KEY_SAVED_ACCOUNTS, "[]"));
            org.json.JSONArray newAccounts = new org.json.JSONArray();
            for (int i = 0; i < accounts.length(); i++) {
                if (!accounts.getJSONObject(i).getString("email").equals(email)) {
                    newAccounts.put(accounts.getJSONObject(i));
                }
            }
            prefs.edit().putString(KEY_SAVED_ACCOUNTS, newAccounts.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMsgNotifications(boolean enabled) { prefs.edit().putBoolean(KEY_MSG_NOTIF, enabled).apply(); }
    public boolean isMsgNotificationsEnabled() { return prefs.getBoolean(KEY_MSG_NOTIF, true); }

    public void setCallNotifications(boolean enabled) { prefs.edit().putBoolean(KEY_CALL_NOTIF, enabled).apply(); }
    public boolean isCallNotificationsEnabled() { return prefs.getBoolean(KEY_CALL_NOTIF, true); }

    public void setVibration(boolean enabled) { prefs.edit().putBoolean(KEY_VIBRO, enabled).apply(); }
    public boolean isVibrationEnabled() { return prefs.getBoolean(KEY_VIBRO, true); }

    public void setUserName(String name) { prefs.edit().putString(KEY_USER_NAME, name).apply(); }
    public String getUserName() { return prefs.getString(KEY_USER_NAME, "Пользователь"); }

    public void setIsLoggedIn(boolean loggedIn) { prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply(); }
    public boolean isLoggedIn() { return prefs.getBoolean(KEY_IS_LOGGED_IN, false); }

    public void setUserEmail(String email) { prefs.edit().putString(KEY_USER_EMAIL, email).apply(); }
    public String getUserEmail() { return prefs.getString(KEY_USER_EMAIL, ""); }

    public void setAvatarUri(String uri) { prefs.edit().putString(KEY_AVATAR_URI, uri).apply(); }
    public String getAvatarUri() { return prefs.getString(KEY_AVATAR_URI, null); }

    public void setDarkMode(boolean enabled) { prefs.edit().putBoolean(KEY_THEME_DARK, enabled).apply(); }
    public boolean isDarkMode() { return prefs.getBoolean(KEY_THEME_DARK, false); }

    public void setEnergySaver(boolean enabled) { prefs.edit().putBoolean(KEY_ENERGY_SAVER, enabled).apply(); }
    public boolean isEnergySaverEnabled() { return prefs.getBoolean(KEY_ENERGY_SAVER, false); }

    public void setAutoDownload(boolean enabled) { prefs.edit().putBoolean(KEY_AUTO_DOWNLOAD, enabled).apply(); }
    public boolean isAutoDownloadEnabled() { return prefs.getBoolean(KEY_AUTO_DOWNLOAD, true); }

    public void setLanguage(String lang) { prefs.edit().putString(KEY_LANGUAGE, lang).apply(); }
    public String getLanguage() { return prefs.getString(KEY_LANGUAGE, "ru"); }

    public void setPrivacyOnline(boolean visible) { prefs.edit().putBoolean(KEY_PRIVACY_ONLINE, visible).apply(); }
    public boolean isPrivacyOnlineVisible() { return prefs.getBoolean(KEY_PRIVACY_ONLINE, true); }
    
    public void logout() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .putString(KEY_USER_NAME, "Пользователь")
                .putString(KEY_USER_EMAIL, "")
                .putString(KEY_AVATAR_URI, null)
                .apply();
    }
}