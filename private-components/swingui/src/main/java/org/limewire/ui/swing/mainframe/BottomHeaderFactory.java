package org.limewire.ui.swing.mainframe;

/**
 * Defines a factory for creating the header panel for the bottom tray.
 */
public interface BottomHeaderFactory {

    /**
     * Creates a DownloadHeaderPanel with the specified components.
     */
    BottomHeaderPanel create(BottomPanel bottomPanel);
}
