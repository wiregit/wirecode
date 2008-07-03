package org.limewire.ui.swing.mainframe;

import javax.swing.JPanel;

import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.View;

import com.limegroup.gui.NavigationTree;
import com.limegroup.gui.downloads.MainDownloadPanel;

public class AppFrame extends SingleFrameApplication {
    
    @Override
    protected void startup() {
        getMainFrame().setTitle("LimeWire");
        getMainFrame().setJMenuBar(new LimeMenuBar());
        show(new LimeWireSwingUI());
    }
    
    @Override
    public void show(View view) {
        // TODO Auto-generated method stub
        super.show(view);
    }

    public static void main(String[] args) {
        launch(AppFrame.class, args);
    }
    public AppFrame() {
        super();
    }

}
