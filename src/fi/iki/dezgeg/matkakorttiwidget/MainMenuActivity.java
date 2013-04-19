package fi.iki.dezgeg.matkakorttiwidget;

import android.os.Bundle;
import android.preference.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.view.Menu;

public class MainMenuActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
    private static final String[] PREF_KEYS = new String[] { "username" };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_menu);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        for (String prefKey : PREF_KEYS)
            onSharedPreferenceChanged(getPreferenceScreen().getSharedPreferences(), prefKey);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        if (key.equals("username")) {
            findPreference(key).setSummary(prefs.getString(key, ""));
        }
    }

}
