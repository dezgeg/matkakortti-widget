package fi.iki.dezgeg.matkakorttiwidget;

import java.util.*;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.*;

public class HomescreenWidgetProvider extends AppWidgetProvider
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);
        System.out.println(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.homescreen_widget);
            remoteViews.setTextViewText(R.id.homescreen_text, new Date() + "");

            Intent intent = new Intent(context, HomescreenWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.homescreen_text, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

}
