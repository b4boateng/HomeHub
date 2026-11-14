package com.homehub.exceptions;

// Names the file and line of the bad row.
public class DataLoadException extends HomeHubException {

    public DataLoadException(String file, int line, String problem) {
        super(file + " line " + line + ": " + problem);
    }
}
