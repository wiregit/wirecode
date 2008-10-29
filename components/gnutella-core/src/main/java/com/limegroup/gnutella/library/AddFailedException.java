package com.limegroup.gnutella.library;

public class AddFailedException extends Exception {
    
    private final FileListChangedEvent.Type type;

    public AddFailedException(FileListChangedEvent.Type type, String reason) {
        super("Add Failed, type: " + type + ", reason: " + reason);
        this.type = type;
    }
    
    public FileListChangedEvent.Type getType() {
        return type;
    }
    
}
