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
        /**
         * Called when a shared file has been added to FileManager
         */
        ADD_FILE,
        
        /**
         * Called when a store file has been added to FileManager
         */
        ADD_STORE_FILE,
        
        /**
         * Called when a shared file is no longer shared, Note that this
         * may not explicitely remove it from the view. It may still be in 
         * a shared folder just no longer editable or shared.
         */
        REMOVE_FILE,
        
        /**
         * Called when a store file is no longer managed by FileManager
         */
        REMOVE_STORE_FILE,
        
        /**
         * Called when the filename on disk for a shared or store file is renamed 
         */
        RENAME_FILE,
        
        /**
         * Called when information stored in the file is changed such as editing
         * ID3 tag information. This will cause disk IO to write the changes and 
         * result in a new SHA-1 being generated for this file
         */
        CHANGE_FILE,
        
        /**
         * Called when the FileDesc and URN have been calculated for a file. This allows
         * other processes to act on this FileDesc and prepare it for displaying
         * and sharing
         */
        LOAD_FILE,
        
        /**
         * Called whenever a FileDesc is removed from FileManager. This behaves much like
         * REMOVE_FD in that it is always called as long as its not a incomplete file.
         */
        REMOVE_URN,
        
        /**
         * Called when a FileDesc has been removed from FileManager. Unlike REMOVE_FILE and
         * REMOVE_STORE_FILE, this gets fired any time a file managed by FileManager is 
         * modified. This event will be called any time one of the following events are
         * generated: REMOVE_FILE, REMOVE_STORE_FILE, RENAME_FILE, CHANGE_FILE
         */
        REMOVE_FD,
        
        /**
         * Called when a ADD_FILE fails to load a file into FileManager 
         */
        ADD_FAILED_FILE,
        
        /**
         * Called when ADD_STORE_FILE fails to load a file into FileManager
         */
        ADD_STORE_FAILED_FILE,
        
        /**
         * Called when ADD_FILE attempts to load a file that already exists in FileManager
         */
        ALREADY_SHARED_FILE,
        
        /**
         * Called when a shared folder and all of its contents has been added to FileManager
         */
        ADD_FOLDER,
        
        /**
         * Called when a store folder and all of its contents has been added to FileManager
         */
        ADD_STORE_FOLDER,
        
        /**
         * Called when a shared folder and all of its contents has been removed from FileManager
         */
        REMOVE_FOLDER,
        
        /**
         * Called when a store folder and all of its contents has been removed from FileManager
         */
        REMOVE_STORE_FOLDER,
        
        /**
         * Called once FileManager has begun a new load process of loading all files in all shared
         * and store directories
         */
        FILEMANAGER_LOAD_STARTED,
        
        /**
         * Called prior to FileManager loading all the shared and store directories and the files
         * contained within. This will completely trash all the previously loaded directories and
         * files.
         */
        FILEMANAGER_LOAD_DIRECTORIES,
        
        /**
         * Called once FileManager is preparing to finish the loading process. 
         * 
         * Loading is completed as follows:
         *  1)Load_finishing
         *  2)save
         *  3)load_complete
         */
        FILEMANAGER_LOAD_FINISHING,
        
        /**
         * Called after load_finishing and prior to load_complete. Allows 
         * anything to write to disk
         */
        FILEMANAGER_SAVE,
        
        /**
         * Called after FileManager has completely finished loading
         */
        FILEMANAGER_LOAD_COMPLETE;
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
        return type.equals(Type.FILEMANAGER_LOAD_DIRECTORIES);
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
