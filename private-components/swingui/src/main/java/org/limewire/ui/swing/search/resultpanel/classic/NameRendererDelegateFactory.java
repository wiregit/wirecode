package org.limewire.ui.swing.search.resultpanel.classic;

import javax.swing.table.TableCellRenderer;

/**
 * Defines a factory for creating instances of NameRendererDelegate.
 */
public interface NameRendererDelegateFactory {

    /**
     * Creates an instance of NameRendererDelegate using the specified 
     * options.
     */
    NameRendererDelegate create(TableCellRenderer defaultRenderer, boolean showAudioArtist);
}
