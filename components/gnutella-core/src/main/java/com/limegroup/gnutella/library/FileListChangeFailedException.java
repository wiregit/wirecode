package com.limegroup.gnutella.library;

import org.limewire.inspection.InspectionHistogram;

public class FileListChangeFailedException extends Exception {
    
    private final FileListChangedEvent event;
    private final Reason reason;
    
    private static final InspectionHistogram<Reason> reasons = new InspectionHistogram<Reason>(); 
    
    public static enum Reason {
        ERROR_LOADING_URNS,
        CANT_CANONICALIZE,
        ALREADY_MANAGED,
        NOT_MANAGEABLE,
        PROGRAMS_NOT_MANAGEABLE,
        REVISIONS_CHANGED,
        CANT_CREATE_FD,
        OLD_WASNT_MANAGED,
        CANT_ADD_TO_LIST
    }

    public FileListChangeFailedException(FileListChangedEvent event, Reason reason) {
        super("Event: " + event + ", Reason: " + reason);
        this.event = event;
        this.reason = reason;
        long count = reasons.count(reason);
        if (count % 10 == 0) {
            System.out.println(reasons);
        }
    }
    
    public FileListChangeFailedException(FileListChangedEvent event, Reason reason, Throwable cause) {
        super("Event: " + event + ", Reason: " + reason, cause);
        this.event = event;
        this.reason = reason;
    }
    
    
    public FileListChangedEvent getEvent() {
        return event;
    }
    
    public Reason getReason() {
        return reason;
    }
    
}
