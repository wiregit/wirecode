package org.limewire.ui.swing.options;

import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Composed abstract pane for the options window that uses a FileTypeSharingPanelManager to
 *  manage file type extensions sharing.  
 */

@Singleton
public final class FileTypeOptionPanel extends OptionPanel {

    private FileTypeOptionPanelManager manager;

    @Inject
    public FileTypeOptionPanel(FileTypeOptionPanelManager fileTypeOptionPanelManager) {
        super(I18n.tr("Sharing Extensions"));
        
        this.manager = fileTypeOptionPanelManager;
        
        this.add(this.manager.getContainer());
    }
    
    @Override
    public void initOptions() {
        this.manager.initOptions();
    }

    @Override
    public void applyOptions() {
        this.manager.applyOptions();
    }

    public boolean isDirty() {
        return this.manager.isDirty();
    }

    @Override
    boolean hasChanged() {
        // TODO Auto-generated method stub
        return false;
    }
    
}
