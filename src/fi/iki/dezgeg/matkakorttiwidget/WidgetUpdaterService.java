package fi.iki.dezgeg.matkakorttiwidget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.util.Timer;

public class WidgetUpdaterService extends IntentService
{
    private Timer timer = new Timer();
    public WidgetUpdaterService()
    {
        super("MatkakorttiWidgetUpdaterService");
    }
    @Override
    public void onHandleIntent(Intent source)
    {
        System.out.println("onHandleIntent");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int[] appWidgetIds = source.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String username = prefs.getString("username", "<not set>");
        String password = prefs.getString("password", "");

        String message;
        boolean isError = false;
        try {
            message = "" + MatkakorttiApi.getMoney(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            message = e.getMessage();
            isError = true;
        }

        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.homescreen_widget);
            remoteViews.setTextViewText(R.id.homescreen_text, message);
            if (isError) {
                remoteViews.setTextColor(R.id.homescreen_text, Color.RED);
                remoteViews.setTextViewTextSize(R.id.homescreen_text, TypedValue.COMPLEX_UNIT_PT, 5.0f);
            } else {
                remoteViews.setTextColor(R.id.homescreen_text, Color.WHITE);
                remoteViews.setTextViewTextSize(R.id.homescreen_text, TypedValue.COMPLEX_UNIT_PT, 12.0f);
            }
            Intent intent = new Intent(this.getApplicationContext(), HomescreenWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.homescreen_text, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

}
