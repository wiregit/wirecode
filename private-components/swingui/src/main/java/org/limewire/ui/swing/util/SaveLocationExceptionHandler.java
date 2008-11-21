package org.limewire.ui.swing.util;

import java.io.File;

import org.limewire.core.api.download.SaveLocationException;

public interface SaveLocationExceptionHandler {
    public interface DownLoadAction {
        void download(File saveFile, boolean overwrite) throws SaveLocationException;
    }

    /**
     * Handles the supplied SaveLocationException. Result could be to eat the
     * exception. To try downloading again using the supplied downloadAction, or
     * to popup a dialogue to try and save the download in a new location.
     */
    public void handleSaveLocationException(final DownLoadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveDir);
}