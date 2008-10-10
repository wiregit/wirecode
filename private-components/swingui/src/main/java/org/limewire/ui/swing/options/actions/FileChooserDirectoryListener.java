package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;

public class FileChooserDirectoryListener extends AbstractAction {

    private JComponent parentComponent;
    private JTextField currentDirectoryTextField;
    
    public FileChooserDirectoryListener(JComponent parentComponent, JTextField currentDirectoryTextField) {
        this.parentComponent = parentComponent;
        this.currentDirectoryTextField = currentDirectoryTextField;
        
        putValue(Action.NAME, I18n.tr("Browse..."));
        putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a different Save Location"));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        File directory = FileChooser.getInputDirectory(parentComponent, new File(currentDirectoryTextField.getText()));
        
        if(directory == null)
            return;
        
        try {
            String newDirectory = directory.getCanonicalPath();
            currentDirectoryTextField.setText(newDirectory);
        }catch(IOException ioe) {}
    }

}
