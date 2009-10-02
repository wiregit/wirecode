package org.limewire.ui.swing.search.resultpanel.classic;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.store.StoreController;
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
     * Creates an instance of StoreNameCellRenderer using the specified style, 
     * event handlers, and display option.
     */
    public StoreNameCellRenderer create(StoreStyle storeStyle, 
            MousePopupListener popupListener, StoreController storeController,
            boolean showAudioArtist) {
        // Return null if style is null or unknown.
        if (storeStyle == null) {
            return null;
        }
        
        // Create renderer based on style type.
        switch (storeStyle.getType()) {
        case STYLE_A: case STYLE_B:
        case STYLE_C: case STYLE_D:
            return new StoreNameCellRendererImpl(storeStyle, showAudioArtist, 
                    categoryIconManager, popupListener, storeController);
            
        default:
            // Return null if type is not recognized.
            return null;
        }
    }
}
