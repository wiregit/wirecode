package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.limewire.setting.SettingsGroupManager;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.options.OptionPanel.ApplyOptionResult;

public class ApplyOptionAction implements ActionListener {

    private OptionsDialog optionDialog;
    
    public ApplyOptionAction(OptionsDialog optionDialog) {
        this.optionDialog = optionDialog;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ApplyOptionResult result = optionDialog.applyOptions();
        if (result.isSuccessful()) {
            SettingsGroupManager.instance().save();
            optionDialog.dispose();
        }
    }
}
