package org.limewire.ui.swing.browser;

import org.limewire.core.impl.mozilla.LimeMozillaDownloadManagerListenerImpl;
import org.limewire.core.settings.MozillaSettings;
import org.limewire.ui.swing.browser.download.LimeMozillaDownloadManager;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIPrefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeMozillaOverrides {

    
    @Inject
   public LimeMozillaOverrides(LimeMozillaDownloadManagerListenerImpl downloadManagerListener) {
        overrideMozillaDefaults(downloadManagerListener);
    }
    
    private void overrideMozillaDefaults(LimeMozillaDownloadManagerListenerImpl mozillaDownloadManagerListener) {
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

        // register our own download manager to replace the one provided by
        // mozilla
        //registerMozillaComponent(mozillaDownloadManager);
        
        nsIDownloadManager downloadManager = XPCOMUtils.getServiceProxy(LimeMozillaDownloadManager.NS_IDOWNLOADMANAGER_CID, nsIDownloadManager.class);
        downloadManager.addListener(mozillaDownloadManagerListener);
    }

//    private void registerMozillaComponent(LimeMozillaSingletonFactory factory) {
//        nsIComponentRegistrar cr = Mozilla.getInstance().getComponentRegistrar();
//        cr.registerFactory(factory.getIID(), factory.getComponentName(), factory.getCID(), factory);
//    }
}
