package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.util.I18n;

import net.miginfocom.swing.MigLayout;

public class WarningMessagesOptionPanel extends OptionPanel {

    private JCheckBox licensedMaterialCheckBox;
    private JButton okButton;
    private JButton cancelButton;
    
    public WarningMessagesOptionPanel(Action okAction, CancelDialogAction cancelAction) {
        setLayout(new MigLayout("gapy 10"));
        
        cancelAction.setOptionPanel(this);
        licensedMaterialCheckBox = new JCheckBox(I18n.tr("Warn me when downloading a file without a license"));
        licensedMaterialCheckBox.setContentAreaFilled(false);
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
        
        add(new JLabel(I18n.tr("Choose which warning messages you'd like to see:")), "span 2, wrap");
        
        add(licensedMaterialCheckBox, "gapleft 25, gapbottom 25, split, wrap");
        
        add(okButton, "split 2, span 2, alignx right");
        add(cancelButton);
    }
    
    @Override
    void applyOptions() {
        // TODO Auto-generated method stub
        
    }

    @Override
    boolean hasChanged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void initOptions() {
        
    }
}
