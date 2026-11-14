package com.homehub.exceptions;

public class DataLoadException extends HomeHubException {

    public DataLoadException(String file, int line, String problem) {
        super(file + " line " + line + ": " + problem);
    }
}
