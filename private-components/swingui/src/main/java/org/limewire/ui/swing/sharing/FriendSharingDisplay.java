package org.limewire.ui.swing.sharing;

public interface FriendSharingDisplay {

    /**
     * Responsible for displaying the "All Friends" friend sharing panel in the main view of the application
     */
    public void displaySharing();

    /**
     * Displays the "All Friends" friend sharing panel, and selects the friend whose id matches the supplied parameter
     * @param id
     */
    public void selectFriendInFileSharingList(String id);
    
    /**
     * Displays the library for the supplied friend name.
     * @param friendName The name of the friend whose library is to be viewed.  Should use friend ID if name is null.
     */
    public void selectFriendLibrary(String friendName);
}
