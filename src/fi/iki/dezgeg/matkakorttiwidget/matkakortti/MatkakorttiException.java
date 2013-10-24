package fi.iki.dezgeg.matkakorttiwidget.matkakortti;

public class MatkakorttiException extends RuntimeException {
    public MatkakorttiException(Throwable throwable) {
        super(throwable);
    }

    public MatkakorttiException() {
        super();
    }

    public MatkakorttiException(String detailMessage) {
        super(detailMessage);
    }

    public MatkakorttiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
