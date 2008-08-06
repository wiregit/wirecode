package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.util.Enumeration;

import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.jdesktop.application.Action;
import org.jdesktop.application.Resource;
import org.jdesktop.application.SingleFrameApplication;
import org.limewire.core.impl.MockModule;
import org.limewire.ui.swing.LimeWireSwingUiModule;
import org.limewire.ui.swing.tray.TrayExitListener;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The entry point for the Swing UI.  If the real core is desired,
 * start from integrated-ui/../Main.  The main method in this class
 * uses the mock-core.
 */
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
        
        // Because we use a browser heavily, which is heavyweight,
        // we must disable all lightweight popups.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        Injector injector = createInjector();

        getMainFrame().setIconImage(frameIcon);
        getMainFrame().setJMenuBar(new LimeMenuBar());

        LimeWireSwingUI ui = injector.getInstance(LimeWireSwingUI.class);
        ui.showTrayIcon();
        addExitListener(new TrayExitListener(ui.getTrayNotifier()));
        
        show(ui);
        ui.goHome();
        ui.focusOnSearch();

        // Keep this here while building UI - ensures we test
        // with proper sizes.
        getMainFrame().setSize(new Dimension(1024, 768));

        started = true;
    }
    
    @Action
    public void minimizeToTray() { // DO NOT CHANGE THIS METHOD NAME!  
        getMainFrame().setState(Frame.ICONIFIED);
        getMainFrame().setVisible(false);
    }    

    @Action
    public void restoreView() { // DO NOT CHANGE THIS METHOD NAME!  
        getMainFrame().setVisible(true);
        getMainFrame().setState(Frame.NORMAL);
    }
    
    public Injector createInjector() {
        if (injector == null) {
            injector = Guice.createInjector(new MockModule(), new LimeWireSwingUiModule());
            return injector;
        } else {
            return Guice.createInjector(injector, new LimeWireSwingUiModule());
        }
    }

    public static void main(String[] args) {
        launch(AppFrame.class, args);
    }

    /**
     * Changes all default background colors equal to Panel.background to the
     * bgColor set in properties. Also sets Table.background.
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
