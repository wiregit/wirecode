package org.limewire.ui.swing.browser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.MozillaSettings;
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
        // addDownloadListener(limeWireCore); TODO remove after we know we no
        // longer need to listen to the mozilla downloader
        updatePreferences();
        replaceDownloadManager(limeWireCore);
    }

    private static void updatePreferences() {
        nsIPrefService prefService = XPCOMUtils.getServiceProxy(
                "@mozilla.org/preferences-service;1", nsIPrefService.class);

        // set default downloads to desktop, we are going ot override this with
        // our own download manager
        prefService.getBranch("browser.download.").setBoolPref("useDownloadDir", 1);
        prefService.getBranch("browser.download.").setIntPref("folderList", 0);

        // setup which mime types do not prompt to download
        prefService.getBranch("browser.helperApps.neverAsk.").setCharPref("saveToDisk",
                MozillaSettings.DOWNLOAD_MIME_TYPES.getValue());
    }

    private static void replaceDownloadManager(LimeWireCore limeWireCore) {
        register(new LimeMozillaDownloadManager(limeWireCore.getDownloadServices(), limeWireCore
                .getRemoteFileDescFactory()));
        register(new LimeMozillaDownloadManagerUIImpl());// removes download
        // list screen
    }

    // TODO Remove this commented out code after it is decided we won't be
    // listening
    // to the mozilla downloads

    // private static void addDownloadListener(LimeWireCore limeWireCore) {
    // register(new LimeMozillaDownloadManagerUIImpl());// removes download list
    // screen
    //
    // nsIDownloadManager nsidownloadManager = XPCOMUtils.getServiceProxy(
    // "@mozilla.org/download-manager;1", nsIDownloadManager.class);
    // nsidownloadManager.cleanUp();
    //
    // DownloadServices downloadServices = limeWireCore.getDownloadServices();
    // nsidownloadManager.addListener(new
    // LimeMozillaDownloadManagerListener(downloadServices));
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

    private static void register(LimeMozillaSelfReferencingFactory factory) {
        nsIComponentRegistrar cr = Mozilla.getInstance().getComponentRegistrar();
        cr.registerFactory(factory.getIID(), factory.getComponentName(), factory.getCID(), factory);
    }

}
