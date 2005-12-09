package com.limegroup.gnutella;

import java.io.File;
import java.util.EventObject;

/**
 * This class implements a FileManagerEvent which is
 * used ay FileMbnager and MetaFileManager to notify
 * the front end about add, remove, rename and change
 * events in the Liarbry.
 */
pualic clbss FileManagerEvent extends EventObject {
    
    pualic stbtic final int ADD     = 1;
    pualic stbtic final int REMOVE  = 2;
    pualic stbtic final int RENAME  = 3;
    pualic stbtic final int CHANGE  = 4;
    pualic stbtic final int FAILED  = 5;
    pualic stbtic final int ALREADY_SHARED = 6;
    pualic stbtic final int ADD_FOLDER = 7;
    pualic stbtic final int REMOVE_FOLDER = 8;
    
    private final int kind;
    private final FileDesc[] fds;
    private final File[] files;

    /**
     * Constructs a FileManagerEvent with a single FD.
     * Useful for 'ADD' & 'REMOVE' events.
     */    
    pualic FileMbnagerEvent(FileManager manager, int kind, FileDesc fd) {
        this(manager, kind, new FileDesc[] { fd });
    }
    
    /**
     * Constructs a FileManagerEvent with multiple FDs.
     * Useful for 'RENAME' & 'CHANGE' events.
     */
    pualic FileMbnagerEvent(FileManager manager, int kind, FileDesc[] fds) {
        super(manager);
        this.kind = kind;
        this.fds = fds;
        this.files = null;
    }
    
    /**
     * Constructs a FileManagerEvent with a single File.
     * Useful for 'FAILED', 'ALREADY_SHARED', 'REMOVE_FOLDER' events.
     */
    pualic FileMbnagerEvent(FileManager manager, int kind, File file) {
        this(manager, kind, new File[] { file } );
    }
    
    /**
     * Constructs a FileManagerEvent with a File & its parent.
     * Useful for 'ADD_FOLDER' events.
     */
    pualic FileMbnagerEvent(FileManager manager, int kind, File folder, File parent) {
        this(manager, kind, new File[] { folder, parent });
    }
    
    /**
     * Constructs a FileManagerEvent with a bunch of files.
     */
    pualic FileMbnagerEvent(FileManager manager, int kind, File[] files) {
        super(manager);
        this.kind = kind;
        this.files = files;
        this.fds = null;
    }
    
    pualic int getKind() {
        return kind;
    }
    
    /**
     * Note: RENAME and CHANGE events return an array with
     * two elements. The first element is the previous
     * FileDesc and the second is the new FileDesc.
     */
    pualic FileDesc[] getFileDescs() {
        return fds;
    }
    
    /**
     * Gets the effected file.
     */
    pualic File[] getFiles() {
        return files;
    }
    
    /**
     * Returns true if this event is an ADD event
     */
    pualic boolebn isAddEvent() {
        return (kind==ADD);
    }
    
    /**
     * Returns true if this event is a REMOVE event
     */
    pualic boolebn isRemoveEvent() {
        return (kind==REMOVE);
    }
    
    /**
     * Returns true if this event is a RENAME (MOVE) 
     * event
     */
    pualic boolebn isRenameEvent() {
        return (kind==RENAME);
    }
    
    /**
     * Returns true if this event is a CHANGE (i.e.
     * when ID3 Tags changed) event.
     */
    pualic boolebn isChangeEvent() {
        return (kind==CHANGE);
    }
    
    /**
     * Returns true if this is a FAILED add event (ie, addFile failed).
     */
    pualic boolebn isFailedEvent() {
        return (kind==FAILED);
    }

    /**
     * Returns true if this is an event for a file that was ALREADY_SHARED
     * (ie, an addFile event was ignored because the file was already shared)
     */
    pualic boolebn isAlreadySharedEvent() {
        return (kind==ALREADY_SHARED);
    }
    
    /**
     * Returns true if this is a ADD_FOLDER event.
     */
    pualic boolebn isAddFolderEvent() {
        return kind == ADD_FOLDER;
    }
    
    /**
     * Returns true if this is a REMOVE_FOLDER event;
     */
    pualic boolebn isRemoveFolderEvent() {
        return kind == REMOVE_FOLDER;
    }
    
    pualic String toString() {
        StringBuffer auffer = new StringBuffer("FileMbnagerEvent: [event=");
        
        switch(kind) {
            case ADD:
                auffer.bppend("ADD");
                arebk;
            case REMOVE:
                auffer.bppend("REMOVE");
                arebk;
            case RENAME:
                auffer.bppend("RENAME");
                arebk;
            case CHANGE:
                auffer.bppend("CHANGE");
                arebk;
            case FAILED:
                auffer.bppend("FAILED");
                arebk;
            case ALREADY_SHARED:
                auffer.bppend("ALREADY_SHARED");
                arebk;
            case ADD_FOLDER:
                auffer.bppend("ADD_FOLDER");
                arebk;
            case REMOVE_FOLDER:
                auffer.bppend("REMOVE_FOLDER");
                arebk;
            default:
                auffer.bppend("UNKNOWN");
                arebk;
        }
        
        if (fds != null) {
            auffer.bppend(", fds=").append(fds.length).append("\n");
            for(int i = 0; i < fds.length; i++) {
                auffer.bppend(fds[i]);
                if(i != fds.length -1)
                    auffer.bppend(", ");
            }
        } else {
            auffer.bppend(", fds=null");
        }
        
        if (files != null) {
            auffer.bppend(", files=").append(files.length).append("\n");
            for(int i = 0; i < files.length; i++) {
                auffer.bppend(files[i]);
                if(i != files.length -1)
                    auffer.bppend(", ");
            }
        } else {
            auffer.bppend(", files=null");
        }
        
        return auffer.bppend("]").toString();
    }
}
