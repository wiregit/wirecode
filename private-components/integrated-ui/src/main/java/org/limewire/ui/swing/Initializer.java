package org.limewire.ui.swing;

import java.awt.Frame;
import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.application.Application;
import org.limewire.core.impl.mozilla.LimeMozillaOverrides;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.IOUtils;
import org.limewire.net.FirewallService;
import org.limewire.nio.NIODispatcher;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;
import org.limewire.ui.support.BugManager;
import org.limewire.ui.support.DeadlockSupport;
import org.limewire.ui.support.ErrorHandler;
import org.limewire.ui.support.FatalBugManager;
import org.limewire.ui.swing.browser.LimeMozillaInitializer;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.SplashWindow;
import org.limewire.ui.swing.event.ExceptionPublishingSwingEventService;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.settings.StartupSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LocaleUtils;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.ui.swing.wizard.IntentDialog;
import org.limewire.util.CommonUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.OSUtils;
import org.limewire.util.Stopwatch;
import org.limewire.util.SystemUtils;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;
import org.mozilla.browser.MozillaPanel;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.limegroup.gnutella.ActiveLimeWireCheck;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeCoreGlue;
import com.limegroup.gnutella.UPnPManager;
import com.limegroup.gnutella.ActiveLimeWireCheck.ActiveLimeWireException;
import com.limegroup.gnutella.LimeCoreGlue.InstallFailedException;
import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.LogUtils;

/** Initializes (creates, starts, & displays) the LimeWire Core & UI. */
public final class Initializer {

    /** The log -- set only after Log4J can be determined. */
    private final Log LOG;
    
    /** Refuse to start after this date */
    private final long EXPIRATION_DATE = Long.MAX_VALUE;
    
    /** True if is running from a system startup. */
    private volatile boolean isStartup = false;
    
    /** The start memory -- only set if debugging. */
    private long startMemory;
    
    /** A stopwatch for debug logging. */
    private final Stopwatch stopwatch;
    
    /** The SplashWindow reference. */
    private final AtomicReference<SplashWindow> splashRef = new AtomicReference<SplashWindow>();
    
    // Providers so we don't guarantee early creation, let it be as lazy as possible.
    @Inject private Provider<ExternalControl> externalControl;
    @Inject private Provider<FirewallService> firewallServices;
    @Inject private Provider<LifecycleManager> lifecycleManager;
    @Inject private Provider<LimeCoreGlue> limeCoreGlue;
    @Inject private Provider<NIODispatcher> nioDispatcher;
    @Inject private Provider<UPnPManager> upnpManager;
    @Inject private Provider<LimeMozillaOverrides> mozillaOverrides;
    
    Initializer() {
        // If Log4J is available then remove the NoOpLog
        if (LogUtils.isLog4JAvailable()) {
            System.getProperties().remove("org.apache.commons.logging.Log");
        }
        
        LOG = LogFactory.getLog(Initializer.class);
        
        if(LOG.isTraceEnabled()) {
            startMemory = Runtime.getRuntime().totalMemory()
                        - Runtime.getRuntime().freeMemory();
            LOG.trace("START Initializer, using: " + startMemory + " memory");
        }
        
        stopwatch = new Stopwatch(LOG);
    }
    
