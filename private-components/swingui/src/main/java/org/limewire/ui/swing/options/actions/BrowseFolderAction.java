package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BrowseFolderAction implements ActionListener {
    
    public BrowseFolderAction() {
        
    }
    
    public void actionPerformed(ActionEvent e) { 
//        File dir = FileChooserHandler.getInputDirectory(OptionsMediator.instance().getMainOptionsComponent(),
//                new File(saveDirectory));
//        
//        // If the user cancelled the file chooser, simply return.
//        if (dir == null)
//            return;
        
//        ValidationResult result = SaveDirectoryHandler.isFolderValidForSaveDirectory(dir);
//        switch(result) {
//        case VALID:
//            break;
//        case BAD_BANNED:
//        case BAD_VISTA:
//        case BAD_SENSITIVE:
//            return; // These already show a warning.
//        case BAD_PERMS:
//        default:
//            // These need another message.
//            GUIMediator.showError(I18n.tr("The selected save folder is invalid. You may not have permissions to write to the selected folder. LimeWire will revert to your previously selected folder."));
//            return;
//        }
//        
//        try {
//            String newDir = dir.getCanonicalPath();
//            if(!newDir.equals(saveDirectory)) {
//                saveField.setText(newDir);
//            }
//        } catch (IOException ioe) {}
    }
}
