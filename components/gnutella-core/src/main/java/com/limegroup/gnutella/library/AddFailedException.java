package com.limegroup.gnutella.library;

public class AddFailedException extends Exception {
    
    private final FileListChangedEvent.Type type;

    public AddFailedException(FileListChangedEvent.Type type) {
        super("Add Failed, type: " + type);
        this.type = type;
    }
    
    public FileListChangedEvent.Type getType() {
        return type;
    }
    
}