    /**
     * Initializes all of the necessary application classes.
     * 
     * If this throws any exceptions, then LimeWire was not able to construct
     * properly and must be shut down.
     */
    void initialize(String args[], Frame awtSplash, Image splashImage) throws Throwable { 
        // ** THE VERY BEGINNING -- DO NOT ADD THINGS BEFORE THIS **
        preinit();
        
        // Various startup tasks...
        setupCallbacksAndListeners();     
        validateStartup(args);
        
        // Creates LimeWire itself.
        Injector injector = createLimeWire();
       
        // Various tasks that can be done after core is glued & started.
        glueCore();        
        validateEarlyCore();
        
        // Validate any arguments or properties outside of the LW environment.
        runExternalChecks(args, injector);

        // Starts some system monitoring for deadlocks.
        DeadlockSupport.startDeadlockMonitoring();
        stopwatch.resetAndLog("Start deadlock monitor");
        
        // Installs properties.
        installProperties();
        
        // show nifty alpha info
//        showAlphaInfo();
        
        //must agree not to use LW for copyright infringement on first running
        confirmIntent(awtSplash);
        
        // Move from the AWT splash to the Swing splash & start early core.
        //assuming not showing splash screen if there are program arguments
        switchSplashes(awtSplash, splashImage, LimeWireUtils.isPro());
        
        startEarlyCore();
        
        // Initialize early UI components, display the setup manager (if necessary),
        // and ensure the save directory is valid.
        initializeEarlyUI();
        
        // Load the UI, system tray & notification handlers,
        // and hide the splash screen & display the UI.
        loadUI();
        
        enablePreferences();
        
        SettingsWarningManager.checkTemporaryDirectoryUsage();
        SettingsWarningManager.checkSettingsLoadSaveFailure();        
        
        // Start the core & run any queued control requests, and load DAAP.
        startCore();
        runQueuedRequests();
        
        // Run any after-init tasks.
        postinit();        
    }
    
//    private void showAlphaInfo() {
//        final String msg = "Welcome to the LimeWire 5 Alpha!\n\n"
//                   + "Here's some important information about the alpha...\n"
//                   + " - There are bugs.  Lots.  It's because this is an alpha.\n"
//                   + "   We're confident the program is stable (it's not going\n"
//                   + "   to delete your computer or anything), but you will run\n"
//                   + "   into some internal errors.\n"
//                   + " - It isn't finished.  We're very actively working on\n"
//                   + "   changes, tweaks, rewrites, and all sorts of things.\n"
//                   + " - Everything can (and probably will) change.  Don't\n"
//                   + "   take inclusion or exclusion of a certain feature to\n"
//                   + "   mean that we're adding or dropping it.  It's probably\n"
//                   + "   just an oversight.\n\n"
//                   + " Thanks!\n"
//                   + "     The LimeWire Team";                   
//        SwingUtils.invokeAndWait(new Runnable() {
//            public void run() {
//                JOptionPane.showMessageDialog(null,
//                        msg,
//                        "LimeWire 5 Alpha Info",
//                        JOptionPane.INFORMATION_MESSAGE);
//            }
//        });
//    }
    
    
    /** 
     * Shows legal conditions and exits if the user does not agree to them.
     * 
     *  Takes a parameter for the splash screen so it can hide it if
     *   the intent dialogue needs to be shown to avoid a troublesome situation
     *   where the splash screen actually covers the shown intent dialogue. 
     */
    private void confirmIntent(final Frame awtSplash) {
        File versionFile = new File(CommonUtils.getUserSettingsDir(), "versions.props");
        Properties properties = new Properties();        
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(versionFile);
            properties.load(inputStream);
        } catch (IOException iox) {
        } finally {
            IOUtils.close(inputStream);
        }

		updateVersionsUsed(properties);
		
