package com.jeffmony.async.http;

public class RedirectLimitExceededException extends Exception {
    public RedirectLimitExceededException(String message) {
        super(message);
    }
}
