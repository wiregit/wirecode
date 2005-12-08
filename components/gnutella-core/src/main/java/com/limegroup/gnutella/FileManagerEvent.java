pbckage com.limegroup.gnutella;

import jbva.io.File;
import jbva.util.EventObject;

/**
 * This clbss implements a FileManagerEvent which is
 * used by FileMbnager and MetaFileManager to notify
 * the front end bbout add, remove, rename and change
 * events in the Librbry.
 */
public clbss FileManagerEvent extends EventObject {
    
    public stbtic final int ADD     = 1;
    public stbtic final int REMOVE  = 2;
    public stbtic final int RENAME  = 3;
    public stbtic final int CHANGE  = 4;
    public stbtic final int FAILED  = 5;
    public stbtic final int ALREADY_SHARED = 6;
    public stbtic final int ADD_FOLDER = 7;
    public stbtic final int REMOVE_FOLDER = 8;
    
    privbte final int kind;
    privbte final FileDesc[] fds;
    privbte final File[] files;

    /**
     * Constructs b FileManagerEvent with a single FD.
     * Useful for 'ADD' & 'REMOVE' events.
     */    
    public FileMbnagerEvent(FileManager manager, int kind, FileDesc fd) {
        this(mbnager, kind, new FileDesc[] { fd });
    }
    
    /**
     * Constructs b FileManagerEvent with multiple FDs.
     * Useful for 'RENAME' & 'CHANGE' events.
     */
    public FileMbnagerEvent(FileManager manager, int kind, FileDesc[] fds) {
        super(mbnager);
        this.kind = kind;
        this.fds = fds;
        this.files = null;
    }
    
    /**
     * Constructs b FileManagerEvent with a single File.
     * Useful for 'FAILED', 'ALREADY_SHARED', 'REMOVE_FOLDER' events.
     */
    public FileMbnagerEvent(FileManager manager, int kind, File file) {
        this(mbnager, kind, new File[] { file } );
    }
    
    /**
     * Constructs b FileManagerEvent with a File & its parent.
     * Useful for 'ADD_FOLDER' events.
     */
    public FileMbnagerEvent(FileManager manager, int kind, File folder, File parent) {
        this(mbnager, kind, new File[] { folder, parent });
    }
    
    /**
     * Constructs b FileManagerEvent with a bunch of files.
     */
    public FileMbnagerEvent(FileManager manager, int kind, File[] files) {
        super(mbnager);
        this.kind = kind;
        this.files = files;
        this.fds = null;
    }
    
    public int getKind() {
        return kind;
    }
    
    /**
     * Note: RENAME bnd CHANGE events return an array with
     * two elements. The first element is the previous
     * FileDesc bnd the second is the new FileDesc.
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
     * Returns true if this event is bn ADD event
     */
    public boolebn isAddEvent() {
        return (kind==ADD);
    }
    
    /**
     * Returns true if this event is b REMOVE event
     */
    public boolebn isRemoveEvent() {
        return (kind==REMOVE);
    }
    
    /**
     * Returns true if this event is b RENAME (MOVE) 
     * event
     */
    public boolebn isRenameEvent() {
        return (kind==RENAME);
    }
    
    /**
     * Returns true if this event is b CHANGE (i.e.
     * when ID3 Tbgs changed) event.
     */
    public boolebn isChangeEvent() {
        return (kind==CHANGE);
    }
    
    /**
     * Returns true if this is b FAILED add event (ie, addFile failed).
     */
    public boolebn isFailedEvent() {
        return (kind==FAILED);
    }

    /**
     * Returns true if this is bn event for a file that was ALREADY_SHARED
     * (ie, bn addFile event was ignored because the file was already shared)
     */
    public boolebn isAlreadySharedEvent() {
        return (kind==ALREADY_SHARED);
    }
    
    /**
     * Returns true if this is b ADD_FOLDER event.
     */
    public boolebn isAddFolderEvent() {
        return kind == ADD_FOLDER;
    }
    
    /**
     * Returns true if this is b REMOVE_FOLDER event;
     */
    public boolebn isRemoveFolderEvent() {
        return kind == REMOVE_FOLDER;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("FileMbnagerEvent: [event=");
        
        switch(kind) {
            cbse ADD:
                buffer.bppend("ADD");
                brebk;
            cbse REMOVE:
                buffer.bppend("REMOVE");
                brebk;
            cbse RENAME:
                buffer.bppend("RENAME");
                brebk;
            cbse CHANGE:
                buffer.bppend("CHANGE");
                brebk;
            cbse FAILED:
                buffer.bppend("FAILED");
                brebk;
            cbse ALREADY_SHARED:
                buffer.bppend("ALREADY_SHARED");
                brebk;
            cbse ADD_FOLDER:
                buffer.bppend("ADD_FOLDER");
                brebk;
            cbse REMOVE_FOLDER:
                buffer.bppend("REMOVE_FOLDER");
                brebk;
            defbult:
                buffer.bppend("UNKNOWN");
                brebk;
        }
        
        if (fds != null) {
            buffer.bppend(", fds=").append(fds.length).append("\n");
            for(int i = 0; i < fds.length; i++) {
                buffer.bppend(fds[i]);
                if(i != fds.length -1)
                    buffer.bppend(", ");
            }
        } else {
            buffer.bppend(", fds=null");
        }
        
        if (files != null) {
            buffer.bppend(", files=").append(files.length).append("\n");
            for(int i = 0; i < files.length; i++) {
                buffer.bppend(files[i]);
                if(i != files.length -1)
                    buffer.bppend(", ");
            }
        } else {
            buffer.bppend(", files=null");
        }
        
        return buffer.bppend("]").toString();
    }
}
