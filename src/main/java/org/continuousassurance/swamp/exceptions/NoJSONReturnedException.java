package org.continuousassurance.swamp.exceptions;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 9/1/15 at  3:09 PM
 */
public class NoJSONReturnedException extends SWAMPException {
    public NoJSONReturnedException() {
    }

    public NoJSONReturnedException(Throwable cause) {
        super(cause);
    }

    public NoJSONReturnedException(String message) {
        super(message);
    }

    public NoJSONReturnedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoJSONReturnedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
