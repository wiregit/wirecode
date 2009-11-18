package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Action to show Downloads and Uploads in the tray. 
 */
class ShowDownloadAndUploadTrayAction extends AbstractAction {

    public ShowDownloadAndUploadTrayAction() {
        super(I18n.tr("Show Downloads + Uploads"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        UploadSettings.SHOW_UPLOADS_IN_TRAY.setValue(true);
        SwingUiSettings.SHOW_TRANSFERS_TRAY.setValue(true);
    }
}
