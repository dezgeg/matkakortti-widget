package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

import java.util.List;

import fi.iki.dezgeg.matkakorttiwidget.R;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.Card;

public class MainMenuActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private List<Card> fetchedCards;
    private static final String[] PREF_KEYS = new String[]{"username", "password"};

    private class FetchCardListTask extends AsyncTask<Void, Void, MatkakorttiApiResult> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            getCardList().removeAll();
            MainMenuActivity.this.setProgressBarIndeterminate(true);
            MainMenuActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected MatkakorttiApiResult doInBackground(Void... unused) {
            try {
                return new MatkakorttiApiResult(MatkakorttiWidgetApp.getCardList());
            } catch (Exception e) {
                return new MatkakorttiApiResult(e);
            }
        }

        @Override
        protected void onPostExecute(MatkakorttiApiResult result) {
            super.onPostExecute(result);

            PreferenceGroup cardList = getCardList();
            if (result.getException() != null) {
                fetchedCards = null;
                EditTextPreference text = new EditTextPreference(MainMenuActivity.this);
                text.setEnabled(false);
                String escaped = result.getException().getMessage(); // TODO: escape
                text.setTitle(Html.fromHtml("<font color='#FF0000'>Error: " + escaped + "</font>"));
                cardList.addPreference(text);
            } else {
                fetchedCards = result.getCardList();
                WidgetUpdaterService.updateWidgets(getApplicationContext(), fetchedCards);

                for (Card card : result.getCardList()) {
                    CheckBoxPreference pref = new CheckBoxPreference(MainMenuActivity.this);
                    pref.setKey("cardSelected_" + card.getId());
                    pref.setTitle(card.getName());
                    cardList.addPreference(pref);
                }
            }
            MainMenuActivity.this.setProgressBarIndeterminateVisibility(false);
        }
    }

    private PreferenceGroup getCardList() {
        return (PreferenceGroup) findPreference("cardList");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.main_menu);
        setContentView(R.layout.settings_menu);

        findPreference("registerLink").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://omamatkakortti.hsl.fi"));
                startActivity(intent);
                return true;
            }
        });
        ((ImageButton)findViewById(R.id.settings_ok_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                MainMenuActivity.this.setResult(RESULT_OK, resultValue);
                MainMenuActivity.this.finish();
            }
        });

        setResult(RESULT_CANCELED);
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }


    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        for (String prefKey : PREF_KEYS)
            updatePrefTitle(getPreferenceScreen().getSharedPreferences(), prefKey);
        updateCardList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        updatePrefTitle(prefs, key);

        if (key.startsWith("cardSelected_") && fetchedCards != null) {
            WidgetUpdaterService.updateWidgets(getApplicationContext(), fetchedCards);
        } else if (key.equals("username") || key.equals("password")) {
            if (!findPreference("username").equals("") && !findPreference("password").equals(""))
                updateCardList();
        }
    }

    private void updateCardList() {
        new FetchCardListTask().execute();
    }

    private void updatePrefTitle(SharedPreferences prefs, String key) {

        if (key.equals("password")) {
            String password = prefs.getString(key, "");
            String masked = "";
            for (int i = 0; i < password.length(); i++)
                masked += "*";

            findPreference(key).setSummary(masked);
        } else if (findPreference(key) instanceof EditTextPreference) {
            findPreference(key).setSummary(prefs.getString(key, ""));
        }
    }

}

class MatkakorttiApiResult {
    private List<Card> cardList;
    private Exception exception;

    MatkakorttiApiResult(List<Card> cardList) {
        this.cardList = cardList;
    }

    MatkakorttiApiResult(Exception exception) {
        this.exception = exception;
    }

    List<Card> getCardList() {
        return cardList;
    }

    Exception getException() {
        return exception;
    }
}
