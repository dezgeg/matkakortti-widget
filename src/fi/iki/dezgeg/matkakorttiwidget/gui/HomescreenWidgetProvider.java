package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;

import fi.iki.dezgeg.matkakorttiwidget.R;

public class HomescreenWidgetProvider extends AppWidgetProvider
{

    @Override
    public void onDisabled(Context context)
    {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);
    }

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
        System.out.println("onUpdate");

        Intent updateIntent = new Intent(context, WidgetUpdaterService.class);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        // Update the widgets via the service
        context.startService(updateIntent);
    }

}
