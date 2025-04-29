package com.tradeplatform.common.exception;

/**
 * Base exception class for all exceptions in the Trade Platform.
 */
public class TradePlatformException extends RuntimeException {

    /**
     * Error code associated with this exception.
     */
    private final String errorCode;

    /**
     * Constructs a new TradePlatformException with the specified detail message.
     *
     * @param message the detail message
     */
    public TradePlatformException(String message) {
        super(message);
        this.errorCode = "TRADE_PLATFORM_ERROR";
    }

    /**
     * Constructs a new TradePlatformException with the specified detail message and error code.
     *
     * @param message   the detail message
     * @param errorCode the error code
     */
    public TradePlatformException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new TradePlatformException with the specified detail message, cause, and error code.
     *
     * @param message   the detail message
     * @param cause     the cause
     * @param errorCode the error code
     */
    public TradePlatformException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new TradePlatformException with the specified cause and error code.
     *
     * @param cause     the cause
     * @param errorCode the error code
     */
    public TradePlatformException(Throwable cause, String errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}