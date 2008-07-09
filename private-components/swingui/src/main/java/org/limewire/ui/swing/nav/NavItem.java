package org.limewire.ui.swing.nav;

/**
 * A token representing a navigable item.
 */
public interface NavItem {

    /** Selects the nav item. */
    void select();

    /** Removes this nav item. */
    void remove();

    /** Returns the title of nav item. */
    String getName();

}
