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
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

import fi.iki.dezgeg.matkakorttiwidget.R;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.Card;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.MatkakorttiException;

import static android.util.Log.d;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private volatile List<Card> fetchedCards;
    private FetchCardListTask fetchCardListTask;

    private static final String[] PREF_KEYS = new String[]{"username", "password"};

    private static final String PER_WIDGET_PREF_STRING_PREFIX = "settings_widgetPrefs_";
    private static final String[][] PER_WIDGET_PREFS = new String[][]{
            {"showName", "true"},
            {"autoHidePeriod", "false"},
    };

    private PreferenceGroup getCardList() {
        return (PreferenceGroup) findPreference("cardList");
    }

    private boolean loginDetailsFilledIn() {
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        return !prefs.getString("username", "").equals("") && !prefs.getString("password", "").equals("");
    }

    private void updateOkButtonEnabledState() {
        boolean enable = loginDetailsFilledIn() && fetchedCards != null && !fetchedCards.isEmpty();
        findViewById(R.id.settings_ok_button).setEnabled(enable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);

        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            appWidgetId = getIntent().getIntExtra("EXTRA_APPWIDGET_ID", AppWidgetManager.INVALID_APPWIDGET_ID);

        d("SettingsActivity", "Launched for AppWidget " + appWidgetId);
        setResult(RESULT_CANCELED);

        addPreferencesFromResource(R.xml.main_menu);
        PreferenceGroup perWidgetPrefs = (PreferenceGroup) findPreference("widgetPrefs");
        for (String[] pair : PER_WIDGET_PREFS) {
            String key = pair[0];
            boolean defaultValue = Boolean.valueOf(pair[1]);

            CheckBoxPreference pref = new CheckBoxPreference(this);
            int resId = getResources().getIdentifier(PER_WIDGET_PREF_STRING_PREFIX + key,
                    "string", "fi.iki.dezgeg.matkakorttiwidget");
            pref.setTitle(localize(resId));
            pref.setDefaultValue(defaultValue);
            pref.setKey(Utils.prefKeyForWidgetId(appWidgetId, key));

            perWidgetPrefs.addPreference(pref);
        }
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
                SettingsActivity.this.setResult(RESULT_OK, resultValue);
                SettingsActivity.this.finish();
            }
        });
    }

    private CharSequence localize(int resId) {
        return getResources().getText(resId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        for (String prefKey : PREF_KEYS)
            updatePrefTitle(getPreferenceScreen().getSharedPreferences(), prefKey);
        updateCardList();
        updateOkButtonEnabledState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        updatePrefTitle(prefs, key);

        if (key.startsWith("widget_") && fetchedCards != null) {
            WidgetUpdaterService.updateWidgets(getApplicationContext(), fetchedCards);
        } else if (key.equals("username") || key.equals("password")) {
                updateCardList();
        }

        updateOkButtonEnabledState();
    }

    private synchronized void updateCardList() {
        if (loginDetailsFilledIn()) {
            if (fetchCardListTask != null && fetchCardListTask.getStatus() != AsyncTask.Status.FINISHED)
                fetchCardListTask.cancel(true);
            fetchCardListTask = new FetchCardListTask();
            fetchCardListTask.execute();
        }
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

    // Helper classes
    private class FetchCardListTask extends AsyncTask<Void, Void, MatkakorttiApiResult> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            getCardList().removeAll();
            SettingsActivity.this.setProgressBarIndeterminate(true);
            SettingsActivity.this.setProgressBarIndeterminateVisibility(true);
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

            final PreferenceGroup cardList = getCardList();
            Exception exc = result.getException();

            if (exc != null) {
                MatkakorttiException apiExc = exc instanceof MatkakorttiException ? (MatkakorttiException) exc : null;

                fetchedCards = null;
                EditTextPreference text = new EditTextPreference(SettingsActivity.this);
                text.setEnabled(false);

                CharSequence escaped;
                if (Utils.isConnectionProblemRelatedException(exc))
                    escaped = localize(R.string.settings_errors_connectionError);
                else if (apiExc != null && !apiExc.isInternalError())
                    escaped = localize(R.string.settings_errors_apiErrorPrefix) + " " +
                            exc.getMessage(); // TODO: escape
                else {
                    escaped = localize(R.string.settings_errors_unexpectedError);
                    Utils.reportException("SettingsActivity", exc);
                }

                text.setTitle(Html.fromHtml("<font color='#FF0000'>" + escaped + "</font>"));
                cardList.addPreference(text);
            } else {
                fetchedCards = result.getCardList();
                WidgetUpdaterService.updateWidgets(getApplicationContext(), fetchedCards);

                // Uh oh. Simulate radio buttons with checkboxes since there is no RadioButtonPreference.
                final String thisWidgetKey = Utils.prefKeyForWidgetId(appWidgetId, "cardSelected");
                String selectedCardId = getPreferenceScreen().getSharedPreferences().getString(thisWidgetKey, "");
                boolean foundSelected = false;
                List<CheckBoxPreference> buttons = new ArrayList<CheckBoxPreference>();
                for (final Card card : result.getCardList()) {
                    final CheckBoxPreference pref = new CheckBoxPreference(SettingsActivity.this);
                    pref.setWidgetLayoutResource(R.layout.radiobutton_preference);
                    pref.setTitle(card.getName());
                    if (!foundSelected && card.getId().equals(selectedCardId)) {
                        pref.setChecked(true);
                        foundSelected = true;
                    }
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference unused) {
                            // !!!!! When we are called, the box has already changed state.

                            // Un-checking the selected box.
                            if (!pref.isChecked()) {
                                pref.setChecked(true);
                                return true;
                            }
                            for (int i = 0; i < cardList.getPreferenceCount(); i++) {
                                CheckBoxPreference cp = ((CheckBoxPreference) cardList.getPreference(i));
                                cp.setChecked(false);
                            }
                            pref.setChecked(true);
                            SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
                            editor.putString(thisWidgetKey, card.getId()).commit();
                            d("SettingsActivity", thisWidgetKey + " setting changing to " + card.getId());
                            return true;
                        }
                    });
                    cardList.addPreference(pref);
                }

                if (!foundSelected && cardList.getPreferenceCount() > 0) {
                    SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
                    editor.putString(thisWidgetKey, fetchedCards.get(0).getId()).commit();
                    d("SettingsActivity", thisWidgetKey + " setting changing to " + fetchedCards.get(0).getId());
                    ((CheckBoxPreference) cardList.getPreference(0)).setChecked(true);
                }
            }
            SettingsActivity.this.setProgressBarIndeterminateVisibility(false);
            updateOkButtonEnabledState();
        }
    }


    static class MatkakorttiApiResult {
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
}
