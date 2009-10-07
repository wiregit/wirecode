package org.limewire.ui.swing;

import java.io.File;

import javax.swing.Action;
import javax.swing.ActionMap;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.ui.swing.mainframe.AboutAction;
import org.limewire.ui.swing.mainframe.OptionsAction;
import org.limewire.ui.swing.menu.ExitAction;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class handles Macintosh specific events. The handled events include the
 * selection of the "About" option in the Mac file menu, the selection of the
 * "Quit" option from the Mac file menu, and the dropping of a file on LimeWire
 * on the Mac, which LimeWire would be expected to handle in some way.
 */
public class MacEventHandler {

    private static MacEventHandler INSTANCE;

    public static synchronized MacEventHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new MacEventHandler();
        }
        return INSTANCE;
    }

    private volatile File lastFileOpened = null;
    private volatile boolean enabled;

    @Inject private volatile ExternalControl externalControl = null;
    @Inject private volatile DownloadManager downloadManager = null;
    @Inject private volatile LifeCycleManager lifecycleManager = null;
    @Inject private volatile ActivityCallback activityCallback = null;
    @Inject private volatile AboutAction aboutAction = null;
    @Inject private volatile OptionsAction optionsAction = null;
    @Inject private volatile ExitAction exitAction = null;
    @Inject private volatile CategoryManager categoryManager = null;

    /** Creates a new instance of MacEventHandler */
    @Inject
    public MacEventHandler() {
        assert ( OSUtils.isMacOSX() ) : "MacEventHandler should only be used on Mac OS-X operating systems.";
        
        try {
            OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[])null));
            OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[])null));
            OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[])null));
            OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("openFile", new Class[] { String.class }));
            OSXAdapter.setReOpenApplicationHandler(this, getClass().getDeclaredMethod("reOpenApplication", (Class[])null));
            
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    @Inject
    public void startup() {
        this.enabled = true;
        
        if (lastFileOpened != null) {
            runFileOpen(lastFileOpened);
        }
    }

    public void preferences() {
        optionsAction.actionPerformed(null);
    }
    
    public void about() {
        aboutAction.actionPerformed(null);
    }
    
    public boolean quit() {
        exitAction.actionPerformed(null);
        
        return true;
    }
  
    public void reOpenApplication() {
        ActionMap map = org.jdesktop.application.Application.getInstance().getContext().getActionManager().getActionMap();
        Action action = map.get("restoreView");
        if (action != null) {
            action.actionPerformed(null);
        }
    }
    
    public void openFile(String filename) {
        File file = new File(filename);
        if (!enabled) {
            lastFileOpened = file;
        } else {
            runFileOpen(file);
        }        
    }

    private void runFileOpen(final File file) {
        String filename = file.getPath();
        if (filename.endsWith("limestart")) {
            LimeWireUtils.setAutoStartupLaunch(true);
        } else if (filename.endsWith("torrent")) {
            if (!lifecycleManager.isStarted()) {
                externalControl.enqueueControlRequest(file.getAbsolutePath());
            } else {
                try {
                    downloadManager.downloadTorrent(file, null, false);
                } catch (DownloadException e) {
                    activityCallback.handleDownloadException(new DownloadAction() {
                        @Override
                        public void download(File saveDirectory, boolean overwrite)
                                throws DownloadException {
                            downloadManager.downloadTorrent(file, saveDirectory, overwrite);
                        }

                        @Override
                        public void downloadCanceled(DownloadException ignored) {
                            //nothing to do
                        }

                    }, e, false);

                }
            }
        } else {
            NativeLaunchUtils.safeLaunchFile(file, categoryManager);
        }
    }
}
