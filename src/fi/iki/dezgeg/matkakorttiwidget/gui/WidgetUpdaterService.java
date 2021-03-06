/*
 * Matkakortti Widget is licensed under GPLv2.
 * See LICENSE.txt for more information.
 */

package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import fi.iki.dezgeg.matkakorttiwidget.R;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.Card;
import fi.iki.dezgeg.matkakorttiwidget.matkakortti.MatkakorttiException;

public class WidgetUpdaterService extends IntentService {
    private boolean initialUpdate = true;
    private boolean validDataOnWidgets = false;
    private static final BigDecimal HUNDRED = new BigDecimal(100);
    public static final DecimalFormat FORMAT_TWO_DIGITS_AFTER_POINT = new DecimalFormat();

    static {
        FORMAT_TWO_DIGITS_AFTER_POINT.setMinimumFractionDigits(2);
        FORMAT_TWO_DIGITS_AFTER_POINT.setMaximumFractionDigits(2);
        FORMAT_TWO_DIGITS_AFTER_POINT.setGroupingUsed(false);
    }

    public WidgetUpdaterService() {
        super("MatkakorttiWidgetUpdaterService");
    }

    @Override
    public void onHandleIntent(Intent source) {
        if (initialUpdate) {
            initialUpdate = false;
            updateWidgets(getApplicationContext(), new ArrayList<Card>(),
                    getResources().getText(R.string.widget_loading));
        }

        Throwable err;
        boolean internalError = true;
        try {
            updateWidgets(getApplicationContext(), MatkakorttiWidgetApp.getCardList());
            validDataOnWidgets = true;
            return;
        } catch (MatkakorttiException me) {
            err = me;
            internalError = me.isInternalError();
        } catch (Exception e) {
            err = e;
        }
        if (Utils.isConnectionProblemRelatedException(err)) {
            if (!validDataOnWidgets)
                updateWidgets(getApplicationContext(), new ArrayList<Card>(),
                        getResources().getText(R.string.widget_connection_error));
            return;
        }
        if (internalError)
            MatkakorttiWidgetApp.reportException("WidgetUpdaterService", err);

        updateWidgets(getApplicationContext(), new ArrayList<Card>(),
                getResources().getText(R.string.widget_other_error));
    }

    public static void updateWidgets(Context context, List<Card> cards) {
        updateWidgets(context, cards, null);
    }

    // 3rd parameter is total hack.
    public static void updateWidgets(Context context, List<Card> cards, CharSequence error) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, HomescreenWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widget);

        for (int widgetId : appWidgetIds) {
            String cardId = prefs.getString(Utils.prefKeyForWidgetId(widgetId, "cardSelected"), "");

            Card card = null;
            // FIXME use a map
            for (Card c : cards) {
                if (c.getId().equals(cardId)) {
                    card = c;
                    break;
                }
            }
            if (card == null) {
                CharSequence e = error != null ? error :
                        context.getResources().getText(R.string.widget_other_error);
                setWidgetError(context, appWidgetManager, widgetId, e);
                continue;
            }

            setWidgetText(context, appWidgetManager, widgetId, card);
        }
    }

    private static boolean getBoolPref(SharedPreferences prefs, int widgetId, String key, boolean def) {
        return prefs.getBoolean(Utils.prefKeyForWidgetId(widgetId, key), def);
    }

    private static void setWidgetText(Context context, AppWidgetManager appWidgetManager, int widgetId,
                                      Card c) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.homescreen_widget);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean showName = getBoolPref(prefs, widgetId, "showName", true);
        setTextOrHide(remoteViews, R.id.homescreen_name, R.id.homescreen_name,
                c.getName(), showName);

        // Show period if: autoHidePeriod not set OR the user has period
        // Show money  if: period NOT show OR the money is non-zero OR
        boolean showPeriod = !getBoolPref(prefs, widgetId, "autoHidePeriod", false) || c.getPeriodExpiryDate() != null;

        String money;
        if (c.getMoney().compareTo(HUNDRED) > 0)
            money = c.getMoney().intValue() + "";
        else
            money = FORMAT_TWO_DIGITS_AFTER_POINT.format(c.getMoney());

        setTextOrHide(remoteViews, R.id.homescreen_money_container, R.id.homescreen_money_text,
                money, true);

        String periodEnd = "- - -";
        if (c.getPeriodExpiryDate() != null) {
            remoteViews.setTextColor(R.id.homescreen_period_text, Color.WHITE);
            periodEnd = new SimpleDateFormat("dd.MM.").format(c.getPeriodExpiryDate()) + "";
        } else {
            remoteViews.setTextColor(R.id.homescreen_period_text, Color.GRAY);
        }
        setTextOrHide(remoteViews, R.id.homescreen_period_container, R.id.homescreen_period_text,
                periodEnd, showPeriod);

        finishWidgetUpdate(context, appWidgetManager, widgetId, remoteViews, R.id.homescreen_layout);
    }

    private static void setTextOrHide(RemoteViews remoteViews, int containerId, int textId, String text, boolean show) {
        if (show) {
            remoteViews.setViewVisibility(containerId, View.VISIBLE);
            remoteViews.setTextViewText(textId, text);
        } else {
            remoteViews.setViewVisibility(containerId, View.GONE);
        }
    }

    private static void setWidgetError(Context context, AppWidgetManager appWidgetManager, int widgetId,
                                       CharSequence error) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.homescreen_widget_warning);
        remoteViews.setTextViewText(R.id.homescreen_warning_text, error);

        finishWidgetUpdate(context, appWidgetManager, widgetId, remoteViews, R.id.homescreen_warning_layout);
    }

    private static void finishWidgetUpdate(Context context, AppWidgetManager appWidgetManager,
                                           int widgetId, RemoteViews remoteViews, int rootWidgetId) {
        Intent reconfigureIntent = new Intent(context, SettingsActivity.class);

        reconfigureIntent.setAction("fi.iki.dezgeg.matkakorttiwidget.APPWIDGET_CONFIGURE");
        reconfigureIntent.putExtra("EXTRA_APPWIDGET_ID", widgetId);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, reconfigureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_CANCEL_CURRENT);
        remoteViews.setOnClickPendingIntent(rootWidgetId, pendingIntent);

        appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }
}
