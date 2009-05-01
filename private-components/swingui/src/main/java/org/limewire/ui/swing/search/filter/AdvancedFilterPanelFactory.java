package org.limewire.ui.swing.search.filter;

/**
 * Defines a factory for creating the advanced filter panel.
 */
public interface AdvancedFilterPanelFactory<E extends FilterableItem> {

    /**
     * Creates a new AdvancedFilterPanel using the specified filterable data
     * source.
     */
    public AdvancedFilterPanel<E> create(FilterableSource<E> filterableSource);
    
}
