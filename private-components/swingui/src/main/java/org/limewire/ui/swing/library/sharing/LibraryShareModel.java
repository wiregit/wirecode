package org.limewire.ui.swing.library.sharing;

import java.beans.PropertyChangeListener;

public interface LibraryShareModel {

    boolean isShared(SharingTarget sharingTarget);

    void shareFriend(SharingTarget sharingTarget);

    void unshareFriend(SharingTarget sharingTarget);

    boolean isGnutellaNetworkSharable();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
