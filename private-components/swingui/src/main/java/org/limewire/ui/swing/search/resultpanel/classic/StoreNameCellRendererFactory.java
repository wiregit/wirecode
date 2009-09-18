package org.limewire.ui.swing.search.resultpanel.classic;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Inject;

/**
 * Factory class to create Classic view renderers for Lime Store results.
 */
class StoreNameCellRendererFactory {

    private final CategoryIconManager categoryIconManager;
    
    /**
     * Constructs a StoreNameCellRendererFactory using the specified services.
     */
    @Inject
    public StoreNameCellRendererFactory(CategoryIconManager categoryIconManager) {
        this.categoryIconManager = categoryIconManager;
    }
    
    /**
     * Creates an instance of StoreNameCellRenderer using the specified style 
     * and display option.
     */
    public StoreNameCellRenderer create(StoreStyle storeStyle, boolean showAudioArtist) {
        
        // TODO return different renderers for different styles
        
        return new StoreNameCellRenderer(storeStyle, categoryIconManager, showAudioArtist);
    }
}
