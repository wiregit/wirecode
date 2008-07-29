package com.limegroup.gnutella;

import java.io.File;
import java.util.EventObject;
import java.util.List;

import org.limewire.util.StringUtils;

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
         * Called when a file has been added to FileManager
         */
        ADD_FILE,
        
        /**
         * Called when a ADD_FILE fails to load a file into FileManager 
         */
        ADD_FAILED_FILE,        
        
        /**
         * Called when ADD_FILE attempts to load a file that already exists in FileManager
         */
        FILE_ALREADY_ADDED,
        
        /**
         * Called when a file is removed from a list, Note that this
         * may not explicitely remove it from the view. It may still be in 
         * a shared folder just no longer editable or shared.
         */
        REMOVE_FILE,
        
        /**
         * Called when the filename on disk is renamed from within the UI
         */
        RENAME_FILE,
        
        /**
         * Called if a rename event has failed
         */
        RENAME_FILE_FAILED,
        
        /**
         * Called when information stored in the file is changed such as editing
         * ID3 tag information. This will cause disk IO to write the changes and 
         * result in a new SHA-1 being generated for this file
         */
        CHANGE_FILE,
        
        /**
         * Called if a change event has failed
         */
        CHANGE_FILE_FAILED,
        
        /**
         * Called when the FileDesc and URN have been calculated for a file. This allows
         * other processes to act on this FileDesc and prepare it for displaying
         * and sharing
         */
        LOAD_FILE,
        
        /**
         * Called whenever a URN is removed from FileManager. 
         */
        REMOVE_URN,
        
        /**
         * Called when a FileDesc has been removed from FileManager. Unlike REMOVE_FILE, 
         * this gets fired any time a file managed by FileManager is 
         * modified. This event will be called any time one of the following events are
         * generated: REMOVE_FILE, RENAME_FILE, CHANGE_FILE
         */
        REMOVE_FD,
        
        /**
         * Called when an IncompleteFileDesc has had its URNs modified.
         */
        INCOMPLETE_URN_CHANGE,
        
        /**
         * Called when a folder and all of its contents has been added to FileManager
         */
        ADD_FOLDER,
        
        /**
         * Called when a folder and all of its contents has been removed from FileManager
         */
        REMOVE_FOLDER,
        
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
    private final FileDesc oldFileDesc;
    private final FileDesc newFileDesc;
    private final File oldFile;
    private final File newFile;
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
        oldFileDesc = null;
        newFileDesc = null;
        oldFile = null;
        newFile = null;
        this.urn = null;
        this.relativeDepth = -1;
        this.rootShare = null;
        this.metaData = null;
    }
    
    public FileManagerEvent(FileManager manager, Type type, FileDesc newFileDesc) {
        super(manager);
        this.type = type;
        this.oldFileDesc = null;
        this.newFileDesc = newFileDesc;
        this.relativeDepth = -1;
        this.rootShare = null;
        this.metaData = null;
        this.urn = null;

        oldFile = null;
        
        if(newFileDesc != null)
            newFile = newFileDesc.getFile();
        else
            newFile = null;
    }
    
    /**
     * Constructs a FileManagerEvent
     */
    public FileManagerEvent(FileManager manager, Type type, FileDesc oldFileDesc, FileDesc newFileDesc) {
        super(manager);
        this.type = type;
        this.oldFileDesc = oldFileDesc;
        this.newFileDesc = newFileDesc;
        this.relativeDepth = -1;
        this.rootShare = null;
        this.metaData = null;
        this.urn = null;
        
        if(oldFileDesc != null)
            oldFile = oldFileDesc.getFile();
        else
            oldFile = null;
        
        if(newFileDesc != null)
            newFile = newFileDesc.getFile();
        else
            newFile = null;
    }
    
    /**
     * Constructs a FileManagerEvent with a bunch of files.
     */
    public FileManagerEvent(FileManager manager, Type type, File newFile) {
        this(manager, type, null, newFile);
    }
    
    public FileManagerEvent(FileManager manager, Type type, File oldFile, File newFile) {
        super(manager);
        this.type = type;
        this.urn = null;
        
        this.newFileDesc = null;
        this.oldFileDesc = null;
        this.newFile = newFile;
        this.oldFile = oldFile;
        
        this.relativeDepth = 0;
        this.rootShare = null;
        this.metaData = null;
    }

    public FileManagerEvent(FileManager manager, Type type, int relativeDepth) {
        super(manager);
        this.type = type;
        this.urn = null;

        this.newFileDesc = null;
        this.oldFileDesc = null;
        this.newFile = null;
        this.oldFile = null;
        
        this.relativeDepth = relativeDepth;
        this.rootShare = null;
        this.metaData = null;
    }
    
    public FileManagerEvent(FileManager manager, Type type, File rootShare, int relativeDepth, File oldFile, File newFile) {
        super(manager);
        this.type = type;
        this.urn = null;

        this.newFileDesc = null;
        this.oldFileDesc = null;
        this.newFile = newFile;
        this.oldFile = oldFile;
        
        this.relativeDepth = relativeDepth;
        this.rootShare = rootShare;
        this.metaData = null;
    }
    
    public FileManagerEvent(FileManager manager, Type type, List<? extends LimeXMLDocument> md, FileDesc newFileDesc) {
        super(manager);
        this.type = type;
        this.urn = null;
        
        this.newFileDesc = newFileDesc;
        this.oldFileDesc = null;
        this.newFile = newFileDesc.getFile();
        this.oldFile = null;
        
        this.metaData = md;
     
        this.relativeDepth = -1;
        this.rootShare = null;
    }
    
    public FileManagerEvent(FileManager manager, Type type, URN urn) {
        super(manager);
        this.type = type;
        this.urn = urn;
        
        oldFileDesc = null;
        newFileDesc = null;
        oldFile = null;
        newFile = null;
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
     * Returns the previous instance of this File that has been replaced with 
     * an instance of a new File. If no old File was being modified this 
     * method returns null.
     */
    public File getOldFile() {
        return oldFile;
    }
    
    /**
     * Returns the File that FileManager was acting upon. NOTE: if only
     * one File was being acted upon, it will be returned with this method.
     */
    public File getNewFile() {
        return newFile;
    }
    
    /**
     * Returns the previous instance of the FileDesc if the FileManager has 
     * modified an existing FileDesc such as a in CHANGE_EVENT or RENAME_EVENT
     * @return
     */
    public FileDesc getOldFileDesc() {
        return oldFileDesc;
    }
    
    /**
     * Returns the newly created FileDesc or null if no FileDesc exists
     * for this event. NOTE: if a FileDesc has been created or only one
     * FileDesc exists, it will be returned with this method
     * @return
     */
    public FileDesc getNewFileDesc() {
        return newFileDesc;
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
     * Returns true if this is an event for a file that was ALREADY_SHARED
     * (ie, an addFile event was ignored because the file was already shared)
     */
    public boolean isAlreadySharedEvent() {
        return (type.equals(Type.FILE_ALREADY_ADDED));
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
        return StringUtils.toString(this, oldFile, newFile, oldFileDesc, newFileDesc);
    }
}
