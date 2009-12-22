package org.limewire.ui.swing.search.resultpanel.list;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator;
import org.limewire.ui.swing.search.resultpanel.classic.StoreRendererResourceManager;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Factory class to create List view renderers for Lime Store results.
 */
class ListViewStoreRendererFactory {

    private final CategoryIconManager categoryIconManager;
    private final Provider<SearchHeadingDocumentBuilder> headingBuilder;
    private final Provider<SearchResultTruncator> headingTruncator;
    private final LibraryMediator libraryMediator;
    private final MainDownloadPanel mainDownloadPanel;
    private final StoreRendererResourceManager storeResourceManager;
    
    /**
     * Constructs a ListViewStoreRendererFactory using the specified services.
     */
    @Inject
    public ListViewStoreRendererFactory(
            CategoryIconManager categoryIconManager,
            Provider<SearchHeadingDocumentBuilder> headingBuilder,
            Provider<SearchResultTruncator> headingTruncator,
            LibraryMediator libraryMediator,
            MainDownloadPanel mainDownloadPanel,
            StoreRendererResourceManager storeResourceManager) {
        this.categoryIconManager = categoryIconManager;
        this.headingBuilder = headingBuilder;
        this.headingTruncator = headingTruncator;
        this.libraryMediator = libraryMediator;
        this.mainDownloadPanel = mainDownloadPanel;
        this.storeResourceManager = storeResourceManager;
    }
    
    /**
     * Creates a List view renderer for the specified store style.
     */
    public ListViewStoreRenderer create(StoreStyle storeStyle, 
            MousePopupListener popupListener, StoreController storeController) {
        // Return null if style is null or unknown.
        if (storeStyle == null) {
            return null;
        }
        
        // Create renderer based on style type.
        switch (storeStyle.getType()) {
        case STYLE_A: case STYLE_B:
            return new ListViewStoreRendererAB(storeStyle, categoryIconManager,
                    headingBuilder, headingTruncator, libraryMediator, mainDownloadPanel,
                    storeResourceManager, popupListener, storeController);
            
        case STYLE_C: case STYLE_D:
            return new ListViewStoreRendererCD(storeStyle, categoryIconManager, 
                    headingBuilder, headingTruncator, libraryMediator, mainDownloadPanel,
                    storeResourceManager, popupListener, storeController);
            
        default:
            return null;
        }
    }
}
