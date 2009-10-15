package org.limewire.ui.swing.mainframe;

import java.util.List;

import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.downloads.DownloadHeaderPanel;

/**
 * Defines a factory for creating the header panel for the bottom tray.
 */
public interface BottomHeaderFactory {

    /**
     * Creates a DownloadHeaderPanel with the specified list of tab actions.
     */
    DownloadHeaderPanel create(List<TabActionMap> tabActionList);
}
