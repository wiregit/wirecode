package org.limewire.ui.swing.search.model;

public interface BrowseStatusListener {
    // TODO parameters - (Map<Presence, BrowseStatus>) or, (BrowseStatus status, Presence... failedPresences)?
    void statusChanged();
}
