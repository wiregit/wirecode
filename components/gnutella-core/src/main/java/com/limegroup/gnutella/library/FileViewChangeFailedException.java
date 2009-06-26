package com.limegroup.gnutella.library;

/** An event that's triggered when adding to a file view failed for some reason. */
public class FileViewChangeFailedException extends Exception {
    
    private final FileViewChangeEvent event;
    private final Reason reason;
    
    public static enum Reason {
        ERROR_LOADING_URNS,
        CANT_CANONICALIZE,
        ALREADY_MANAGED,
        NOT_MANAGEABLE,
        PROGRAMS_NOT_MANAGEABLE,
        CANT_CREATE_FD,
        OLD_WASNT_MANAGED,
        CANT_ADD_TO_LIST,
        DANGEROUS_FILE
    }

    /** Constructs the event with a particular reason. */
    public FileViewChangeFailedException(FileViewChangeEvent event, Reason reason) {
        super("Event: " + event + ", Reason: " + reason);
        this.event = event;
        this.reason = reason;
    }
    
    /** Constructs the event with a reason & a Throwable as a cause. */
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
