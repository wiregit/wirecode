package org.limewire.ui.swing.search.resultpanel;

/**
 * This class provides a mock implementation of the FromActions interface.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FromActionsMockImpl implements FromActions {

    public void chatWith(String name) {
        System.out.println("starting chat with " + name);
    }

    public void showFilesSharedBy(String name) {
        System.out.println("showing files shared by " + name);
    }

    public void viewLibraryOf(String name) {
        System.out.println("viewing library of " + name);
    }
}