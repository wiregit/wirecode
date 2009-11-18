package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Action to show only Downloads in the tray. 
 */
class ShowDownloadOnlyTrayAction extends AbstractAction {
   
    public ShowDownloadOnlyTrayAction() {
        super(I18n.tr("Show Downloads Only"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUiSettings.SHOW_TRANSFERS_TRAY.setValue(true);
        UploadSettings.SHOW_UPLOADS_IN_TRAY.setValue(false);
    }
}
