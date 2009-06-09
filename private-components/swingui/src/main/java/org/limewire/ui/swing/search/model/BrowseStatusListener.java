package org.limewire.ui.swing.search.model;

import org.limewire.ui.swing.search.model.browse.BrowseStatus;


public interface BrowseStatusListener {
    // TODO parameters - (Map<Presence, BrowseStatus>) or, (BrowseStatus status, Presence... failedPresences)?
    void statusChanged(BrowseStatus status);
}
