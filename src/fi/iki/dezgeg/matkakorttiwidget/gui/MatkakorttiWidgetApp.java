package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

import fi.iki.dezgeg.matkakorttiwidget.matkakortti.Card;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.MatkakorttiApi;

public class MatkakorttiWidgetApp extends Application {
    private static SharedPreferences prefs;

    // Synchronized since when the app is started from the IDE,
    // the SettingsActivity and the widget updater run concurrently ->
    // both log in to Oma Matkakortti simultaneously -> session errors.
    public static synchronized List<Card> getCardList() throws Exception {
        String username = prefs.getString("username", "<not set>");
        String password = prefs.getString("password", "");

        return new MatkakorttiApi(username, password).getCards();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }
}