package org.limewire.ui.swing.sharing;

public interface BuddySharingDisplay {

    /**
     * Responsible for displaying the "All Friends" buddy sharing panel in the main view of the application
     */
    public void displaySharing();

    /**
     * Displays the "All Friends" buddy sharing panel, and selects the buddy whose id matches the supplied parameter
     * @param buddyId
     */
    public void selectBuddy(String buddyId);
}
