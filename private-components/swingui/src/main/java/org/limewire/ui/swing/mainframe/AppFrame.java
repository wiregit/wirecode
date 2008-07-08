package org.limewire.ui.swing.mainframe;

import java.awt.Dimension;

import org.jdesktop.application.SingleFrameApplication;

public class AppFrame extends SingleFrameApplication {
    
    @Override
    protected void startup() {
        getMainFrame().setJMenuBar(new LimeMenuBar());        
        LimeWireSwingUI ui = new LimeWireSwingUI();
        show(ui);
        ui.goHome();
        
        // Keep this here while building UI - ensures we test 
        // with proper sizes.
        getMainFrame().setSize(new Dimension(1024, 768));
    }
    

    public static void main(String[] args) {
        launch(AppFrame.class, args);
    }
    
}
