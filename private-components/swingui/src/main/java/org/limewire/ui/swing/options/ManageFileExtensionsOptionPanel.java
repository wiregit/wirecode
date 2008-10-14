package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.util.I18n;

public class ManageFileExtensionsOptionPanel extends OptionPanel {

    private JButton okButton;
    private JButton cancelButton;
    
    public ManageFileExtensionsOptionPanel(Action okAction, CancelDialogAction cancelAction) {
        setLayout(new MigLayout("gapy 10"));
        
        cancelAction.setOptionPanel(this);
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
        
        cancelAction.setOptionPanel(this);
        add(new JLabel(I18n.tr("Select which file extensions belong in Audio, Video, etc..")), "span, wrap");
        
        add(okButton, "skip 1, alignx right, split 2");
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
        // TODO Auto-generated method stub
        
    }

}
