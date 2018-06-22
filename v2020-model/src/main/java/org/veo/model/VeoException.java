package org.veo.model;
/**
 * This exception is thrown if an element to be created already exists.
 */
public class VeoException extends RuntimeException {

    public static final String ELEMENT_NOT_EXISTS = "Element with uuid %s does not exists.";

    public enum Error {ELEMENT_NOT_FOUND, ELEMENT_EXISTS, PARSE_EXCEPTION}

    private Error error;

    public VeoException(Error error, String message) {
        super(message);
        this.error = error;
    }

    public VeoException(Error error, String message, Throwable cause) {
        super(message,cause);
        this.error = error;
    }

    public Error getError() {
        return error;
    }
}