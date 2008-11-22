/**
 * 
 */
package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.player.api.AudioPlayer;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.CategoryUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

public class RecentDownloadsMenu extends JMenu {
    private final AudioPlayer audioPlayer;

    private final JMenuItem emptyItem;

    private final Action clearMenu;

    public RecentDownloadsMenu(String name, final LibraryManager libraryManager,
            final AudioPlayer audioPlayer) {
        super(name);
        this.audioPlayer = audioPlayer;

        emptyItem = new JMenuItem(I18n.tr("(empty)"));
        emptyItem.setEnabled(false);

        clearMenu = new AbstractAction(I18n.tr("Clear list")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadSettings.RECENT_DOWNLOADS.clear();
            }
        };

        populateRecentDownloads();

        DownloadSettings.RECENT_DOWNLOADS.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        populateRecentDownloads();
                    }
                });
            }
        });
    }

    private void populateRecentDownloads() {
        // TODO how long can this list get?
        removeAll();
        Set<File> files = null;
        synchronized (DownloadSettings.RECENT_DOWNLOADS) {
            files = new HashSet<File>(DownloadSettings.RECENT_DOWNLOADS.getValue());
        }
        if (files.size() > 0) {
            for (final File file : files) {
                addRecentDownloadAction(file);
            }
            addSeparator();
            add(clearMenu);
        } else {
            add(emptyItem);
        }
    }

    private void addRecentDownloadAction(final File file) {
        add(new AbstractAction(file.getName()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                MediaType mediaType = MediaType.getMediaTypeForExtension(FileUtils
                        .getFileExtension(file));
                Category category = CategoryUtils.getCategory(mediaType);

                switch (category) {
                case AUDIO:
                    audioPlayer.stop();
                    audioPlayer.loadSong(file);
                    audioPlayer.playSong();
                    break;
                case DOCUMENT:
                case IMAGE:
                case VIDEO:
                    NativeLaunchUtils.launchFile(file);
                    break;
                case PROGRAM:
                case OTHER:
                    NativeLaunchUtils.launchExplorer(file);
                    break;
                default:
                    break;
                }
            }
        });
    }
}