package org.limewire.core.api.callback;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;

/**
 * This class acts as a means of getting user input on issues happening in the
 * core.
 */
public interface GuiCallback {

    void handleSaveLocationException(DownloadAction downLoadAction, SaveLocationException sle, boolean supportsNewSaveDir);

}
