package org.limewire.ui.swing.search;

import org.limewire.core.api.endpoint.RemoteHost;

/**
 * This interface describes methods that can be invoked from the FromWidget.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public interface RemoteHostActions {
    void chatWith(RemoteHost person);

    void showFilesSharedBy(RemoteHost person);

    void viewLibraryOf(RemoteHost person);
}
