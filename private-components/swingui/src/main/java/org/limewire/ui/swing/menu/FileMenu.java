package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Application;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.player.api.AudioPlayer;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.mainframe.MainPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SimpleNavSelectable;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.CategoryUtils;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.ui.swing.util.SaveLocationExceptionHandlerImpl;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileMenu extends JMenu {
    private final Navigator navigator;

    @Inject
    public FileMenu(DownloadListManager downloadListManager, Navigator navigator,
            LibraryManager libraryManager, final MainPanel mainPanel,
            SaveLocationExceptionHandler saveLocationExceptionHandler, MagnetFactory magnetFactory,
            SearchHandler searchHandler, final AudioPlayer audioPlayer) {
        super(I18n.tr("File"));
        this.navigator = navigator;
        add(buildOpenFileAction(downloadListManager, mainPanel, saveLocationExceptionHandler));
        add(buildOpenLinkAction(downloadListManager, magnetFactory, saveLocationExceptionHandler,
                mainPanel, searchHandler));
        add(getRecentDownloadsMenu(downloadListManager, libraryManager, audioPlayer));
        addSeparator();
        add(getAddFileAction(libraryManager, mainPanel));
        add(getAddFolderAction(libraryManager, mainPanel));
        addSeparator();
        add(new SignInOutAction());
        addSeparator();
        add(new AbstractAction(I18n.tr("E&xit")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Application.getInstance().exit(e);
            }
        });
    }

    private Action getAddFolderAction(final LibraryManager libraryManager, final MainPanel mainPanel) {
        return new AbstractAction(I18n.tr("Add Folder To Library")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> folders = FileChooser.getInput(mainPanel, I18n.tr("Add Folder(s)"), I18n
                        .tr("Add Folder(s)"), FileChooser.getLastInputDirectory(),
                        JFileChooser.DIRECTORIES_ONLY, JFileChooser.APPROVE_OPTION, true,
                        new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                return f.isDirectory();
                            }

                            @Override
                            public String getDescription() {
                                return I18n.tr("All Folders");
                            }
                        });

                if (folders != null) {
                    for (File folder : folders) {
                        libraryManager.getLibraryManagedList().addFolder(folder);
                    }
                }

            }
        };
    }

    private Action getAddFileAction(final LibraryManager libraryManager, final MainPanel mainPanel) {
        return new AbstractAction(I18n.tr("Add File To Library")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> files = FileChooser.getInput(mainPanel, I18n.tr("Add File(s)"), I18n
                        .tr("Add Files"), FileChooser.getLastInputDirectory(),
                        JFileChooser.FILES_ONLY, JFileChooser.APPROVE_OPTION, true,
                        new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                return f.isDirectory()
                                        || libraryManager.getLibraryData().isFileManageable(f);
                            }

                            @Override
                            public String getDescription() {
                                return I18n.tr("Valid Files");
                            }
                        });

                if (files != null) {
                    for (File file : files) {
                        libraryManager.getLibraryManagedList().addFile(file);
                    }
                }
            }
        };
    }

    private JMenu getRecentDownloadsMenu(DownloadListManager downloadListManager,
            final LibraryManager libraryManager, final AudioPlayer audioPlayer) {
        final JMenu recentDownloads = new JMenu(I18n.tr("Recent Downloads"));
        final JMenuItem emptyItem = new JMenuItem(I18n.tr("(empty)"));
        emptyItem.setEnabled(false);
        recentDownloads.add(emptyItem);

        final Action clearMenu = new AbstractAction(I18n.tr("Clear list")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadSettings.RECENT_DOWNLOADS.clear();
                recentDownloads.removeAll();
                recentDownloads.add(emptyItem);
            }
        };

        populateRecentDownloads(recentDownloads, clearMenu, emptyItem, libraryManager, audioPlayer);

        DownloadSettings.RECENT_DOWNLOADS.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        populateRecentDownloads(recentDownloads, clearMenu, emptyItem,
                                libraryManager, audioPlayer);
                    }
                });
            }
        });

        return recentDownloads;
    }

    private void populateRecentDownloads(final JMenu recentDownloads, final Action clearMenu,
            JMenuItem emptyItem, final LibraryManager libraryManager, final AudioPlayer audioPlayer) {
        // TODO do not rebuild whole menu, just add and remove needed components
        recentDownloads.removeAll();
        //DownloadSettings.RECENT_DOWNLOADS.clean();

        Set<File> files = null;
        files = new HashSet<File>(DownloadSettings.RECENT_DOWNLOADS.getValue());
        if (files.size() > 0) {
            for (final File file : files) {
                addRecentDownloadAction(recentDownloads, audioPlayer, file);
            }
            recentDownloads.addSeparator();
            recentDownloads.add(clearMenu);
        } else {
            recentDownloads.add(emptyItem);
        }
    }

    private void addRecentDownloadAction(final JMenu recentDownloads,
            final AudioPlayer audioPlayer, final File file) {
        recentDownloads.add(new AbstractAction(file.getName()) {
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

    private Action buildOpenFileAction(final DownloadListManager downloadListManager,
            final MainPanel mainPanel,
            final SaveLocationExceptionHandler saveLocationExceptionHandler) {
        return new AbstractAction(I18n.tr("&Open File")) {
            @Override
            public void actionPerformed(ActionEvent e) {

                List<File> files = FileChooser.getInput(mainPanel, I18n.tr("Open File"), I18n
                        .tr("Open"), FileChooser.getLastInputDirectory(), JFileChooser.FILES_ONLY,
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
                            DownloadItem item = downloadListManager.addTorrentDownload(file, null,
                                    false);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select(SimpleNavSelectable.create(item));
                        } catch (SaveLocationException sle) {
                            saveLocationExceptionHandler.handleSaveLocationException(
                                    new SaveLocationExceptionHandlerImpl.DownLoadAction() {
                                        @Override
                                        public void download(File saveFile, boolean overwrite)
                                                throws SaveLocationException {
                                            DownloadItem item = downloadListManager
                                                    .addTorrentDownload(file, saveFile, overwrite);
                                            navigator.getNavItem(NavCategory.DOWNLOAD,
                                                    MainDownloadPanel.NAME).select(
                                                    SimpleNavSelectable.create(item));
                                        }
                                    }, sle, false);
                        }
                    }
                }
            }
        };
    }

    private Action buildOpenLinkAction(final DownloadListManager downloadListManager,
            final MagnetFactory magnetFactory,
            final SaveLocationExceptionHandler saveLocationExceptionHandler,
            final MainPanel mainPanel, final SearchHandler searchHandler) {
        return new AbstractAction(I18n.tr("Open &Link")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final LocationDialogue locationDialogue = new LocationDialogue();
                locationDialogue.setLocationRelativeTo(mainPanel);
                locationDialogue.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final URI uri = locationDialogue.getURI();
                        if (uri != null) {
                            if (magnetFactory.isMagnetLink(uri)) {
                                MagnetLink[] magnetLinks = magnetFactory.parseMagnetLink(uri);
                                if (magnetLinks.length == 0) {
                                    throw new UnsupportedOperationException(
                                            "TODO implement user feedback. Error parsing magnet link.");
                                }

                                for (final MagnetLink magnet : magnetLinks) {
                                    if (magnet.isDownloadable()) {
                                        downloadMagnet(downloadListManager,
                                                saveLocationExceptionHandler, mainPanel, magnet);
                                    } else if (magnet.isKeywordTopicOnly()) {
                                        searchHandler.doSearch(DefaultSearchInfo
                                                .createKeywordSearch(magnet.getQueryString(),
                                                        SearchCategory.ALL));
                                    } else {
                                        throw new UnsupportedOperationException(
                                                "TODO implement user feedback.");
                                        // TODO error case?
                                    }
                                }
                            } else {
                                downloadTorrent(downloadListManager, saveLocationExceptionHandler,
                                        mainPanel, uri);
                            }
                        }
                    }

                    private void downloadTorrent(final DownloadListManager downloadListManager,
                            final SaveLocationExceptionHandler saveLocationExceptionHandler,
                            final MainPanel mainPanel, final URI uri) {
                        try {
                            DownloadItem item = downloadListManager.addTorrentDownload(uri, false);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select(SimpleNavSelectable.create(item));
                        } catch (SaveLocationException sle) {
                            saveLocationExceptionHandler.handleSaveLocationException(
                                    new SaveLocationExceptionHandlerImpl.DownLoadAction() {
                                        @Override
                                        public void download(File saveFile, boolean overwrite)
                                                throws SaveLocationException {
                                            DownloadItem item = downloadListManager
                                                    .addTorrentDownload(uri, overwrite);
                                            navigator.getNavItem(NavCategory.DOWNLOAD,
                                                    MainDownloadPanel.NAME).select(
                                                    SimpleNavSelectable.create(item));
                                        }
                                    }, sle, false);
                        }
                    }

                    private void downloadMagnet(final DownloadListManager downloadListManager,
                            final SaveLocationExceptionHandler saveLocationExceptionHandler,
                            final MainPanel mainPanel, final MagnetLink magnet) {
                        try {
                            downloadListManager.addDownload(magnet, null, false);
                        } catch (SaveLocationException e1) {
                            saveLocationExceptionHandler.handleSaveLocationException(
                                    new SaveLocationExceptionHandler.DownLoadAction() {
                                        @Override
                                        public void download(File saveFile, boolean overwrite)
                                                throws SaveLocationException {

                                            DownloadItem item = downloadListManager.addDownload(
                                                    magnet, saveFile, overwrite);
                                            navigator.getNavItem(NavCategory.DOWNLOAD,
                                                    MainDownloadPanel.NAME).select(
                                                    SimpleNavSelectable.create(item));
                                        }
                                    }, e1, true);
                        }
                    }
                });

                locationDialogue.setVisible(true);

            }
        };
    }

    public static class SignInOutAction extends AbstractAction {
        private static final String SIGN_INTO_FRIENDS_TEXT = I18n.tr("&Sign into Friends");

        private static final String SIGN_OUT_OF_FRIENDS_TEXT = I18n.tr("&Sign out of Friends");

        boolean signedIn = false;

        public SignInOutAction() {
            super(SIGN_INTO_FRIENDS_TEXT);
            EventAnnotationProcessor.subscribe(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!signedIn) {
                new DisplayFriendsToggleEvent(true).publish();
            } else {
                new SignoffEvent().publish();
            }
        }

        @EventSubscriber
        public void handleSignon(XMPPConnectionEstablishedEvent event) {
            putValue(Action.NAME, SIGN_OUT_OF_FRIENDS_TEXT);
            signedIn = true;
        }

        @EventSubscriber
        public void handleSignoff(SignoffEvent event) {
            putValue(Action.NAME, SIGN_INTO_FRIENDS_TEXT);
            signedIn = false;
        }
    }
}