        String exists = properties.getProperty(LimeWireUtils.getLimeWireVersion());
        if (exists == null || !exists.equals("true")) {
            SwingUtils.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    if (awtSplash != null) {
                        awtSplash.setVisible(false);
                    }
                    //must warn users about sharing documents again after an upgrade
                    SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD.setValue(true);
                    boolean confirmed = new IntentDialog(LimeWireUtils.getLimeWireVersion()).confirmLegal();
                    if (!confirmed) {
                        System.exit(0);
                    }
                    
                    if (awtSplash != null) {
                        awtSplash.setVisible(true);
                    }
                }
            });
            
            properties.put(LimeWireUtils.getLimeWireVersion(), "true");
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(versionFile);
                properties.store(outputStream, "Started & Ran Versions");
            } catch (IOException ignored) {
            } finally {
                IOUtils.close(outputStream);
            }
        }
    }

    /**
     * Fills in any previously run versions from the versions.props file. 
     */
    private void updateVersionsUsed(Properties properties) {
        for(Object key : properties.keySet()) {
		    String versionString = key.toString();
		    try {
                Version version = new Version(versionString);
                InstallSettings.PREVIOUS_RAN_VERSIONS.add(version.getVersion());
            } catch (VersionFormatException e) {
               //do nothing
            }
		}
    }
  

    /** Initializes the very early things. */
    /*
     * DO NOT CHANGE THIS WITHOUT KNOWING WHAT YOU'RE DOING.
     * PREINSTALL MUST BE DONE BEFORE ANYTHING ELSE IS REFERENCED.
     * (Because it sets the preference directory in CommonUtils.)
     */
    private void preinit() {
        // Make sure the settings directory is set.
        try {
            LimeCoreGlue.preinstall();
            stopwatch.resetAndLog("Preinstall");
        } catch(InstallFailedException ife) {
            GuiUtils.hideAndDisposeAllWindows();
            failPreferencesPermissions();
        }

        // Before anything, set a default L&F, so that
        // if an error occurs, we can display the error
        // message with the right L&F.
        SwingUtils.invokeLater(new Runnable() {
            public void run() {
                String name = UIManager.getSystemLookAndFeelClassName();                
                if(OSUtils.isLinux()) {
                    //mozswing on linux is not compatible with the gtklook and feel in jvms less than 1.7
                    //forcing cross platform look and feel for linux.
                    name = UIManager.getCrossPlatformLookAndFeelClassName();
                }           
                try {
                    UIManager.setLookAndFeel(name);
                } catch(Throwable ignored) {}
            }
        });        
    }
    
    /** Installs all callbacks & listeners. */
    private void setupCallbacksAndListeners() {
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                BugManager.instance();
            }
        });
        // Set the error handler so we can receive core errors.
        ErrorService.setErrorCallback(new ErrorHandler());

        // set error handler for uncaught exceptions originating from non-LW
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandlerImpl());
        stopwatch.resetAndLog("ErrorHandler install");
        
        // Set the messaging handler so we can receive core messages
        MessageService.setCallback(new MessageHandler());
        stopwatch.resetAndLog("MessageHandler install");
        
        // Set the default event error handler so we can receive uncaught
        // AWT errors.
        DefaultErrorCatcher.install();
        stopwatch.resetAndLog("DefaultErrorCatcher install");

        //Enable the EDT event service (used by the EventBus library) that publishes to LW error handling
        ExceptionPublishingSwingEventService.install();
        stopwatch.resetAndLog("DefaultErrorCatcher install");
        
        if (OSUtils.isMacOSX()) {
            // Raise the number of allowed concurrent open files to 1024.
            SystemUtils.setOpenFileLimit(1024);
            stopwatch.resetAndLog("Open file limit raise");     

            MacEventHandler.instance();
            stopwatch.resetAndLog("MacEventHandler instance");
        }
    }
    
    /**
     * Ensures this should continue running, by checking
     * for expiration failures or startup settings. 
     */
    private void validateStartup(String[] args) {        
        // check if this version has expired.
        if (System.currentTimeMillis() > EXPIRATION_DATE) {
            GuiUtils.hideAndDisposeAllWindows();
            failExpired();
        }
        
        // Yield so any other events can be run to determine
        // startup status, but only if we're going to possibly
        // be starting...
        if(StartupSettings.RUN_ON_STARTUP.getValue()) {
            stopwatch.reset();
            Thread.yield();
            stopwatch.resetAndLog("Thread yield");
        }
        
        if (args.length >= 1 && "-startup".equals(args[0]))
            isStartup = true;
        
        if (isStartup) {
            args = null; // reset for later Active check
            // if the user doesn't want to start on system startup, exit the
            // JVM immediately
            if(!StartupSettings.RUN_ON_STARTUP.getValue())
                System.exit(0);
        }
        
        // Exit if another LimeWire is already running...
        if(!StartupSettings.ALLOW_MULTIPLE_INSTANCES.getValue()) {
            ActiveLimeWireCheck activeCheck = ActiveLimeWireCheck.instance();
            stopwatch.resetAndLog("Create ActiveLimeWireCheck");
            try {
                if(activeCheck.checkForActiveLimeWire(args))
                    System.exit(0);
            } catch(ActiveLimeWireException e) {
                LOG.debug(e);
                stopwatch.resetAndLog("Warn user about running instance");
                GuiUtils.hideAndDisposeAllWindows();
                if(!warnAlreadyRunning())
                    System.exit(0);
            }
            stopwatch.resetAndLog("Run ActiveLimeWireCheck");
        }
    }
    
    /** Wires together LimeWire. */
    private Injector createLimeWire() {
        stopwatch.reset();
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireModule(), new AbstractModule() {
            @Override
            protected void configure() {
                requestStaticInjection(AppFrame.class);
                requestInjection(Initializer.this);
            }
        });
        stopwatch.resetAndLog("Create injector");
        return injector;
    }
    
    /** Wires together remaining non-Guiced pieces. */
    private void glueCore() {
        limeCoreGlue.get().install();
        stopwatch.resetAndLog("Install core glue");
    }
    
    /** Tasks that can be done after core is created, before it's started. */
    private void validateEarlyCore() {        
        // See if our NIODispatcher clunked out.
        if(!nioDispatcher.get().isRunning()) {
            GuiUtils.hideAndDisposeAllWindows();
            failInternetBlocked();
        }
        stopwatch.resetAndLog("Check for NIO dispatcher");
    }    

    /**
     * Initializes any code that is dependent on external controls.
     * Specifically, GURLHandler & MacEventHandler on OS X,
     * ensuring that multiple LimeWire's can't run at once,
     * and processing any arguments that were passed to LimeWire.
     */ 
    private void runExternalChecks(String[] args, Injector injector) {        
        stopwatch.resetAndLog("Get externalControl");
        if(OSUtils.isMacOSX()) {
            GURLHandler.getInstance().enable(externalControl.get());
            stopwatch.resetAndLog("Enable GURL");
            injector.injectMembers(MacEventHandler.instance());
            stopwatch.resetAndLog("Enable macEventHandler");
        }
        
        // Test for preexisting LimeWire and pass it a magnet URL if one
        // has been passed in.
        if (args.length > 0 && !args[0].equals("-startup")) {
            String arg = ExternalControl.preprocessArgs(args);
            stopwatch.resetAndLog("Preprocess args");
            externalControl.get().enqueueControlRequest(arg);
            stopwatch.resetAndLog("Enqueue control req");
        }
    }
    
    /** Installs any system properties. */
    private void installProperties() {        
        System.setProperty("http.agent", LimeWireUtils.getHttpServer());
        stopwatch.resetAndLog("set system properties");
        
        if (OSUtils.isMacOSX()) {
            System.setProperty("user.fullname", MacOSXUtils.getUserName()); // for DAAP
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            stopwatch.resetAndLog("set OSX properties");
        }

        SwingUtils.invokeAndWait(new Runnable() {
            public void run() {
                LocaleUtils.setLocaleFromPreferences();                
                LocaleUtils.validateLocaleAndFonts();
            }
        });
        stopwatch.resetAndLog("set locale");
    }
    
    /** Starts any early core-related functionality. */
    private void startEarlyCore() {        
        // Add this running program to the Windows Firewall Exceptions list
        boolean inFirewallException = firewallServices.get().addToFirewall();
        stopwatch.resetAndLog("add firewall exception");
        
        if(!inFirewallException) {
            lifecycleManager.get().loadBackgroundTasks();
            stopwatch.resetAndLog("load background tasks");
        }
    }
    
    /** Switches from the AWT splash to the Swing splash. */
    private void switchSplashes(final Frame awtSplash, final Image splashImage, final boolean isPro) {
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                splashRef.set(new SplashWindow(splashImage, isPro, LocaleUtils.getCurrentLocale(), 4));
                if(!isStartup) {
                    splashRef.get().begin();
                    stopwatch.resetAndLog("begin splash window");
                }
            }
        });
        
        if(awtSplash != null) {
            awtSplash.dispose();
            stopwatch.resetAndLog("dispose AWT splash");
        }
    }
    
    /**
     * Initializes any early UI tasks, such as HTML loading & the Bug Manager.
     */
    private void initializeEarlyUI() {
        // Load up the HTML engine.
        splashRef.get().setStatusText(I18n.tr("Muddling Mint..."));        //html engine
        stopwatch.resetAndLog("update splash for HTML engine");

        SwingUtils.invokeAndWait(new Runnable() {
            public void run() {
                stopwatch.resetAndLog("enter evt queue");
                JLabel label = new JLabel();
                // setting font and color to null to minimize generated css
                // script, which causes a parser exception under circumstances
                label.setFont(null);
                label.setForeground(null);
                BasicHTML.createHTMLView(label, "<html>.</html>");
                stopwatch.resetAndLog("create HTML view");
            }
        });
        stopwatch.resetAndLog("return from evt queue");
        
        splashRef.get().setStatusText(I18n.tr("Scouring NYC for Limes..."));           //loading browser
        // Not pretty but Mozilla initialization errors should not crash the
        // program
        if (LimeMozillaInitializer.shouldInitialize()) {
            // See LWC-2860 for why we change Turkish -> English.
            // If MozSwing ever fixes this for us, we can remove this workaround.
            Locale locale = Locale.getDefault();
            if(locale.getLanguage().equals("tr")) {
                Locale.setDefault(Locale.ENGLISH);
            }
            try {
                LimeMozillaInitializer.initialize();
                mozillaOverrides.get().overrideMozillaDefaults();
            } catch (Exception e) {
                // If it failed, don't keep the wrong locale active.
                Locale.setDefault(locale);
                LOG.error("Mozilla initialization failed");
            }
            
            stopwatch.resetAndLog("Load XUL Library Path");
            SwingUtils.invokeAndWait(new Runnable() {
                public void run() {
                    stopwatch.resetAndLog("enter evt queue");
                    new MozillaPanel();
                    stopwatch.resetAndLog("Load MozillaPanel");
                }
            });
        }
        
        stopwatch.resetAndLog("return from evt queue");
    }
    
    /** Loads the UI. */
    private void loadUI() {
        splashRef.get().setStatusText(I18n.tr("Squeezing Limes..."));        //loading user interface
        stopwatch.resetAndLog("update splash for UI");
        
        DefaultErrorCatcher.storeCaughtBugs();
        String[] launchParams = isStartup ? new String[] { AppFrame.STARTUP } : new String[0];
        Application.launch(AppFrame.class, launchParams);
        // Initialize late tasks, like Icon initialization & install listeners.
        loadLateTasksForUI();
        
        SwingUtils.invokeAndWait(new Runnable() {
            public void run() {
                splashRef.get().dispose();
                splashRef.set(null);
                List<Throwable> caughtBugs = DefaultErrorCatcher.getAndResetStoredBugs();
                if(!AppFrame.isStarted()) {
                    // Report the last bug that caused us to fail.
                    assert caughtBugs.size() > 0;
                    FatalBugManager.handleFatalBug(caughtBugs.get(caughtBugs.size()-1));
                } else {
                    for(Throwable throwable : caughtBugs) {
                        ErrorService.error(throwable, "Startup Error");
                    }
                }
            }
        });
    }  
    
    private void enablePreferences() {        
        if (OSUtils.isMacOSX()) {
            MacEventHandler.instance().enablePreferences();
        }
    }
    
    /** Runs any late UI tasks, such as initializing Icons, I18n support. */
    private void loadLateTasksForUI() {
        // Touch the I18N stuff to ensure it loads properly.
        splashRef.get().setStatusText(I18n.tr("Prepping Mojitos..."));  //other languages?
        I18NConvert.instance();
        stopwatch.resetAndLog("I18nConvert instance");
    }
    
    /** Starts the core. */
    private void startCore() {
        // Start the backend threads.  Note that the GUI is not yet visible,
        // but it needs to be constructed at this point  
        lifecycleManager.get().start();
        stopwatch.resetAndLog("lifecycle manager start");
        
        if (!ConnectionSettings.DISABLE_UPNP.getValue()) {
            upnpManager.get().start();
            stopwatch.resetAndLog("start UPnPManager");
        }
    }
    
    /** Runs control requests that we queued early in initializing. */
    private void runQueuedRequests() {        
        // Activate a download for magnet URL locally if one exists
        externalControl.get().runQueuedControlRequest();
        stopwatch.resetAndLog("run queued control req");
    }
    
    /** Runs post initialization tasks. */
    private void postinit() {
        if(LOG.isTraceEnabled()) {
            long stopMemory = Runtime.getRuntime().totalMemory()
                            - Runtime.getRuntime().freeMemory();
            LOG.trace("STOP Initializer, using: " + stopMemory +
                      " memory, consumed: " + (stopMemory - startMemory));
        }
    }
    
    /**
     * Sets the startup property to be true.
     */
    void setStartup() {
        isStartup = true;
    }
    
    /** Fails because alpha expired. */
    private void failExpired() {
        fail(I18n.tr("This Alpha version has expired.  Press Ok to exit. "));
    }
    
    /** Fails because internet is blocked. */
    private void failInternetBlocked() {
        fail(I18n
                .tr("LimeWire was unable to initialize and start. This is usually due to a firewall program blocking LimeWire\'s access to the internet or loopback connections on the local machine. Please allow LimeWire access to the internet and restart LimeWire."));
    }
    
    /** Fails because preferences can't be set. */
    private void failPreferencesPermissions() {
        fail(I18n.tr("LimeWire could not create a temporary preferences folder.\n\nThis is generally caused by a lack of permissions.  Please make sure that LimeWire (and you) have access to create files/folders on your computer.  If the problem persists, please visit www.limewire.com and click the \'Support\' link.\n\nLimeWire will now exit.  Thank You."));
    }
   
    /** Shows a msg & fails. */
    private void fail(final String msgKey) {
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null,
                        new MultiLineLabel(msgKey, 300),
                        I18n.tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        System.exit(1);
    }
    
    /**
     * Warns the user that another instance of LimeWire appears to be
     * running and that it should be shut down before proceeding.
     * 
     * @return true if the user chooses to proceed.
     */
    private boolean warnAlreadyRunning() {
        final AtomicInteger response =
            new AtomicInteger(JOptionPane.CANCEL_OPTION);
        final String message = I18n.tr("Another instance of LimeWire " +
                "appears to be running. Please completely shut down all " +
                "other instances of LimeWire before continuing. If this " +
                "problem persists, please restart your computer.");
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                response.set(JOptionPane.showConfirmDialog(null,
                        new MultiLineLabel(message, 300),
                        I18n.tr("LimeWire is already running"),
                        JOptionPane.OK_CANCEL_OPTION));
            }
        });
        return response.get() == JOptionPane.OK_OPTION;
    }
}

