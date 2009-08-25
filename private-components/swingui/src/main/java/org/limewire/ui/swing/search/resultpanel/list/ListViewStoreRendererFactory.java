package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultMenuFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Provider;

/**
 * Factory class to create List view renderers for Lime Store results.
 */
class ListViewStoreRendererFactory {

    private final CategoryIconManager categoryIconManager;
    private final Provider<SearchHeadingDocumentBuilder> headingBuilder;
    private final Provider<SearchResultTruncator> headingTruncator;
    private final SearchResultMenuFactory popupMenuFactory;
    private final StoreController storeController;
    
    /**
     * Constructs a ListViewStoreRendererFactory using the specified services.
     */
    public ListViewStoreRendererFactory(
            CategoryIconManager categoryIconManager,
            Provider<SearchHeadingDocumentBuilder> headingBuilder,
            Provider<SearchResultTruncator> headingTruncator,
            SearchResultMenuFactory popupMenuFactory,
            StoreController storeController) {
        this.categoryIconManager = categoryIconManager;
        this.headingBuilder = headingBuilder;
        this.headingTruncator = headingTruncator;
        this.popupMenuFactory = popupMenuFactory;
        this.storeController = storeController;
    }
    
    /**
     * Creates a List view renderer for the specified store style.
     */
    public ListViewStoreRenderer create(StoreStyle storeStyle) {
        // Create renderer based on style type.
        switch (storeStyle.getType()) {
        case STYLE_A: case STYLE_B:
            return new ListViewStoreRendererAB(storeStyle, categoryIconManager,
                    headingBuilder, headingTruncator, popupMenuFactory, storeController);
            
        case STYLE_C: case STYLE_D:
            return new ListViewStoreRendererCD(storeStyle, categoryIconManager, 
                    headingBuilder, headingTruncator, popupMenuFactory, storeController);
            
        default:
            // Return default renderer so store results may still be viewed in
            // spite of an unrecognized store style.
            // TODO review - maybe send notification of incorrect condition
            return new ListViewStoreRendererAB(storeStyle, categoryIconManager, 
                    headingBuilder, headingTruncator, popupMenuFactory, storeController);
        }
    }
}
