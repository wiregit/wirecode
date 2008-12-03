package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.Resource;
import org.jdesktop.application.SessionStorage;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.core.api.Application;
import org.limewire.core.impl.MockModule;
import org.limewire.inject.Modules;
import org.limewire.ui.swing.LimeWireSwingUiModule;
import org.limewire.ui.swing.browser.LimeMozillaInitializer;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.event.AboutDisplayEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.ExitApplicationEvent;
import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.tray.TrayExitListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.wizard.SetupWizard;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaInitialization.InitStatus;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

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
    @Resource private Color bgColor;
    @Resource private Color glassPaneColor;
    
    @Inject private Application application;
    @Inject private LimeWireSwingUI ui;
    @Inject private SetupWizard setupWizard;
    @Inject private OptionsDialog options;

    public static boolean isStarted() {
        return started;
    }
    
    /**
     * JDesktop's built-in storage isn't what we want -- don't use it.
     * There's issues with it restoring sizes/position if you closed
     * while minimized or other weirdness.  Unfortunately, changing
     * it is package-private.
     */
    private void changeSessionStorage(ApplicationContext ctx, SessionStorage storage) {
        try {
            Method m = ctx.getClass().getDeclaredMethod("setSessionStorage", SessionStorage.class);
            m.setAccessible(true);
            m.invoke(ctx, storage);
        } catch(Throwable oops) {}
    }
    
    @Override
    protected void initialize(String[] args) {
        changeSessionStorage(getContext(), new NullSessionStorage(getContext()));
        
        GuiUtils.assignResources(this);        
        initColors();
        
        // Because we use a browser heavily, which is heavyweight,
        // we must disable all lightweight popups.
        if(MozillaInitialization.getStatus() != InitStatus.FAILED) {
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        }
        
        // Necessary to allow popups to behave normally.
        UIManager.put("PopupMenu.consumeEventOnClose", false);
    }

    @Override
    protected void startup() {        
        String title = getContext().getResourceMap().getString("Application.title");
        JFrame frame = new LimeJFrame(title);
        frame.setName("mainFrame");
        getMainView().setFrame(frame);

        // Create the Injector for the UI.
        assert ui == null;
        createUiInjector();
        assert ui != null;

        ui.showTrayIcon();
        getMainFrame().setJMenuBar(ui.getMenuBar());
        
        addExitListener(new TrayExitListener(ui.getTrayNotifier()));
        addExitListener(new ShutdownListener(getMainFrame(), application));
        
        // We set the size here to avoid flickering, 
        // because JDesktop sets visibile to true immediately.
        setWindowPosition(getMainFrame());
        
        show(ui);      
        restoreView();
        
        ui.goHome();
        ui.focusOnSearch();
        
        // We have to reset the window position in order to 
        // ensure visible components are given the right size.
        setWindowPosition(getMainFrame());

        started = true;
    }
    
    // TODO: Use prior positioning.
    private void setWindowPosition(JFrame frame) {
        frame.setSize(new Dimension(1024, 768));
        frame.setLocationRelativeTo(null);
        frame.validate();
    }
    
    @Override
    protected void ready() {
        if (setupWizard.shouldShowWizard()) {
            JXPanel glassPane = new JXPanel();
            glassPane.setOpaque(false);
            glassPane.setBackgroundPainter(new AbstractPainter<JComponent>() {
                @Override
                protected void doPaint(Graphics2D g, JComponent object, int width, int height) {
                    g.setPaint(glassPaneColor);
                    g.fillRect(0, 0, width, height);
                }
            });
            getMainView().getFrame().setGlassPane(glassPane);

            glassPane.setVisible(true);
            setupWizard.showDialogIfNeeded(getMainFrame());
            glassPane.setVisible(false);
        }
        
        EventAnnotationProcessor.subscribe(this);
        
        for(ApplicationLifecycleListener listener : lifecycleListeners) {
            listener.startupComplete();
        }
    }
    
    @EventSubscriber
    public void handleShowAboutWindow(AboutDisplayEvent event) {
        new AboutWindow(getMainFrame(), application).showDialog();
    }
    
    @EventSubscriber
    public void handleShowOptionsDialog(OptionsDisplayEvent event) {
        if (!options.isVisible()) {
            options.setLocationRelativeTo(GuiUtils.getMainFrame());
            options.setVisible(true);
        }
    }
    
    @EventSubscriber
    public void handleExitApplication(ExitApplicationEvent event) {
        exit();
    }
    
    @Action
    public void showAboutWindow() { // DO NOT CHANGE THIS METHOD NAME!  
        handleShowAboutWindow(null);
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
    
    public Injector createUiInjector() {
        Module thiz = new AbstractModule() {
            @Override
            protected void configure() {
                bind(AppFrame.class).toInstance(AppFrame.this);
            }
        };
        if (injector == null) {
            LimeMozillaInitializer.initialize();
            injector = Guice.createInjector(Stage.PRODUCTION, new MockModule(), new LimeWireSwingUiModule(), thiz);
            return injector;
        } else {
            List<Module> modules = new ArrayList<Module>();
            modules.add(thiz);
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
        private final Application application;
        private final JFrame mainFrame;
        
        public ShutdownListener(JFrame mainFrame, Application application) {
            this.mainFrame = mainFrame;
            this.application = application;
        }        
        
        @Override
        public boolean canExit(EventObject event) {
            return true;
        }
        
        @Override
        public void willExit(EventObject event) {
            mainFrame.setVisible(false);
            System.out.println("Shutting down...");
            application.stopCore();
            System.out.println("Shut down");
        }
        
    }
}
