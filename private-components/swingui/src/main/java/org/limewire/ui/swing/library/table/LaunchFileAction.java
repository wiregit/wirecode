package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Tries to safely launch the given file.
 * If it is an allowed file type it will be launched, 
 * otherwise explorer will be opened to the files location
 */
class LaunchFileAction extends AbstractAction {
    
//    private final LocalFileItem localFile;
    
    @Inject
    public LaunchFileAction() {//String name, LocalFileItem localFile) {
        super(I18n.tr("Play/Open/View"));
//        this.localFile = localFile;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        NativeLaunchUtils.safeLaunchFile(localFile.getFile());
    }
}