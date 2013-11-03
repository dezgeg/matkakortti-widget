/*
 * Matkakortti Widget is licensed under GPLv2.
 * See LICENSE.txt for more information.
 */

package fi.iki.dezgeg.matkakorttiwidget.matkakortti;

public class MatkakorttiException extends RuntimeException {
    private boolean isInternalError;

    public boolean isInternalError() {
        return isInternalError;
    }

    public MatkakorttiException(String detailMessage, boolean isInternalError) {
        this(detailMessage, null, isInternalError);
    }

    public MatkakorttiException(String detailMessage, Throwable throwable, boolean isInternalError) {
        super(detailMessage, throwable);
        this.isInternalError = isInternalError;
    }
}
