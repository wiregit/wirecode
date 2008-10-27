package org.limewire.ui.swing.options;

import javax.swing.JFrame;

import org.limewire.ui.swing.util.I18n;


/**
 * Composed abstract pane for the options window that uses a FileTypeSharingPanelManager to
 *  manage file type extensions sharing.  
 */
public final class FileTypeOptionPanel extends OptionPanel {

    private FileTypeOptionPanelManager manager;

    public FileTypeOptionPanel() {
        super(I18n.tr(FileTypeOptionPanelManager.TITLE));
        
        this.manager = new FileTypeOptionPanelManager(this);
        
        this.manager.initOptions();
        
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

    public static void main(String args[]) {
        
        FileTypeOptionPanel a = new FileTypeOptionPanel();
        
        JFrame frame = new JFrame();
        
        frame.add(a);
        frame.pack();
        frame.setVisible(true);
    }
    
}
