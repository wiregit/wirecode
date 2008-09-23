package org.limewire.ui.swing;

import java.awt.Frame;
import java.awt.Image;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.application.Application;
import org.limewire.core.impl.mozilla.LimeMozillaOverrides;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.StartupSettings;
import org.limewire.service.ErrorService;
import org.limewire.ui.support.BugManager;
import org.limewire.ui.support.DeadlockSupport;
import org.limewire.ui.support.ErrorHandler;
import org.limewire.ui.swing.browser.LimeMozillaInitializer;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.SplashWindow;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LocaleUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.OSUtils;
import org.limewire.util.Stopwatch;
import org.limewire.util.SystemUtils;
import org.mozilla.browser.MozillaPanel;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeCoreGlue;
import com.limegroup.gnutella.LimeWireCore;
import com.limegroup.gnutella.LimeCoreGlue.InstallFailedException;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.LogUtils;
import com.limegroup.gnutella.util.MacOSXUtils;

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
     //   LimeWireUI limewireGUI = createLimeWire(); 
        LimeWireCore limeWireCore = createLimeWire();
        Injector injector = limeWireCore.getInjector();
        
        LimeMozillaOverrides mozillaOverrides = injector.getInstance(LimeMozillaOverrides.class);
       
        // Various tasks that can be done after core is glued & started.
        glueCore(limeWireCore);        
        validateEarlyCore(limeWireCore);
        
        // Validate any arguments or properties outside of the LW environment.
        runExternalChecks(limeWireCore, args);

        // Starts some system monitoring for deadlocks.
        DeadlockSupport.startDeadlockMonitoring();
        stopwatch.resetAndLog("Start deadlock monitor");
        
        // Installs properties.
        installProperties();
        
        // Construct the SetupManager, which may or may not be shown.
//        final SetupManager setupManager = new SetupManager(limeWireCore.getFirewallService());
//        stopwatch.resetAndLog("construct SetupManager");

        // Move from the AWT splash to the Swing splash & start early core.
        switchSplashes(awtSplash, splashImage);
        startEarlyCore(/*setupManager,*/ limeWireCore);
        
        // Initialize early UI components, display the setup manager (if necessary),
        // and ensure the save directory is valid.
        initializeEarlyUI(mozillaOverrides);
//        startSetupManager(setupManager);
        validateSaveDirectory();
        
        // Load the UI, system tray & notification handlers,
        // and hide the splash screen & display the UI.
        loadUI();
        loadTrayAndNotifications();
        hideSplashAndShowUI();
        
        // Initialize late tasks, like Icon initialization & install listeners.
        loadLateTasksForUI();
        installListenersForUI(limeWireCore);
        
        // Start the core & run any queued control requests, and load DAAP.
        startCore(limeWireCore);
        runQueuedRequests(limeWireCore);
        
        // Run any after-init tasks.
        postinit();        
    }
    
    
    /** Initializes the very early things. */
    /*
     * DO NOT CHANGE THIS WITHOUT KNOWING WHAT YOU'RE DOING.
     * PREINSTALL MUST BE DONE BEFORE ANYTHING ELSE IS REFERENCED.
     * (Because it sets the preference directory in CommonUtils.)
     */
    private void preinit() {
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
        
        // Make sure the settings directory is set.
        try {
            LimeCoreGlue.preinstall();
            stopwatch.resetAndLog("Preinstall");
        } catch(InstallFailedException ife) {
            failPreferencesPermissions();
        }
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
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable throwable) {
                if(LimeWireUtils.isTestingVersion()) {
                    ErrorService.error(throwable, "Uncaught thread error: " +thread.getName());
                }
            }
        });
        stopwatch.resetAndLog("ErrorHandler install");
        
        // Set the messaging handler so we can receive core messages
