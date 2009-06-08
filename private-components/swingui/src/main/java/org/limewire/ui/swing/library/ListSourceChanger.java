package org.limewire.ui.swing.library;


/**
 * Allows an EventList to be filtered based on user. The main EventList
 * contains a composite list of files associated with 0 or more friends.
 * When applying a filter on a friend, only files associated with that
 * friend will be displayed. 
 */
public interface ListSourceChanger {
    //TODO: fix javadoc
    /** 
     * Sets the friend to filter with. This will apply the filter to the source 
     * list matching only files in the source list associated with this friend. 
     * To remove filtering set friend to null.
     */
    public void setCurrentList(String listId);
    
    /** 
     * Returns the current friend that is being filtered with, 
     * null if no filtering is being done 
     */
    public String getCurrentListID();
    
    /** Adds a listener to be notified when the current friend changes. */
    public void addListener(ListChangedListener listener);
    
    /** A listener for when the friend in the library changes. */
    public static interface ListChangedListener {
        /** 
         * Notification that the current visible friend has changed. If no
         * filtering is being applied, friend will return null.
         */
        public void idChanged(String currentId);
    }
}
