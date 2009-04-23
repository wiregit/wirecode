package com.limegroup.gnutella.library;

public class FileViewChangeFailedException extends Exception {
    
    private final FileViewChangeEvent event;
    private final Reason reason;
    
    public static enum Reason {
        ERROR_LOADING_URNS,
        CANT_CANONICALIZE,
        ALREADY_MANAGED,
        NOT_MANAGEABLE,
        PROGRAMS_NOT_MANAGEABLE,
        REVISIONS_CHANGED,
        CANT_CREATE_FD,
        OLD_WASNT_MANAGED,
        CANT_ADD_TO_LIST,
        DANGEROUS_FILE
    }

    public FileViewChangeFailedException(FileViewChangeEvent event, Reason reason) {
        super("Event: " + event + ", Reason: " + reason);
        this.event = event;
        this.reason = reason;
    }
    
    public FileViewChangeFailedException(FileViewChangeEvent event, Reason reason, Throwable cause) {
        super("Event: " + event + ", Reason: " + reason, cause);
        this.event = event;
        this.reason = reason;
    }
    
    
    public FileViewChangeEvent getEvent() {
        return event;
    }
    
    public Reason getReason() {
        return reason;
    }
    
}
