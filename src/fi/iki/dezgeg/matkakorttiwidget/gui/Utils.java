package fi.iki.dezgeg.matkakorttiwidget.gui;

import java.net.UnknownHostException;

public class Utils {
    public static boolean isConnectionProblemRelatedException(Throwable e) {
        if (e instanceof UnknownHostException)
            return true;

        if (e.getCause() != null)
            return isConnectionProblemRelatedException(e.getCause());

        return false;
    }
}
