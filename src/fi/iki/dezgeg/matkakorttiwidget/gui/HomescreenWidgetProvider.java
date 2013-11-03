/*
 * Matkakortti Widget is licensed under GPLv2.
 * See LICENSE.txt for more information.
 */

package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class HomescreenWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        System.out.println("onUpdate");

        Intent updateIntent = new Intent(context, WidgetUpdaterService.class);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        // Update the widgets via the service
        context.startService(updateIntent);
    }

}
