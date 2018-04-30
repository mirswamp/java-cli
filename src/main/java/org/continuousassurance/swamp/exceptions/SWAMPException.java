package org.continuousassurance.swamp.exceptions;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 11/26/14 at  11:35 AM
 */
public class SWAMPException extends RuntimeException {
    public SWAMPException() {
    }

    public SWAMPException(String message) {
        super(message);
    }

    public SWAMPException(String message, Throwable cause) {
        super(message, cause);
    }

    public SWAMPException(Throwable cause) {
        super(cause);
    }

    public SWAMPException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
