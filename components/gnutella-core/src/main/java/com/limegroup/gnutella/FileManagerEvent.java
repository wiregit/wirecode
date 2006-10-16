package com.limegroup.gnutella;

import java.io.File;
import java.util.EventObject;

/**
 * This class implements a FileManagerEvent which is
 * used by FileManager and MetaFileManager to notify
 * the front end about add, remove, rename and change
 * events in the Library.
 */
public class FileManagerEvent extends EventObject {
    
    public static final int ADD     = 1;
    public static final int REMOVE  = 2;
    public static final int RENAME  = 3;
    public static final int CHANGE  = 4;
    public static final int FAILED  = 5;
    public static final int ALREADY_SHARED = 6;
    public static final int ADD_FOLDER = 7;
    public static final int REMOVE_FOLDER = 8;
    
    private final int kind;
    private final FileDesc[] fds;
    private final File[] files;

    /**
     * Constructs a FileManagerEvent with a single FD.
     * Useful for 'ADD' & 'REMOVE' events.
     */    
    public FileManagerEvent(FileManager manager, int kind, FileDesc fd) {
        this(manager, kind, new FileDesc[] { fd });
    }
    
    /**
     * Constructs a FileManagerEvent with multiple FDs.
     * Useful for 'RENAME' & 'CHANGE' events.
     */
    public FileManagerEvent(FileManager manager, int kind, FileDesc[] fds) {
        super(manager);
        this.kind = kind;
        this.fds = fds;
        this.files = null;
    }
    
    /**
     * Constructs a FileManagerEvent with a single File.
     * Useful for 'FAILED', 'ALREADY_SHARED', 'REMOVE_FOLDER' events.
     */
    public FileManagerEvent(FileManager manager, int kind, File file) {
        this(manager, kind, new File[] { file } );
    }
    
    /**
     * Constructs a FileManagerEvent with a File & its parent.
     * Useful for 'ADD_FOLDER' events.
     */
    public FileManagerEvent(FileManager manager, int kind, File folder, File parent) {
        this(manager, kind, new File[] { folder, parent });
    }
    
    /**
     * Constructs a FileManagerEvent with a bunch of files.
     */
    public FileManagerEvent(FileManager manager, int kind, File[] files) {
        super(manager);
        this.kind = kind;
        this.files = files;
        this.fds = null;
    }
    
    public int getKind() {
        return kind;
    }
    
    /**
     * Note: RENAME and CHANGE events return an array with
     * two elements. The first element is the previous
     * FileDesc and the second is the new FileDesc.
     */
    public FileDesc[] getFileDescs() {
        return fds;
    }
    
    /**
     * Gets the effected file.
     */
    public File[] getFiles() {
        return files;
    }
    
    /**
     * Returns true if this event is an ADD event
     */
    public boolean isAddEvent() {
        return (kind==ADD);
    }
    
    /**
     * Returns true if this event is a REMOVE event
     */
    public boolean isRemoveEvent() {
        return (kind==REMOVE);
    }
    
    /**
     * Returns true if this event is a RENAME (MOVE) 
     * event
     */
    public boolean isRenameEvent() {
        return (kind==RENAME);
    }
    
    /**
     * Returns true if this event is a CHANGE (i.e.
     * when ID3 Tags changed) event.
     */
    public boolean isChangeEvent() {
        return (kind==CHANGE);
    }
    
    /**
     * Returns true if this is a FAILED add event (ie, addFile failed).
     */
    public boolean isFailedEvent() {
        return (kind==FAILED);
    }

    /**
     * Returns true if this is an event for a file that was ALREADY_SHARED
     * (ie, an addFile event was ignored because the file was already shared)
     */
    public boolean isAlreadySharedEvent() {
        return (kind==ALREADY_SHARED);
    }
    
    /**
     * Returns true if this is a ADD_FOLDER event.
     */
    public boolean isAddFolderEvent() {
        return kind == ADD_FOLDER;
    }
    
    /**
     * Returns true if this is a REMOVE_FOLDER event;
     */
    public boolean isRemoveFolderEvent() {
        return kind == REMOVE_FOLDER;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder("FileManagerEvent: [event=");
        
        switch(kind) {
            case ADD:
                buffer.append("ADD");
                break;
            case REMOVE:
                buffer.append("REMOVE");
                break;
            case RENAME:
                buffer.append("RENAME");
                break;
            case CHANGE:
                buffer.append("CHANGE");
                break;
            case FAILED:
                buffer.append("FAILED");
                break;
            case ALREADY_SHARED:
                buffer.append("ALREADY_SHARED");
                break;
            case ADD_FOLDER:
                buffer.append("ADD_FOLDER");
                break;
            case REMOVE_FOLDER:
                buffer.append("REMOVE_FOLDER");
                break;
            default:
                buffer.append("UNKNOWN");
                break;
        }
        
        if (fds != null) {
            buffer.append(", fds=").append(fds.length).append("\n");
            for(int i = 0; i < fds.length; i++) {
                buffer.append(fds[i]);
                if(i != fds.length -1)
                    buffer.append(", ");
            }
        } else {
            buffer.append(", fds=null");
        }
        
        if (files != null) {
            buffer.append(", files=").append(files.length).append("\n");
            for(int i = 0; i < files.length; i++) {
                buffer.append(files[i]);
                if(i != files.length -1)
                    buffer.append(", ");
            }
        } else {
            buffer.append(", files=null");
        }
        
        return buffer.append("]").toString();
    }
}
