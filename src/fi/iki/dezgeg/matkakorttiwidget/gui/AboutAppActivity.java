package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.WebView;

import fi.iki.dezgeg.matkakorttiwidget.R;

public class AboutAppActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView vw = new WebView(this);
        vw.loadUrl("file:///android_asset/about.html");
        vw.setBackgroundColor(0);

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.settings_aboutDialog_title)
                .setView(vw)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        PreferenceManager.getDefaultSharedPreferences(AboutAppActivity.this)
                                .edit()
                                .putBoolean("disclaimerShown", true)
                                .commit();
                        AboutAppActivity.this.finish();
                    }
                }).show();
    }
}
