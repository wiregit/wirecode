package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ManageFileExtensionsOptionPanel extends OptionPanel {

    private final JButton okButton;
    private final JButton cancelButton;
    
    private FileTypeOptionPanelManager manager;

    @Inject
    public ManageFileExtensionsOptionPanel(FileTypeOptionPanelManager fileTypeOptionPanelManager) {
        this.manager = fileTypeOptionPanelManager;
        
        this.setLayout(new MigLayout("gapy 10, nogrid, fill"));
        
        Action okAction = new OKDialogAction(); 
        CancelDialogAction cancelAction = new CancelDialogAction();
            
        // okAction.setOptionPanel(this);
        cancelAction.setOptionPanel(this);
            
        this.okButton = new JButton(okAction);
        this.cancelButton = new JButton(cancelAction);
            
        add(new JLabel(I18n.tr("Select which file extensions belong in Audio, Video, etc..")), "span, wrap");

        this.add(this.manager.getContainer(), "span, wrap");
        
        add(okButton, "tag ok");
        add(cancelButton, "tag cancel");
    }
    
    @Override
    public void initOptions() {
        this.manager.initOptions();
    }

    @Override
    public void applyOptions() {
        this.manager.applyOptions();
    }

    @Override
    boolean hasChanged() {
        return this.manager.hasChanged();
    }
}
