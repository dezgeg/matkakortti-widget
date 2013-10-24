package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.*;
import android.content.*;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.text.Html;
import android.view.Menu;
import android.view.Window;

import java.util.List;

import fi.iki.dezgeg.matkakorttiwidget.R;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.Card;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.MatkakorttiApi;

public class MainMenuActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
    private static final String[] PREF_KEYS = new String[] { "username", "password" };
    private class FetchCardListTask extends AsyncTask<MatkakorttiApi, Void, MatkakorttiApiResult> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            getCardList().removeAll();
            MainMenuActivity.this.setProgressBarIndeterminate(true);
            MainMenuActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected MatkakorttiApiResult doInBackground(MatkakorttiApi... matkakorttiApis) {
            MatkakorttiApi api = matkakorttiApis[0];
            try {
                return new MatkakorttiApiResult(api.getCards());
            } catch (Exception e) {
                return new MatkakorttiApiResult(e);
            }
        }

        @Override
        protected void onPostExecute(MatkakorttiApiResult result) {
            super.onPostExecute(result);

            PreferenceGroup cardList = getCardList();
            if (result.getException() != null) {
                EditTextPreference text = new EditTextPreference(MainMenuActivity.this);
                text.setEnabled(false);
                String escaped = result.getException().getMessage(); // TODO: escape
                text.setTitle(Html.fromHtml("<font color='#FF0000'>Error: " + escaped + "</font>"));
                cardList.addPreference(text);
            } else {
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_menu);
        getCardList().removeAll();
    }

    private PreferenceGroup getCardList() {
        return (PreferenceGroup) findPreference("cardList");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        for (String prefKey : PREF_KEYS)
            updatePrefTitle(getPreferenceScreen().getSharedPreferences(), prefKey);
        updateCardList();
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
        updatePrefTitle(prefs, key);
        if (key.equals("username") || key.equals("password")) {
            if(!findPreference("username").equals("") && !findPreference("password").equals("")) {
                updateCardList();
            }
        }
    }

    private void updateCardList() {
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        MatkakorttiApi api = new MatkakorttiApi(prefs.getString("username", ""), prefs.getString("password", ""));
        new FetchCardListTask().execute(api);
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
class MatkakorttiApiResult
{
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
