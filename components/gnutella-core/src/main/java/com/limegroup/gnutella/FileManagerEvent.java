package com.limegroup.gnutella;

import java.io.File;
import java.util.EventObject;
import java.util.List;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * This class implements a FileManagerEvent which is
 * used by FileManager and MetaFileManager to notify
 * the front end about add, remove, rename and change
 * events in the Library.
 */
@SuppressWarnings("serial")
public class FileManagerEvent extends EventObject {
    
    public static enum Type {
        ADD_FILE,
        REMOVE_FILE,
        RENAME_FILE,
        CHANGE_FILE,
        REMOVE_URN,
        REMOVE_FD,
        ADD_FAILED_FILE,
        ALREADY_SHARED_FILE,
        ADD_FOLDER,
        REMOVE_FOLDER,
        LOAD_FILE,
        FILEMANAGER_LOAD_STARTED,
        FILEMANAGER_LOADING,
        FILEMANAGER_LOAD_FINISHING,
        FILEMANAGER_SAVE,
        FILEMANAGER_LOAD_COMPLETE,
        ADD_STORE_FILE,
        ADD_STORE_FAILED_FILE,
        REMOVE_STORE_FILE,
        ADD_STORE_FOLDER,
        REMOVE_STORE_FOLDER;
    }
    
    private final Type type;
    private final FileDesc[] fds;
    private final File[] files;
    private final URN urn;
    
    private final int relativeDepth;
    private final File rootShare;
    private final List<? extends LimeXMLDocument> metaData;

    /**
     * Constructs a FileManagerEvent
     */
    public FileManagerEvent(FileManager manager, Type type) {
        super(manager);
        this.type = type;
        this.fds = null;
        this.files = null;
        this.urn = null;
        this.relativeDepth = -1;
        this.rootShare = null;
        this.metaData = null;
    }
    
    /**
     * Constructs a FileManagerEvent
     */
    public FileManagerEvent(FileManager manager, Type type, FileDesc... fds) {
        super(manager);
        this.type = type;
        this.fds = fds;
        this.relativeDepth = -1;
        this.rootShare = null;
        this.metaData = null;
        this.urn = null;
        
        this.files = new File[fds != null ? fds.length : 0];
        for (int i = 0; fds != null && i < fds.length; i++) {
            files[i] = fds[i].getFile();
        }
    }
    
    /**
     * Constructs a FileManagerEvent with a bunch of files.
     */
    public FileManagerEvent(FileManager manager, Type type, File... files) {
        super(manager);
        this.type = type;
        this.files = files;
        this.fds = null;
        this.urn = null;
        this.relativeDepth = 0;
        this.rootShare = null;
        this.metaData = null;
    }

    public FileManagerEvent(FileManager manager, Type type, File rootShare, int relativeDepth, File... files) {
        super(manager);
        this.type = type;
        this.files = files;
        this.fds = null;
        this.urn = null;
        
        this.relativeDepth = relativeDepth;
        this.rootShare = rootShare;
        this.metaData = null;
    }
    
    public FileManagerEvent(FileManager manager, Type type, List<? extends LimeXMLDocument> md, FileDesc... fds) {
        super(manager);
        this.type = type;
        this.fds = fds;
        this.metaData = md;
        
        this.relativeDepth = -1;
        this.rootShare = null;
        this.urn = null;
        
        this.files = new File[fds != null ? fds.length : 0];
        for (int i = 0; fds != null && i < fds.length; i++) {
            files[i] = fds[i].getFile();
        }
    }
    
    public FileManagerEvent(FileManager manager, Type type, URN urn) {
        super(manager);
        this.type = type;
        this.urn = urn;
        
        this.fds = null;
        this.files = null;
        this.relativeDepth = -1;
        this.rootShare = null;
        this.metaData = null;
    }
    
    /**
     * Returns the type of the Event
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Returns the FileManager which fired the Event
     */
    public FileManager getFileManager() {
        return (FileManager)getSource();
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
     * Gets the URN
     */
    public URN getURN() {
        return urn;
    }

    /** 
     * Gets the the relative depth of the folder from where the sharing was initiated from
     *  with an ADD_FOLDER event
     */
    public int getRelativeDepth() {
        return relativeDepth;
    }
    
    /** 
     * Gets the top level directory that sharing was recursivly initiated on
     *  for an ADD_FOLDER event
     */
    public File getRootShare() {
        return rootShare;
    }
    
    /**
     * Gets any XML docs associated with this
     */
    public List<? extends LimeXMLDocument> getMetaData() {
        return metaData;
    }
    
    /**
     * Returns true if this event is an ADD event
     */
    public boolean isAddEvent() {
        return (type.equals(Type.ADD_FILE));
    }
    
    /**
	 * Returns true if this event is an ADD_STORE_FILE event
	 */
    public boolean isAddStoreEvent() {
        return (type.equals(Type.ADD_STORE_FILE));
    }
    
    /**
     * Returns true if this event is a REMOVE event
     */
    public boolean isRemoveEvent() {
        return (type.equals(Type.REMOVE_FILE));
    }
    
    /**
     * Returns true if this event is a RENAME (MOVE) 
     * event
     */
    public boolean isRenameEvent() {
        return (type.equals(Type.RENAME_FILE));
    }
    
    /**
     * Returns true if this event is a CHANGE (i.e.
     * when ID3 Tags changed) event.
     */
    public boolean isChangeEvent() {
        return (type.equals(Type.CHANGE_FILE));
    }
    
    /**
     * Returns true if this is a FAILED add event (ie, addFile failed).
     */
    public boolean isFailedEvent() {
        return (type.equals(Type.ADD_FAILED_FILE));
    }

    /**
     * Returns true if this is an event for a file that was ALREADY_SHARED
     * (ie, an addFile event was ignored because the file was already shared)
     */
    public boolean isAlreadySharedEvent() {
        return (type.equals(Type.ALREADY_SHARED_FILE));
    }
    
    /**
     * Returns true if this is a ADD_FOLDER event.
     */
    public boolean isAddFolderEvent() {
        return type.equals(Type.ADD_FOLDER);
    }
    
    /**
     * Returns true if this is a REMOVE_FOLDER event
     */
    public boolean isRemoveFolderEvent() {
        return type.equals(Type.REMOVE_FOLDER);
    }
    
    /**
     * Returns true if this is a FILEMANAGER_LOADING event
     */
    public boolean isFileManagerLoading() {
        return type.equals(Type.FILEMANAGER_LOADING);
    }
    
    /**
     * Returns true if this is a FILEMANAGER_LOADED event
     */
    public boolean isFileManagerLoaded() {
        return type.equals(Type.FILEMANAGER_LOAD_COMPLETE);
    }
       
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("FileManagerEvent: [event=").append(type);
        
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
