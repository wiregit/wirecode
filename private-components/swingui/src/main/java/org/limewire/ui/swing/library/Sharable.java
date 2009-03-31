package org.limewire.ui.swing.library;

import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.sharing.ShareWidget;

public interface Sharable<T> {
    
    public void enableMyLibrarySharing(ShareWidget<T> shareWidget, ShareListManager shareListManager);

}
