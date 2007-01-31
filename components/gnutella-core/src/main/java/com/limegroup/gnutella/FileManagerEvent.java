package com.limegroup.gnutella;

import java.io.File;
import java.util.EventObject;

/**
 * This class implements a FileManagerEvent which is
 * used by FileManager and MetaFileManager to notify
 * the front end about add, remove, rename and change
 * events in the Library.
 */
@SuppressWarnings("serial")
public class FileManagerEvent extends EventObject {
    
    public static enum Type {
        ADD,
        REMOVE,
        RENAME,
        CHANGE,
        FAILED,
        ALREADY_SHARED,
        ADD_FOLDER,
        REMOVE_FOLDER;
    }
    
    private final Type kind;
    private final FileDesc[] fds;
    private final File[] files;

    /**
     * Constructs a FileManagerEvent
     */
    public FileManagerEvent(FileManager manager, Type kind, FileDesc... fds) {
        super(manager);
        this.kind = kind;
        this.fds = fds;
        this.files = null;
    }
    
    /**
     * Constructs a FileManagerEvent with a bunch of files.
     */
    public FileManagerEvent(FileManager manager, Type kind, File... files) {
        super(manager);
        this.kind = kind;
        this.files = files;
        this.fds = null;
    }
    
    public Type getKind() {
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
        return (kind.equals(Type.ADD));
    }
    
    /**
     * Returns true if this event is a REMOVE event
     */
    public boolean isRemoveEvent() {
        return (kind.equals(Type.REMOVE));
    }
    
    /**
     * Returns true if this event is a RENAME (MOVE) 
     * event
     */
    public boolean isRenameEvent() {
        return (kind.equals(Type.RENAME));
    }
    
    /**
     * Returns true if this event is a CHANGE (i.e.
     * when ID3 Tags changed) event.
     */
    public boolean isChangeEvent() {
        return (kind.equals(Type.CHANGE));
    }
    
    /**
     * Returns true if this is a FAILED add event (ie, addFile failed).
     */
    public boolean isFailedEvent() {
        return (kind.equals(Type.FAILED));
    }

    /**
     * Returns true if this is an event for a file that was ALREADY_SHARED
     * (ie, an addFile event was ignored because the file was already shared)
     */
    public boolean isAlreadySharedEvent() {
        return (kind.equals(Type.ALREADY_SHARED));
    }
    
    /**
     * Returns true if this is a ADD_FOLDER event.
     */
    public boolean isAddFolderEvent() {
        return kind.equals(Type.ADD_FOLDER);
    }
    
    /**
     * Returns true if this is a REMOVE_FOLDER event;
     */
    public boolean isRemoveFolderEvent() {
        return kind.equals(Type.REMOVE_FOLDER);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder("FileManagerEvent: [event=").append(kind);
        
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
