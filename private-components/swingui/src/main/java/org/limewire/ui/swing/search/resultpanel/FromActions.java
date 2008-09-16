package org.limewire.ui.swing.search.resultpanel;

/**
 * This interface describes methods that can be invoked from the FromWidget.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public interface FromActions {
    void chatWith(String name);
    void showFilesSharedBy(String name);
    void viewLibraryOf(String name);
}
