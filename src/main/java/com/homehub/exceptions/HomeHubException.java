package com.homehub.exceptions;

// Common parent for our exceptions, unchecked so callers are not forced to catch.
public class HomeHubException extends RuntimeException {

    public HomeHubException(String message) {
        super(message);
    }
}
