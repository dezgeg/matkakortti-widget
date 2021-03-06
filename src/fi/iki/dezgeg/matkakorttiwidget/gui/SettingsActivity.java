/*
 * Matkakortti Widget is licensed under GPLv2.
 * See LICENSE.txt for more information.
 */

package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import fi.iki.dezgeg.matkakorttiwidget.R;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.Card;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.MatkakorttiException;

import static android.util.Log.d;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    private boolean isInitialConfigure;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private List<Card> fetchedCards;
    private long lastCardListUpdate;
    private FetchCardListTask fetchCardListTask;

    private static final long MIN_UPDATE_INTERVAL_MS = 5 * 60 * 1000;
    private static final String[] PREF_KEYS = new String[]{"username", "password"};

    private static final String PER_WIDGET_PREF_STRING_PREFIX = "settings_widgetPrefs_";
    private static final String[][] PER_WIDGET_PREFS = new String[][]{
            {"showName", "true"},
            {"autoHidePeriod", "false"},
    };


    private PreferenceGroup getCardListPrefGroup() {
        return (PreferenceGroup) findPreference("cardList");
    }

    private boolean loginDetailsFilledIn() {
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        return !prefs.getString("username", "").equals("") && !prefs.getString("password", "").equals("");
    }

    private void updateOkButtonEnabledState() {
        boolean enable = !isInitialConfigure ||
                (loginDetailsFilledIn() && fetchedCards != null && !fetchedCards.isEmpty());
        findViewById(R.id.settings_ok_button).setEnabled(enable);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("cardList", fetchedCards == null ? null : fetchedCards.toArray(new Card[0]));
        outState.putLong("lastCardListUpdate", lastCardListUpdate);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        d("SettingsActivity", "onNewIntent: " + intent.toString());
        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle state) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(state);

        d("SettingsActivity", "onCreate");
        setResult(RESULT_CANCELED);
        if (state != null) {
            lastCardListUpdate = state.getLong("lastCardListUpdate", 0);
            Card[] arr = (Card[]) state.getSerializable("cardList");
            if (arr != null)
                fetchedCards = Arrays.asList(arr);
        }

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
        ((ImageButton) findViewById(R.id.settings_ok_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                SettingsActivity.this.setResult(RESULT_OK, resultValue);
                if (isInitialConfigure)
                    SettingsActivity.this.finish();
                else
                    SettingsActivity.this.moveTaskToBack(true);
            }
        });
    }

    private CharSequence localize(int resId, Object... args) {
        return getResources().getString(resId, args);
    }

    @Override
    protected void onResume() {
        super.onResume();

        isInitialConfigure = getIntent().getAction().equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            appWidgetId = getIntent().getIntExtra("EXTRA_APPWIDGET_ID", AppWidgetManager.INVALID_APPWIDGET_ID);
        d("SettingsActivity", "Launched for AppWidget " + appWidgetId + ", initial: " + isInitialConfigure);

        if (!getPreferenceScreen().getSharedPreferences().getBoolean("disclaimerShown", false)) {
            Intent show = new Intent("fi.iki.dezgeg.matkakorttiwidget.SHOW_ABOUT_DIALOG");
            show.setClass(this, AboutAppActivity.class);
            startActivity(show);
        }
        populateCardListPrefGroup();

        PreferenceGroup perWidgetPrefs = (PreferenceGroup) findPreference("widgetPrefs");
        perWidgetPrefs.removeAll();
        for (String[] pair : PER_WIDGET_PREFS) {
            String key = pair[0];
            boolean defaultValue = Boolean.valueOf(pair[1]);

            CheckBoxPreference pref = new CheckBoxPreference(this);
            int titleResId = getResources().getIdentifier(PER_WIDGET_PREF_STRING_PREFIX + key + "_title",
                    "string", "fi.iki.dezgeg.matkakorttiwidget");
            int summaryResId = getResources().getIdentifier(PER_WIDGET_PREF_STRING_PREFIX + key + "_summary",
                    "string", "fi.iki.dezgeg.matkakorttiwidget");
            pref.setTitle(localize(titleResId));
            if (summaryResId > 0)
                pref.setSummary(summaryResId);

            pref.setDefaultValue(defaultValue);
            pref.setKey(Utils.prefKeyForWidgetId(appWidgetId, key));

            perWidgetPrefs.addPreference(pref);
        }
        if (MatkakorttiWidgetApp.DEBUG) {
            Preference forgetDisclaimerShown = new Preference(this);
            forgetDisclaimerShown.setTitle("Forget disclaimer shown state");
            forgetDisclaimerShown.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getPreferenceScreen().getSharedPreferences().edit().remove("disclaimerShown").commit();
                    return false;
                }
            });
            perWidgetPrefs.addPreference(forgetDisclaimerShown);

            Preference crashMe = new Preference(this);
            crashMe.setTitle("Crash me");
            crashMe.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    throw new RuntimeException("Crash me!");
                }
            });
            perWidgetPrefs.addPreference(crashMe);
        }

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        for (String prefKey : PREF_KEYS)
            updatePrefTitle(getPreferenceScreen().getSharedPreferences(), prefKey);
        updateCardList(false);
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
            updateCardList(true);
        }

        updateOkButtonEnabledState();
    }

    private synchronized void updateCardList(boolean cancelExisting) {
        if (!loginDetailsFilledIn())
            return;
        boolean existing = fetchCardListTask != null &&
                fetchCardListTask.getStatus() != AsyncTask.Status.FINISHED;
        if (existing && !cancelExisting)
            return;

        long current = new Date().getTime();
        if (!cancelExisting && (current - lastCardListUpdate) < MIN_UPDATE_INTERVAL_MS)
            return;

        if (existing)
            fetchCardListTask.cancel(true);
        fetchCardListTask = new FetchCardListTask();
        fetchCardListTask.execute();
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

    private void setSelectedCardForThisWidget(String cardId, String thisWidgetKey) {
        SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
        editor.putString(thisWidgetKey, cardId).commit();
        d("SettingsActivity", thisWidgetKey + " setting changing to " + cardId);
    }

    private void populateCardListPrefGroup() {
        if (fetchedCards == null)
            return;

        final PreferenceGroup cardListPrefGroup = getCardListPrefGroup();
        cardListPrefGroup.removeAll();

        // Uh oh. Simulate radio buttons with checkboxes since there is no RadioButtonPreference.
        final String thisWidgetKey = Utils.prefKeyForWidgetId(appWidgetId, "cardSelected");
        String selectedCardId = getPreferenceScreen().getSharedPreferences().getString(thisWidgetKey, "");
        boolean foundSelected = false;
        for (final Card card : fetchedCards) {
            final CheckBoxPreference pref = new CheckBoxPreference(SettingsActivity.this);
            pref.setWidgetLayoutResource(R.layout.radiobutton_preference);
            pref.setTitle(card.getName());
            pref.setSummary(radioButtonSummaryForCard(card));
            if (!foundSelected && card.getId().equals(selectedCardId)) {
                pref.setChecked(true);
                foundSelected = true;
            }
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                // !!!!! When we are called, the box has already changed state.
                @Override
                public boolean onPreferenceClick(Preference unused) {
                    // Trivial case: Un-checking the selected box, undo it.
                    if (!pref.isChecked()) {
                        pref.setChecked(true);
                        return true;
                    }
                    // Clear all others...
                    for (int i = 0; i < cardListPrefGroup.getPreferenceCount(); i++) {
                        CheckBoxPreference cp = ((CheckBoxPreference) cardListPrefGroup.getPreference(i));
                        cp.setChecked(false);
                    }
                    pref.setChecked(true); // ...but not this one.

                    setSelectedCardForThisWidget(card.getId(), Utils.prefKeyForWidgetId(appWidgetId, "cardSelected"));
                    return true;
                }
            });
            cardListPrefGroup.addPreference(pref);
        }

        if (!foundSelected && cardListPrefGroup.getPreferenceCount() > 0) {
            setSelectedCardForThisWidget(fetchedCards.get(0).getId(), thisWidgetKey);
            ((CheckBoxPreference) cardListPrefGroup.getPreference(0)).setChecked(true);
        }
    }

    private CharSequence radioButtonSummaryForCard(Card card) {
        CharSequence period;
        if (card.getPeriodExpiryDate() == null)
            period = localize(R.string.settings_cardList_card_noPeriod);
        else
            period = localize(R.string.settings_cardList_card_hasPeriod,
                    new SimpleDateFormat("dd.MM.yyyy").format(card.getPeriodExpiryDate()));
        CharSequence money = localize(R.string.settings_cardList_card_hasMoney,
                WidgetUpdaterService.FORMAT_TWO_DIGITS_AFTER_POINT.format(card.getMoney()));
        return money.toString() + " " + period;
    }

    // Helper classes
    private class FetchCardListTask extends AsyncTask<Void, Void, MatkakorttiApiResult> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            SettingsActivity.this.setProgressBarIndeterminate(true);
            SettingsActivity.this.setProgressBarIndeterminateVisibility(true);

            getCardListPrefGroup().removeAll();
            Preference text = new Preference(SettingsActivity.this);
            text.setTitle(Html.fromHtml("<font color='#888888'>" + localize(R.string.settings_cardList_loading) + "</font>"));
            getCardListPrefGroup().addPreference(text);

            d("FetchCardListTask", "Updater task starting...");
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
            Exception exc = result.getException();

            if (exc == null && !result.getCardList().isEmpty()) {
                lastCardListUpdate = new Date().getTime();
                fetchedCards = result.getCardList();
                WidgetUpdaterService.updateWidgets(getApplicationContext(), fetchedCards);
                populateCardListPrefGroup();
            } else {
                if (exc == null && result.getCardList().isEmpty())
                    exc = new MatkakorttiException(localize(R.string.settings_errors_accountHasNoCards).toString(), false);
                MatkakorttiException apiExc = exc instanceof MatkakorttiException ? (MatkakorttiException) exc : null;

                lastCardListUpdate = 0;
                fetchedCards = null;
                Preference text = new Preference(SettingsActivity.this);

                CharSequence escaped;
                if (Utils.isConnectionProblemRelatedException(exc))
                    escaped = localize(R.string.settings_errors_connectionError);
                else if (apiExc != null && !apiExc.isInternalError())
                    escaped = localize(R.string.settings_errors_apiErrorPrefix) + " " +
                            exc.getMessage(); // TODO: escape
                else {
                    escaped = localize(R.string.settings_errors_unexpectedError);
                    MatkakorttiWidgetApp.reportException("SettingsActivity", exc);
                }
                text.setTitle(Html.fromHtml("<font color='#FF0000'>" + escaped + "</font>"));

                final CharSequence finalMsg = escaped;
                text.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(SettingsActivity.this)
                                .setCancelable(true)
                                .setTitle(R.string.settings_errors_errorDialog_title)
                                .setMessage(finalMsg)
                                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        dialog.cancel();
                                    }
                                })
                                .create().show();
                        return true;
                    }
                });
                getCardListPrefGroup().removeAll();
                getCardListPrefGroup().addPreference(text);
            }
            SettingsActivity.this.setProgressBarIndeterminateVisibility(false);
            updateOkButtonEnabledState();
            d("FetchCardListTask", "Updater task ended...");
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
