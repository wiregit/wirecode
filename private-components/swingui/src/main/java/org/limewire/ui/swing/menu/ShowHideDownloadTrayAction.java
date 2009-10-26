package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class ShowHideDownloadTrayAction extends AbstractAction {
    private static final String showDownloadTrayText = I18n.tr("Show Download Tray");
    private static final String hideDownloadTrayText = I18n.tr("Hide Download Tray");
    
    @Inject
    public ShowHideDownloadTrayAction() {
        updateText();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DownloadSettings.SHOW_DOWNLOADS_TRAY.setValue(
                !DownloadSettings.SHOW_DOWNLOADS_TRAY.getValue());
    }

    private void updateText() {
        if(!isEnabled()) {
            this.putValue(Action.NAME, hideDownloadTrayText);
        } else if (DownloadSettings.SHOW_DOWNLOADS_TRAY.getValue()) {
            this.putValue(Action.NAME, hideDownloadTrayText);
        } else {
            this.putValue(Action.NAME, showDownloadTrayText);
        }
    }
}
