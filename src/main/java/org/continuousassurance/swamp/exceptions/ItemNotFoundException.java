package org.continuousassurance.swamp.exceptions;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 11/26/14 at  11:36 AM
 */
public class ItemNotFoundException extends SWAMPException {
    public ItemNotFoundException() {
    }

    public ItemNotFoundException(String message) {
        super(message);
    }

    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemNotFoundException(Throwable cause) {
        super(cause);
    }

    public ItemNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
