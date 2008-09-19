package org.limewire.core.impl.mozilla;

import org.limewire.core.settings.MozillaSettings;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIPrefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This class overrides mozilla defaults with values we want it to run with
 * inside of limewire.
 */
@Singleton
public class LimeMozillaOverrides {

    private static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    @Inject
    public LimeMozillaOverrides(LimeMozillaDownloadManagerListenerImpl downloadManagerListener) {
        overrideMozillaDefaults(downloadManagerListener);
    }

    private void overrideMozillaDefaults(
            LimeMozillaDownloadManagerListenerImpl mozillaDownloadManagerListener) {
        // lookup the preferences service by contract id.
        // by getting a proxy we do not need to run code through mozilla thread.
        nsIPrefService prefService = XPCOMUtils.getServiceProxy(
                "@mozilla.org/preferences-service;1", nsIPrefService.class);

        // set default downloads to desktop, we are going to override this with
        // our own download manager This will prevent the save dialogue from
        // opening
        prefService.getBranch("browser.download.").setBoolPref("useDownloadDir", 1);
        prefService.getBranch("browser.download.").setIntPref("folderList", 2);
        String downloadDir = MozillaSettings.DOWNLOAD_DIR.getValue().getAbsolutePath();
        prefService.getBranch("browser.download.").setCharPref("dir", downloadDir);
        prefService.getBranch("browser.download.manager.").setBoolPref("showWhenStarting", 0);
        prefService.getBranch("browser.download.manager.").setBoolPref("showAlertOnComplete", 0);

        // setup which mime types do not prompt to download
        // this will prevent the save or open dialogue from prompting
        prefService.getBranch("browser.helperApps.neverAsk.").setCharPref("saveToDisk",
                MozillaSettings.DOWNLOAD_MIME_TYPES.getValue());

        // adding the download listener
        nsIDownloadManager downloadManager = XPCOMUtils.getServiceProxy(NS_IDOWNLOADMANAGER_CID,
                nsIDownloadManager.class);
        downloadManager.addListener(mozillaDownloadManagerListener);
    }
}
