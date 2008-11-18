package org.limewire.ui.swing.util;

import java.awt.Component;
import java.io.File;

import org.limewire.core.api.download.SaveLocationException;

public interface SaveLocationExceptionHandler {
    public interface DownLoadAction {
        void download(File saveFile, boolean overwrite) throws SaveLocationException;
    }

    public void handleSaveLocationException(final DownLoadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveDir,
            final Component component);
}