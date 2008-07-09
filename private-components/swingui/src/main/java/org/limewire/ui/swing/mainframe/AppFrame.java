package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.util.Enumeration;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.jdesktop.application.Resource;
import org.jdesktop.application.SingleFrameApplication;
import org.limewire.ui.swing.util.GuiUtils;

public class AppFrame extends SingleFrameApplication {
	  
	/** Default background color for panels */
	@Resource
	private Color bgColor;
        
    @Resource
    private Image limeImage;
	
    @Override
    protected void startup() {
    	GuiUtils.injectFields(this);
    	initColors();
        
    	getMainFrame().setIconImage(limeImage);
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
    
    /**
	 * Changes all default background colors equal to Panel.background to the
	 * bgColor set in properties.  Also sets Table.background.
	 */
	private void initColors() {
		ColorUIResource bgColorResource = new ColorUIResource(bgColor);
		Color oldBgColor = UIManager.getDefaults().getColor("Panel.background");
		UIDefaults uiDefaults = UIManager.getDefaults();
		Enumeration<?> enumeration = uiDefaults.keys();
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			if (key.toString().indexOf("background") != -1) {
				if (uiDefaults.get(key).equals(oldBgColor)) {
					UIManager.getDefaults().put(key, bgColorResource);
				}
			}
		}
		
		uiDefaults.put("Table.background", bgColorResource);
	}
}
