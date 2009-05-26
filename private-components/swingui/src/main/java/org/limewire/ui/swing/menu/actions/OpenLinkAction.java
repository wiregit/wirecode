package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;

import javax.swing.JOptionPane;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MagnetHandler;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * An action to prompt the user to open and download a link. Two link types are
 * supported: magnet links and torrent links.
 */
public class OpenLinkAction extends AbstractAction {

    private final DownloadListManager downloadListManager;

    private final Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler;

    private final MagnetFactory magnetFactory;

    private final Provider<MagnetHandler> magnetHandler;

    @Inject
    public OpenLinkAction(DownloadListManager downloadListManager,
            Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler, MagnetFactory magnetFactory,
            Provider<MagnetHandler> magnetHandler) {
        super(I18n.tr("Open &Link..."));
        this.downloadListManager = downloadListManager;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.magnetFactory = magnetFactory;
        this.magnetHandler = magnetHandler;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Get owner frame.
        // Create dialog.
        final LocationDialog locationDialogue = new LocationDialog();
        locationDialogue.setTitle(I18n.tr("Open Link"));
        locationDialogue.setLocationRelativeTo(GuiUtils.getMainFrame());
        locationDialogue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final URI uri = locationDialogue.getURI();
                if (uri != null) {
                    if (magnetFactory.isMagnetLink(uri)) {
                        MagnetLink[] magnetLinks = magnetFactory.parseMagnetLink(uri);
                        if (magnetLinks.length == 0) {
                            FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), I18n
                                    .tr("Magnet link is empty."), I18n.tr("Open Link"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        }

                        for (final MagnetLink magnet : magnetLinks) {
                            magnetHandler.get().handleMagnet(magnet);
                        }

                    } else {
                        downloadTorrent(downloadListManager, saveLocationExceptionHandler, uri);
                    }
                }
            }

            private void downloadTorrent(final DownloadListManager downloadListManager,
                    final Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler, final URI uri) {
                try {
                    downloadListManager.addTorrentDownload(uri, false);
                } catch (SaveLocationException sle) {
                    saveLocationExceptionHandler.get().handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            downloadListManager.addTorrentDownload(uri,
                                    overwrite);
                        }
                    }, sle, false);
                }
            }
        });

        locationDialogue.setVisible(true);

    }
}