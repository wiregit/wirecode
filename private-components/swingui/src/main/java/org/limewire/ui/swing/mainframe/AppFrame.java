package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.jdesktop.application.Resource;
import org.jdesktop.application.SingleFrameApplication;
import org.limewire.ui.swing.util.GuiUtils;

public class AppFrame extends SingleFrameApplication {
	  
	/**
	 * Default background color for panels
	 */
	@Resource
	private Color bgColor;
	
    @Override
    protected void startup() {
    	GuiUtils.injectFields(this);
    	ColorUIResource bgColorResource = new ColorUIResource(bgColor);
    	UIManager.getDefaults().put("Panel.background", bgColorResource);
    	
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
