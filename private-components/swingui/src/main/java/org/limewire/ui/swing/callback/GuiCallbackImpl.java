package org.limewire.ui.swing.callback;

import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.callback.GuiCallbackService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GuiCallbackImpl implements GuiCallback {
    @Inject
    public GuiCallbackImpl(GuiCallbackService guiCallbackService) {
        guiCallbackService.setGuiCallback(this);
    }

    @Override
    public void handleSaveLocationException() {
        System.out.println("get to steppin");
    }
}
