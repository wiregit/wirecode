package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.jdesktop.application.Action;
import org.jdesktop.application.Resource;
import org.jdesktop.application.SingleFrameApplication;
import org.limewire.core.impl.MockModule;
import org.limewire.inject.Modules;
import org.limewire.ui.swing.LimeWireSwingUiModule;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.tray.TrayExitListener;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.limegroup.gnutella.LifecycleManager;

/**
 * The entry point for the Swing UI.  If the real core is desired,
 * start from integrated-ui/../Main.  The main method in this class
 * uses the mock-core.
 */
public class AppFrame extends SingleFrameApplication {

    @Inject
    private static volatile Injector injector;

    private static volatile boolean started;
    private static List<ApplicationLifecycleListener> lifecycleListeners = new ArrayList<ApplicationLifecycleListener>();

    /** Default background color for panels */
    @Resource
    private Color bgColor;

    public static boolean isStarted() {
        return started;
    }

    @Override
    protected void startup() {
        GuiUtils.assignResources(this);        
        initColors();
        
        String title = getContext().getResourceMap().getString("Application.title");
        JFrame frame = new LimeJFrame(title);
        frame.setName("mainFrame");
        getMainView().setFrame(frame);
        
        // Because we use a browser heavily, which is heavyweight,
        // we must disable all lightweight popups.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        Injector localInjector = createInjector();

        getMainFrame().setJMenuBar(new LimeMenuBar());

        LimeWireSwingUI ui = localInjector.getInstance(LimeWireSwingUI.class);
        ui.showTrayIcon();
        addExitListener(new TrayExitListener(ui.getTrayNotifier()));
        addExitListener(new ShutdownListener(getMainFrame(), localInjector.getInstance(LifecycleManager.class)));
        
        show(ui);        
        restoreView();
        
        ui.goHome();
        ui.focusOnSearch();

        // Keep this here while building UI - ensures we test
        // with proper sizes.
        getMainFrame().setSize(new Dimension(1024, 768));

        started = true;
        
        for(ApplicationLifecycleListener listener : lifecycleListeners) {
            listener.startupComplete();
        }
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
        getMainFrame().toFront();
    }
    
    public Injector createInjector() {
        if (injector == null) {
            injector = Guice.createInjector(Stage.PRODUCTION, new MockModule(), new LimeWireSwingUiModule());
            return injector;
        } else {
            List<Module> modules = new ArrayList<Module>();
            modules.add(new LimeWireSwingUiModule());
            modules.add(Modules.providersFrom(injector)); // Add all the parent bindings
            return Guice.createInjector(Stage.PRODUCTION, modules);
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
    
    public static void addApplicationLifecycleListener(ApplicationLifecycleListener listener) {
        lifecycleListeners.add(listener);
    }
    
    public static void removeApplicationLifecycleListener(ApplicationLifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }
    
    
    private static class ShutdownListener implements ExitListener {
        private final LifecycleManager manager;
        private final JFrame mainFrame;
        
        public ShutdownListener(JFrame mainFrame, LifecycleManager manager) {
            this.mainFrame = mainFrame;
            this.manager = manager;
        }        
        
        @Override
        public boolean canExit(EventObject event) {
            return true;
        }
        
        @Override
        public void willExit(EventObject event) {
            mainFrame.setVisible(false);
            System.out.println("Shutting down...");
            manager.shutdown();
            System.out.println("Shut down");
        }
        
    }
}
