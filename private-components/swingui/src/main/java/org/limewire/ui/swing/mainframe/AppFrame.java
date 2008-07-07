package org.limewire.ui.swing.mainframe;

import javax.swing.UIManager;

import org.jdesktop.application.SingleFrameApplication;

public class AppFrame extends SingleFrameApplication {
    
    @Override
    protected void startup() {
        UIManager.put("Tree.paintLines", Boolean.FALSE);
        
        getMainFrame().setTitle("LimeWire");
        getMainFrame().setJMenuBar(new LimeMenuBar());
        show(new LimeWireSwingUI());
    }
    
    

    public static void main(String[] args) {
        launch(AppFrame.class, args);
    }
    
}
