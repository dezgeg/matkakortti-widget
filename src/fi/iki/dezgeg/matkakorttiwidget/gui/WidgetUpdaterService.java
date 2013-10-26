package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            updateWidgets(getApplicationContext(), new ArrayList<Card>(), e.getMessage());
        }
    }

    public static void updateWidgets(Context context, List<Card> cards) {
        updateWidgets(context, cards, null);
    }

    // 3rd parameter is total hack.
    public static void updateWidgets(Context context, List<Card> cards, String error) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, HomescreenWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widget);

        for (int widgetId : appWidgetIds) {
            String cardId = prefs.getString("cardSelected_" + widgetId, "");

            Card card = null;
            // FIXME use a map
            for (Card c : cards) {
                if (c.getId().equals(cardId)) {
                    card = c;
                    break;
                }
            }
            if (card == null) {
                String e = error != null ? error: "Card doesn't exist";
                setWidgetError(context, appWidgetManager, widgetId, e);
                continue;
            }
            String periodEnd = "---";
            if (card.getPeriodExpiryDate() != null)
                periodEnd = new SimpleDateFormat("dd.MM.").format(card.getPeriodExpiryDate()) + "";
            setWidgetText(context, appWidgetManager, widgetId, card.getName(), card.getMoney() + "", periodEnd);
        }
    }

    private static void setWidgetText(Context context, AppWidgetManager appWidgetManager, int widgetId,
                                      String name, String money, String period) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.homescreen_widget);
        remoteViews.setTextViewText(R.id.homescreen_name, name);
        remoteViews.setTextViewText(R.id.homescreen_money_text, money);
        remoteViews.setTextViewText(R.id.homescreen_period_text, period);

        finishWidgetUpdate(context, appWidgetManager, widgetId, remoteViews, R.id.homescreen_layout);
    }

    private static void setWidgetError(Context context, AppWidgetManager appWidgetManager, int widgetId,
                                       String error) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.homescreen_widget_warning);
        remoteViews.setTextViewText(R.id.homescreen_warning_text, error);

        finishWidgetUpdate(context, appWidgetManager, widgetId, remoteViews, R.id.homescreen_warning_layout);
    }

    private static void finishWidgetUpdate(Context context, AppWidgetManager appWidgetManager,
                                           int widgetId, RemoteViews remoteViews, int rootWidgetId) {
        Intent reconfigureIntent = new Intent(context, MainMenuActivity.class);

        reconfigureIntent.setAction("fi.iki.dezgeg.matkakorttiwidget.APPWIDGET_CONFIGURE");
        reconfigureIntent.putExtra("EXTRA_APPWIDGET_ID", widgetId);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, reconfigureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_CANCEL_CURRENT);
        remoteViews.setOnClickPendingIntent(rootWidgetId, pendingIntent);

        appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

}
