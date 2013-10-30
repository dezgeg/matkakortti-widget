package fi.iki.dezgeg.matkakorttiwidget.gui;

import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private static Date lastErrorReport;
    private static long MIN_REPORT_INTERVAL_MS = 12 * 3600 * 1000;
    private static final String ERROR_REPORT_URL = "https://tmtynkky.users.cs.helsinki.fi/error.php";

    public static void reportException(final String where, final Throwable e) {
        e.printStackTrace();
        if (lastErrorReport != null
                && lastErrorReport.getTime() - new Date().getTime() <= MIN_REPORT_INTERVAL_MS)
            return;
        lastErrorReport = new Date();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);

                    List<NameValuePair> data = new ArrayList<NameValuePair>();
                    data.add(new BasicNameValuePair("where", where));
                    data.add(new BasicNameValuePair("backtrace", sw.toString()));

                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost post = new HttpPost(ERROR_REPORT_URL);
                    try {
                        post.setEntity(new UrlEncodedFormEntity(data));
                    } catch (UnsupportedEncodingException uee) {
                    }
                    try {
                        httpclient.execute(post);
                        System.out.println("Error reported.");
                    } catch (IOException e1) {
                    }
                } catch (Throwable t) {
                    // ignore
                }
                return null;
            }
        }.execute();
    }
}
