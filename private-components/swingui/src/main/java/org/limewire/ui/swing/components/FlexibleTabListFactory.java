package org.limewire.ui.swing.components;

/**
 * Defines a factory for creating FlexibleTabList containers.
 */
public interface FlexibleTabListFactory {

    /**
     * Creates a FlexibleTabList using the specified collection of action maps.
     */
    FlexibleTabList create(Iterable<? extends TabActionMap> actionMaps);
    
    /**
     * Creates a FlexibleTabList using the specified array of action maps.
     */
    FlexibleTabList create(TabActionMap... actionMaps);
    
}
