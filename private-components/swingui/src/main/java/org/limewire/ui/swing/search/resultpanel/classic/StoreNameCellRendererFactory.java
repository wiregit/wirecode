package org.limewire.ui.swing.search.resultpanel.classic;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.store.StoreControllerFactory;
import org.limewire.ui.swing.util.CategoryIconManager;

import com.google.inject.Inject;

/**
 * Factory class to create Classic view renderers for Lime Store results.
 */
class StoreNameCellRendererFactory {

    private final CategoryIconManager categoryIconManager;
    private final StoreControllerFactory storeControllerFactory;
    
    /**
     * Constructs a StoreNameCellRendererFactory using the specified services.
     */
    @Inject
    public StoreNameCellRendererFactory(CategoryIconManager categoryIconManager,
            StoreControllerFactory storeControllerFactory) {
        this.categoryIconManager = categoryIconManager;
        this.storeControllerFactory = storeControllerFactory;
    }
    
    /**
     * Creates an instance of StoreNameCellRenderer using the specified style 
     * and display option.
     */
    public StoreNameCellRenderer create(StoreStyle storeStyle, boolean showAudioArtist) {
        // Return null if style is null or unknown.
        if (storeStyle == null) {
            return null;
        }
        
        // Create renderer based on style type.
        switch (storeStyle.getType()) {
        case STYLE_A: case STYLE_B:
            return new StoreNameCellRendererAB(storeStyle, showAudioArtist, 
                    categoryIconManager, storeControllerFactory.create());
            
        case STYLE_C: case STYLE_D:
            return new StoreNameCellRendererCD(storeStyle, showAudioArtist, 
                    categoryIconManager, storeControllerFactory.create());
            
        default:
            return null;
        }
    }
}
