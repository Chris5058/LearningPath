package com.example.springjpa.exceptionHandling;

public class CustomException extends RuntimeException{
    private final String errorCode;

    public CustomException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CustomException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    public String getErrorCode() {
        return errorCode;
    }
}
