package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Factory class to create List view renderers for Lime Store results.
 */
class ListViewStoreRendererFactory {

    private final CategoryIconManager categoryIconManager;
    private final StoreManager storeManager;
    private final Provider<SearchHeadingDocumentBuilder> headingBuilder;
    
    /**
     * Constructs a ListViewStoreRendererFactory using the specified services.
     */
    @Inject
    public ListViewStoreRendererFactory(
            CategoryIconManager categoryIconManager,
            StoreManager storeManager,
            Provider<SearchHeadingDocumentBuilder> headingBuilder) {
        this.categoryIconManager = categoryIconManager;
        this.storeManager = storeManager;
        this.headingBuilder = headingBuilder;
    }
    
    /**
     * Creates a List view renderer using the current store style.  If the
     * current style is not available, then the default style is used.
     */
    public ListViewStoreRenderer create() {
        StoreStyle storeStyle = storeManager.getStoreStyle();
        if (storeStyle == null) {
            storeStyle = new DefaultListStoreStyle();
        }
        
        return create(storeStyle);
    }
    
    /**
     * Creates a List view renderer for the specified store style.
     */
    public ListViewStoreRenderer create(StoreStyle storeStyle) {
        // Create renderer based on style type.
        switch (storeStyle.getType()) {
        case STYLE_A: case STYLE_B:
            return new ListViewStoreRendererAB(categoryIconManager, headingBuilder, storeStyle);
        case STYLE_C: case STYLE_D:
            return new ListViewStoreRendererCD(categoryIconManager, headingBuilder, storeStyle);
        default:
            // TODO review for correctness - maybe throw exception
            return new ListViewStoreRendererAB(categoryIconManager, headingBuilder, storeStyle);
        }
    }
}
