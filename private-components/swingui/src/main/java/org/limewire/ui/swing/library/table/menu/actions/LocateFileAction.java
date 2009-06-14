/**
 * 
 */
package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class LocateFileAction extends AbstractAction {
    
//    private final LocalFileItem localFile;
    
    @Inject
    public LocateFileAction(){//LocalFileItem file) {
        super(I18n.tr("Locate on Disk"));
//        this.localFile = file;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        NativeLaunchUtils.launchExplorer(localFile.getFile());
    }
}