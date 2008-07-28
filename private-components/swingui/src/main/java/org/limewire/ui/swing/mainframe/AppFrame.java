package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.jdesktop.application.Resource;
import org.jdesktop.application.SingleFrameApplication;
import org.limewire.core.impl.MockModule;
import org.limewire.ui.swing.LimeWireSwingUiModule;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.XMPPErrorListener;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

public class AppFrame extends SingleFrameApplication {
    
    @Inject
    private static volatile Injector injector;
    
    private static volatile boolean started;
	  
	/** Default background color for panels */
	@Resource
	private Color bgColor;

	
	@Resource
	private Image frameIcon;
	
	public static boolean isStarted() {
	    return started;
	}
	
    @Override
    protected void startup() {
    	GuiUtils.assignResources(this);
    	initColors();
    	
    	Injector injector = createInjector();
        
    	getMainFrame().setIconImage(frameIcon);
        getMainFrame().setJMenuBar(new LimeMenuBar());    
        
        LimeWireSwingUI ui = injector.getInstance(LimeWireSwingUI.class);
        show(ui);
        ui.goHome();
        ui.focusOnSearch();
        
        // Keep this here while building UI - ensures we test 
        // with proper sizes.
        getMainFrame().setSize(new Dimension(1024, 768));
        
        started = true;
    }
    
    public Injector createInjector() {
        if(injector == null) {
            injector = Guice.createInjector(new MockModule(), new LimeWireSwingUiModule());
            return injector;
        } else {
            List<Module> modules = new ArrayList<Module>();
            modules.add(new LimeWireSwingUiModule());
            Injector newInjector = Guice.createInjector(injector, Stage.PRODUCTION, modules);
            newInjector.getInstance(FileOfferHandler.class);
            newInjector.getInstance(RosterListener.class);
            newInjector.getInstance(XMPPErrorListener.class);
            return newInjector;
        }
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
