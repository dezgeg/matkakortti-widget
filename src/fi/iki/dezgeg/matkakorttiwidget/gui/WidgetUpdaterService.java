package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.List;

import fi.iki.dezgeg.matkakorttiwidget.R;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.Card;

public class WidgetUpdaterService extends IntentService
{
    public WidgetUpdaterService()
    {
        super("MatkakorttiWidgetUpdaterService");
    }
    @Override
    public void onHandleIntent(Intent source)
    {
        System.out.println("onHandleIntent: " + source.toString());

        try {
            updateWidgets(getApplicationContext(), MatkakorttiWidgetApp.getCardList());
        } catch (Exception e) {
            setWidgetText(getApplicationContext(), e.getMessage(), true);
        }
    }

    public static void updateWidgets(Context context, List<Card> cards) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String message = "";
        boolean anyCards = false;
        for (Card card : cards) {
            if (!prefs.getBoolean("cardSelected_" + card.getId(), false))
                continue;

            message = card.getMoney() + "";
            if (card.getPeriodExpiryDate() != null)
                message += "\n" + new SimpleDateFormat("dd.MM.").format(card.getPeriodExpiryDate());
            anyCards = true;
        }

        if (!anyCards)
            setWidgetText(context, "No cards selected!", true);
        else
            setWidgetText(context, message, false);
    }

    private static void setWidgetText(Context context, String message, boolean isError) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, HomescreenWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widget);


        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.homescreen_widget);
            remoteViews.setTextViewText(R.id.homescreen_text, message);
            if (isError) {
                remoteViews.setTextColor(R.id.homescreen_text, Color.RED);
                remoteViews.setTextViewTextSize(R.id.homescreen_text, TypedValue.COMPLEX_UNIT_PT, 5.0f);
            } else {
                remoteViews.setTextColor(R.id.homescreen_text, Color.WHITE);
                remoteViews.setTextViewTextSize(R.id.homescreen_text, TypedValue.COMPLEX_UNIT_PT, 12.0f);
            }

            Intent mainMenuIntent = new Intent(context, MainMenuActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainMenuIntent, 0);

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
