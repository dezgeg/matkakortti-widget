package fi.iki.dezgeg.matkakorttiwidget;

import java.util.*;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.widget.*;

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
        System.out.println(username);
        System.out.println("wtf");
        
        double money;
        try {
            money = MatkakorttiApi.getMoney(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            money = -42;
        }

        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.homescreen_widget);
            remoteViews.setTextViewText(R.id.homescreen_text, money + "");

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
