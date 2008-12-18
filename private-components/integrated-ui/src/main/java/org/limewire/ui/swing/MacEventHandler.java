package org.limewire.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import net.roydesign.event.ApplicationEvent;
import net.roydesign.mac.MRJAdapter;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.event.AboutDisplayEvent;
import org.limewire.ui.swing.event.ExitApplicationEvent;
import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.event.RestoreViewEvent;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SimpleNavSelectable;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.browser.ExternalControl;

/**
 * This class handles Macintosh specific events. The handled events include the
 * selection of the "About" option in the Mac file menu, the selection of the
 * "Quit" option from the Mac file menu, and the dropping of a file on LimeWire
 * on the Mac, which LimeWire would be expected to handle in some way.
 */
@Singleton
public class MacEventHandler {

    private volatile File lastFileOpened = null;

    private volatile boolean enabled;

    private volatile ExternalControl externalControl = null;

    private volatile Initializer initializer = null;

    private volatile LifeCycleManager lifecycleManager = null;

    private volatile Navigator navigator;

    private volatile DownloadListManager downloadListManager;

    private volatile SaveLocationExceptionHandler saveLocationExceptionHandler;

    /** Creates a new instance of MacEventHandler */
    @Inject
    private MacEventHandler(ExternalControl externalControl, Initializer initializer,
            DownloadListManager downloadListManager, LifeCycleManager lifecycleManager,
            Navigator navigator, SaveLocationExceptionHandler saveLocationExceptionHandler) {

        this.externalControl = externalControl;
        this.initializer = initializer;
        this.enabled = true;
        this.downloadListManager = downloadListManager;
        this.lifecycleManager = lifecycleManager;
        this.navigator = navigator;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        
        MRJAdapter.addAboutListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                new AboutDisplayEvent().publish();
            }
        });

        MRJAdapter.addQuitApplicationListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                handleQuit();
            }
        });

        MRJAdapter.addOpenDocumentListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                File file = ((ApplicationEvent) evt).getFile();
                handleOpenFile(file);
            }
        });

        MRJAdapter.addReopenApplicationListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                handleReopen();
            }
        });
    }

    public void runExternalChecks() {
        if (lastFileOpened != null) {
            runFileOpen(lastFileOpened);
        }
    }

    /**
     * Enable preferences.
     */
    public void enablePreferences() {
        MRJAdapter.setPreferencesEnabled(true);

        MRJAdapter.addPreferencesListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                new OptionsDisplayEvent().publish();
            }
        });
    }

    private void handleQuit() {
        new ExitApplicationEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Shutdown"))
                .publish();
    }

    /**
     * This method handles a request to open the specified file.
     */
    private void handleOpenFile(File file) {
        if (!enabled) {
            lastFileOpened = file;
        } else {
            runFileOpen(file);
        }
    }

    private void runFileOpen(final File file) {
        String filename = file.getPath();
        if (filename.endsWith("limestart")) {
            initializer.setStartup();
        } else if (filename.endsWith("torrent")) {
            if (!lifecycleManager.isStarted()) {
                externalControl.enqueueControlRequest(file.getAbsolutePath());
            } else {
                try {
                    DownloadItem item = downloadListManager.addTorrentDownload(file, null, false);
                    navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select(
                            SimpleNavSelectable.create(item));
                } catch (SaveLocationException sle) {
                    saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            DownloadItem item = downloadListManager.addTorrentDownload(file,
                                    saveFile, overwrite);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select(SimpleNavSelectable.create(item));
                        }
                    }, sle, false);
                }
            }
        } else {
            NativeLaunchUtils.safeLaunchFile(file);
        }
    }

    private void handleReopen() {
        new RestoreViewEvent().publish();
    }
}
