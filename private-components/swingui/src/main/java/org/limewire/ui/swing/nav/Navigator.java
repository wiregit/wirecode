package org.limewire.ui.swing.nav;

import javax.swing.JComponent;

/**
 * An interface allowing panels to be added as navigable,
 * or removed.
 */
public interface Navigator {

    public static enum NavCategory {
        LIBRARY, LIMEWIRE, SEARCH, DOWNLOAD
    }

    /**
     * Adds a panel described by the given name, in the category.
     * 
     * @param category The category this belongs in
     * @param name The name this should be rendered with
     * @param panel The panel to display when selected
     * @param userRemovable true if this should be rendered with a 'remove' icon
     * @return A {@link NavItem} that can be used to select or remove the item.
     */
    public NavItem addNavigablePanel(NavCategory category, String name, JComponent panel, boolean userRemovable);
    
    /**
     * Adds a listener that is notified when a {@link NavItem} is selected.
     */
    public void addNavListener(NavSelectionListener itemListener);
    
    /**
     * Removes the listener from being notified when a {@link NavItem} is selected.
     */
    public void removeNavListener(NavSelectionListener itemListener);

}