package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Inject;

/**
 * Factory class to create List view renderers for Lime Store results.
 */
class ListViewStoreRendererFactory {

    private final CategoryIconManager categoryIconManager;
    private final StoreManager storeManager;
    
    /**
     * Constructs a ListViewStoreRendererFactory using the specified services.
     */
    @Inject
    public ListViewStoreRendererFactory(
            CategoryIconManager categoryIconManager,
            StoreManager storeManager) {
        this.categoryIconManager = categoryIconManager;
        this.storeManager = storeManager;
    }
    
    /**
     * Creates a List view renderer for store results.
     */
    public ListViewStoreRenderer create() {
        StoreStyle storeStyle = storeManager.getStoreStyle();
        
        // TODO creates renderer based on style type
        switch (storeStyle.getType()) {
        case A: case B:
        case C: case D:
        default:
            return new ListViewStoreRenderer(categoryIconManager, storeStyle);
        }
    }
}
