package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.limewire.ui.swing.options.OptionsDialog;

public class CancelOptionAction implements ActionListener {

    OptionsDialog optionDialog;
    
    public CancelOptionAction(OptionsDialog optionDialog) {
        this.optionDialog = optionDialog;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        optionDialog.setVisible(false);
    }
}
