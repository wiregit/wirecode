/**
 * 
 */
package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.limewire.core.api.download.DownLoadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.mainframe.MainPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SimpleNavSelectable;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.util.FileUtils;

public class OpenFileAction extends AbstractAction {

    private final Navigator navigator;
    
    private final DownloadListManager downloadListManager;

    private final MainPanel mainPanel;

    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    public OpenFileAction(Navigator navigator, String name, DownloadListManager downloadListManager,
            MainPanel mainPanel, SaveLocationExceptionHandler saveLocationExceptionHandler) {
        super(name);
        this.navigator = navigator;
        this.downloadListManager = downloadListManager;
        this.mainPanel = mainPanel;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<File> files = FileChooser.getInput(mainPanel, I18n.tr("Open File"), I18n.tr("Open"),
                FileChooser.getLastInputDirectory(), JFileChooser.FILES_ONLY,
                JFileChooser.APPROVE_OPTION, true, new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        String extension = FileUtils.getFileExtension(f);
                        return f.isDirectory() || "torrent".equalsIgnoreCase(extension);
                    }

                    @Override
                    public String getDescription() {
                        return I18n.tr(".torrent files");
                    }
                });

        if (files != null) {
            for (final File file : files) {
                try {
                    DownloadItem item = downloadListManager.addTorrentDownload(file, null, false);
                    navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select(
                            SimpleNavSelectable.create(item));
                } catch (SaveLocationException sle) {
                    saveLocationExceptionHandler.handleSaveLocationException(new DownLoadAction() {
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
        }
    }
}