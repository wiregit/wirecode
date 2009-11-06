package org.limewire.ui.swing.search.resultpanel.classic;

import javax.swing.table.TableCellRenderer;

import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.store.StoreController;

/**
 * Defines a factory for creating instances of NameRendererDelegate.
 */
public interface NameRendererDelegateFactory {

    /**
     * Creates an instance of NameRendererDelegate using the specified 
     * options.
     */
    NameRendererDelegate create(TableCellRenderer defaultRenderer,
            MousePopupListener storePopupListener, StoreController storeController,
            boolean showAudioArtist);
}
