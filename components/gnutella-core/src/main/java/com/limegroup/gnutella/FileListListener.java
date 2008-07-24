package com.limegroup.gnutella;


/**
 * A listener for a FileList. Events are fired when the FileList
 * is modified.
 */
public interface FileListListener {

    /**
     * Fired when a new FileDesc has been added to the list.
     * @param fileDesc FileDesc that was added
     */
    public void addEvent(FileDesc fileDesc);
    
    /**
     * Fired when a FileDesc has been removed from the list.
     * @param fileDesc FileDesc that was removed.
     */
    public void removeEvent(FileDesc fileDesc);
    
    /**
     * Fired when a FileDesc already in the list has been modified.
     * @param oldDesc FileDesc that was already in the list
     * @param newDesc FileDesc that has replaced the oldFileDesc
     */
    public void changeEvent(FileDesc oldDesc, FileDesc newDesc); 
}