//        org.limewire.service.MessageService.setCallback(new MessageHandler());
//        stopwatch.resetAndLog("MessageHandler install");
        
        // Set the default event error handler so we can receive uncaught
        // AWT errors.
        DefaultErrorCatcher.install();
        stopwatch.resetAndLog("DefaultErrorCatcher install");
        
        if (OSUtils.isMacOSX()) {
            // Raise the number of allowed concurrent open files to 1024.
            SystemUtils.setOpenFileLimit(1024);
            stopwatch.resetAndLog("Open file limit raise");     

//            MacEventHandler.instance();
//            stopwatch.resetAndLog("MacEventHandler instance");
        }
    }
    
    /**
     * Ensures this should continue running, by checking
     * for expiration failures or startup settings. 
     */
    private void validateStartup(String[] args) {        
        // check if this version has expired.
        if (System.currentTimeMillis() > EXPIRATION_DATE) 
            failExpired();
        
        // If this is a request to launch a pmf then just do it and exit.
//        if ( args.length >= 2 && "-pmf".equals(args[0]) ) {
//            PackagedMediaFileLauncher.launchFile(args[1], false); 
//            System.exit(0);
//        }
        
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
//        ActiveLimeWireCheck activeLimeWireCheck = new ActiveLimeWireCheck(args, StartupSettings.ALLOW_MULTIPLE_INSTANCES.getValue());
//        stopwatch.resetAndLog("Create ActiveLimeWireCheck");
//        if (activeLimeWireCheck.checkForActiveLimeWire()) {
//            System.exit(0);
//        }
//        stopwatch.resetAndLog("Run ActiveLimeWireCheck");
    }
    
    /** Wires together LimeWire. */
    private LimeWireCore createLimeWire() {
        stopwatch.reset();
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireModule(), new AbstractModule() {
            @Override
            protected void configure() {
                requestStaticInjection(AppFrame.class);
            }
        });
        stopwatch.resetAndLog("Create injector");
        return injector.getInstance(LimeWireCore.class);
    }
    
    /** Wires together remaining non-Guiced pieces. */
    private void glueCore(LimeWireCore limeWireCore) {
        limeWireCore.getLimeCoreGlue().install();
        stopwatch.resetAndLog("Install core glue");

// TODO: Do we want to update the UI (which is visible at this point) ?
//        ServiceRegistry registry = limeWireCore.getServiceRegistry();
//        registry.addListener(new ServiceRegistryListener() {
//            public void initializing(final Service service) {}
//            
//            public void starting(final Service service) {
//                SwingUtilities.invokeLater(new Runnable() {
//                    public void run() {
//                        splashRef.get().setStatusText(I18n.tr("Starting {0}", I18n.tr(service.getServiceName())));
//                    }
//                });
//            }
//            
//            public void stopping(final Service service) {}
//        });
//        stopwatch.resetAndLog("add service registry listener");
    }
    
    /** Tasks that can be done after core is created, before it's started. */
    private void validateEarlyCore(LimeWireCore limeWireCore) {        
        // See if our NIODispatcher clunked out.
        if(!limeWireCore.getNIODispatcher().isRunning()) {
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
    private void runExternalChecks(LimeWireCore limeWireCore, String[] args) {        
//        ExternalControl externalControl = limeWireCore.getExternalControl();
//        stopwatch.resetAndLog("Get externalControl");
//        if(OSUtils.isMacOSX()) {
//            GURLHandler.getInstance().enable(externalControl);
//            stopwatch.resetAndLog("Enable GURL");
//            MacEventHandler.instance().enable(externalControl, this);
//            stopwatch.resetAndLog("Enable macEventHandler");
//        }
        
        // Test for preexisting LimeWire and pass it a magnet URL if one
        // has been passed in.
//        if (args.length > 0 && !args[0].equals("-startup")) {
//            String arg = ExternalControl.preprocessArgs(args);
//            stopwatch.resetAndLog("Preprocess args");
//            externalControl.enqueueControlRequest(arg);
//            stopwatch.resetAndLog("Enqueue control req");
//        }
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
    private void startEarlyCore(/*SetupManager setupManager, */LimeWireCore limeWireCore) {        
        // Add this running program to the Windows Firewall Exceptions list
        boolean inFirewallException = limeWireCore.getFirewallService().addToFirewall();
        stopwatch.resetAndLog("add firewall exception");
        
        if(!inFirewallException /*&& !setupManager.shouldShowFirewallWindow()*/) {
            limeWireCore.getLifecycleManager().loadBackgroundTasks();
            stopwatch.resetAndLog("load background tasks");
        }
    }
    
    /** Switches from the AWT splash to the Swing splash. */
    private void switchSplashes(final Frame awtSplash, final Image splashImage) {
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                splashRef.set(new SplashWindow(splashImage, Locale.getDefault(), 4));
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
    
    /** Initializes any early UI tasks, such as HTML loading & the Bug Manager. 
     * @param mozillaOverrides 
     * @param mozillaOverrides */
    private void initializeEarlyUI(LimeMozillaOverrides mozillaOverrides) {
        // Load up the HTML engine.
        splashRef.get().setStatusText(I18n.tr("Loading HTML Engine..."));
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
        
        splashRef.get().setStatusText(I18n.tr("Loading Browser..."));
        // Not pretty but Mozilla initialization errors should not crash the
        // program
        if (OSUtils.isWindows() || OSUtils.isMacOSX() || OSUtils.isLinux()) {
            try {
                LimeMozillaInitializer.initialize();
                mozillaOverrides.overrideMozillaDefaults();
            } catch (Exception e) {
                LOG.error("Mozilla initialization failed");
            }
        }
        stopwatch.resetAndLog("Load XUL Library Path");
        SwingUtils.invokeAndWait(new Runnable() {
            public void run() {
                stopwatch.resetAndLog("enter evt queue");
                new MozillaPanel();
                stopwatch.resetAndLog("Load MozillaPanel");
            }
        });
        
        stopwatch.resetAndLog("return from evt queue");
    }
    
    /** Starts the SetupManager, if necessary. */
//    private void startSetupManager(final SetupManager setupManager) {        
//        // Run through the initialization sequence -- this must always be
//        // called before GUIMediator constructs the LibraryTree!
//        GUIMediator.safeInvokeAndWait(new Runnable() {
//            public void run() {
//                stopwatch.resetAndLog("event evt queue");
//                // Then create the setup manager if needed.
//                setupManager.createIfNeeded();     
//                stopwatch.resetAndLog("create setupManager if needed");
//            }
//        });
//        stopwatch.resetAndLog("return from evt queue");
//    }
    
    /** Ensures the save directory is valid. */
    private void validateSaveDirectory() {        
//        // Make sure the save directory is valid.
//        SaveDirectoryHandler.validateSaveDirectoryAndPromptForNewOne();
//        stopwatch.resetAndLog("check save directory validity");
    }
    
    /** Loads the UI. */
    private void loadUI() {
        splashRef.get().setStatusText(I18n.tr("Loading User Interface..."));
        stopwatch.resetAndLog("update splash for UI");
        
        Application.launch(AppFrame.class, new String[0]);
        
        SwingUtils.invokeAndWait(new Runnable() {
            public void run() {
                splashRef.get().dispose();
                if(!AppFrame.isStarted()) {
                    System.exit(1);
                }
            }
        });
//
//        // To prevent deadlocks, the GUI must be constructed in the Swing thread.
//        // (Except on OS X, which is strange.)
//        if (OSUtils.isMacOSX()) {
//            GUIMediator.instance();
//            stopwatch.resetAndLog("OSX GUIMediator instance");
//        } else {
//            GUIMediator.safeInvokeAndWait(new Runnable() {
//                public void run() {
//                    stopwatch.resetAndLog("enter evt queue");
//                    GUIMediator.instance();
//                    stopwatch.resetAndLog("GUImediator instance");
//                }
//            });
//            stopwatch.resetAndLog("return from evt queue");
//        }
//        
//        GUIMediator.setSplashScreenString(I18n.tr("Loading Core Components..."));
//        stopwatch.resetAndLog("update splash for core");
    }
    
    /** Loads the system tray & other notifications. */
    private void loadTrayAndNotifications() {        
//        // Create the user desktop notifier object.
//        // This must be done before the GUI is made visible,
//        // otherwise the user can close it and not see the
//        // tray icon.
//        GUIMediator.safeInvokeAndWait(new Runnable() {
//                public void run() {
//                    stopwatch.resetAndLog("enter evt queue");
//                    
//                    NotifyUserProxy.instance();
//                    stopwatch.resetAndLog("NotifYUserProxy instance");
//                    
//                    if (!ApplicationSettings.DISPLAY_TRAY_ICON.getValue())
//                        NotifyUserProxy.instance().hideTrayIcon();
//                    
//                    SettingsWarningManager.checkTemporaryDirectoryUsage();
//                    SettingsWarningManager.checkSettingsLoadSaveFailure();
//                    
//                    stopwatch.resetAndLog("end notify runner");
//                }
//        });
//        stopwatch.resetAndLog("return from evt queue");
    }
    
    /** Hides the splash screen and sets the UI for allowing viz. */
    private void hideSplashAndShowUI() {        
//        // Hide the splash screen and recycle its memory.
//        if(!isStartup) {
//            SplashWindow.instance().dispose();
//            stopwatch.resetAndLog("hide splash");
//        }
//        
//        GUIMediator.allowVisibility();
//        stopwatch.resetAndLog("allow viz");
//        
//        // Make the GUI visible.
//        if(!isStartup) {
//            GUIMediator.setAppVisible(true);
//            stopwatch.resetAndLog("set app visible TRUE");
//        } else {
//            GUIMediator.startupHidden();
//            stopwatch.resetAndLog("start hidden");
//        }
    }
    
    /** Runs any late UI tasks, such as initializing Icons, I18n support. */
    private void loadLateTasksForUI() {        
//        // Initialize IconManager.
//        GUIMediator.setSplashScreenString(I18n.tr("Loading Icons..."));
//        GUIMediator.safeInvokeAndWait(new Runnable() {
//            public void run() {
//                IconManager.instance();
//            }
//        });
//        stopwatch.resetAndLog("IconManager instance");
//
        // Touch the I18N stuff to ensure it loads properly.
        splashRef.get().setStatusText(I18n.tr("Loading Internationalization Support..."));
        I18NConvert.instance();
        stopwatch.resetAndLog("I18nConvert instance");
    }
    
    /** Sets up any listeners for the UI. */
    private void installListenersForUI(LimeWireCore limeWireCore) {        
//        limeWireCore.getFileManager().addFileEventListener(new FileManagerWarningManager(NotifyUserProxy.instance()));
//        limeWireCore.getFileManager().addFileEventListener(LibraryMediator.instance());
    }
    
    /** Starts the core. */
    private void startCore(LimeWireCore limeWireCore) {
        // Start the backend threads.  Note that the GUI is not yet visible,
        // but it needs to be constructed at this point  
        limeWireCore.getLifecycleManager().start();
        stopwatch.resetAndLog("lifecycle manager start");
        
        if (!ConnectionSettings.DISABLE_UPNP.getValue()) {
            limeWireCore.getUPnPManager().start();
            stopwatch.resetAndLog("start UPnPManager");
        }
        
//        // Instruct the gui to perform tasks that can only be performed
//        // after the backend has been constructed.
//        GUIMediator.instance().coreInitialized();        
//        stopwatch.resetAndLog("core initialized");
    }
    
    /** Runs control requests that we queued early in initializing. */
    private void runQueuedRequests(LimeWireCore limeWireCore) {        
        // Activate a download for magnet URL locally if one exists
        limeWireCore.getExternalControl().runQueuedControlRequest();
        stopwatch.resetAndLog("run queued control req");
    }
    
    /** Runs post initialization tasks. */
    private void postinit() {
//        
//        // Tell the GUI that loading is all done.
//        GUIMediator.instance().loadFinished();
//        stopwatch.resetAndLog("load finished");
//        
//        // update the repaintInterval after the Splash is created,
//        // so that the splash gets the smooth animation.
//        if(OSUtils.isMacOSX())
//            UIManager.put("ProgressBar.repaintInterval", new Integer(500));
//        
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
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(null,
                            new MultiLineLabel(msgKey, 300),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (InterruptedException ignored) {
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if(cause instanceof RuntimeException)
                throw (RuntimeException)cause;
            if(cause instanceof Error)
                throw (Error)cause;
            throw new RuntimeException(cause);
        }
        System.exit(1);
    }
    
}

