package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Action to show or hide the Uploads tray.
 */
class UploadTrayAction extends AbstractAction {
    private static final String SHOW_UPLOAD_TEXT = I18n.tr("Show Upload Tray");
    private static final String HIDE_UPLOAD_TEXT = I18n.tr("Hide Upload Tray");

    @Inject
    public UploadTrayAction() {
        updateText();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Toggle setting.
        UploadSettings.SHOW_UPLOADS_TRAY.setValue(
                !UploadSettings.SHOW_UPLOADS_TRAY.getValue());
    }
    
    /**
     * Updates the display text for the action.
     */
    private void updateText() {
        if (!isEnabled()) {
            putValue(Action.NAME, HIDE_UPLOAD_TEXT);
        } else if (UploadSettings.SHOW_UPLOADS_TRAY.getValue()) {
            putValue(Action.NAME, HIDE_UPLOAD_TEXT);
        } else {
            putValue(Action.NAME, SHOW_UPLOAD_TEXT);
        }
    }
}
