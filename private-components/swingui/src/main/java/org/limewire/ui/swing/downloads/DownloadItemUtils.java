package org.limewire.ui.swing.downloads;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class DownloadItemUtils {
    
    private DownloadItemUtils() {}
    
    /**
     * Launches the download, loading the launchable portion in the background
     * if necessary.
     */
    public static void launch(final DownloadItem downloadItem, final CategoryManager categoryManager) {
        assert EventQueue.isDispatchThread();
        assert downloadItem.isLaunchable();
        
        // Warn user that the file has not been scanned for viruses.
        if (downloadItem.getState() == DownloadState.SCAN_FAILED ||
                downloadItem.getState() == DownloadState.SCAN_FAILED_DOWNLOADING_DEFINITIONS) {
            String message = I18n.tr("This file has not been scanned for viruses.  Do you want to launch anyway?");
            int answer = FocusJOptionPane.showConfirmDialog(GuiUtils.getMainFrame(),
                    new MultiLineLabel(message, 400), I18n.tr("Message"), 
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        GuiUtils.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                return downloadItem.getLaunchableFile();
            }
            
            @Override
            protected void done() {
                GuiUtils.getMainFrame().setCursor(Cursor.getDefaultCursor());
                File file;
                try {
                    file = get();
                    if(file != null) {
                        PlayerUtils.playOrLaunch(file, categoryManager);
                    }
                } catch (InterruptedException ignored) {
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }.execute();
    }

}
