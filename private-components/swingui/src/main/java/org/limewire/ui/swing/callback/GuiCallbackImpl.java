package org.limewire.ui.swing.callback;

import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GuiCallbackImpl implements GuiCallback {
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    @Inject
    public GuiCallbackImpl(GuiCallbackService guiCallbackService, SaveLocationExceptionHandler saveLocationExceptionHandler) {
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        guiCallbackService.setGuiCallback(this);
    }

    @Override
    public void handleSaveLocationException(DownloadAction downLoadAction,
            SaveLocationException sle, boolean supportsNewSaveDir) {
        saveLocationExceptionHandler.handleSaveLocationException(downLoadAction, sle, supportsNewSaveDir);
    }
}
