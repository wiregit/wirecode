package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Actions to hide the Downloads/Uploads Tray 
 */
class HideTransferTrayAction extends AbstractAction {

    public HideTransferTrayAction() {
        super(I18n.tr("Hide Transfer Tray"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUiSettings.SHOW_TRANSFERS_TRAY.setValue(false);
    }
}
