package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import fi.iki.dezgeg.matkakorttiwidget.matkakortti.Card;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.MatkakorttiApi;

public class MatkakorttiWidgetApp extends Application {
    private static SharedPreferences prefs;
    public static final boolean DEBUG = true;

    // Synchronized since when the app is started from the IDE,
    // the SettingsActivity and the widget updater run concurrently ->
    // both log in to Oma Matkakortti simultaneously -> session errors.
    public static synchronized List<Card> getCardList() throws Exception {
        String username = prefs.getString("username", "<not set>");
        String password = prefs.getString("password", "");

        if (DEBUG && username.equals("dezgegt"))
            return Arrays.asList(
                    new Card("Tyhja", "a", BigDecimal.ZERO, null),
                    new Card("Rahaa", "b", BigDecimal.TEN, null),
                    new Card("Kautta", "c", BigDecimal.ZERO, new Date(113, 11, 28)),
                    new Card("Raha+Kausi", "d", new BigDecimal("99.99"), new Date()),
                    new Card("Paljon rahaa", "e", new BigDecimal("199.99"), null));
        return new MatkakorttiApi(username, password).getCards();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }
}