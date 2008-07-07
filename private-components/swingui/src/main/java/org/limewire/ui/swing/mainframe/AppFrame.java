package org.limewire.ui.swing.mainframe;

import org.jdesktop.application.SingleFrameApplication;

public class AppFrame extends SingleFrameApplication {
    
    @Override
    protected void startup() {
        getMainFrame().setTitle("LimeWire");
        getMainFrame().setJMenuBar(new LimeMenuBar());
        
        LimeWireSwingUI ui = new LimeWireSwingUI();
        show(ui);
        ui.goHome();
    }
    

    public static void main(String[] args) {
        launch(AppFrame.class, args);
    }
    
}
