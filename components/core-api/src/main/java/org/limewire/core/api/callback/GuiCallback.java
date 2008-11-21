package org.limewire.core.api.callback;

import org.limewire.core.api.download.DownLoadAction;
import org.limewire.core.api.download.SaveLocationException;

/**
 * This class acts as a means of getting user input on issues happening in the
 * core.
 */
public interface GuiCallback {

    void handleSaveLocationException(DownLoadAction downLoadAction, SaveLocationException sle, boolean supportsNewSaveDir);

}
