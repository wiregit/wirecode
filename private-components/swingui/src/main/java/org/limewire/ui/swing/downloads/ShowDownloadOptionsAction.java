package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Opens the Options menu and shows the Download tab.
 */
class ShowDownloadOptionsAction extends AbstractAction {

    @Inject
    public ShowDownloadOptionsAction() {
        super(I18n.tr("Download Options..."));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        new OptionsDisplayEvent(OptionsDialog.DOWNLOADS).publish();
    }
}
