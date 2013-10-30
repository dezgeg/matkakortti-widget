package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.content.SharedPreferences;

import org.apache.http.conn.HttpHostConnectException;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

public class Utils {
    public static boolean isConnectionProblemRelatedException(Throwable e) {
        System.out.println(e);
        if (e instanceof UnknownHostException)
            return true;
        if (e instanceof SocketException)
            return true;

        if (e.getCause() != null)
            return isConnectionProblemRelatedException(e.getCause());

        return false;
    }

    public static String prefKeyForWidgetId(int appWidgetId, String key) {
        return "widget_" + key + "_" + appWidgetId;
    }

    public static void dumpPrefs(SharedPreferences prefs) {
        System.out.println("Preferences:");
        for (Map.Entry<String, ?> pair : prefs.getAll().entrySet()) {
            System.out.println("    " + pair.getKey() + " -> " + pair.getValue());
        }
        System.out.println();
    }

    public static void reportException(String where, Throwable e) {
        e.printStackTrace();
    }
}
