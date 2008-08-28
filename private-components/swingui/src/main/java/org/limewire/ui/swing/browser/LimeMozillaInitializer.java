package org.limewire.ui.swing.browser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.Expand;
import org.limewire.io.IOUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.mozilla.browser.IMozillaWindow;
import org.mozilla.browser.IMozillaWindowFactory;
import org.mozilla.browser.MozillaConfig;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaWindow;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.WindowCreator;
import org.mozilla.interfaces.nsIComponentRegistrar;
import org.mozilla.interfaces.nsIPrefBranch;
import org.mozilla.interfaces.nsIPrefService;
import org.mozilla.xpcom.Mozilla;

import com.limegroup.gnutella.LimeWireCore;

public class LimeMozillaInitializer {

    private static final Log LOG = LogFactory.getLog(LimeMozillaInitializer.class);

    private LimeMozillaInitializer() {
    }

    public static void initialize(LimeWireCore limeWireCore) {
        File xulInstallPath = new File(CommonUtils.getUserSettingsDir(), "/browser");
        // Check to see if the correct version of XUL exists.
        File xulFile = new File(xulInstallPath, "xul-v2.0b2-do-not-remove");
        if (!xulFile.exists()) {
            if (LOG.isDebugEnabled())
                LOG.debug("unzip xulrunner to " + xulInstallPath);
            FileUtils.deleteRecursive(xulInstallPath);
            InputStream in = null;
            try {
                in = new BufferedInputStream(CommonUtils.getResourceStream(getResourceName()));
                Expand.expandFile(in, xulInstallPath, true, null);
                xulFile.createNewFile();
            } catch (IOException e) {
                ErrorService.error(e);
            } finally {
                IOUtils.close(in);
            }
        }

        String newLibraryPath = System.getProperty("java.library.path") + File.pathSeparator
                + xulInstallPath.getAbsolutePath();
        System.setProperty("java.library.path", newLibraryPath);

        MozillaConfig.setXULRunnerHome(xulInstallPath);
        File profileDir = new File(CommonUtils.getUserSettingsDir(), "/mozilla-profile");
        profileDir.mkdirs();
        MozillaConfig.setProfileDir(profileDir);
        WindowCreator.setWindowFactory(new IMozillaWindowFactory() {
            @Override
            public IMozillaWindow create(boolean attachNewBrowserOnCreation) {
                MozillaPopupWindow popupWindow = new MozillaPopupWindow(attachNewBrowserOnCreation);
                MozillaWindow window = new MozillaWindow(popupWindow);
                popupWindow.setContainerWindow(window);
                return window;
            }
        });
        MozillaInitialization.initialize();

        overrideMozillaComponents(limeWireCore);

        if (LOG.isDebugEnabled())
            LOG.debug("Moz Summary: " + MozillaConfig.getConfigSummary());
    }

    private static void overrideMozillaComponents(LimeWireCore limeWireCore) {
        // addDownloadListener(limeWireCore);
        updatePreferences();
        replaceDownloadManager(limeWireCore);
    }

    private static void updatePreferences() {
        nsIPrefService prefService = XPCOMUtils.getServiceProxy(
                "@mozilla.org/preferences-service;1", nsIPrefService.class);

        nsIPrefBranch prefBranch = prefService.getBranch("browser.download.");
        boolean useDownloadDir = prefBranch.getBoolPref("useDownloadDir");
        prefService.getBranch("browser.download.").setBoolPref("useDownloadDir", 1);
        prefService.getBranch("browser.download.").setIntPref("folderList", 2);
        prefService.getBranch("browser.download.").setCharPref("dir",
                "/home/pvertenten/Desktop/testmoz");
        useDownloadDir = prefService.getBranch("browser.download.").getBoolPref("useDownloadDir");
    }

    private static void replaceDownloadManager(LimeWireCore limeWireCore) {
        register(new MozillaDownloadManager(limeWireCore.getDownloadServices(),
                limeWireCore.getRemoteFileDescFactory()));
        register(new NsIDownloadManagerUIImpl());// removes download list screen
        // register(new MozillaFilePicker());
    }

    // private static void addDownloadListener(LimeWireCore limeWireCore) {
    // register(new NsIDownloadManagerUIImpl());// removes download list screen
    //
    // nsIDownloadManager nsidownloadManager = XPCOMUtils.getServiceProxy(
    // "@mozilla.org/download-manager;1", nsIDownloadManager.class);
    // nsidownloadManager.cleanUp();
    //
    // DownloadManager downloadManager = limeWireCore.getDownloadManager();
    // nsidownloadManager.addListener(new
    // MozillaDownloadManagerListener(downloadManager));
    // }

    private static String getResourceName() {
        if (OSUtils.isWindows()) {
            return "xulrunner-win32.zip";
        } else if (OSUtils.isMacOSX()) {
            return "xulrunner_macosx-universal.zip";
        } else if (OSUtils.isLinux()) {
            return "xulrunner-linux.zip";
        } else {
            throw new IllegalStateException("no resource for OS: " + OSUtils.getOS());
        }
    }

    private static void register(NsISelfReferencingFactory factory) {
        nsIComponentRegistrar cr = Mozilla.getInstance().getComponentRegistrar();
        cr.registerFactory(factory.getIID(), factory.getComponentName(), factory.getCID(), factory);
    }

}
