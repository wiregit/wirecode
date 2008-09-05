package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Enumeration;
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
import org.limewire.core.settings.MozillaSettings;
import org.limewire.inject.Modules;
import org.limewire.ui.swing.LimeWireSwingUiModule;
import org.limewire.ui.swing.browser.download.LimeMozillaDownloadManager;
import org.limewire.ui.swing.browser.download.LimeMozillaSingletonFactory;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.tray.TrayExitListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIComponentRegistrar;
import org.mozilla.interfaces.nsIPrefService;
import org.mozilla.xpcom.Mozilla;

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

        LimeMozillaDownloadManager mozillaDownloadManager =  localInjector.getInstance(LimeMozillaDownloadManager.class);
        
        //change default mozilla behavior
        overrideMozillaDefaults(mozillaDownloadManager);
        
        getMainFrame().setJMenuBar(new LimeMenuBar());

        LimeWireSwingUI ui = localInjector.getInstance(LimeWireSwingUI.class);
        ui.showTrayIcon();
        addExitListener(new TrayExitListener(ui.getTrayNotifier()));
        
        show(ui);        
        restoreView();
        
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

    private void overrideMozillaDefaults(LimeMozillaDownloadManager mozillaDownloadManager) {
        // lookup the preferences service by contract id.
        // by getting a proxy we do not need to run code through mozilla thread.
        nsIPrefService prefService = XPCOMUtils.getServiceProxy(
                "@mozilla.org/preferences-service;1", nsIPrefService.class);

        // set default downloads to desktop, we are going to override this with
        // our own download manager This will prevent the save dialogue from
        // opening
        prefService.getBranch("browser.download.").setBoolPref("useDownloadDir", 1);
        prefService.getBranch("browser.download.").setIntPref("folderList", 0);
        prefService.getBranch("browser.download.manager.").setBoolPref("showWhenStarting", 0);

        // setup which mime types do not prompt to download
        // this will prevent the save or open dialogue from prompting
        prefService.getBranch("browser.helperApps.neverAsk.").setCharPref("saveToDisk",
                MozillaSettings.DOWNLOAD_MIME_TYPES.getValue());

        // register our own download manager to replace the one provided by
        // mozilla
        registerMozillaComponent(mozillaDownloadManager);
    }

    private void registerMozillaComponent(LimeMozillaSingletonFactory factory) {
        nsIComponentRegistrar cr = Mozilla.getInstance().getComponentRegistrar();
        cr.registerFactory(factory.getIID(), factory.getComponentName(), factory.getCID(), factory);
    }

}
